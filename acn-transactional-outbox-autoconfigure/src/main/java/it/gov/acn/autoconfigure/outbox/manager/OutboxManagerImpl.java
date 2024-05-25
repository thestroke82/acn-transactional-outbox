package it.gov.acn.autoconfigure.outbox.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

public class OutboxManagerImpl implements OutboxManager {

    private final DataProvider dataProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public OutboxManagerImpl(DataProvider dataProvider) {
        this.dataProvider = dataProvider;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
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
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event to JSON: ", e);
        }
    }
}