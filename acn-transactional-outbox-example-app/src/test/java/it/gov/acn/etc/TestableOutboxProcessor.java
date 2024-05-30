package it.gov.acn.etc;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;

public class TestableOutboxProcessor extends OutboxProcessor {
  public TestableOutboxProcessor(
      OutboxConfiguration outboxConfiguration) {
    super(outboxConfiguration);
  }

  public void doProcess() {
    super.doProcess();
  }


}
