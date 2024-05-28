package it.gov.acn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.acn.integration.KafkaTemplate;
import it.gov.acn.model.ConstituencyCreatedEvent;
import it.gov.acn.model.MockKafkaBrokerMessage;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.OutboxItemHandlerProvider;
import it.gov.acn.repository.MockKafkaBrokerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockKafkaService implements OutboxItemHandlerProvider {
    private final KafkaTemplate kafkaTemplate;
    private final ObjectMapper jacksonObjectMapper;

    @Override
    public void handle(OutboxItem outboxItem) {
        if(outboxItem==null){
            return;
        }
        if(outboxItem.getEventType().equals(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL)) {
          ConstituencyCreatedEvent event = null;
          try {
            event = jacksonObjectMapper.readValue(outboxItem.getEvent(), ConstituencyCreatedEvent.class);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
          MockKafkaBrokerMessage mockKafkaBrokerMessage = MockKafkaBrokerMessage.builder()
                  .id(UUID.fromString(event.getEventId()))
                  .creationDate(Instant.now())
                  .payload(outboxItem.getEvent())
                  .build();
          kafkaTemplate.send(mockKafkaBrokerMessage);
        }else{
            throw  new UnsupportedOperationException("Event type not supported: "+outboxItem.getEventType());
        }
    }
}
