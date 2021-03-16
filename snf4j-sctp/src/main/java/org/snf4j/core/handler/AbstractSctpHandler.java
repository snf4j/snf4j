/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * -----------------------------------------------------------------------------
 */
package org.snf4j.core.handler;

import java.nio.ByteBuffer;

import org.snf4j.core.session.DefaultSctpSessionConfig;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;

/**
 * Base implementation of the {@link ISctpHandler} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractSctpHandler extends AbstractHandler implements ISctpHandler {
	
	/**
	 * Default constructor creating an unnamed SCTP handler.
	 */
	protected AbstractSctpHandler() {
		super(new DefaultSctpSessionConfig());
	}
	
	/**
	 * Constructor creating a named SCTP handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractSctpHandler(String name) {
		super(name, new DefaultSctpSessionConfig());
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
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * It does nothing by default.
	 */
	@Override
	public void read(Object msg) {
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * It returns {@code HandlerResult.CONTINUE} by default.
	 */
	@Override
	public HandlerResult notification(Notification notification, SctpNotificationType type) {
		return HandlerResult.CONTINUE;
	}

}
