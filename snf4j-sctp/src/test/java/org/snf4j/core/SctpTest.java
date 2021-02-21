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

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assume;
import org.snf4j.core.InternalSctpSession.SctpRecord;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodec;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;

import com.sun.nio.sctp.SctpChannel;

public class SctpTest {
	
	long TIMEOUT = 2000;
	
	int PORT = 7779;
	
	SctpServer s;
	
	SctpClient c;

	TestAllocator allocator;
	
	public final static boolean SUPPORTED;
	
	StringBuilder trace = new StringBuilder();
	
	static {
		boolean supported;
		
		try {
			SctpChannel.open();
			supported = true;
		}
		catch (Exception e) {
			supported = false;
		}
		SUPPORTED = supported;
	}
	
	public static void assumeSupported() {
		Assume.assumeTrue(SUPPORTED);
	}
	
	public static boolean isOptimized(SctpSession session) {
		return session.optimizeBuffers && session.optimizeCopying;
	}
	
	@After
	public void after() throws Exception {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
	}

	void trace(String s) {
		synchronized (trace) {
			trace.append(s);
			trace.append('|');
		}
	}
	
	String getTrace() {
		String s;
		
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	void startClientServer() throws Exception {
		s = new SctpServer(PORT);
		c = new SctpClient(PORT);
		s.start();
		c.start();
		c.waitForSessionReady(TIMEOUT);
		s.waitForSessionReady(TIMEOUT);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
	}
	
	void waitFor(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
		}
	}
	
	void waitForReady(long millis) throws Exception {
		c.waitForSessionReady(millis);
		s.waitForSessionReady(millis);
		assertEquals("SCR|SOP|RDY|", c.getTrace());
		assertEquals("SCR|SOP|RDY|", s.getTrace());
	}
	
	void stopClientAndClearTraces(long millis) throws Exception {
		c.stop(millis);
		c.waitForSessionEnding(millis);
		s.waitForSessionEnding(millis);
		clearTraces();
	}
	
	void clearTraces() {
		c.getTrace();
		s.getTrace();
	}
	
	Packet nop(String payload) {
		return new Packet(PacketType.NOP, payload);
	}
	
	byte[] nopb(String payload) {
		return nop(payload).toBytes();
	}
	
	byte[] nopb(String payload, int off, int padding) {
		return nop(payload).toBytes(off, padding);
	}
	
	ByteBuffer nopbb;
	
	ByteBuffer nopbb(String payload) {
		return nopbb = ByteBuffer.wrap(nopb(payload));
	}

	ByteBuffer nopbb(String payload, int padding) {
		return nopbb = ByteBuffer.wrap(nopb(payload,0,padding));
	}
	
	ByteBuffer nopbba(String payload) {
		byte[] b = nopb(payload);
		
		nopbb = allocator.allocate(b.length);
		nopbb.put(b).flip();
		return nopbb;
	}
	
	Packet nop(String head, String tail, char mid, int midLength) {
		char[] payload = new char[tail.length() + tail.length() + midLength];
		
		Arrays.fill(payload, head.length(), head.length() + midLength, mid);
		System.arraycopy(head.toCharArray(), 0, payload, 0, head.length());
		System.arraycopy(tail.toCharArray(), 0, payload, payload.length - tail.length(), tail.length());
		return nop(new String(payload));
	}
	
	byte[] nopb(String head, String tail, char mid, int midLength) {
		return nop(head, tail, mid, midLength).toBytes();
	}
	
	ImmutableSctpMessageInfo info(int streamNumber) {
		return ImmutableSctpMessageInfo.create(streamNumber);
	}
	
	ImmutableSctpMessageInfo info(int streamNumber, int protocolID) {
		return ImmutableSctpMessageInfo.create(streamNumber, protocolID);
	}
	
	ImmutableSctpMessageInfo info(int streamNumber, int protocolID, boolean unordered) {
		return ImmutableSctpMessageInfo.create(streamNumber, protocolID, unordered);
	}

	public static int countRDNOP(String s, byte[] payload) {
		int off = 0;
		String rdnop = "DR|NOP(" + new String(payload) + ")|";
		int i;
		int count = 0;
		
		while ((i = s.indexOf(rdnop, off)) != -1) {
			off = i + rdnop.length();
			count++;
		}
		return count;
	}

	public static SelectionKey getKey(SctpServer server) {
		return server.session.channel.keyFor(server.loop.selector);
	}
	
