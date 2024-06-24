package it.gov.acn.outbox.model;

import java.time.Instant;
import java.util.Comparator;
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

    private String groupId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public Instant getLastAttemptDate() {
        return lastAttemptDate;
    }

    public void setLastAttemptDate(Instant lastAttemptDate) {
        this.lastAttemptDate = lastAttemptDate;
    }

    public Instant getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Instant completionDate) {
        this.completionDate = completionDate;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getGroupId() { return groupId; }

    public void setGroupId(String groupId) { this.groupId = groupId; }
}
