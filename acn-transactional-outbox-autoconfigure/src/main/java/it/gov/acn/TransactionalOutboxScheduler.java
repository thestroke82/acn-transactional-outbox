package it.gov.acn;

import it.gov.acn.config.TransactionalOutboxProperties;
import it.gov.acn.outboxprocessor.OutboxProcessor;
import it.gov.acn.outboxprocessor.OutboxProcessorConfiguration;
import it.gov.acn.outboxprocessor.model.DataProvider;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class TransactionalOutboxScheduler {
    private final TransactionalOutboxProperties properties;
    private final TaskScheduler taskScheduler;

    private final DataProvider dataProvider;

    public TransactionalOutboxScheduler(
        TransactionalOutboxProperties properties, TaskScheduler taskScheduler, DataProvider dataProvider
    ) {
        this.dataProvider = dataProvider;
        this.properties = properties;
        this.taskScheduler = taskScheduler;
        this.schedule();
    }

    public void schedule(){
        OutboxProcessor outboxProcessor = new OutboxProcessor(
            new OutboxProcessorConfiguration("Forgive me father for I have sinned",this.dataProvider)
        );

        taskScheduler.scheduleWithFixedDelay(outboxProcessor::process,
                Duration.of(properties.getFixedDelay(), ChronoUnit.MILLIS));
    }
}
