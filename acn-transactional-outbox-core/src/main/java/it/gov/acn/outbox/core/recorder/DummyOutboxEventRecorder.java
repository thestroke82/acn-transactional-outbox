package it.gov.acn.outbox.core.recorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyOutboxEventRecorder implements OutboxEventRecorder {
    private final Logger logger = LoggerFactory.getLogger(DummyOutboxEventRecorder.class);
    @Override
    public void recordEvent(Object event, String type, String groupId) {
        logger.debug("[DummyOutboxEventRecorder.recordEvent] Event to save: {} (group id: {})",
                event.toString(),
                groupId);
    }
}
