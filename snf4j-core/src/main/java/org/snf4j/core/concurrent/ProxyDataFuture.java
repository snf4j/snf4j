package org.snf4j.core.concurrent;

public class ProxyDataFuture<V> extends AbstractFuture<V> {

	private DataFuture<V> future;
	
	private long expected;
	
	private V data;
	
	ProxyDataFuture(DataFuture<V> future, long expected) {
		this.future = future;
		this.expected = expected;
	}
	
	void set(V data) {
		this.data = data;
	}
	
	@Override
	public boolean isDone() {
		return isSuccessful() || future.isDone();
	}
	
	@Override
	public boolean isSuccessful() {
		return future.length() >= expected;
	}
	
	@Override
	public boolean isCancelled() {
		return !isSuccessful() && future.isCancelled(); 
	}
	
	@Override
	public boolean isFailed() {
		return !isSuccessful() && future.isFailed();
	}
	
	public Throwable cause() {
		return isSuccessful() ? null : future.cause();
	}
	
	@Override
	protected FutureLock getLock() {
		return future.getLock();
	}

	@Override
	public V getNow() {
		return data;
	}

}
