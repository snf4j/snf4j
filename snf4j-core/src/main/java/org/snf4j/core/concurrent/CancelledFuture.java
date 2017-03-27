package org.snf4j.core.concurrent;


public final class CancelledFuture<V> extends CompletedFuture<V> {

	public static final IFuture<Void> VOID = new CancelledFuture<Void>();
	
	public CancelledFuture() {
		super(FutureState.CANCELLED);
	}
	
}
