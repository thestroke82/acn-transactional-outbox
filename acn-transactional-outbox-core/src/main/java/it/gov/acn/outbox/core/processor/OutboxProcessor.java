package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.core.observability.OutboxMetricsCollector;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.Sort;
import it.gov.acn.outbox.provider.DataProvider;
import it.gov.acn.outbox.provider.LockingProvider;
import it.gov.acn.outbox.provider.OutboxItemHandlerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class OutboxProcessor {
    private final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);


    private final int maxAttempts;
    private final DataProvider dataProvider;
    private final OutboxItemHandlerProvider outboxItemHandlerProvider;
    private final LockingProvider lockingProvider;
    private final OutboxItemSelectionStrategy outboxItemSelectionStrategy;
    private final OutboxMetricsCollector outboxMetricsCollector;
    private final OutboxItemGroupingStrategy outboxItemGroupingStrategy;

    protected OutboxProcessor(
        int backoffBase,
        int maxAttempts,
        DataProvider dataProvider,
        OutboxItemHandlerProvider outboxItemHandlerProvider,
        LockingProvider lockingProvider
    ) {
        this.maxAttempts = maxAttempts;
        this.dataProvider = dataProvider;
        this.outboxItemHandlerProvider = outboxItemHandlerProvider;
        this.lockingProvider = lockingProvider;
        this.outboxMetricsCollector = OutboxMetricsCollector.getInstance();
        //TODO: Use a factory to create the OutboxItemSelectionStrategy, for now only one strategy is available
        this.outboxItemSelectionStrategy = new ExponentialBackoffStrategy(backoffBase);
        //TODO: Use a factory in the future
        this.outboxItemGroupingStrategy = new NullableIdGroupingStrategy();


    }

    public void process(){
        Object lock = this.lockingProvider.lock().orElse(null);
        if(lock == null){
            return;
        }
        try {
            this.doProcess();
        }catch (Exception e) {
            logger.error("Error processing outbox items", e);
        }finally {
            this.lockingProvider.release(lock);
        }
    }

    // this represents the "critical section" and its modifier is protected for testing purposes
    protected void doProcess(){
        // oldest events first
        Sort sort = Sort.of(Sort.Property.CREATION_DATE, Sort.Direction.ASC);

        // load all outstanding items on a simple basis: when they have no completion date and are below the max attempts
        List<OutboxItem> outstandingItems =
            this.dataProvider.find(false, maxAttempts+1, sort);

        // group the items and pick one or more item from each group
        outstandingItems = this.outboxItemGroupingStrategy.execute(outstandingItems);

        // select the outbox items to process in a more detailed way (in memory)
        // currently, the exponential backoff strategy is the only one implemented
        outstandingItems = this.outboxItemSelectionStrategy.execute(outstandingItems);

        if(outstandingItems.isEmpty()){
            return;
        }

        // that's the actual processing of the outbox items
        outstandingItems.forEach(this::processOutboxItem);
    }

    private void processOutboxItem(OutboxItem outboxItem){
        try {
            this.outboxItemHandlerProvider.handle(outboxItem);
            this.setOutboxSuccess(outboxItem);
        } catch (Exception e){
            logger.error("Error processing outbox item {}", outboxItem.getId(), e);
            this.setOutboxFailure(outboxItem, e.getMessage());
        }
    }

    private void setOutboxSuccess(OutboxItem outboxItem){
        Instant now = Instant.now();
        outboxItem.setCompletionDate(now);
        outboxItem.setAttempts(outboxItem.getAttempts() + 1);
        outboxItem.setLastAttemptDate(now);
        this.dataProvider.update(outboxItem);
        this.outboxMetricsCollector.incrementSuccesses();
    }

    private void setOutboxFailure(OutboxItem outboxItem, String errorMessage){
        Instant now = Instant.now();
        outboxItem.setAttempts(outboxItem.getAttempts() + 1);
        outboxItem.setLastAttemptDate(now);
        outboxItem.setLastError(errorMessage);
        this.dataProvider.update(outboxItem);
        this.outboxMetricsCollector.incrementFailures();
        if(this.maxAttempts<=outboxItem.getAttempts()){
            this.outboxMetricsCollector.incrementDlq();
        }
    }

}
