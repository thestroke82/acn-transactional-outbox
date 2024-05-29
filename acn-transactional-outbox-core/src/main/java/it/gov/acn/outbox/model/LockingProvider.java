package it.gov.acn.outbox.model;

import java.util.Optional;

public interface LockingProvider {
  Optional<Object> lock();

  void release(Object lock);

  void releaseAllLocks();
}
