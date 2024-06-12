package it.gov.acn.autoconfigure.outbox.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import it.gov.acn.outbox.core.observability.Observer;
import it.gov.acn.outbox.core.observability.OutboxMetricsCollector;

public class OutboxPrometheusExposer implements Observer {
  private final OutboxMetricsCollector collector;

  public OutboxPrometheusExposer(MeterRegistry meterRegistry) {
    collector = OutboxMetricsCollector.getInstance();
    collector.addObserver(this);
    initialize(meterRegistry);
  }

  private Counter queued;
  private Counter successes;
  private Counter failures;
  private Counter dlq;

  public void initialize(MeterRegistry meterRegistry) {
    this.queued = Counter.builder("outbox.queued")
        .description("Number of queued events")
        .register(meterRegistry);

    this.successes = Counter.builder("outbox.successes")
        .description("Number of events successfully processed")
        .register(meterRegistry);

    this.failures = Counter.builder("outbox.failures")
        .description("Number of events failed to be processed")
        .register(meterRegistry);

    this.dlq = Counter.builder("outbox.dlq")
        .description("Number of events in Dead Letter Queue (DLQ)")
        .register(meterRegistry);
  }

  @Override
  public void update() {
    this.queued.increment(collector.getQueued() - this.queued.count());
    this.successes.increment(collector.getSuccesses() - this.successes.count());
    this.failures.increment(collector.getFailures() - this.failures.count());
    this.dlq.increment(collector.getDlq() - this.dlq.count());
  }
}
