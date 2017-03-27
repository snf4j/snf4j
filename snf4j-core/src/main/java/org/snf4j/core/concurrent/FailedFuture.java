package org.snf4j.core.concurrent;

public class FailedFuture<V> extends CompletedFuture<V> {
	
	private final Throwable cause;
	
	FailedFuture(Throwable cause) {
		super(FutureState.FAILED);
		this.cause = cause;
	}
	
	@Override
	public Throwable cause() {
		return cause;
	}

}
