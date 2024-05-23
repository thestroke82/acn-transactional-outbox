package it.gov.acn.outboxprocessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxProcessor {
    private Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);
    private OutboxProcessorConfiguration command;

    public OutboxProcessor(OutboxProcessorConfiguration command) {
        this.command = command;
        if(this.command == null || this.command.getTestString() == null){
            throw new IllegalArgumentException("Command or message cannot be null");
        }
    }

    public void process() {
        logger.info("Processing outbox, they told me to say {}",this.command.getTestString());
    }


}
