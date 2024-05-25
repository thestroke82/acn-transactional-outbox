package it.gov.acn.outbox.model;

import java.util.List;
import java.util.UUID;

public interface DataProvider {

    List<OutboxItem> find(boolean completed, int maxAttempts);
    List<OutboxItem> find(boolean completed, int maxAttempts, Sort sort);
    OutboxItem findById(UUID id);
    void save(OutboxItem item);
    void update(OutboxItem item);



}
