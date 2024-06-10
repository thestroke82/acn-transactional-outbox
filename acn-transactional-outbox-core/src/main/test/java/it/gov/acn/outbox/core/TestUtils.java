package it.gov.acn.outbox.core;

import java.lang.reflect.Field;

public class TestUtils {

  /**
   * Sets the value of a private field in a given object.
   *
   * @param object    The object containing the private field.
   * @param fieldName The name of the private field.
   * @param newValue  The new value to set for the field.
   */
  public static void setPrivateField(Object object, String fieldName, Object newValue) {
    try {
      Field field = object.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(object, newValue);
    } catch (NoSuchFieldException e) {
      System.err.println("No such field: " + fieldName);
    } catch (IllegalAccessException e) {
      System.err.println("Illegal access to field: " + fieldName);
    } catch (SecurityException e) {
      System.err.println("Security violation: Unable to access field: " + fieldName);
    } catch (Exception e) {
      System.err.println("An unexpected error occurred while setting the field: " + fieldName);
      e.printStackTrace();
    }
  }
}
