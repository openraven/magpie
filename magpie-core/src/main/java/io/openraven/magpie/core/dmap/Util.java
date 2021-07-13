package io.openraven.magpie.core.dmap;


import io.openraven.magpie.core.dmap.service.DMapLambdaServiceImpl;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class Util {
  private Util() {}

  public static String getResourceAsString(String resourcePath) {
    return new Scanner(
      Objects.requireNonNull(DMapLambdaServiceImpl.class.getResourceAsStream(resourcePath)),
      StandardCharsets.UTF_8)
      .useDelimiter("\\A").next();
  }
}
