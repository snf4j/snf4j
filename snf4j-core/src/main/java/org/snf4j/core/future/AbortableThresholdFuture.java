package org.snf4j.core.future;

class AbortableThresholdFuture<V> extends ThresholdFuture<V> implements IAbortableFuture {
	
	AbortableThresholdFuture(DataFuture<V> future, long threshold) {
		super(future, threshold);
	}
	
	@Override
	public boolean isDone() {
		return state.get() != null ? true : super.isDone();
	}
	
	@Override
	public boolean isSuccessful() {
		return state.get() != null ? false : super.isSuccessful();
	}

	@Override
	public boolean isCancelled() {
		return state.get() != null ? state.get() == FutureState.CANCELLED : super.isCancelled(); 
	}
	
	@Override
	public boolean isFailed() {
		return state.get() != null ? state.get() == FutureState.FAILED : super.isFailed(); 
	}
	
	public Throwable cause() {
		return state.get() != null ? cause : super.cause();
	}
	
	@Override
	public void abort(Throwable cause) {
		if (cause != null) {
			if (setState(FutureState.FAILED)) {
				this.cause = cause;
				notifyWaiters();
			}
		}
		else if (setState(FutureState.CANCELLED)) {
			notifyWaiters();
		}
	}
}
