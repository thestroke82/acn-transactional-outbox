package it.gov.acn.outboxprocessor.model;

import java.time.Instant;
import java.util.UUID;

public class OutboxItem {
    private UUID id;
    private String eventType;
    private Instant creationDate;
    private Instant lastAttemptDate;
    private Instant completionDate;
    private int attempts;
    private String event;
    private String lastError;
}
