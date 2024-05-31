package it.gov.acn.outbox.core.scheduler;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;
import it.gov.acn.outbox.model.SchedulingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxScheduler {
    private final Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);

    private final SchedulingProvider schedulingProvider;
    private final long fixedDelay;
    private final OutboxProcessor outboxProcessor;

    OutboxScheduler(
        SchedulingProvider schedulingProvider,
        long fixedDelay,
        OutboxProcessor outboxProcessor
    ){
        this.schedulingProvider = schedulingProvider;
        this.fixedDelay = fixedDelay;
        this.outboxProcessor = outboxProcessor;
        this.schedule();
    }

    public void schedule(){
        this.schedulingProvider.schedule(this::safeProcess, fixedDelay);
    }

    private void safeProcess(){
        try {
            this.outboxProcessor.process();
        } catch (Exception e){
            logger.error("Error processing outbox", e);
        }
    }

}
