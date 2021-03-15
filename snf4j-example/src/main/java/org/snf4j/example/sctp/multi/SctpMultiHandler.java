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
package org.snf4j.example.sctp.multi;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import org.snf4j.core.ImmutableSctpMessageInfo;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.allocator.ThreadLocalCachingAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractSctpHandler;
import org.snf4j.core.handler.SctpNotificationType;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSctpSessionConfig;
import org.snf4j.core.session.ISctpMultiSession;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

class SctpMultiHandler extends AbstractSctpHandler {
	
	final static IByteBufferAllocator ALLOCATOR = new ThreadLocalCachingAllocator(true);

	final private AssociationManager associations;
	
	SctpMultiHandler(SocketAddress... peers) {
		associations = new AssociationManager(this, Server.MAX_COUNT, peers);
	}
	
	@Override
	public ISctpMultiSession getSession() {
		return (ISctpMultiSession) super.getSession();
	}
	
	ImmutableSctpMessageInfo immutableMsgInfo(MessageInfo msgInfo) {
		return ImmutableSctpMessageInfo.create(msgInfo.association(), msgInfo.streamNumber());
	}
	
	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		AssociationContext ctx = associations.getContext(msgInfo.association());
		
		if (!ctx.isBlocked()) {
			if (ctx.incCounter().isDone()) {
				log("sending to " + ctx.peer + ": 100%");
				log("shutting down " + ctx.peer);
				getSession().shutdown(msgInfo.association());
				return;
			}
			else if (ctx.updateProgress()) {
				log("sending to " + ctx.peer + ": " + ctx.getProgress() + "%");
			}
		}
		getSession().writenf(msg, immutableMsgInfo(msgInfo));
	}
	
	ByteBuffer initialMsg() {
		ByteBuffer msg = getSession().allocate(Server.SIZE);
		
		return msg;
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			log("open " + addresses(getSession().getLocalAddresses()));
			for (int i=0; i<associations.contexts.length; ++i) {
				AssociationContext ctx = associations.contexts[i];
				
				log("sending to " + ctx.peer);
				getSession().writenf(initialMsg(), 
					ImmutableSctpMessageInfo.create(ctx.peer, 1)
				);
			}
			break;
		}
	}

	String addresses(Set<SocketAddress> addresses) {
		StringBuilder sb = new StringBuilder();
		
		for (SocketAddress address: addresses) {
			sb.append(address);
			sb.append(';');
		}
		return sb.toString();
	}
	
	String prefix(Notification n) {
		Association a = n.association();
		
		if (a != null) {
			return "association=" + a.associationID()+ " remote=" + 
					addresses(((ISctpMultiSession)getSession()).getRemoteAddresses(a)) + " ";
		}
		return "association=n/a ";
	}
	
	@SuppressWarnings("incomplete-switch")
	void notification(AssociationChangeNotification n) {
		log(prefix(n) + "association_change(" + n.event().name() + ")");
		switch (n.event()) {
		case COMM_UP:
			associations.getContext(n.association()).resetCounter();
			break;
			
		case SHUTDOWN:
			associations.getContext(n.association()).block();
			break;
		}
	}
	
	void notification(SendFailedNotification n) {
		log(prefix(n) + "send_failed(to " + n.address() + ")");
		associations.getContext(n.address()).block();
	}
	
	void notification(ShutdownNotification n) {
		log(prefix(n) + "shutdown");
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public HandlerResult notification(Notification notification, SctpNotificationType type) {
		switch (type) {
		case ASSOCIATION_CHANGE:
			notification((AssociationChangeNotification)notification);
			break;
			
		case SEND_FAILED:
			notification((SendFailedNotification)notification);
			break;
			
		case SHUTDOWN:
			notification((ShutdownNotification)notification);
			break;
			
		}
		return super.notification(notification, type);
	}
	
	void log(String msg) {
		System.out.println("[INFO] " +msg);
	}
	
	@Override
	public void exception(Throwable t) {
		System.err.println("[ERROR] " + t);
	}
	
	@Override
	public ISctpSessionConfig getConfig() {
		return (ISctpSessionConfig) new DefaultSctpSessionConfig()
				.setOptimizeDataCopying(true);
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public IByteBufferAllocator getAllocator() {
				return ALLOCATOR;
			}
		};
	}
}
