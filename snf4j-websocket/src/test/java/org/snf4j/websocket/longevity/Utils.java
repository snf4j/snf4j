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
package org.snf4j.websocket.longevity;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Base64;
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
import org.snf4j.websocket.SSLWebSocketSession;
import org.snf4j.websocket.WebSocketSession;
import org.snf4j.websocket.frame.BinaryFrame;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.TextFrame;

public class Utils implements Config {
	
	
	static List<SelectorLoop> loops = new ArrayList<SelectorLoop>();
	
	static List<Integer> freePorts = new ArrayList<Integer>();
	
	static List<Integer> ports = new ArrayList<Integer>();

	static List<Integer> sslPorts = new ArrayList<Integer>();
	
	public static Random random = new Random(System.currentTimeMillis());
	
	static List<ISession> sessions = new ArrayList<ISession>();
	
	static ExecutorService pool = Executors.newFixedThreadPool(10);
	
	static {
		for (int i=0; i<100; ++i) {
			freePorts.add(i+FIRST_PORT);
		}
		
		for (int i=0; i<10; ++i) {
			try {
				SelectorLoop loop = new SelectorLoop();

				loop.setPool(new DefaultSelectorLoopPool(5));
				loop.start();
				loops.add(loop);
			}
			catch (Exception e) {
				System.out.println("ERR: " + e.getMessage());
			}
		}
		System.out.println("Loops stated");
	}
	
	public static boolean randomBoolean(int ratio) {
		int i = random.nextInt(100);
		
		return i < ratio;
	}
	
	static Packet randomPacket() {
		return new Packet(PacketType.ECHO, random.nextInt(MAX_PACKET_SIZE));
	}
	
	static Frame randomFrame() {
		if (Utils.randomBoolean(Utils.BINARY_FRAME_RATIO)) {
			return new BinaryFrame(randomPacket().getBytes());
		}
		return new TextFrame(Base64.getEncoder().encodeToString(randomPacket().getBytes()));
	}
	
	static Packet randomNopPacket() {
		return new Packet(PacketType.NOP, random.nextInt(MAX_PACKET_SIZE));
	}

	static Frame randomNopFrame() {
		return new BinaryFrame(randomNopPacket().getBytes());
	}
	
	static void printPorts() {
		System.out.print("SSL ports: ");
		for (Integer port: sslPorts) {
			System.out.print(port);
			System.out.print(" ");
		}
		System.out.print(", ports: ");
		for (Integer port: ports) {
			System.out.print(port);
			System.out.print(" ");
		}
		System.out.println("");
	}
	
	static void configPorts() {
		for (int port: SSL_PORTS) {
			freePorts.remove(0);
			Utils.sslPorts.add(port);
		}
		for (int port: PORTS) {
			freePorts.remove(0);
			Utils.ports.add(port);
		}
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
			
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), port));
			
			StreamSession s;

			if (ssl) {
				s = new SSLWebSocketSession(new ClientHandler(new URI("wss://" + HOST + ":" + port + "/")), true);
			}
			else {
				s = new WebSocketSession(new ClientHandler(new URI("ws://" + HOST + ":" + port + "/")), true);
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
