package it.gov.acn;

import it.gov.acn.config.TransactionalOutboxProperties;
import it.gov.acn.outboxprocessor.OutboxProcessor;
import it.gov.acn.outboxprocessor.ProcessOutboxCommand;
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
        OutboxProcessor outboxProcessor = new OutboxProcessor(new ProcessOutboxCommand("Forgive me father for I have sinned"));

        taskScheduler.scheduleWithFixedDelay(outboxProcessor::process,
                Duration.of(properties.getFixedDelay(), ChronoUnit.MILLIS));
    }
}
