package it.gov.acn.outbox.core.processor;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.acn.outbox.core.TestUtils;
import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.observability.OutboxMetricsCollector;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.LockingProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class OutboxProcessorTest {

    @Mock
    private OutboxConfiguration outboxConfiguration;

    @Mock
    private DataProvider dataProvider;

    @Mock
    private LockingProvider lockingProvider;

    @Mock
    private OutboxItemHandlerProvider outboxItemHandlerProvider;

    private OutboxProcessor outboxProcessor;


    private final OutboxMetricsCollector outboxMetricsCollector = Mockito.spy(OutboxMetricsCollector.getInstance());

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(outboxConfiguration.getDataProvider()).thenReturn(dataProvider);
        when(outboxConfiguration.getOutboxItemHandlerProvider()).thenReturn(outboxItemHandlerProvider);
        when(outboxConfiguration.getLockingProvider()).thenReturn(lockingProvider);
        when(lockingProvider.lock()).thenReturn(Optional.of("lock"));
        doNothing().when(lockingProvider).release(any());

        outboxProcessor = OutboxProcessorFactory.createOutboxProcessor(outboxConfiguration);

        TestUtils.setPrivateField(outboxProcessor, "outboxMetricsCollector", outboxMetricsCollector);
    }

    @Test
    public void given_no_outstanding_items_when_process_then_verify_no_handle() {
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(Collections.emptyList());
        outboxProcessor.process();
        verify(dataProvider, times(1)).find(anyBoolean(), anyInt(), any());
        verify(outboxItemHandlerProvider, times(0)).handle(any());
    }

    @Test
    public void given_outstanding_items_when_process_then_verify_handle_them() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));
        outboxProcessor.process();
        verify(dataProvider, times(1)).find(anyBoolean(), anyInt(), any());
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem);
    }

    @Test
    public void given_outstanding_items_with_group_ids_when_process_then_verify_handle_them() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId("group2");
        outboxItem3.setCreationDate(Instant.now().minus(30, ChronoUnit.MINUTES));

        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(
                List.of(outboxItem1, outboxItem3, outboxItem2));
        outboxProcessor.process();
        verify(dataProvider, times(1)).find(anyBoolean(), anyInt(), any());
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem1);
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem2);
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem3);
    }

    @Test
    public void given_outstanding_items_with_group_ids_and_failed_when_process_then_verify_handle_them() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        outboxItem2.setAttempts(outboxConfiguration.getMaxAttempts()+1);
        outboxItem2.setLastAttemptDate(Instant.now().plus(10, ChronoUnit.HOURS));
        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId("group2");
        outboxItem3.setCreationDate(Instant.now().minus(30, ChronoUnit.MINUTES));

        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(
                List.of(outboxItem1, outboxItem3, outboxItem2));
        outboxProcessor.process();
        verify(dataProvider, times(1)).find(anyBoolean(), anyInt(), any());
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem1);
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem2);
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem3);
    }

    @Test
    public void given_outstanding_items_with_group_ids_and_null_group_and_failed_when_process_then_verify_handle_them() {
        var outboxItem1 = new OutboxItem();
        outboxItem1.setGroupId("group1");
        outboxItem1.setCreationDate(Instant.now().minus(5, ChronoUnit.MINUTES));
        var outboxItem2 = new OutboxItem();
        outboxItem2.setGroupId("group1");
        outboxItem2.setCreationDate(Instant.now().minus(10, ChronoUnit.MINUTES));
        outboxItem2.setAttempts(outboxConfiguration.getMaxAttempts()+1);
        outboxItem2.setLastAttemptDate(Instant.now().plus(10, ChronoUnit.HOURS));
        var outboxItem3 = new OutboxItem();
        outboxItem3.setGroupId("group2");
        outboxItem3.setCreationDate(Instant.now().minus(30, ChronoUnit.MINUTES));
        var outboxItem4 = new OutboxItem();
        outboxItem4.setGroupId(null);
        outboxItem4.setCreationDate(Instant.now().minus(30, ChronoUnit.MINUTES));
        var outboxItem5 = new OutboxItem();
        outboxItem5.setGroupId(null);
        outboxItem5.setAttempts(outboxConfiguration.getMaxAttempts()+1);
        outboxItem5.setLastAttemptDate(Instant.now().plus(10, ChronoUnit.HOURS));
        outboxItem5.setCreationDate(Instant.now().minus(60, ChronoUnit.MINUTES));

        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(
                List.of(outboxItem1, outboxItem3, outboxItem2, outboxItem5, outboxItem4));
        outboxProcessor.process();
        verify(dataProvider, times(1)).find(anyBoolean(), anyInt(), any());
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem1);
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem2);
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem3);
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem4);
        verify(outboxItemHandlerProvider, times(0)).handle(outboxItem5);
    }

    @Test
    public void given_outbox_item_when_process_then_update_item() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));
        outboxProcessor.process();
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem);
        verify(dataProvider, times(1)).update(outboxItem);
    }

    @Test
    public void given_outbox_item_success_when_process_then_update_item_with_success_and_increment_successes() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));

        ArgumentCaptor<OutboxItem> captor = ArgumentCaptor.forClass(OutboxItem.class);

        outboxProcessor.process();
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem);
        verify(dataProvider, times(1)).update(captor.capture());
        verify(outboxMetricsCollector, times(1)).incrementSuccesses();

        // Verify arguments
        List<OutboxItem> allValues = captor.getAllValues();
        OutboxItem lastValue = allValues.get(allValues.size() - 1);
        assertNull(lastValue.getLastError());
        assertNotNull(lastValue.getCompletionDate());
        assertEquals(1, lastValue.getAttempts());
    }

    @Test
    public void given_outbox_item_failure_when_process_then_update_item_with_error_and_increment_failures() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));

        ArgumentCaptor<OutboxItem> captor = ArgumentCaptor.forClass(OutboxItem.class);
        doThrow(new RuntimeException("Test exception")).when(outboxItemHandlerProvider).handle(outboxItem);


        outboxProcessor.process();
        verify(outboxItemHandlerProvider, times(1)).handle(outboxItem);
        verify(dataProvider, times(1)).update(captor.capture());
        verify(outboxMetricsCollector, times(1)).incrementFailures();

        // Verify arguments
        List<OutboxItem> allValues = captor.getAllValues();
        OutboxItem lastValue = allValues.get(allValues.size() - 1);
        assertNotNull(lastValue.getLastError());
        assertTrue(lastValue.getLastError().contains("Test exception"));
        assertEquals(1, lastValue.getAttempts());
    }

    @Test
    public void given_outbox_items_when_process_then_verify_locking() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));
        outboxProcessor.process();
        verify(lockingProvider, times(1)).lock();
        verify(lockingProvider, times(1)).release(any());
    }

    @Test
    public void given_outbox_items_lock_taken_when_process_then_verify_skip() {
        OutboxItem outboxItem = new OutboxItem();
        when(dataProvider.find(anyBoolean(), anyInt(), any())).thenReturn(List.of(outboxItem));
        when(lockingProvider.lock()).thenReturn(Optional.empty());

        outboxProcessor.process();
        verify(lockingProvider, times(1)).lock();
        verify(lockingProvider, times(0)).release(any());
    }
}