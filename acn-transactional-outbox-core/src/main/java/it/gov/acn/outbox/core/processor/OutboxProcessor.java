package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.OutboxItemHandlerProvider;
import it.gov.acn.outbox.model.Sort;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxProcessor {
    private final Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxConfiguration outboxConfiguration;
    private final DataProvider dataProvider;
    private final OutboxItemHandlerProvider outboxItemHandlerProvider;
    private final OutboxItemSelectionStrategy outboxItemSelectionStrategy;

    public OutboxProcessor(OutboxConfiguration outboxConfiguration) {
        this.outboxConfiguration = outboxConfiguration;
        this.dataProvider = this.outboxConfiguration.getDataProvider();
        this.outboxItemHandlerProvider = this.outboxConfiguration.getOutboxItemHandlerProvider();
        //TODO: Use a factory to create the OutboxItemSelectionStrategy, for now only one strategy is available
        this.outboxItemSelectionStrategy =
                new ExponentialBackoffStrategy(this.outboxConfiguration.getBackoffBase());
    }

    public void process(){

        // oldest events first
        Sort sort = Sort.of(Sort.Property.CREATION_DATE, Sort.Direction.ASC);

        // load all outstanding items on a simple basis: when they have no completion date and are below the max attempts
        List<OutboxItem> outstandingItems =
                this.dataProvider.find(false, this.outboxConfiguration.getMaxAttempts()+1, sort);

        // select the outbox items to process in a more detailed way (in memory)
        // currently, the exponential backoff strategy is the only one implemented
        outstandingItems = this.outboxItemSelectionStrategy.execute(outstandingItems);

        if(outstandingItems.isEmpty()){
            logger.trace("No outbox items to process. See you later!");
            return;
        }

        logger.trace("Kafka outbox scheduler processing {} items", outstandingItems.size());

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
    }

    private void setOutboxFailure(OutboxItem outboxItem, String errorMessage){
        Instant now = Instant.now();
        outboxItem.setAttempts(outboxItem.getAttempts() + 1);
        outboxItem.setLastAttemptDate(now);
        outboxItem.setLastError(errorMessage);
        this.dataProvider.update(outboxItem);
    }

}
