package com.transferwise.common.baseutils.function;

@FunctionalInterface
public interface RunnableWithException {
	void run() throws Exception;
}
