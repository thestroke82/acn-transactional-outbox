package it.gov.acn.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class MockKafkaBrokerMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private Instant creationDate;

  @Column(columnDefinition = "text")
  private String payload;
}
