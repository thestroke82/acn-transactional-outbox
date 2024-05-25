package it.gov.acn.outboxprocessor.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DataProvider {

    List<OutboxItem> find(boolean completed, int maxAttempts);
    OutboxItem findById(UUID id);
    void save(OutboxItem item);
    void update(OutboxItem item);
}
