package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;

public class OutboxProcessorFactory {

  public static OutboxProcessor createOutboxProcessor(OutboxConfiguration outboxConfiguration) {
    return new OutboxProcessor(
        outboxConfiguration.getBackoffBase(),
        outboxConfiguration.getMaxAttempts(),
        outboxConfiguration.getDataProvider(),
        outboxConfiguration.getOutboxItemHandlerProvider(),
        outboxConfiguration.getLockingProvider()
    );
  }

}
