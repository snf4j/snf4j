package org.snf4j.core.handler;

public class SessionHandshakeTimeoutException extends SessionException {

	private static final long serialVersionUID = 5821450417870443424L;

	public SessionHandshakeTimeoutException() {
		super("Session handshake timed out");
	}
}
