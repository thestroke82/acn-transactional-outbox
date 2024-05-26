package it.gov.acn.service;

import it.gov.acn.model.Constituency;
import it.gov.acn.model.ConstituencyCreatedEvent;
import it.gov.acn.outbox.core.OutboxManager;
import it.gov.acn.repository.ConstituencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConstituencyService {

    private final ConstituencyRepository constituencyRepository;
    private final OutboxManager outboxManager;

    @Transactional
    public Constituency saveConstituency(Constituency constituency) {
        Constituency ret = this.constituencyRepository.save(constituency);
        ConstituencyCreatedEvent event = ConstituencyCreatedEvent.builder()
                .payload(constituency)
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();
        this.outboxManager.recordEvent(event, event.getEventType());
        return ret;
    }
}