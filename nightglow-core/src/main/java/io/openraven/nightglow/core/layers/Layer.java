package io.openraven.nightglow.core.layers;

import io.openraven.nightglow.core.fifos.FifoException;

public interface Layer {

  void exec() throws FifoException;
  String getName();
}
