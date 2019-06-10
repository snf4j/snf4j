/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class EngineServer {

	private final static ILogger LOGGER = LoggerFactory.getLogger(EngineServer.class);

	volatile EngineStreamSession session;
	final TraceBuilder trace = new TraceBuilder();
	SelectorLoop loop;
	int port;
	long timeout;
	volatile boolean directAllocator;
	volatile boolean waitForInboundCloseMessage;
	
	AtomicBoolean sessionLock = new AtomicBoolean(false);
	AtomicBoolean readLock = new AtomicBoolean(false);
	AtomicBoolean sessionEndingLock = new AtomicBoolean(false);
	AtomicBoolean sessionReadyLock = new AtomicBoolean(false);
	
	public EngineServer(int port, long timeout) {
		this.port = port;
		this.timeout = timeout;
	}
	
	public EngineStreamSession getSession() throws InterruptedException {
		if (session == null) {
			LockUtils.waitFor(sessionLock, timeout);
		}
		return session;
	}
	
	public void waitForDataRead(long millis) throws InterruptedException {
		LockUtils.waitFor(readLock, millis);
	}

	public void resetDataRead() throws InterruptedException {
		synchronized(readLock) {
			readLock.set(false);
		}
	}
	
	public void waitForSessionEnding(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionEndingLock, millis);
	}

	public void waitForSessionReady(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionReadyLock, millis);
	}

	public void waitForFinish(long millis) throws InterruptedException {
		LockUtils.waitFor(sessionEndingLock, millis);
		Thread.sleep(100);
	}
	
	public void start(final TestEngine engine) throws Exception {
		loop =  new SelectorLoop();
		
		loop.start();
		
		engine.setTrace(trace);
		
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.socket().bind(new InetSocketAddress(port));
		
		// Register the listener
		loop.register(channel, new IStreamSessionFactory() {

			@Override
			public StreamSession create(SocketChannel channel)
					throws Exception {
				return new EngineStreamSession(engine, new EngineHandler(), LOGGER);
			}

			@Override
			public void registered(ServerSocketChannel channel) {
			}

			@Override
			public void closed(ServerSocketChannel channel) {
			}
		}).sync(timeout);
	}
	
	public void stop() throws InterruptedException {
		if (loop != null) {
			loop.quickStop();
			loop.join(timeout);
			loop = null;
		}		
	}
	
	void trace(String s) {
		trace.append(s);
	}
	
	String getTrace(boolean clear) {
		return trace.get(clear);
	}
	
	class SessionStructureFactory implements ISessionStructureFactory {

		@Override
		public IByteBufferAllocator getAllocator() {
			return directAllocator ? new DefaultAllocator(true) : DefaultAllocator.DEFAULT;
			
		}

		@Override
		public ConcurrentMap<Object, Object> getAttributes() {
			return null;
		}
		
	}
	
	public class EngineHandler extends AbstractStreamHandler {

		@SuppressWarnings("incomplete-switch")
		@Override
		public void event(SessionEvent event) {
			trace(event.name().substring(0, 2));
			switch (event) {
			case OPENED:
				session = (EngineStreamSession) getSession();
				LockUtils.notify(sessionLock);
				break;
				
			case READY:
				LockUtils.notify(sessionReadyLock);
				break;
				
			case ENDING:
				LockUtils.notify(sessionEndingLock);
				break;
				
			}
		}
		
		@Override
		public void read(byte[] data) {
			trace("R" + new String(data));
			LockUtils.notify(readLock);
		}

		@Override
		public void exception(Throwable t) {
			trace("EX");
		}

		@Override
		public boolean incident(SessionIncident incident, Throwable t) {
			trace("I");
			return false;
		}
		
		@Override
		public ISessionStructureFactory getFactory() {
			return new SessionStructureFactory();
		}
		
		@Override
		public ISessionConfig getConfig() {
			return new DefaultSessionConfig()
				.setWaitForInboundCloseMessage(waitForInboundCloseMessage);
		}
	}	
}
