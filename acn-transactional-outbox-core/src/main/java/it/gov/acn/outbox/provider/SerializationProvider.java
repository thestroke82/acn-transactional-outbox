package it.gov.acn.outbox.provider;

public interface SerializationProvider {

  String writeValueAsString(Object value) throws Exception;
}
