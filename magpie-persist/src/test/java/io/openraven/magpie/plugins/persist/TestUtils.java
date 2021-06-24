package io.openraven.magpie.plugins.persist;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class TestUtils {

  protected static String getResourceAsString(String resourcePath) {
    return new Scanner(Objects.requireNonNull(TestUtils.class.getResourceAsStream(resourcePath)), StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }
}
