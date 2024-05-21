package it.gov.acn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxProcessor {
    private Logger logger = LoggerFactory.getLogger(OutboxProcessor.class);


    private OutboxProcessorConfiguration configuration;

    public OutboxProcessor(OutboxProcessorConfiguration configuration) {
        this.configuration = configuration;
    }

    public void process() {
        logger.info("Processing outbox, they told me to say {}", configuration.getTestPhrase());
    }


}
