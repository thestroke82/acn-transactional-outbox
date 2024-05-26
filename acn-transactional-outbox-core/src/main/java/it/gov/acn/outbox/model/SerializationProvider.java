package it.gov.acn.outbox.model;

public interface SerializationProvider {
    String writeValueAsString(Object value) throws Exception;
}
