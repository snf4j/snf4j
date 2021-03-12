package org.snf4j.core.future;

public interface IAbortableFuture {
	
	void abort(Throwable cause);
}
