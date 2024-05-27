package it.gov.acn;


import it.gov.acn.model.Constituency;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.repository.MockKafkaBrokerRepository;
import it.gov.acn.service.ConstituencyService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true",
    "acn.outbox.scheduler.fixed-delay=3000",
    "acn.outbox.scheduler.backoff-base=45"
})
@ExtendWith(MockitoExtension.class)
public class OutboxSchedulerIntegrationTest extends PostgresTestContext{
  private final long fixedDelay = 3000;
  private final long backoffBase = 45;
  @Autowired
  private ConstituencyService constituencyService;

  @SpyBean
  private MockKafkaBrokerRepository mockKafkaBrokerRepository;

  @Autowired
  private DataProvider dataProvider;

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
  void when_saveConstituency_then_scheduler_fails_db_exception_succeeds_second_time(){
    Mockito.doThrow(new RuntimeException("DB test exception")).when(mockKafkaBrokerRepository).save(Mockito.any());

    Instant now = Instant.now();
    Constituency constituency = TestUtils.createTestConstituency();
    constituencyService.saveConstituency(constituency);

    Awaitility.await()
        .atMost((fixedDelay*2)+500, TimeUnit.MILLISECONDS)
        .untilAsserted(() ->
            Mockito.verify(mockKafkaBrokerRepository, Mockito.times(1)).save(Mockito.any())
        );

    Assertions.assertEquals(0, findCompletedOutboxItems().size());
    List<OutboxItem> notCompletedOutboxItems = findNotCompletedOutboxItems();
    Assertions.assertEquals(1, notCompletedOutboxItems.size());
    Assertions.assertEquals("DB test exception", notCompletedOutboxItems.get(0).getLastError());
    Assertions.assertNotNull(notCompletedOutboxItems.get(0).getLastAttemptDate());
    Assertions.assertTrue(notCompletedOutboxItems.get(0).getLastAttemptDate().isAfter(now));
    Assertions.assertNull(notCompletedOutboxItems.get(0).getCompletionDate());

    Mockito.reset(mockKafkaBrokerRepository);

    Awaitility.await()
        .atMost(calculateBackoff(notCompletedOutboxItems.get(0))
            .plus(1, ChronoUnit.SECONDS)
            .plus(fixedDelay, ChronoUnit.MILLIS))
        .untilAsserted(() ->
            Mockito.verify(mockKafkaBrokerRepository, Mockito.times(2)).save(Mockito.any())
        );

    Assertions.assertEquals(0, findNotCompletedOutboxItems().size());
    List<OutboxItem> completedOutboxItems = findCompletedOutboxItems();
    Assertions.assertEquals(1, completedOutboxItems.size());
    Assertions.assertEquals("DB test exception", completedOutboxItems.get(0).getLastError());
    Assertions.assertNotNull(completedOutboxItems.get(0).getLastAttemptDate());
    Assertions.assertNotNull(completedOutboxItems.get(0).getCompletionDate());
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
    return Duration.ofSeconds((long)
        Math.pow(backoffBase, item.getAttempts()));
  }

}
