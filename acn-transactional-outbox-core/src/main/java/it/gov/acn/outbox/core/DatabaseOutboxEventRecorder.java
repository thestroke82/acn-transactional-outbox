package it.gov.acn.outbox.core;

import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.SerializationProvider;

import java.time.Instant;
import java.util.UUID;

public class DatabaseOutboxEventRecorder implements OutboxEventRecorder {

    private final DataProvider dataProvider;
    private final SerializationProvider serializationProvider;

    public DatabaseOutboxEventRecorder(DataProvider dataProvider, SerializationProvider serializationProvider) {
        this.dataProvider = dataProvider;
        this.serializationProvider = serializationProvider;
    }

    @Override
    public void recordEvent(Object event, String type) {
        OutboxItem entry = new OutboxItem();
        Instant now = Instant.now();
        entry.setId(UUID.randomUUID());
        entry.setEventType(type);
        entry.setCreationDate(now);
        entry.setAttempts(0);
        entry.setEvent(serializeToJson(event));
        dataProvider.save(entry);
    }

    private String serializeToJson(Object event) {
        try {
            return serializationProvider.writeValueAsString(event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event to JSON: ", e);
        }
    }
}