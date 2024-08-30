package it.gov.acn.outbox.core.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OutboxMetricsCollectorTest {

  private OutboxMetricsCollector collector;

  @BeforeEach
  public void setUp() {
    collector = OutboxMetricsCollector.getInstance();
    // Reset collector state
    collector.reset();
  }

  @Test
  public void test_increment_queued() {
    long initial = collector.getQueued();
    collector.incrementQueued();
    assertEquals(initial + 1, collector.getQueued());
    assertEquals(0, collector.getSuccesses());
    assertEquals(0, collector.getDlq());
    assertEquals(0, collector.getFailures());
  }

  @Test
  public void test_increment_successes() {
    long initial = collector.getSuccesses();
    collector.incrementSuccesses();
    assertEquals(initial + 1, collector.getSuccesses());
    assertEquals(0, collector.getQueued());
    assertEquals(0, collector.getDlq());
    assertEquals(0, collector.getFailures());
  }

  @Test
  public void test_increment_failures() {
    long initial = collector.getFailures();
    collector.incrementFailures();
    assertEquals(initial + 1, collector.getFailures());
    assertEquals(0, collector.getQueued());
    assertEquals(0, collector.getDlq());
    assertEquals(0, collector.getSuccesses());
  }

  @Test
  public void test_increment_dlq() {
    long initial = collector.getDlq();
    collector.incrementDlq();
    assertEquals(initial + 1, collector.getDlq());
    assertEquals(0, collector.getQueued());
    assertEquals(0, collector.getFailures());
    assertEquals(0, collector.getSuccesses());
  }

  @Test
  public void test_observation_start() throws InterruptedException {
    Thread.sleep(500);
    assertTrue(collector.getObservationStart().isBefore(Instant.now()));
  }

  @Test
  public void test_observer_pattern() {
    final int[] updated = {0};
    Observer mockObserver = new Observer() {
      @Override
      public void update() {
        updated[0]++;
      }
    };
    collector.addObserver(mockObserver);
    collector.incrementQueued();
    assertEquals(1, updated[0]);
    collector.incrementSuccesses();
    collector.incrementSuccesses();
    collector.incrementDlq();
    assertEquals(4, updated[0]);
  }

}
