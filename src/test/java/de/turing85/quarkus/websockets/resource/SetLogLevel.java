package de.turing85.quarkus.websockets.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SetLogLevel implements QuarkusTestResourceLifecycleManager {
  public static final String MIN_LEVEL = "min-level";
  public static final String ROOT_CATEGORY = "root";
  private Map<String, String> categoriesWithLevel = new HashMap<>();

  @Override
  public void init(Map<String, String> initArgs) {
    categoriesWithLevel = initArgs.entrySet().stream().collect(
        Collectors.toMap(entry -> SetLogLevel.mapCategory(entry.getKey()), Map.Entry::getValue));
  }

  private static String mapCategory(String category) {
    return switch (category) {
      case MIN_LEVEL -> "quarkus.log.min-level";
      case ROOT_CATEGORY -> "quarkus.log.level";
      default -> "quarkus.log.category.\"%s\".level".formatted(category);
    };
  }

  @Override
  public Map<String, String> start() {
    return categoriesWithLevel;
  }

  @Override
  public void stop() {
    categoriesWithLevel.clear();
  }
}
