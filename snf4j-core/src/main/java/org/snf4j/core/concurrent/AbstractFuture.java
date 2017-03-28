package org.snf4j.core.concurrent;

import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.session.ISession;

abstract class AbstractFuture<V> implements IFuture<V> {
	
	AtomicReference<FutureState> state = new AtomicReference<FutureState>();

	private final ISession session;
	
	AbstractFuture(ISession session) {
		this.session = session;
	}
	
	boolean setState(FutureState state) {
		return this.state.compareAndSet(null, state);
	}
	
	@Override
	public ISession getSession() {
		return session;
	}
	
	protected String toStringDetails() {
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(50);
		String details;
		
		if (session != null) {
			sb.append(session.toString());
			sb.append('-');
		}
		sb.append(getClass().getSimpleName());
		sb.append("[");
		
		if (isSuccessful()) {
			sb.append("successful");
		}
		else if (isFailed()) {
			sb.append("failed:");
			sb.append(cause());
		}
		else if (isCancelled()) {
			sb.append("canceled");
		}
		else {
			sb.append("incomplete");
		}
		details = toStringDetails();
		if (details != null) {
			sb.append(',');
			sb.append(details);
		}
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public boolean isDone() {
		return state.get() != null;
	}
	
	@Override
	public boolean isCancelled() {
		return state.get() == FutureState.CANCELLED;
	}	
	
	@Override
	public boolean isSuccessful() {
		return state.get() == FutureState.SUCCESSFUL;
	}

	@Override
	public boolean isFailed() {
		return state.get() == FutureState.FAILED;
	}
	
	@Override
	public V getNow() {
		return null;
	}
}
