package org.snf4j.core.handler;

import org.snf4j.core.ImmutableSctpMessageInfo;

public class SctpSendingFailureException extends Exception {
	
	private static final long serialVersionUID = -8776550059570729769L;

	private final ImmutableSctpMessageInfo messageInfo;
	
	public SctpSendingFailureException(ImmutableSctpMessageInfo messageInfo, Throwable cause) {
		super(cause);
		this.messageInfo = messageInfo;
	}
	
	public ImmutableSctpMessageInfo getMessageInfo() {
		return messageInfo;
	}
}
