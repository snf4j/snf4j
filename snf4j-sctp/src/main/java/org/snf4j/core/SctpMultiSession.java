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
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.snf4j.core.future.CancelledFuture;
import org.snf4j.core.future.FailedFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SuccessfulFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SctpNotificationType;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISctpMultiSession;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SctpMultiChannel;

/**
 * The Stream Control Transmission Protocol (SCTP) multi-session.
 * <p>
 * It uses the message-oriented connected SCTP socket as described in the IETF
 * RFC 4960 "Stream Control Transmission Protocol".
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SctpMultiSession extends InternalSctpSession implements ISctpMultiSession {

	private final static ILogger LOGGER = LoggerFactory.getLogger(SctpMultiSession.class);
	
	private Set<Association> shutdowns;
	
	private Set<Association> pendingShutdowns;
	
	SctpMultiSession(String name, ISctpHandler handler) {
		super(name, handler, LOGGER);
	}
	
	SctpMultiSession(ISctpHandler handler) {
		this(null, handler);
	}
	
	@Override
	public ISctpMultiSession getParent() {
		return null;
	}
	
	@Override
	public IFuture<Void> shutdown(final Association association) {
		InternalSelectorLoop loop = this.loop;
		final SelectableChannel channel = this.channel;
		
		if (loop == null || channel == null) {
			return new CancelledFuture<Void>(this);
		}
		if (loop.inLoop()) {
			try {
				((SctpMultiChannel)channel).shutdown(association);		
			}
			catch (Throwable t) {
				return new FailedFuture<Void>(this,t);				
			}
			return new SuccessfulFuture<Void>(this);
		}
		
		final TaskFuture<Void> future = new TaskFuture<Void>(this);
		
		loop.executenf(new Runnable() {

			@Override
			public void run() {
				try {
					((SctpMultiChannel)channel).shutdown(association);
					future.success();
				}
				catch (Throwable t) {
					future.abort(t);
				}
			}
		});
		return future;
	}

	@Override
	public Association getAssociation() {
		Iterator<Association> i = getAssociations().iterator();
		
		if (i.hasNext()) {
			return i.next();
		}
		return null;
	}
	
	@Override
	public Set<Association> getAssociations() {
		SelectableChannel channel = this.channel;
		
		if (channel != null && channel.isOpen()) {
			try {
				return ((SctpMultiChannel)channel).associations();
			}
			catch (IOException e) {
				//Ignore
			}
		}
		return Collections.emptySet();
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	HandlerResult notification(Notification notification, SctpNotificationType type) {
		if (type == SctpNotificationType.ASSOCIATION_CHANGE) {
			switch (((AssociationChangeNotification)notification).event()) {
			case SHUTDOWN:
				if (shutdowns != null) {
					shutdowns.remove(notification.association());
				}
				break;
			
			case COMM_UP:
				if (shutdowns != null) {
					if (pendingShutdowns == null) {
						pendingShutdowns = new HashSet<Association>();
					}
					pendingShutdowns.add(notification.association());
				}
				break;
			}
			if (((AssociationChangeNotification)notification).event() == AssocChangeEvent.SHUTDOWN) {
			}
		}
		return super.notification(notification, type);
	}
	
	@Override
	boolean closeNow() {
		if (shutdowns != null) {
			if (pendingShutdowns != null) {
				shutdown(pendingShutdowns);
				pendingShutdowns.clear();
			}
			return shutdowns.isEmpty();
		}
		return false;
	}
	
	/** Always run in the loop's thread */
	void shutdown(Set<Association> associations) {
		SctpMultiChannel smc = (SctpMultiChannel) channel;
		
		shutdowns = new HashSet<Association>();
		try {
			if (associations == null) {
				associations = smc.associations();
			}
			for (Association association :associations) {
				smc.shutdown(association);
				shutdowns.add(association);
			}
		}
		catch (Exception e) {
			elogger.error(logger, "Shutting down of ssociations failed for {}: {}", this, e);
		}
	}
	
	boolean shutdown(SctpMultiChannel channel) throws Exception {
		InternalSelectorLoop loop = this.loop;
		Set<Association> associations;
		
		if (loop == null || (associations = channel.associations()).isEmpty()) {
			return false;
		}
		
		if (loop.inLoop()) {
			shutdown(associations);
		}
		else {
			loop.executenf(new Runnable() {

				@Override
				public void run() {
					shutdown((Set<Association>)null);
				}
			});
		}
		return true;
	}
	
	@Override
	public SocketAddress getRemoteAddress() {
		Iterator<Association> i = getAssociations().iterator();
		
		while (i.hasNext()) {
			Iterator<SocketAddress> i2 = getRemoteAddresses(i.next()).iterator();
			
			if (i2.hasNext()) {
				return i2.next();
			}
		}
		return null;
	}
	
	@Override
	public Set<SocketAddress> getRemoteAddresses() {
		Iterator<Association> i = getAssociations().iterator();
		
		if (i.hasNext()) {
			Set<SocketAddress> addresses = new HashSet<SocketAddress>();
			
			do {
				addresses.addAll(getRemoteAddresses(i.next()));
			}
			while (i.hasNext());
			return addresses;
		}
		return Collections.emptySet();
	}
	
	@Override
	public Set<SocketAddress> getRemoteAddresses(Association association) {
		if (association == null) throw new IllegalArgumentException("association is null");
		return getAddresses(association, false);
	}
	
	@Override
	Set<SocketAddress> getAddresses(Association association, boolean local) {
		SelectableChannel channel = this.channel;
		
		if (channel instanceof SctpMultiChannel && channel.isOpen()) {
			try {
				if (local) {
					return ((SctpMultiChannel)channel).getAllLocalAddresses();
				}
				else {
					return ((SctpMultiChannel)channel).getRemoteAddresses(association);
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return Collections.emptySet();
	}

	@Override
	void bind(InetAddress address) throws IOException {
		((SctpMultiChannel) channel).bindAddress(address);		
	}

	@Override
	void unbind(InetAddress address) throws IOException {
		((SctpMultiChannel) channel).unbindAddress(address);		
	}

	private final void checkConfig() {
		if (!defaultPeerAddress) throw new IllegalStateException("default peer address is not configured");
	}
	
	@Override
	IFuture<Void> writeFuture(long expectedLen) {
		if (expectedLen == -1) {
			return futuresController.getCancelledFuture();
		}
		return futuresController.getAbortableWriteFuture(expectedLen);
	}
	
	@Override
	public IFuture<Void> write(byte[] msg) {
		checkConfig();
		return super.write(msg, null);
	}
	
	@Override
	public IFuture<Void> write(byte[] msg, int offset, int length) {
		checkConfig();
		return super.write(msg, offset, length, null);
	}

	@Override
	public void writenf(byte[] msg) {
		checkConfig();
		super.writenf(msg, null);
	}
	
	@Override
	public void writenf(byte[] msg, int offset, int length) {
		checkConfig();
		super.writenf(msg, offset, length, null);
	}

	@Override
	public IFuture<Void> write(ByteBuffer msg) {
		checkConfig();
		return super.write(msg, null);
	}

	@Override
	public IFuture<Void> write(ByteBuffer msg, int length) {
		checkConfig();
		return super.write(msg, length, null);
	}

	@Override
	public void writenf(ByteBuffer msg) {
		checkConfig();
		super.writenf(msg, null);
	}
	
	@Override
	public void writenf(ByteBuffer msg, int length) {
		checkConfig();
		super.writenf(msg, length, null);
	}

	@Override
	public IFuture<Void> write(Object msg) {
		checkConfig();
		return super.write(msg, null);
	}

	@Override
	public void writenf(Object msg) {
		checkConfig();
		super.writenf(msg, null);
	}
	
}
