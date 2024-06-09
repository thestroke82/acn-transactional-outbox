package it.gov.acn.etc;

import it.gov.acn.outbox.core.recorder.OutboxEventRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TransactionTestClass {
    private final OutboxEventRecorder outboxEventRecorder;

    @Transactional
    public void throwExceptionAfterOutboxEventRecord() {
        this.outboxEventRecorder.recordEvent("{\"test\":\"test\"}", "test");
        throw new RuntimeException("test");
    }
}
