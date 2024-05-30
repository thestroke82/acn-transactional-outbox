package it.gov.acn;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.acn.autoconfigure.outbox.config.DefaultConfiguration;
import it.gov.acn.etc.TestUtils;
import it.gov.acn.integration.KafkaTemplate;
import it.gov.acn.model.Constituency;
import it.gov.acn.model.ConstituencyCreatedEvent;
import it.gov.acn.model.MockKafkaBrokerMessage;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=3000",
    "acn.outbox.scheduler.backoff-base=1",
    "logging.level.it.gov.acn=TRACE"
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

  @Test
  void given_kafka_down_when_save_multiple_constituency_then_kafka_up_again_messages_are_delivered_in_order()
      throws InterruptedException, JsonProcessingException {
    Mockito.doThrow(new RuntimeException("Kafka Exception")).when(kafkaTemplate).send(Mockito.any());

    Instant now = Instant.now();
    Constituency constituency1 = TestUtils.createTestConstituency();
    Constituency constituency2 = TestUtils.createTestConstituency();
    Constituency constituency3 = TestUtils.createTestConstituency();

    constituencyService.saveConstituency(constituency1);
    Thread.sleep(100);
    constituencyService.saveConstituency(constituency2);
    Thread.sleep(100);
    constituencyService.saveConstituency(constituency3);

    Awaitility.await()
        .atMost(fixedDelay+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(3)).send(Mockito.any())
        );


    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(3, notCompletedOutboxItems.size());
    Assertions.assertEquals("Kafka Exception", notCompletedOutboxItems.get(0).getLastError());
    Assertions.assertEquals("Kafka Exception", notCompletedOutboxItems.get(1).getLastError());
    Assertions.assertEquals("Kafka Exception", notCompletedOutboxItems.get(2).getLastError());

    Mockito.reset(kafkaTemplate);

    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(2))
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(3)).send(Mockito.any())
        );

    Thread.sleep(500);
    Assertions.assertEquals(0, findNotCompletedOutboxItems().size());
    List<OutboxItem> completedOutboxItems = findCompletedOutboxItems();
    Assertions.assertEquals(3, completedOutboxItems.size());

    List<MockKafkaBrokerMessage> messages = this.oderByOriginalEventCreationDate(
        StreamSupport.stream(
            this.mockKafkaBrokerRepository.findAll().spliterator(),
            false
        ).toList()
    );

    ConstituencyCreatedEvent event = objectMapper.readValue(messages.get(0).getPayload()
        , ConstituencyCreatedEvent.class);
    Assertions.assertEquals("ConstituencyCreatedEvent", event.getEventType());
    Assertions.assertEquals(constituency1.getId(), event.getPayload().getId());

    event = objectMapper.readValue(messages.get(1).getPayload()
        , ConstituencyCreatedEvent.class);
    Assertions.assertEquals("ConstituencyCreatedEvent", event.getEventType());
    Assertions.assertEquals(constituency2.getId(), event.getPayload().getId());

    event = objectMapper.readValue(messages.get(2).getPayload()
        , ConstituencyCreatedEvent.class);
    Assertions.assertEquals("ConstituencyCreatedEvent", event.getEventType());
    Assertions.assertEquals(constituency3.getId(), event.getPayload().getId());

  }

  @Test
  void given_multiple_constituencies_when_kafka_fails_randomly_then_events_are_received_in_order() throws InterruptedException, JsonProcessingException {

    AtomicInteger failures = new AtomicInteger();

    Mockito.doAnswer(new Answer<String>() {
      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        if (new Random().nextBoolean()) {
          failures.incrementAndGet();
          throw new RuntimeException("Kafka Exception");
        } else {
          invocation.callRealMethod();
        }
        return null;
      }
    }).when(kafkaTemplate).send(Mockito.any());

    // Create and save multiple constituencies
    Constituency constituency1 = TestUtils.createTestConstituency();
    Constituency constituency2 = TestUtils.createTestConstituency();
    Constituency constituency3 = TestUtils.createTestConstituency();
    Constituency constituency4 = TestUtils.createTestConstituency();
    Constituency constituency5 = TestUtils.createTestConstituency();

    constituencyService.saveConstituency(constituency1);
    constituencyService.saveConstituency(constituency2);
    constituencyService.saveConstituency(constituency3);
    constituencyService.saveConstituency(constituency4);
    constituencyService.saveConstituency(constituency5);

    // Use Awaitility to wait the fixed delay plus a little extra time
    Awaitility.await()
        .atMost(fixedDelay+2000, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(5)).send(Mockito.any())
        );

    // Retrieve all the messages from the mock Kafka broker repository, the failed ones must be missing
    List<MockKafkaBrokerMessage> messages = this.oderByOriginalEventCreationDate(
        StreamSupport.stream(
            this.mockKafkaBrokerRepository.findAll().spliterator(),
            false
        ).toList()
    );

    Assertions.assertEquals(5-failures.get(), messages.size());

    // Reset the mock to simulate a successful send
    Mockito.reset(kafkaTemplate);

    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();

    // Use Awaitility to wait the next run of the scheduler
    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0))
            .plus(5, ChronoUnit.SECONDS)
        )
        .untilAsserted(() ->
            Mockito.verify(kafkaTemplate, Mockito.times(failures.get())).send(Mockito.any())
        );

    Thread.sleep(1000);

    messages = this.oderByOriginalEventCreationDate(
        StreamSupport.stream(
            this.mockKafkaBrokerRepository.findAll().spliterator(),
            false
        ).toList()
    );

    // Assert that the messages are in the correct order
    ConstituencyCreatedEvent event1 = objectMapper.readValue(messages.get(0).getPayload(), ConstituencyCreatedEvent.class);
    ConstituencyCreatedEvent event2 = objectMapper.readValue(messages.get(1).getPayload(), ConstituencyCreatedEvent.class);
    ConstituencyCreatedEvent event3 = objectMapper.readValue(messages.get(2).getPayload(), ConstituencyCreatedEvent.class);
    ConstituencyCreatedEvent event4 = objectMapper.readValue(messages.get(3).getPayload(), ConstituencyCreatedEvent.class);
    ConstituencyCreatedEvent event5 = objectMapper.readValue(messages.get(4).getPayload(), ConstituencyCreatedEvent.class);

    Assertions.assertEquals(constituency1.getId(), event1.getPayload().getId());
    Assertions.assertEquals(constituency2.getId(), event2.getPayload().getId());
    Assertions.assertEquals(constituency3.getId(), event3.getPayload().getId());
    Assertions.assertEquals(constituency4.getId(), event4.getPayload().getId());
    Assertions.assertEquals(constituency5.getId(), event5.getPayload().getId());
  }


  private List<MockKafkaBrokerMessage> oderByOriginalEventCreationDate(List<MockKafkaBrokerMessage> messages){
    List<MockKafkaBrokerMessage> mutable = new ArrayList<>(messages);
    mutable.sort((a,b)->{
      try {
        ConstituencyCreatedEvent eventA = objectMapper.readValue(a.getPayload(), ConstituencyCreatedEvent.class);
        ConstituencyCreatedEvent eventB = objectMapper.readValue(b.getPayload(), ConstituencyCreatedEvent.class);
        return eventA.getTimestamp().compareTo(eventB.getTimestamp());
      } catch (JsonProcessingException e) {
        e.printStackTrace();
        return 0;
      }
    });
    return mutable;
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
