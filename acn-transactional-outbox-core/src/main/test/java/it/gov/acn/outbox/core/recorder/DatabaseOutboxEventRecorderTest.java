package it.gov.acn.outbox.core.recorder;

import it.gov.acn.outbox.core.TestUtils;
import it.gov.acn.outbox.core.observability.OutboxMetricsCollector;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.SerializationProvider;
import it.gov.acn.outbox.provider.TransactionManagerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class DatabaseOutboxEventRecorderTest {

    @Mock
    private DataProvider dataProvider;
    @Mock
    private SerializationProvider serializationProvider;
    @Mock
    private TransactionManagerProvider transactionManagerProvider;
    @Mock
    private OutboxMetricsCollector outboxMetricsCollector;

    @Captor
    private ArgumentCaptor<OutboxItem> outboxItemCaptor;

    private DatabaseOutboxEventRecorder databaseOutboxEventRecorder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        databaseOutboxEventRecorder = new DatabaseOutboxEventRecorder(dataProvider, serializationProvider, transactionManagerProvider);
        TestUtils.setPrivateField(databaseOutboxEventRecorder, "outboxMetricsCollector", outboxMetricsCollector);
    }

    @Test
    public void when_record_event_verify_invocations() throws Exception {
        String eventType = "testType";
        String serializedEvent = "serializedEvent";
        Object event = new Object();

        when(serializationProvider.writeValueAsString(event)).thenReturn(serializedEvent);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(transactionManagerProvider).executeInTransaction(any());

        databaseOutboxEventRecorder.recordEvent(event, eventType);

        verify(transactionManagerProvider, times(1)).executeInTransaction(any());
        verify(dataProvider, times(1)).save(outboxItemCaptor.capture());
        verify(outboxMetricsCollector, times(1)).incrementQueued();

        OutboxItem outboxItem = outboxItemCaptor.getValue();
        assertEquals(eventType, outboxItem.getEventType());
        assertEquals(serializedEvent, outboxItem.getEvent());
    }
}