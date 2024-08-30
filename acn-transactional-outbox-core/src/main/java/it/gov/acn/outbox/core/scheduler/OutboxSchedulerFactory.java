package it.gov.acn.outbox.core.scheduler;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;

public class OutboxSchedulerFactory {

  public static OutboxScheduler createOutboxScheduler(
      OutboxConfiguration outboxConfiguration,
      OutboxProcessor outboxProcessor
  ) {
    return new OutboxScheduler(
        outboxConfiguration.getSchedulingProvider(),
        outboxConfiguration.getFixedDelay(),
        outboxProcessor
    );
  }
}
