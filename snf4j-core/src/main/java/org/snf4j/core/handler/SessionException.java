package org.snf4j.core.handler;

public class SessionException extends Exception {
	
	private static final long serialVersionUID = -4166432567474000446L;

	public SessionException() {
	}

	public SessionException(String message) {
		super(message);
	}
	
	public SessionException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public SessionException(Throwable cause) {
		super(cause);
	}

}
