package it.gov.acn.outbox.core.observability;

import java.util.ArrayList;
import java.util.List;

public class Observable {
  private final List<Observer> observers = new ArrayList<>();
  public void addObserver(Observer observer) {
    if(observer == null) throw new IllegalArgumentException("Observer cannot be null");
    observers.add(observer);
  }
  public void removeObserver(Observer observer) {
    if(observer == null) throw new IllegalArgumentException("Observer cannot be null");
    observers.remove(observer);
  }
  public void notifyObservers() {
    for(Observer observer : observers) {
      observer.update();
    }
  }
}
