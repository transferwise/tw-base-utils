package com.transferwise.common.baseutils.threads;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ThreadInfo {

  private Long id;
  private long cpuTime;
  private long userTime;
  private Thread.State state;
  private int priority;
  private String name;
  private String groupName;
  private boolean daemon;

  private StackTraceElement[] stackTrace;
}