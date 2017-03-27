package org.snf4j.core.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class CompletedFuture<V> implements IFuture<V> {

	private final FutureState state;
	
	CompletedFuture(FutureState state) {
		this.state = state;
	}
	
	@Override
	public V getNow() {
		return null;
	}

	@Override
	public boolean cancel(boolean arg0) {
		return false;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return null;
	}

	@Override
	public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
		return null;
	}

	@Override
	public boolean isCancelled() {
		return state == FutureState.CANCELLED;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public IFuture<V> await() {
		return this;
	}

	@Override
	public IFuture<V> await(long timeoutMillis) {
		return this;
	}

	@Override
	public IFuture<V> await(long timeout, TimeUnit unit) {
		return this;
	}

	@Override
	public IFuture<V> awaitUninterruptibly() {
		return this;
	}

	@Override
	public IFuture<V> awaitUninterruptibly(long timeoutMillis) {
		return this;
	}

	@Override
	public IFuture<V> awaitUninterruptibly(long timeout, TimeUnit unit) {
		return this;
	}

	@Override
	public IFuture<V> sync() {
		return this;
	}

	@Override
	public IFuture<V> sync(long timeoutMillis) {
		return this;
	}

	@Override
	public IFuture<V> sync(long timeout, TimeUnit unit) {
		return this;
	}

	@Override
	public IFuture<V> syncUninterruptibly() {
		return this;
	}

	@Override
	public IFuture<V> syncUninterruptibly(long timeoutMillis) {
		return this;
	}

	@Override
	public IFuture<V> syncUninterruptibly(long timeout, TimeUnit unit) {
		return this;
	}
	
	@Override
	public Throwable cause() {
		return null;
	}

	@Override
	public boolean isSuccessful() {
		return state == FutureState.SUCCESSFUL;
	}

	@Override
	public boolean isFailed() {
		return state == FutureState.FAILED;
	}

}
