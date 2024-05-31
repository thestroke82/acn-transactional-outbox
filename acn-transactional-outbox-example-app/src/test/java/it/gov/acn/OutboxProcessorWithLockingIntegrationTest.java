package it.gov.acn;

import it.gov.acn.etc.TestableOutboxProcessor;
import it.gov.acn.outbox.core.configuration.OutboxConfiguration;
import it.gov.acn.outbox.model.LockingProvider;
import it.gov.acn.outbox.core.scheduler.OutboxScheduler;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
    "acn.outbox.scheduler.enabled=true"
})
@ExtendWith(MockitoExtension.class)
public class OutboxProcessorWithLockingIntegrationTest extends PostgresTestContainerConfiguration{

  @SpyBean
  private LockingProvider lockingProvider;

  private TestableOutboxProcessor outboxProcessor;

  @Autowired
  private OutboxConfiguration outboxConfiguration;

  @PostConstruct
  public void init(){
    outboxProcessor = Mockito.spy(new TestableOutboxProcessor(outboxConfiguration));
  }


  @Autowired
  private LockProvider lockProvider;

  @BeforeEach
  public void beforeEach() {
    Mockito.reset(outboxProcessor);
    Mockito.reset(lockingProvider);
    lockingProvider.releaseAllLocks();
  }


  @Test
  public void given_lock_failure_when_process_then_doProcess_not_called() {
    Mockito.doReturn(Optional.empty()).when(lockingProvider).lock();
    outboxProcessor.process();
    Mockito.verify(outboxProcessor, Mockito.never()).doProcess();
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
  public void given_multiple_threads_when_process_then_only_one_process_at_a_time(){
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

  @Test
  public void given_multiple_threads_with_random_work_times_when_process_then_only_one_process_at_a_time(){
    assert outboxProcessor != null;
    AtomicInteger criticalSectionCounter = new AtomicInteger(0);

    Mockito.doAnswer(invocation -> {
      criticalSectionCounter.incrementAndGet();
      try {
        Thread.sleep(new Random().nextInt(5000));
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      invocation.callRealMethod();
      return null;
    }).when(outboxProcessor).doProcess();

    List<CompletableFuture<?>> futures = new ArrayList<>();
    for(int i=0; i<5; i++){
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        outboxProcessor.process();
        return null;
      });
      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // only one thread should have entered the critical section
    Assertions.assertEquals(1, criticalSectionCounter.get());
  }

  @Test
  void test_lockProvider(){
    assert outboxProcessor != null;
    AtomicInteger criticalSectionCounter = new AtomicInteger(0);


    List<CompletableFuture<?>> futures = new ArrayList<>();

    for(int i=0; i<10; i++){
      CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
        LockConfiguration lockConfiguration = new LockConfiguration(
            Instant.now(),"test-zi", Duration.ofSeconds(10), Duration.ofMillis(100));
        Optional<SimpleLock> lock = lockProvider.lock(lockConfiguration);
        if(lock.isPresent()){
          criticalSectionCounter.incrementAndGet();
          try {
            Thread.sleep(new Random().nextInt(4000));
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }finally{
            lock.get().unlock();
          }
        }
      });
      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // only one thread should have entered the critical section
    Assertions.assertEquals(1, criticalSectionCounter.get());
  }

}
