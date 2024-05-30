package it.gov.acn;

import it.gov.acn.etc.TestableOutboxProcessor;
import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.core.processor.OutboxProcessor;
import it.gov.acn.outbox.scheduler.OutboxScheduler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true"
})
@ExtendWith(MockitoExtension.class)
@Disabled
public class OutboxProcessorWithLockingIntegrationTest extends PostgresTestContext{
  private TestableOutboxProcessor outboxProcessor;
  private OutboxScheduler outboxScheduler;
  @Autowired
  public void setOutboxScheduler(OutboxScheduler outboxScheduler) {
    this.outboxScheduler = outboxScheduler;
    OutboxConfiguration outboxConfiguration = (OutboxConfiguration)ReflectionTestUtils.getField(outboxScheduler, "outboxConfiguration");
    outboxProcessor = new TestableOutboxProcessor(outboxConfiguration);
    outboxProcessor = Mockito.spy(outboxProcessor);
    assert outboxProcessor != null;
    ReflectionTestUtils.setField(outboxScheduler, "outboxProcessor", outboxProcessor);
  }


  @Test
  public void given_single_thread_when_process_then_locking_always_succeeds() {
    assert outboxProcessor != null;
    for(int i=0; i<10; i++){
      outboxProcessor.process();
    }
    Mockito.verify(outboxProcessor, Mockito.times(10)).process();
  }

  @Test
  public void given_multiple_threads_when_process_then_only_one_process_at_a_time()
      throws Exception {
    assert outboxProcessor != null;
    AtomicInteger criticalSectionCounter = new AtomicInteger(0);

    Mockito.doAnswer(invocation -> {
      criticalSectionCounter.incrementAndGet();
      invocation.callRealMethod();
      return null;
    }).when(outboxProcessor).doProcess();

    List<CompletableFuture<?>> futures = new ArrayList<>();

    for(int i=0; i<10; i++){
      CompletableFuture<?> future = CompletableFuture.runAsync(() -> outboxProcessor.process());
      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // only one thread should have entered the critical section
    Assertions.assertEquals(1, criticalSectionCounter.get());
  }



}
