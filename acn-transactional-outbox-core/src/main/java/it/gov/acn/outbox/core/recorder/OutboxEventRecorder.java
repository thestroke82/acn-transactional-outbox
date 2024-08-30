package it.gov.acn.outbox.core.recorder;

public interface OutboxEventRecorder {

  void recordEvent(Object event, String type, String groupId);

  default void recordEvent(Object event, String type) {
    recordEvent(event, type, null);
  }
}