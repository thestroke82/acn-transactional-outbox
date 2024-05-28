package it.gov.acn;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.acn.autoconfigure.outbox.config.DefaultConfiguration;
import it.gov.acn.integration.KafkaTemplate;
import it.gov.acn.model.Constituency;
import it.gov.acn.outbox.core.processor.OutboxProcessor;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.scheduler.OutboxScheduler;
import it.gov.acn.repository.ConstituencyRepository;
import it.gov.acn.repository.MockKafkaBrokerRepository;
import it.gov.acn.service.ConstituencyService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=3000",
    "acn.outbox.scheduler.backoff-base=1"
})
@ExtendWith(MockitoExtension.class)
public class OutboxSchedulerIntegrationTest extends PostgresTestContext{
  private final long fixedDelay = 3000;
  private final int backoffBase = 1;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  @Autowired
  private ConstituencyService constituencyService;
  @Autowired
  private ConstituencyRepository constituencyRepository;
  @Autowired
  private JdbcTemplate jdbcTemplate;

  @SpyBean
  private MockKafkaBrokerRepository mockKafkaBrokerRepository;
  
  @SpyBean
  private KafkaTemplate kafkaTemplate;

  @SpyBean
  private OutboxScheduler outboxScheduler;

  @SpyBean
  private DataProvider dataProvider;


  @AfterEach
  public void afterEach() {
    jdbcTemplate.execute("TRUNCATE TABLE "+ DefaultConfiguration.TABLE_NAME);
    mockKafkaBrokerRepository.deleteAll();
    constituencyRepository.deleteAll();
  }

  @Test
  void when_saveConstituency_then_scheduler_happy_path(){
    Constituency constituency = TestUtils.createTestConstituency();
    constituencyService.saveConstituency(constituency);    Awaitility.await()

        .atMost(fixedDelay+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(mockKafkaBrokerRepository, Mockito.times(1)).save(Mockito.any())
        );
  }

  @Test
  void when_process_outbox_throws_exception_then_scheduling_continues(){

    // simulate the .process() method rethrowing a runtime exception
    Mockito.doThrow(new RuntimeException("Test exception")).when(dataProvider)
        .find(Mockito.anyBoolean(), Mockito.anyInt());

    Constituency constituency = TestUtils.createTestConstituency();
    constituencyService.saveConstituency(constituency);

    Awaitility.await()
        .atMost(fixedDelay+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(dataProvider, Mockito.times(1)).find(Mockito.anyBoolean(), Mockito.anyInt(), Mockito.any())
        );

    Mockito.reset(dataProvider);

    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    OutboxItem outboxItem = notCompletedOutboxItems.get(0);

    Awaitility.await()
        .atMost(calculateBackoff(outboxItem)
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Assertions.assertEquals(1, findCompletedOutboxItems().size())
        );

    Assertions.assertEquals(0, findNotCompletedOutboxItems().size());

  }

  @Test
  void when_saveConstituency_then_scheduler_fails_db_exception_succeeds_second_time()
      throws InterruptedException {
    Mockito.doThrow(new RuntimeException("Kafka Exception")).when(kafkaTemplate).send(Mockito.any());

    Instant now = Instant.now();
    Constituency constituency = TestUtils.createTestConstituency();
    constituencyService.saveConstituency(constituency);

    Awaitility.await()
        .atMost(fixedDelay+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(1)).send(Mockito.any())
        );

    Thread.sleep(500);
    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    Assertions.assertEquals("Kafka Exception", notCompletedOutboxItems.get(0).getLastError());
    Assertions.assertNotNull(notCompletedOutboxItems.get(0).getLastAttemptDate());
    Assertions.assertTrue(notCompletedOutboxItems.get(0).getLastAttemptDate().isAfter(now));
    Assertions.assertNull(notCompletedOutboxItems.get(0).getCompletionDate());

    Mockito.reset(kafkaTemplate);

    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0))
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Assertions.assertEquals(0, findNotCompletedOutboxItems().size())
        );

    List<OutboxItem> completedOutboxItems = findCompletedOutboxItems();
    Assertions.assertEquals(1, completedOutboxItems.size());
    Assertions.assertEquals("Kafka Exception", completedOutboxItems.get(0).getLastError());
    Assertions.assertNotNull(completedOutboxItems.get(0).getLastAttemptDate());
    Assertions.assertNotNull(completedOutboxItems.get(0).getCompletionDate());
  }

  @Test
  void when_saveConstituency_then_scheduler_fails_3_times_outbox_item_not_picked_up_ever_again()
      throws InterruptedException {
    Mockito.doThrow(new RuntimeException("Kafka Exception")).when(kafkaTemplate).send(Mockito.any());

    Instant now = Instant.now();
    Constituency constituency = TestUtils.createTestConstituency();
    constituencyService.saveConstituency(constituency);

    Awaitility.await()
        .atMost(fixedDelay+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(1)).send(Mockito.any())
        );

    Thread.sleep(500);

    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    OutboxItem outboxItem = notCompletedOutboxItems.get(0);
    Assertions.assertEquals("Kafka Exception", outboxItem.getLastError());
    Assertions.assertNotNull(outboxItem.getLastAttemptDate());
    Assertions.assertNull(outboxItem.getCompletionDate());

    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0))
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(2)).send(Mockito.any())
        );

    Thread.sleep(500);

    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    outboxItem = notCompletedOutboxItems.get(0);
    Assertions.assertEquals("Kafka Exception", outboxItem.getLastError());
    Assertions.assertNotNull(outboxItem.getLastAttemptDate());
    Assertions.assertNull(outboxItem.getCompletionDate());


    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0))
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(3)).send(Mockito.any())
        );

    Thread.sleep(500);

    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    outboxItem = notCompletedOutboxItems.get(0);
    Assertions.assertEquals("Kafka Exception", outboxItem.getLastError());
    Assertions.assertNotNull(outboxItem.getLastAttemptDate());
    Assertions.assertNull(outboxItem.getCompletionDate());

    Mockito.reset(kafkaTemplate);

    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0)).multipliedBy(2))
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(0)).send(Mockito.any())
        );


  }

  private List<OutboxItem> findAllOutboxItems(){
    List<OutboxItem> ret = findCompletedOutboxItems();
    ret.addAll(findNotCompletedOutboxItems());
    return ret;
  }
  private List<OutboxItem> findCompletedOutboxItems(){
    return this.dataProvider.find(true, Integer.MAX_VALUE);
  }
  private List<OutboxItem> findNotCompletedOutboxItems(){
    return this.dataProvider.find(false, Integer.MAX_VALUE);
  }

  private Duration calculateBackoff(OutboxItem item) {
    return Duration.ofMinutes((long)
        Math.pow(backoffBase, item.getAttempts()));
  }

  private String outboxItemToString(OutboxItem item){
    if(item==null){
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss:SSS");
    return "OutboxItem{" +
        "id=" + item.getId() +
        ", eventType='" + item.getEventType() + '\'' +
        ", creationDate=" + (item.getCreationDate()!=null? getFormattedInstant(item.getCreationDate()): "null") +
        ", lastAttemptDate=" + (item.getLastAttemptDate()!=null? getFormattedInstant(item.getLastAttemptDate()): "null") +
        ", completionDate=" + (item.getCompletionDate()!=null? getFormattedInstant(item.getCompletionDate()): "null") +
        ", attempts=" + item.getAttempts() +
        '}';
  }

  private String getFormattedInstant(Instant instant){
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        .withZone(ZoneId.of("Europe/Rome"));
    return formatter.format(instant);
  }

}
