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
package org.snf4j.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.Set;

import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SctpNotificationType;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISctpSession;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SctpChannel;

/**
 * The Stream Control Transmission Protocol (SCTP) session.
 * <p>
 * It uses the message-oriented connected SCTP socket as described in the IETF
 * RFC 4960 "Stream Control Transmission Protocol".
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SctpSession extends InternalSctpSession implements ISctpSession {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(SctpSession.class);
	
	private boolean shutdown;
	
	/**
	 * Constructs a named SCTP session associated with a handler.
	 * 
	 * @param name
	 *            the name for this session, or <code>null</code> if the
	 *            handler's name should be used for this session's name
	 * @param handler
	 *            the handler that should be associated with this session
	 */
	public SctpSession(String name, ISctpHandler handler) {
		super(name, handler, LOGGER);
	}

	/**
	 * Constructs a SCTP session associated with a handler. 
	 * 
	 * @param handler
	 *            the handler that should be associated with this session
	 */
	public SctpSession(ISctpHandler handler) {
		this(null, handler);
	}
	
	@Override
	HandlerResult notification(Notification notification, SctpNotificationType type) {
		if (type == SctpNotificationType.ASSOCIATION_CHANGE) {
			if (((AssociationChangeNotification)notification).event() == AssocChangeEvent.SHUTDOWN) {
				shutdown = true;
			}
		}
		return super.notification(notification, type);
	}
	
	boolean markedShutdown() {
		return shutdown;
	}
	
	@Override
	public void close() {
		closeCalled.set(true);
		close(false);
	}
	
	@Override
	public Association getAssociation() {
		SelectableChannel channel = this.channel;
		
		if (channel != null && channel.isOpen()) {
			try {
				return ((SctpChannel)channel).association();
			}
			catch (IOException e) {
				//Ignore
			}
		}
		return null;
	}
	
	@Override
	Set<SocketAddress> getAddresses(Association association, boolean local) {
		SelectableChannel channel = this.channel;
		
		if (channel instanceof SctpChannel && channel.isOpen()) {
			try {
				if (local) {
					return ((SctpChannel)channel).getAllLocalAddresses();
				}
				else {
					return ((SctpChannel)channel).getRemoteAddresses();
				}
			} catch (IOException e) {
				// Ignore
			}
		}
		return Collections.emptySet();
	}
	
	@Override
	void bind(InetAddress address) throws IOException {
		((SctpChannel) channel).bindAddress(address);		
	}
	
	@Override
	void unbind(InetAddress address) throws IOException {
		((SctpChannel) channel).unbindAddress(address);
	}
	
}
