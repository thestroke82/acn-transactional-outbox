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

        // group the items
        var outstandingItemsGroups = this.outboxItemGroupingStrategy.group(outstandingItems);

        //Process each group separately
        for (var itemsInGroup : outstandingItemsGroups) {
            this.processOutboxItemGroup(itemsInGroup);
        }
    }

    /**
     * Given a group of outbox items, process them in order.
     * Items are passed to the handle provider in order, from the oldest to the youngest.
     * If an item cannot be processed right now, it stops the group processing altogether.
     * If an item fails to be processed, it stops the group processing.
     * @param outstandingItems The items in a group to process
     */
    private void processOutboxItemGroup(List<OutboxItem> outstandingItems) {
        for (var item : outstandingItems) {
            //Process each item in a group until we find one that cannot be processed
            //(note: cannot be processed doesn't mean the item is in th DLQ, it cannot be
            //processed right now)
            if ( ! this.outboxItemSelectionStrategy.filter(item)) {
                return;
            }

            //Stop at the first TEMPORARY error since the item will be processed again
            //and will introduce a reordering in the group if we don't stop.
            //Definitive errors don't do this since the item is never processed again
            if (this.processOutboxItem(item) == ProcessingResult.TEMPORARY_ERROR) {
                return;
            }
        }
    }


    /**
     * Process an item.
     * Return the state of the processing. A temporary error won't prevent the item from being
     * process again later, a definitive error put the item in the DLQ (where it won't be processed again).
     *
     * @param outboxItem The item to process
     * @return The result of the processing
     */
    private ProcessingResult processOutboxItem(OutboxItem outboxItem){
        try {
            this.outboxItemHandlerProvider.handle(outboxItem);
            this.setOutboxSuccess(outboxItem);

            return ProcessingResult.SUCCESS;
        } catch (Exception e){
            logger.error("Error processing outbox item {}", outboxItem.getId(), e);
            return this.setOutboxFailure(outboxItem, e.getMessage())
                    ? ProcessingResult.DEFINITIVE_ERROR
                    : ProcessingResult.TEMPORARY_ERROR;
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

    /**
     * Mark an item as failed. If an item failed more than the configured amount of times,
     * it is put in the DLQ and never processed again.
     * @param outboxItem The item that failed
     * @param errorMessage The error message of the failure
     * @return true if the item failed definitively (won't be tried again)
     */
    private boolean setOutboxFailure(OutboxItem outboxItem, String errorMessage){
        Instant now = Instant.now();
        outboxItem.setAttempts(outboxItem.getAttempts() + 1);
        outboxItem.setLastAttemptDate(now);
        outboxItem.setLastError(errorMessage);
        this.dataProvider.update(outboxItem);
        this.outboxMetricsCollector.incrementFailures();
        if(this.maxAttempts<=outboxItem.getAttempts()){
            this.outboxMetricsCollector.incrementDlq();
            return true;
        }
        return false;
    }

    private enum ProcessingResult {
        SUCCESS,
        TEMPORARY_ERROR,
        DEFINITIVE_ERROR
    }

}
