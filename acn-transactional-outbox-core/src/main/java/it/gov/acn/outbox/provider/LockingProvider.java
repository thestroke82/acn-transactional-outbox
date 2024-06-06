package it.gov.acn.outbox.provider;

import java.util.Optional;

public interface LockingProvider {
  Optional<Object> lock();

  void release(Object lock);

  void releaseAllLocks();
}
