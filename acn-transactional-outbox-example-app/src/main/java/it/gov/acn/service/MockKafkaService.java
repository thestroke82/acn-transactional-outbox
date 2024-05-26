package it.gov.acn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.acn.model.ConstituencyCreatedEvent;
import it.gov.acn.model.MockKafkaBroker;
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
    private final MockKafkaBrokerRepository mockKafkaBrokerRepository;
    private final ObjectMapper jacksonObjectMapper;

    @Override
    public void handle(OutboxItem outboxItem) {
        if(outboxItem==null){
            return;
        }
        if(outboxItem.getEventType().equals(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL)) {
            ConstituencyCreatedEvent event = jacksonObjectMapper.convertValue(outboxItem.getEvent(), ConstituencyCreatedEvent.class);
            MockKafkaBroker mockKafkaBroker = MockKafkaBroker.builder()
                    .id(UUID.fromString(event.getEventId()))
                    .creationDate(Instant.now())
                    .payload(outboxItem.getEvent())
                    .build();
            mockKafkaBrokerRepository.save(mockKafkaBroker);
        }else{
            throw  new UnsupportedOperationException("Event type not supported: "+outboxItem.getEventType());
        }
    }
}
