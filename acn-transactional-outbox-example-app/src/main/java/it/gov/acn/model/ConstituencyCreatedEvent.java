package it.gov.acn.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ConstituencyCreatedEvent{
  public static String EVENT_TYPE_LITERAL = "ConstituencyCreatedEvent";

  private String eventType = EVENT_TYPE_LITERAL;

  private String eventId;
  private Instant timestamp;
  private Constituency payload;

  @Builder
  public ConstituencyCreatedEvent(String eventId, Instant timestamp, Constituency payload) {
    this.eventId = eventId;
    this.timestamp = timestamp;
    this.payload = payload;
  }
}