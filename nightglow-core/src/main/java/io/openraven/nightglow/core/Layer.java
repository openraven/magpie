package io.openraven.nightglow.core;

import java.util.Set;
import java.util.regex.Pattern;

public interface Layer {

  void exec() throws FifoException;

  default boolean matches(String text, Set<Pattern> patterns) {
    return patterns.stream().filter(p -> p.matcher(text).matches()).count() > 0;
  }
}
