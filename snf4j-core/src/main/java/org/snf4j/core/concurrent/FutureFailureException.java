package org.snf4j.core.concurrent;

public class FutureFailureException extends Exception {
	
	private static final long serialVersionUID = 8900016824500566670L;

	public FutureFailureException(Throwable cause) {
		super(cause);
	}
}
