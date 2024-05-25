package it.gov.acn.autoconfigure.outbox.manager;

public interface OutboxManager {
    void recordEvent(Object event, String type);
}