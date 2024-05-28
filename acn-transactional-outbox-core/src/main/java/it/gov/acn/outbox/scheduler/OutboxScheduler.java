package it.gov.acn.outbox.scheduler;

import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxScheduler {
    private Logger logger = LoggerFactory.getLogger(OutboxScheduler.class);

    private OutboxConfiguration outboxConfiguration;
    private OutboxProcessor outboxProcessor;

    public OutboxScheduler(OutboxConfiguration outboxConfiguration) {
        this.outboxConfiguration = outboxConfiguration;
        this.outboxProcessor = new OutboxProcessor(outboxConfiguration);
        this.schedule();
    }

    public void schedule(){
        logger.debug("Scheduling outbox processor with conf: "+outboxConfiguration.toString());
        this.outboxConfiguration.getSchedulingProvider()
                .schedule(this::safeProcess, outboxConfiguration.getFixedDelay());
    }

    private void safeProcess(){
        try {
            this.outboxProcessor.process();
        } catch (Exception e){
            logger.error("Error processing outbox", e);
        }
    }

}
