package it.gov.acn;

import it.gov.acn.autoconfigure.outbox.config.DefaultConfiguration;
import it.gov.acn.model.Constituency;
import it.gov.acn.model.ConstituencyCreatedEvent;
import it.gov.acn.outbox.core.recorder.OutboxEventRecorder;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.repository.ConstituencyRepository;
import it.gov.acn.service.ConstituencyService;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest(properties = {
        "acn.outbox.scheduler.enabled=true",
        "acn.outbox.scheduler.fixed-delay=3000",
})
public class ConstituencyServiceOutboxTransactionalIntegrationTests extends PostgresTestContext{

    @Autowired
    private ConstituencyService constituencyService;

    @SpyBean
    private OutboxEventRecorder outboxManager;

    @SpyBean
    private ConstituencyRepository constituencyRepository;

    // only for testing purposes
    @SpyBean
    private DataProvider dataProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void afterEach() {
        jdbcTemplate.execute("TRUNCATE TABLE "+ DefaultConfiguration.TABLE_NAME);
        constituencyRepository.deleteAll();
    }
    @BeforeEach
    public void beforeEach() {
        jdbcTemplate.execute("TRUNCATE TABLE "+ DefaultConfiguration.TABLE_NAME);
        constituencyRepository.deleteAll();
    }

    @Test
    void when_saveConstituency_then_outboxManagerIsCalled() {
        Constituency constituency = buildRandomConstituency();
        constituencyService.saveConstituency(constituency);
        Mockito.verify(outboxManager, Mockito.times(1)).recordEvent(Mockito.any(),Mockito.anyString());
    }

    @Test
    void when_saveConstituency_then_outboxManager_saves_event() {
        Constituency constituency = buildRandomConstituency();
        constituencyService.saveConstituency(constituency);
        List<OutboxItem> outboxItems = this.findOutboxItemsByEventType(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL);
        assertEquals(1,outboxItems.size());
        assertNotNull(constituencyRepository.findById(constituency.getId()));
    }

    @Test
    void when_saveConstituency_outboxManager_exception_then_rollback() {
        Mockito.doThrow(new RuntimeException("OutboxManager test exception"))
                .when(outboxManager).recordEvent(Mockito.any(),Mockito.anyString());
        Assertions.assertThrows(RuntimeException.class, () -> {
            constituencyService.saveConstituency(new Constituency());
        });
        List<OutboxItem> outboxItems = this.findOutboxItemsByEventType(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL);
        assertEquals(0,outboxItems.size());
        assertEquals(0,constituencyRepository.count(), "No constituency should be saved");
    }

     @Test
    void when_saveConstituency_outboxManager_dataprovider_exception_then_rollback() {
        Mockito.doThrow(new RuntimeException("DataProvider test exception"))
                .when(dataProvider).save(Mockito.any());
        Assertions.assertThrows(RuntimeException.class, () -> {
            constituencyService.saveConstituency(new Constituency());
        });
        List<OutboxItem> outboxItems = this.findOutboxItemsByEventType(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL);
        assertEquals(0,outboxItems.size());
        assertEquals(0,constituencyRepository.count(), "No constituency should be saved");
    }

    @Test
    void when_saveConstituency_constituencyRepository_exception_then_rollback() {
        Mockito.doThrow(new RuntimeException("ConstituencyRepository test exception"))
                .when(constituencyRepository).save(Mockito.any());
        Assertions.assertThrows(RuntimeException.class, () -> {
            constituencyService.saveConstituency(new Constituency());
        });
        List<OutboxItem> outboxItems = this.findOutboxItemsByEventType(ConstituencyCreatedEvent.EVENT_TYPE_LITERAL);
        assertEquals(0,outboxItems.size());
        assertEquals(0,constituencyRepository.count(), "No constituency should be saved");
    }

    private Constituency buildRandomConstituency() {
        Constituency constituency = new Constituency();
        constituency.setId(UUID.randomUUID());
        constituency.setName("Enel");
        constituency.setAddress("Via Roma 1");
        return constituency;
    }

    private List<OutboxItem> findOutboxItemsByEventType(String eventType) {
        return dataProvider.find(false,Integer.MAX_VALUE).stream()
                .filter(outboxItem -> outboxItem.getEventType().equals(eventType))
                .collect(Collectors.toList());
    }

}
