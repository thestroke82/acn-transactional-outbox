package it.gov.acn.etc;

import java.util.Optional;
import org.springframework.core.env.Environment;

public class PropertiesHelper {

  public static Optional<Boolean> getBooleanProperty(String key, Environment environment) {
    String property = environment.getProperty(key);
    return Optional.ofNullable(property).map(Boolean::parseBoolean);
  }

  public static Optional<String> getStringProperty(String key, Environment environment) {
    String property = environment.getProperty(key);
    return Optional.ofNullable(property);
  }

  public static  Optional<Long> getLongProperty(String key, Environment environment) {
    String property = environment.getProperty(key);
    try {
      return Optional.ofNullable(property).map(Long::parseLong);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }
}
