package it.gov.acn.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConstituencyCreatedEvent {

  public static final String EVENT_TYPE_LITERAL = "ConstituencyCreatedEvent";

  private final String eventType = EVENT_TYPE_LITERAL;

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