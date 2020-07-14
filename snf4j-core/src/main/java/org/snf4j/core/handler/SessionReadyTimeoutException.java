package org.snf4j.core.handler;

public class SessionReadyTimeoutException extends SessionException {

	private static final long serialVersionUID = 5821450417870443424L;

	public SessionReadyTimeoutException() {
		super("Session ready timed out");
	}
}
