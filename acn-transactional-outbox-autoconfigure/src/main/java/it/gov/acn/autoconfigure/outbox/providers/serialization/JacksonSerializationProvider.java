package it.gov.acn.autoconfigure.outbox.providers.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.gov.acn.outbox.model.SerializationProvider;

public class JacksonSerializationProvider implements SerializationProvider {

    private final ObjectMapper objectMapper;

    public JacksonSerializationProvider() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    @Override
    public String writeValueAsString(Object value) throws Exception {
        return this.objectMapper.writeValueAsString(value);
    }
}
