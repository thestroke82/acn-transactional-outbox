package it.gov.acn.outbox.scheduler;

import it.gov.acn.outbox.core.OutboxConfiguration;
import it.gov.acn.outbox.core.OutboxProcessor;
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
                .schedule(outboxProcessor::process, outboxConfiguration.getFixedDelay());
    }

}
