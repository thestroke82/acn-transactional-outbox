package it.gov.acn.integration;

import it.gov.acn.model.MockKafkaBrokerMessage;
import it.gov.acn.repository.MockKafkaBrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockKafkaTemplate implements KafkaTemplate{

  private final MockKafkaBrokerRepository mockKafkaBrokerRepository;

  // it mocks a kafka template by means of an ad-hoc repository
  @Override
  public void send(MockKafkaBrokerMessage message) {
    this.mockKafkaBrokerRepository.save(message);
  }
}
