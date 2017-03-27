package org.snf4j.core.concurrent;


public final class SuccessfulFuture<V> extends CompletedFuture<V> {

	public static final IFuture<Void> VOID = new SuccessfulFuture<Void>();
	
	public SuccessfulFuture() {
		super(FutureState.SUCCESSFUL);
	}

}
