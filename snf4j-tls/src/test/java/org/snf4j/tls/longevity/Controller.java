/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.longevity;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.session.ISession;
import org.snf4j.tls.TLSSession;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandler;
import org.snf4j.tls.engine.EngineParametersBuilder;

public class Controller implements Config {
	
	static final Object portLock = new Object();
	
	static final List<Integer> freePorts = new ArrayList<Integer>();
	
	static final List<Integer> rawPorts = new ArrayList<Integer>();
	
	static final List<Integer> sslPorts = new ArrayList<Integer>();
	
	static final List<SelectorLoop> clientLoops = new ArrayList<SelectorLoop>();
	
	static final List<SelectorLoop> serverLoops = new ArrayList<SelectorLoop>();
	
	public final static Random random = new Random(System.currentTimeMillis());
	
	static int clients;
	
	static final List<ISession> sessions = new ArrayList<ISession>();
	
	static ISession longestSession = null;

	static {
		for (int i=0; i<LISTENER_COUNT; ++i) {
			freePorts.add(FIRST_LISTENING_PORT + i);
		}
		
		for (int i=0; i<CLIENT_LOOP_COUNT; ++i) {
			try {
				SelectorLoop loop = new SelectorLoop("cli-"+(i+1));
				
				loop.start();
				clientLoops.add(loop);
			}
			catch (Exception e) {
				error(e.toString());
			}
		}
		
		for (int i=0; i<SERVER_LOOP_COUNT; ++i) {
			try {
				SelectorLoop loop = new SelectorLoop("srv-" + (i+1));
				
				if (SERVER_LOOP_POOL_SIZE > 0) {
					loop.setPool(new DefaultSelectorLoopPool(SERVER_LOOP_POOL_SIZE));
				}
				loop.start();
				serverLoops.add(loop);
			}
			catch (Exception e) {
				error(e.toString());
			}
		}
	}
	
	static void start() throws Exception {
		for (int i=0; i<LISTENER_COUNT; ++i) {
			createListener(SSL).sync();
		}
		createSession(SSL);
	}
	
	static IFuture<Void> createListener(boolean ssl) throws Exception {
		synchronized (portLock) {
			int port = freePorts.remove(0);

			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(port));
			
			if (ssl) {
				sslPorts.add(port);
			}
			else {
				rawPorts.add(port);
			}
			
			int i = LISTENER_COUNT - freePorts.size();
			SelectorLoop loop = serverLoops.get(i % serverLoops.size());
			
			return loop.register(channel, new SessionFactory(ssl));
		}
	}
	
	static ISession createSession(boolean ssl) throws Exception {
		int port;
		
		synchronized (portLock) {
			if (ssl) {
				port = sslPorts.get(random.nextInt(sslPorts.size()));
			}
			else {
				port = rawPorts.get(random.nextInt(rawPorts.size()));
			}
		}
		
		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), port));

		StreamSession s;
		
		if (ssl) {
			EngineParametersBuilder builder = new EngineParametersBuilder()
					.delegatedTaskMode(DelegatedTaskMode.ALL)
					.compatibilityMode(false);

			s = new TLSSession(
					builder.build(), 
					new EngineHandler(SessionConfig.km,SessionConfig.tm), 
					new ClientHandler(), 
					true);
		}
		else {
			s = new StreamSession(new ClientHandler());
		}
		
		SelectorLoop loop = clientLoops.get(random.nextInt(clientLoops.size()));
		loop.register(channel, s);
		return s;
	}
	
	static void sessionCreated(ISession s, boolean client) {
		boolean create;
		
		synchronized (sessions) {
			if (client) {
				clients++;
			}
			if (longestSession == null || longestSession.getCreationTime() > s.getCreationTime()) {
				longestSession = s;
				Metric.longestSession(s);
			}
			sessions.add(s);
			Metric.sessionCreated(s);
			create = client && clients < MAX_SESSIONS/2;
		}
		if (create) {
			try {
				Controller.createSession(SSL);
			}
			catch (Exception e) {
				Controller.error(e.toString());
			}
		}
	}
	
	static void sessionEnding(ISession s, boolean client) {
		boolean create;
		
		synchronized (sessions) {
			if (client) {
				clients--;
			}
			sessions.remove(s);
			if (longestSession == s) {
				long longest = Long.MAX_VALUE;
				int count = Math.min(sessions.size(), 20);
				
				for (int i=0; i<count; ++i) {
					long t = sessions.get(i).getCreationTime();
					
					if (t < longest) {
						longestSession = sessions.get(i);
						longest = t;
					}
				}
				Metric.longestSession(longestSession);
			}
			Metric.sessionEnding(s);
			create = client && clients < MAX_SESSIONS/2;
		}
		if (create) {
			try {
				Controller.createSession(SSL);
			}
			catch (Exception e) {
				Controller.error(e.toString());
			}
		}
	}
	
	static void error(String s) {
		System.err.println("[ERR] " + s);
	}
}
