package it.gov.acn.outbox.core.observability;

import java.time.Instant;

public class OutboxMetricsCollector extends Observable{
  private static final OutboxMetricsCollector instance;

  private Instant observationStart;
  private long queued;
  private long successes;
  private long failures;
  private long dlq;

  private OutboxMetricsCollector() {}

  static {
    instance = new OutboxMetricsCollector();
    initialize();
  }
  public static OutboxMetricsCollector getInstance() {
    return instance;
  }

  public void incrementQueued() {
    this.queued = this.safeIncrement(this.queued);
    super.notifyObservers();
  }
  public void incrementSuccesses() {
    this.successes = this.safeIncrement(this.successes);
    super.notifyObservers();
  }
  public void incrementFailures() {
    this.failures = this.safeIncrement(this.failures);
    super.notifyObservers();
  }
  public void incrementDlq() {
    this.dlq = this.safeIncrement(this.dlq);
    super.notifyObservers();
  }

  public Instant getObservationStart() {
    return observationStart;
  }

  public long getQueued() {
    return queued;
  }

  public long getSuccesses() {
    return successes;
  }

  public long getFailures() {
    return failures;
  }

  public long getDlq() {
    return dlq;
  }

  private static void initialize() {
    instance.queued = 0;
    instance.successes = 0;
    instance.failures = 0;
    instance.dlq = 0;
    instance.observationStart = Instant.now();
  }

  private long safeIncrement(long a) {
    if (a == Long.MAX_VALUE) {
      throw new ArithmeticException("Long overflow: " + a + " + 1");
    }
    return a + 1;
  }
}
