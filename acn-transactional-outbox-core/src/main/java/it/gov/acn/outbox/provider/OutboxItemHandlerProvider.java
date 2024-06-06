package it.gov.acn.outbox.provider;

import it.gov.acn.outbox.model.OutboxItem;

public interface OutboxItemHandlerProvider {
    void handle(OutboxItem outboxItem);
}
