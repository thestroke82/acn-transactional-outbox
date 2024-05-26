package it.gov.acn.outbox.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyOutboxManager implements OutboxManager{
    private final Logger logger = LoggerFactory.getLogger(DummyOutboxManager.class);
    @Override
    public void recordEvent(Object event, String type) {
        logger.debug("[DummyOutboxManager.recordEvent] Event to save: "+event.toString());
    }
}