	public void setAllocator(TestAllocator a) {
		this.allocator = a;
		prevAllocated = prevReleased = 0;
	}
	
	int prevAllocated, prevReleased;
	
	public void assertAllocator(int allocated, int released, int size) {
		waitFor(50);
		prevAllocated = allocator.getAllocatedCount();
		prevReleased = allocator.getReleasedCount();
		assertEquals("alloceded", allocated, prevAllocated);
		assertEquals("released", released, prevReleased);
		assertEquals("szie", size, allocator.getSize());
	}
	
	public void assertAllocatorDeltas(int allocated, int released, int size) {
		waitFor(50);
		int newAllocated = allocator.getAllocatedCount();
		int newReleased = allocator.getReleasedCount();
		assertEquals("alloceded", allocated, newAllocated - prevAllocated);
		assertEquals("released", released, newReleased - prevReleased);
		assertEquals("szie", size, allocator.getSize());
		prevReleased = newReleased;
		prevAllocated = newAllocated;
	}
	
	ByteBuffer allocated(int i) {
		return allocator.getAllocated().get(i);
	}
	
	ByteBuffer released(int i) {
		return allocator.getReleased().get(i);
	}
	
	public void releaseReadBuffer(SctpServer server) {
		server.allocator.release(server.readBuffer);
	}
	
	public void sleepLoop(SelectorLoop loop, final long millis) throws InterruptedException {
		final AtomicBoolean lock = new AtomicBoolean();
		
		loop.execute(new Runnable() {

			@Override
			public void run() {
				LockUtils.notify(lock);
				try {
					Thread.sleep(millis);
				} catch (InterruptedException e) {
				}
			}
		});
		LockUtils.waitFor(lock, millis);
	}
	
	InetSocketAddress address(int port) {
		return new InetSocketAddress("127.0.0.1", port);
	}
	
	InetSocketAddress address(SocketAddress address, int port) {
		return new InetSocketAddress(((InetSocketAddress)address).getAddress(), port);
	}
	
	InetSocketAddress address(InetAddress address, int port) {
		return new InetSocketAddress(address, port);
	}
	
	ByteBuffer getFragment(SctpSession session) throws Exception {
		Field f = InternalSctpSession.class.getDeclaredField("fragments");
		Field f2 = SctpFragments.class.getDeclaredField("fragment");
		
		f.setAccessible(true);
		f2.setAccessible(true);
		return (ByteBuffer) f2.get(f.get(session));
	}

	ByteBuffer getFragment(SctpServer server) throws Exception {
		return getFragment(server.session);
	}
	
	ByteBuffer getIn(SctpSession session) throws Exception {
		Field f = InternalSctpSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		return (ByteBuffer) f.get(session);
	}
	
	ByteBuffer getIn(SctpServer server) throws Exception {
		return getIn(server.session);
	}
	
	void setIn(SctpSession session, ByteBuffer in) throws Exception {
		Field f = InternalSctpSession.class.getDeclaredField("inBuffer");
		
		f.setAccessible(true);
		f.set(session, in);
	}

	void setIn(SctpServer server, ByteBuffer in) throws Exception {
		setIn(server.session, in);
	}
	
	@SuppressWarnings("unchecked")
	Queue<SctpRecord> getOut(SctpSession session) throws Exception {
		Field f = InternalSctpSession.class.getDeclaredField("outQueue");
		
		f.setAccessible(true);
		return (Queue<SctpRecord>) f.get(session);
	}
	
	Queue<SctpRecord> getOut(SctpServer server) throws Exception {
		return getOut(server.session);
	}
	
	void addCodecs(SctpServer server, ICodec<?,?>... codecs) {
		server.codecExecutor = codec(codecs);
	}
	
	void addCodecs(SctpServer server, int streamNum, int protoID, ICodec<?,?>... codecs) {
		if (server.codecExecutor == null) {
			addCodecs(server);
		}
		server.addCodec(streamNum, protoID, codec(codecs));
	}

	DefaultCodecExecutor codec(ICodec<?,?>... codecs) {
		DefaultCodecExecutor exec = new DefaultCodecExecutor();
		int d = 1, e = 1;
		
		for (ICodec<?, ?> codec: codecs) {
			if (codec instanceof IEncoder) {
				exec.getPipeline().add("E"+e++, (IEncoder<?,?>)codec);
			}
			else {
				exec.getPipeline().add("D"+d++, (IDecoder<?,?>)codec);
			}
		}
		return exec;
	}
	
}
