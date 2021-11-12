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
package org.snf4j.core.proxy;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IStreamSession;

abstract public class AbstractSocksProxyHandler extends AbstractProxyHandler {
	
	private final InetSocketAddress address;
	
	private final List<ISocksReply> replies = new ArrayList<ISocksReply>(2);
	
	private List<ISocksReplyListener> replyListeners;
	
	volatile AbstractSocksState state;
	
	protected AbstractSocksProxyHandler(InetSocketAddress address, long connectionTimeout, ISessionConfig config, ISessionStructureFactory factory) {
		super(connectionTimeout, config, factory);
		checkNull(address, "address");
		this.address = address;
	}

	protected AbstractSocksProxyHandler(InetSocketAddress address, ISessionConfig config, ISessionStructureFactory factory) {
		super(config, factory);
		checkNull(address, "address");
		this.address = address;
	}
	
	final void checkNull(Object value, String name) {
		if (value == null) {
			throw new IllegalArgumentException(name + " is null");
		}
	}
	
	public InetSocketAddress getAddress() {
		return address;
	}
	
	public ISocksReply[] getReplies() {
		ISocksReply[] replyArray;
		
		synchronized (replies) {
			replyArray = replies.toArray(new ISocksReply[replies.size()]);
		}
		return replyArray;
	}
	
	public void addReplyListener(ISocksReplyListener listener) {
		synchronized (replies) {
			if (replyListeners == null) {
				replyListeners = new LinkedList<ISocksReplyListener>();
			}
			replyListeners.add(listener);
		}
	}
	
	int reply(ISocksReply reply) {
		ISocksReplyListener[] listeners;
		int replyIndex;
		
		synchronized (replies) {
			replies.add(reply);
			replyIndex = replies.size();
			if (replyListeners == null) {
				return replyIndex;
			}
			listeners = replyListeners.toArray(new ISocksReplyListener[replyListeners.size()]);
		}	
		for (ISocksReplyListener listener: listeners) {
			listener.replyReceived(reply, replyIndex);
		}
		return replyIndex;
	}
	
	void flipAndWrite(ByteBuffer buf) {
		IStreamSession session = getSession(); 
		
		buf.flip();
		session.writenf(buf);
		if (!session.isDataCopyingOptimized()) {
			session.release(buf);
		}
	}

	@Override
	public int available(ByteBuffer data, boolean flipped) {
		return state.available(data, flipped);
	}
	
	@Override
	public int available(byte[] data, int off, int len) {
		return state.available(data, off, len);
	}
	
	@Override
	public void read(ByteBuffer data) {
		byte[] bytes = new byte[data.remaining()];
		
		data.get(bytes);
		getSession().release(data);
		read(bytes);
	}

	@Override
	public void read(byte[] data) {
		AbstractSocksState nextState = state.read(data);
		
		if (nextState != state) {
			if (nextState == null) {
				state = new SocksDoneState(this);
				getSession().getPipeline().markDone();
				getSession().close();
			}
			else {
				state = nextState;
				state.handleReady();
			}
		}
	}

	@Override
	protected void handleReady() throws Exception {
		state.handleReady();
	}	
	
	@Override
	public void read(Object msg) {
	}

	@Override
	public void event(SessionEvent event) {
		if (event == SessionEvent.OPENED) {
			getSession().getPipeline().markUndone(new ProxyConnectionException("Incomplete " + protocol() + " proxy protocol"));
		}
		super.event(event);
	}

	abstract String protocol();
}
