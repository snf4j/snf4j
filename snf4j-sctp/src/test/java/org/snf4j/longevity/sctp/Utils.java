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
package org.snf4j.longevity.sctp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.snf4j.core.SctpRegistrator;
import org.snf4j.core.SctpSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.pool.DefaultSelectorLoopPool;
import org.snf4j.core.session.ISession;
import org.snf4j.longevity.Packet;
import org.snf4j.longevity.PacketType;
import org.snf4j.longevity.Statistics;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;


public class Utils implements Config {
	
	public static final Random RANDOM = new Random(System.currentTimeMillis());

	static List<SelectorLoop> loops = new ArrayList<SelectorLoop>();
	
	static List<Integer> freePorts = new ArrayList<Integer>();
	
	static List<Integer> ports = new ArrayList<Integer>();
	
	static List<ISession> sessions = new ArrayList<ISession>();
	
	static {
		for (int i=0; i<100; i++) {
			freePorts.add(i + FIRST_PORT);
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
		System.out.println("Loops started");
	}
	
	public static boolean randomBoolean(int ratio) {
		return RANDOM.nextInt(100) < ratio;
	}
	
	public static Packet randomPacket() {
		return new Packet(PacketType.ECHO, RANDOM.nextInt(MAX_PACKET_SIZE));
	}
	
	public static Packet randomNopPacket() {
		return new Packet(PacketType.NOP, RANDOM.nextInt(MAX_PACKET_SIZE));
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
	
	public static IFuture<Void> createListener() throws Exception {
		synchronized (freePorts) {
			int port = freePorts.remove(0);
			
			SctpServerChannel channel = SctpServerChannel.open();
			channel.configureBlocking(false);
			channel.bind(new InetSocketAddress(port));
			
			ports.add(port);
			
			SelectorLoop loop = loops.get(RANDOM.nextInt(loops.size()));
			return SctpRegistrator.register(loop, channel, new SessionFactory());
		}
	}
	
	public static ISession createSession() throws Exception {
		synchronized (freePorts) {
			int port = ports.get(RANDOM.nextInt(ports.size()));
			
			if (randomBoolean(NO_CONNECTION_RATIO)) {
				port = FIRST_PORT - 1;
			}
			
			SctpChannel channel = SctpChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), port));
			
			SctpSession s = new SctpSession(new ClientHandler());
			
			if (port < FIRST_PORT) {
				s.getAttributes().put(ClientHandler.NO_CONNECTION_KEY, "");
			}
			
			SelectorLoop loop = loops.get(RANDOM.nextInt(loops.size()));
			SctpRegistrator.register(loop, channel, s);
			return s;
		}
	}
	
	static void sessionCreated(ISession s) {
		synchronized (sessions) {
			findLongestSession();
			sessions.add(s);
			Statistics.updateSessions(sessions.size());
			Statistics.incTotalSessions();
			if (sessions.size() < MAX_SESSIONS) {
				try {
					Utils.createSession();
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
					Utils.createSession();
				} catch (Exception e) {
				}
			}
		}
	}
}
