package it.gov.acn.outboxprocessor.model;

import java.time.Instant;
import java.util.List;

public interface DataProvider {

    List<OutboxItem> find(boolean completed, int maxAttempts);

}
