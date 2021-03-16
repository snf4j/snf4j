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

import org.snf4j.core.ISctpReader;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;

/**
 * Extends the {@link IHandler} interface to cover the SCTP functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpHandler extends IHandler, ISctpReader {
	
	/**
	 * Set the SCTP session that will be associated with this handler.
	 * 
	 * @param session
	 *            the SCTP session
	 * @throws IllegalArgumentException
	 *             when the session argument is not an instance of the
	 *             {@link ISctpSession} interface.
	 */
	@Override
	void setSession(ISession session);

	/**
	 * Returns the SCTP session that is associated with this
	 * handler.
	 * 
	 * @return the SCTP session
	 */
	@Override
	ISctpSession getSession();
	
	/**
	 * Returns the configuration object that will be used to configure the
	 * behavior of the associated SCTP session.
	 * 
	 * @return the configuration object
	 * @see ISctpSessionConfig
	 */
	@Override
	ISctpSessionConfig getConfig();
	
	/**
	 * Not used.
	 */
	@Override
	void read(byte[] data);
	
	/**
	 * Not used.
	 */
	@Override
	void read(ByteBuffer data);
	
	/**
	 * Not used.
	 */
	@Override
	void read(Object msg);
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is also called when the associated session is configured with a
	 * codec pipeline in which the last decoder produces outbound object(s) of the
	 * {@code byte[]} type.
	 */
	@Override
	void read(byte[] msg, MessageInfo msgInfo);

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is also called when the associated session is configured with a
	 * codec pipeline in which the last decoder produces outbound object(s) of the
	 * {@link ByteBuffer} type.
	 */
	void read(ByteBuffer msg, MessageInfo msgInfo);
	
	/**
	 * Called when a new message was received and decoded from the input buffer.
	 * This method is called when the associated session is configured with a codec
	 * pipeline in which the last decoder produces outbound object(s) of type
	 * different than the {@code byte[]} and {@link ByteBuffer}.
	 * 
	 * @param msg     the message that was received and decoded from the input
	 *                buffer.
	 * @param msgInfo additional ancillary information about the received message.
	 */
	void read(Object msg, MessageInfo msgInfo);

	/**
	 * Handles notifications from the SCTP stack.
	 * 
	 * @param notification a notification from the SCTP stack
	 * @param type         the type of received notification
	 * @return the handler result
	 */
	HandlerResult notification(Notification notification, SctpNotificationType type);
}
