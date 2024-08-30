package it.gov.acn.outbox.model;

public class Sort {

  private Property property;
  private Direction direction;

  public static enum Direction {
    ASC, DESC
  }

  public static enum Property {
    CREATION_DATE, LAST_ATTEMPT_DATE, COMPLETION_DATE, ATTEMPTS
  }

  public static Sort defaultSort() {
    return of(Property.CREATION_DATE, Direction.ASC);
  }

  public static Sort of(Property property) {
    Sort sort = new Sort();
    sort.setProperty(property);
    sort.setDirection(Direction.ASC);
    return sort;
  }

  public static Sort of(Property property, Direction direction) {
    Sort sort = new Sort();
    sort.setProperty(property);
    sort.setDirection(direction);
    return sort;
  }

  public Property getProperty() {
    return property;
  }

  public void setProperty(Property property) {
    this.property = property;
  }

  public Direction getDirection() {
    return direction;
  }

  public void setDirection(Direction direction) {
    this.direction = direction;
  }
}