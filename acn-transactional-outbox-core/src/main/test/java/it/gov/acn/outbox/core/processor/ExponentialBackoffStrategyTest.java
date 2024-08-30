package it.gov.acn.outbox.core.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.model.OutboxItem;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ExponentialBackoffStrategyTest {

  @Mock
  private OutboxConfiguration outboxConfiguration;

  private ExponentialBackoffStrategy exponentialBackoffStrategy;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    when(outboxConfiguration.getBackoffBase()).thenReturn(2);
    exponentialBackoffStrategy = new ExponentialBackoffStrategy(outboxConfiguration.getBackoffBase());
  }

  @Test
  public void given_no_outstanding_items_when_execute_then_return_empty() {
    List<OutboxItem> result = exponentialBackoffStrategy.filter(Collections.emptyList());
    assertTrue(result.isEmpty());
  }

  @Test
  public void given_eligible_outstanding_items_when_execute_then_return_item() {
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(0);
    outboxItem.setLastAttemptDate(Instant.now().minusSeconds(10));
    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(1, result.size());
  }

  @Test
  public void given_ineligible_outstanding_items_when_execute_then_return_empty() {
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(1);
    outboxItem.setLastAttemptDate(Instant.now());
    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertTrue(result.isEmpty());
  }

  @Test
  public void given_1attempt_backoff_elapsed_when_execute_then_return_item() {
    int backoffBase = 2;
    Mockito.when(outboxConfiguration.getBackoffBase()).thenReturn(backoffBase);
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(1);
    outboxItem.setLastAttemptDate(
        Instant.now().minus(backoffBase, ChronoUnit.MINUTES)
            .minus(1, ChronoUnit.SECONDS));

    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(1, result.size());
  }

  @Test
  public void given_1attempt_backoff_not_elapsed_when_execute_then_return_no_items() {
    int backoffBase = 2;
    Mockito.when(outboxConfiguration.getBackoffBase()).thenReturn(backoffBase);
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(1);

    outboxItem.setLastAttemptDate(
        Instant.now().minus(backoffBase, ChronoUnit.MINUTES)
            .plus(10, ChronoUnit.SECONDS));

    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(0, result.size());
  }

  @Test
  public void given_2attempts_backoff_elapsed_when_execute_then_return_item() {
    int backoffBase = 2;
    Mockito.when(outboxConfiguration.getBackoffBase()).thenReturn(backoffBase);
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(2);
    outboxItem.setLastAttemptDate(
        Instant.now().minus(calculateBackoff(outboxItem.getAttempts(), backoffBase), ChronoUnit.MINUTES)
            .minus(1, ChronoUnit.SECONDS));

    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(1, result.size());
  }

  @Test
  public void given_2attempts_backoff_not_elapsed_when_execute_then_return_no_items() {
    int backoffBase = 2;
    Mockito.when(outboxConfiguration.getBackoffBase()).thenReturn(backoffBase);
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(2);
    outboxItem.setLastAttemptDate(
        Instant.now().minus(calculateBackoff(outboxItem.getAttempts(), backoffBase), ChronoUnit.MINUTES)
            .plus(10, ChronoUnit.SECONDS));

    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(0, result.size());
  }

  @Test
  public void given_null_list_when_execute_then_return_null() {
    assertNull(exponentialBackoffStrategy.filter((List<OutboxItem>) null));
  }

  @Test
  public void given_null_lastAttemptDate_when_execute_then_return_item() {
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(1);
    outboxItem.setLastAttemptDate(null);
    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(1, result.size());
  }

  @Test
  public void given_negative_attempts_when_execute_then_return_item() {
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(-1);
    outboxItem.setLastAttemptDate(Instant.now().minusSeconds(10));
    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertEquals(1, result.size());
  }

  @Test
  public void given_future_lastAttemptDate_when_execute_then_return_empty() {
    OutboxItem outboxItem = new OutboxItem();
    outboxItem.setAttempts(1);
    outboxItem.setLastAttemptDate(Instant.now().plusSeconds(10));
    List<OutboxItem> result = exponentialBackoffStrategy.filter(List.of(outboxItem));
    assertTrue(result.isEmpty());
  }


  private int calculateBackoff(int attempts, int backoffBase) {
    return (int) Math.pow(backoffBase, attempts);
  }
}