package com.transferwise.common.baseutils.concurrency;

import java.time.Duration;

public interface ScheduledTaskExecutor {
	TaskHandle scheduleAtFixedInterval(Runnable task, Duration initialDelay, Duration period);
	TaskHandle scheduleOnce(Runnable task, Duration initialDelay);

	void start();
	void stop();
	boolean hasStopped();
	boolean waitUntilStopped(Duration waitTime);

	interface TaskHandle {
		void stop();
		boolean hasStopped();
		@SuppressWarnings("UnusedReturnValue")
		boolean waitUntilStopped(Duration waitTime);
		boolean isWorking();
	}
}