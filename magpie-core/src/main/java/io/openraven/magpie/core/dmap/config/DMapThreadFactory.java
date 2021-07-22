package io.openraven.magpie.core.dmap.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DMapThreadFactory implements ThreadFactory {
  private final ThreadFactory delegate = Executors.defaultThreadFactory();
  private final AtomicInteger threadCount = new AtomicInteger(0);
  private final String namePrefix;

  public DMapThreadFactory(String namePrefix) {
    this.namePrefix = namePrefix;
  }

  @Override
  public Thread newThread(Runnable r) {
    return null;
  }
}
