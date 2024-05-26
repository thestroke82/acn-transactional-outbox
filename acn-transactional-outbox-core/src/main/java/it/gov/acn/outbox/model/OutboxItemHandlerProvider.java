package it.gov.acn.outbox.model;

public interface OutboxItemHandlerProvider {
    void handle(OutboxItem outboxItem);
}
