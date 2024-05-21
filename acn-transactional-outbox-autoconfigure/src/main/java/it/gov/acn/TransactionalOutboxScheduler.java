package it.gov.acn;

import it.gov.acn.config.TransactionalOutboxProperties;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class TransactionalOutboxScheduler {
    private TransactionalOutboxProperties properties;
    private TaskScheduler taskScheduler;

    public TransactionalOutboxScheduler(TransactionalOutboxProperties properties, TaskScheduler taskScheduler) {
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.schedule();
    }

    public void schedule(){
        OutboxProcessorConfiguration outboxProcessorConfiguration = new OutboxProcessorConfiguration();
        outboxProcessorConfiguration.setTestPhrase("Here for the glory of the Lord");
        OutboxProcessor outboxProcessor = new OutboxProcessor(outboxProcessorConfiguration);

        taskScheduler.scheduleWithFixedDelay(outboxProcessor::process,
                Duration.of(properties.getFixedDelay(), ChronoUnit.MILLIS));
    }
}
