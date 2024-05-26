package it.gov.acn.outbox.core;

public interface OutboxEventRecorder {
    void recordEvent(Object event, String type);
}