package it.gov.acn.autoconfigure.outbox.providers.locking;

import it.gov.acn.outbox.model.LockingProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

public class SchedlockLockProvider implements LockingProvider {
  private final List<SimpleLock> activeLocks =
      Collections.synchronizedList(new ArrayList<>());
  private final LockProvider lockProvider;

  public SchedlockLockProvider(LockProvider lockProvider) {
    this.lockProvider = lockProvider;
  }

  @Override
  public synchronized Optional<Object> lock() {
    Optional<SimpleLock> lock = this.lockProvider.lock(getLockConfiguration());
    lock.ifPresent(activeLocks::add);
    return lock.map(l -> l); // casts to object the Optional argument
  }

  @Override
  public synchronized void release(Object lock) {
    if(lock != null){
      SimpleLock simpleLock = (SimpleLock) lock;
      activeLocks.remove(simpleLock);
      simpleLock.unlock();
    }
  }

  @Override
  public synchronized void releaseAllLocks() {
    activeLocks.forEach(SimpleLock::unlock);
    activeLocks.clear();
  }

  // TODO: should be created in a more extensible and cleaner way
  public LockConfiguration getLockConfiguration(){
    return new LockConfiguration(Instant.now(), "acn_transactional_outbox",
        Duration.ofSeconds(120), Duration.ofMillis(200));
  }
}
