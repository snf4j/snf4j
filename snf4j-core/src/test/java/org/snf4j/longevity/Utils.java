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
package org.snf4j.longevity;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.snf4j.core.SSLSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.StreamSession;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.session.ISession;

public class Utils implements Config {
	
	
	static List<SelectorLoop> loops = new ArrayList<SelectorLoop>();
	
	static List<Integer> freePorts = new ArrayList<Integer>();
	
	static List<Integer> ports = new ArrayList<Integer>();

	static List<Integer> sslPorts = new ArrayList<Integer>();
	
	static Random random = new Random(System.currentTimeMillis());
	
	static List<ISession> sessions = new ArrayList<ISession>();
	
	static ExecutorService pool = Executors.newFixedThreadPool(10);
	
	static {
		for (int i=0; i<100; ++i) {
			freePorts.add(i+FIRST_PORT);
		}
		
		for (int i=0; i<10; ++i) {
			try {
				SelectorLoop loop = new SelectorLoop();

				if (i % 2 == 0) {
					loop.setPool(new DefaultSelectorLoopPool(5));
				}
				loop.start();
				loops.add(loop);
			}
			catch (Exception e) {
				System.out.println("ERR: " + e.getMessage());
			}
		}
		System.out.println("Loops stated");
	}
	
	static boolean randomBoolean(int ratio) {
		int i = random.nextInt(100);
		
		return i < ratio;
	}
	
	static Packet randomPacket() {
		return new Packet(PacketType.ECHO, random.nextInt(MAX_PACKET_SIZE));
	}
	
	static Packet randomNopPacket() {
		return new Packet(PacketType.NOP, random.nextInt(MAX_PACKET_SIZE));
	}
	
	static IFuture<Void> createListener(boolean ssl) throws Exception {
		synchronized (freePorts) {
			int port = freePorts.remove(0);

			ServerSocketChannel channel = ServerSocketChannel.open();
			channel.configureBlocking(false);
			channel.socket().bind(new InetSocketAddress(port));
			
			if (ssl) {
				sslPorts.add(port);
			}
			else {
				ports.add(port);
			}
			
			SelectorLoop loop = loops.get(random.nextInt(loops.size()));
			return loop.register(channel, new SessionFactory(ssl));
		}
	}
	
	static ISession createSession(boolean ssl) throws Exception {
		synchronized (freePorts) {
			int port;
			
			if (ssl) {
				port = sslPorts.get(random.nextInt(sslPorts.size()));
			}
			else {
				port = ports.get(random.nextInt(ports.size()));
			}
			
			if (randomBoolean(NO_CONNECTION_RATIO)) {
				port = FIRST_PORT-1;
			}
			
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), port));
			
			StreamSession s;

			if (ssl) {
				s = new SSLSession(new ClientHandler(), true);
				if (!randomBoolean(DEFAULT_EXECUTOR_RATIO)) {
					((SSLSession)s).setExecutor(pool);
				}
			}
			else {
				s = new StreamSession(new ClientHandler());
			}
			if (port < FIRST_PORT) {
				s.getAttributes().put(ClientHandler.NO_CONNECTION_KEY, "");
			}
			
			SelectorLoop loop = loops.get(random.nextInt(loops.size()));
			loop.register(channel, s);
			return s;
		}
	}
	
	static void findLongestSession() {
		long time = System.currentTimeMillis();
		long max = 0;
		
		for (ISession s: sessions) {
			long m = time - s.getCreationTime();
			
			if (m > max) {
				max = m;
			}
		}
		Statistics.updateLongestSession(max);
	}
	
	static void sessionCreated(ISession s) {
		synchronized (sessions) {
			findLongestSession();
			sessions.add(s);
			Statistics.updateSessions(sessions.size());
			Statistics.incTotalSessions();
			if (s instanceof SSLSession) {
				Statistics.incSslSessions();
			}
			if (sessions.size() < MAX_SESSIONS) {
				try {
					Utils.createSession(Utils.randomBoolean(Utils.SSL_SESSION_RATIO));
				} catch (Exception e) {
				}
			}
		}
	}
	
	static void sessionEnding(ISession s) {
		synchronized (sessions) {
			sessions.remove(s);
			findLongestSession();
			Statistics.updateSessions(sessions.size());
			Statistics.sessionEnding(s);
			if (sessions.size() < MAX_SESSIONS) {
				try {
					Utils.createSession(Utils.randomBoolean(Utils.SSL_SESSION_RATIO));
				} catch (Exception e) {
				}
			}
		}
	}
}
