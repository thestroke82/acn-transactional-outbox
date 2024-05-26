package it.gov.acn.outbox.core.processor;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.model.DataProvider;
import it.gov.acn.outbox.model.OutboxItem;
import it.gov.acn.outbox.model.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class OutboxProcessor {
    private Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);

    private OutboxConfiguration outboxConfiguration;
    private DataProvider dataProvider;
    private OutboxItemSelectionStrategy outboxItemSelectionStrategy;

    public OutboxProcessor(OutboxConfiguration outboxConfiguration) {
        this.outboxConfiguration = outboxConfiguration;
        this.dataProvider = this.outboxConfiguration.getDataProvider();
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

        // select the outbox items to process in a more detailed way, with in-memory filtering
        // currently, the exponential backoff strategy is the only one implemented
        outstandingItems = this.outboxItemSelectionStrategy.execute(outstandingItems);

        if(outstandingItems.isEmpty()){
            logger.trace("No outbox items to process. See you later!");
            return;
        }

        logger.trace("Kafka outbox scheduler processing {} items", outstandingItems.size());
        // outstandingItems.forEach(this.kafkaOutboxProcessor::processOutbox);
    }

}
