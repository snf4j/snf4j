package org.snf4j.core.handler;

import java.nio.ByteBuffer;

import org.snf4j.core.session.DefalutSctpSessionConfig;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;

abstract public class AbstractSctpHandler extends AbstractHandler implements ISctpHandler {
	
	protected AbstractSctpHandler() {
		super(new DefalutSctpSessionConfig());
	}
	
	protected AbstractSctpHandler(String name) {
		super(name, new DefalutSctpSessionConfig());
	}

	@Override
	public void setSession(ISession session) {
		if (session instanceof ISctpSession) {
			super.setSession(session);
		}
		else {
			throw new IllegalArgumentException("session is not an instance of ISctpSession");
		}
	}
	
	@Override
	public ISctpSession getSession() {
		return (ISctpSession) super.getSession();
	}
	
	@Override
	public ISctpSessionConfig getConfig() {
		return (ISctpSessionConfig) super.getConfig();
	}
	
	@Override
	public void read(byte[] msg, MessageInfo msgInfo) {
		read((Object)msg, msgInfo);
	}

	@Override
	public void read(ByteBuffer msg, MessageInfo msgInfo) {
		read((Object)msg, msgInfo);
	}
	
	@Override
	public void read(Object msg) {
	}
	
	@Override
	public Object getNotificationAttachment() {
		return null;
	}
	
	@Override
	public NotificationHandler<Object> getNotificationHandler() {
		return null;
	}
}
