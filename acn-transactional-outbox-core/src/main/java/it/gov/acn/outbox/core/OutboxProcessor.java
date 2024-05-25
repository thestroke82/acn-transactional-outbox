package it.gov.acn.outbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxProcessor {
    private Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);

    private OutboxConfiguration outboxConfiguration;

    public OutboxProcessor(OutboxConfiguration outboxConfiguration) {
        this.outboxConfiguration = outboxConfiguration;
    }

    public void process(){
        logger.debug("Processing outbox messages...");
    }

}
