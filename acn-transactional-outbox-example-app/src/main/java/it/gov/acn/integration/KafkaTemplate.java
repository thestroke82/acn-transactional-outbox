package it.gov.acn.integration;

import it.gov.acn.model.MockKafkaBrokerMessage;


public interface KafkaTemplate {
 // it mocks a kafka template by means of an ad-hoc repository
 void send(MockKafkaBrokerMessage message);
}
