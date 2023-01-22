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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public class TLSEngineTest extends EngineTest {
	
	ByteBuffer in;
	
	ByteBuffer out;
	
	ByteBuffer in0;
	
	ByteBuffer out0;
	
	@Override
	public void before() throws Exception {
		super.before();
		in = ByteBuffer.allocate(100000);
		out = ByteBuffer.allocate(100000);
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	void clear() {
		in.clear();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}

	void clear(byte[] bytes) {
		in.clear();
		in.put(bytes);
		in.flip();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	void flip() {
		ByteBuffer tmp = in;
		in = out;
		out = tmp;
		in.flip();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	byte[] out() {
		ByteBuffer dup = out.duplicate();
		dup.flip();
		byte[] data = new byte[dup.remaining()];
		dup.get(data);
		return data;
	}

	SSLEngine sslServer() throws Exception {
		SSLContextBuilder builder = SSLContextBuilder.forServer(key("RSA", "rsa"), cert("rsasha256"));
		builder.protocol("TLSv1.3");
		SSLContext ctx = builder.build();
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forServer(ctx);
		return engineBuilder.build();
	}
	
	void assertInOut(int inChange, int outChange) {
		if (inChange == 0) {
			assertEquals(in, in0);
		} else if (inChange < 0) {
			assertEquals(in0.limit(), in.limit());
			assertEquals(in0.capacity(), in.capacity());
			assertTrue(in.position()-in0.position() >= -inChange);
		} else {
			assertEquals(in0.limit(), in.limit());
			assertEquals(in0.capacity(), in.capacity());
			assertEquals(inChange, in.position()-in0.position());
			
		}

		if (outChange == 0) {
			assertEquals(out, out0);
		} else if (outChange < 0) {
			assertEquals(out0.limit(), out.limit());
			assertEquals(out0.capacity(), out.capacity());
			assertTrue(out.position()-out0.position() >= -outChange);
		} else  {
			assertEquals(out0.limit(), out.limit());
			assertEquals(out0.capacity(), out.capacity());
			assertEquals(outChange, out.position()-out0.position());
		}
	}
	
	static void assertResult(IEngineResult result, Status s, HandshakeStatus hs, int consumed, int produced) {
		assertSame(s, result.getStatus());
		assertSame(hs, result.getHandshakeStatus());

		if (consumed == -1){
			assertTrue(result.bytesConsumed() > 0);
		}
		else {
			assertEquals(consumed, result.bytesConsumed());
		}
		
		if (produced == -1){
			assertTrue(result.bytesProduced() > 0);
		}
		else {
			assertEquals(produced, result.bytesProduced());
		}
	}

	@Test
	public void testClient() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParameters(DelegatedTaskMode.CERTIFICATES), handler);
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, tls.getHandshakeStatus());
		tls.beginHandshake();
		assertEquals(HandshakeStatus.NEED_WRAP, tls.getHandshakeStatus());

		//ClientHello ->
		clear();
		assertEquals(HandshakeStatus.NEED_WRAP, tls.getHandshakeStatus());
		IEngineResult r = tls.wrap(in, out);
		assertInOut(0, -1);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, 0, out.position());
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());

		//ClientHello <-
		flip();
		ssl.unwrap(in, out);
		ssl.getDelegatedTask().run();
		assertNull(ssl.getDelegatedTask());

		//ServerHello ->
		clear();
		ssl.wrap(in, out);

		//ServerHello <-
		flip();
		r = tls.unwrap(in, out);
		assertInOut(in.limit(), 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, in.position(), 0);
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		
		//EncryptedExtensions... ->
		clear();
		ssl.wrap(in, out);
		
		flip();
		r = tls.unwrap(in, out);
		assertInOut(in.limit(), 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_TASK, in.position(), 0);
		assertEquals(HandshakeStatus.NEED_TASK, tls.getHandshakeStatus());
		assertEquals(0, in.remaining());
		
		Runnable task = tls.getDelegatedTask();
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		assertNull(tls.getDelegatedTask());

		clear();
		r = tls.unwrap(in, out);
		assertInOut(0, 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		r = tls.wrap(in, out);
		assertInOut(0, 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		
		task.run();
		r = tls.unwrap(in, out);
		assertInOut(0, 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);
		assertEquals(HandshakeStatus.NEED_WRAP, tls.getHandshakeStatus());
		
		clear();
		r = tls.wrap(in, out);
		assertInOut(0, out.position());
		assertResult(r, Status.OK, HandshakeStatus.FINISHED, 0, out.position());
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, tls.getHandshakeStatus());
		
		flip();
		assertEquals("FINISHED", ssl.unwrap(in, out).getHandshakeStatus().name());
		
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		r = tls.wrap(in, out);
		assertInOut(10, 32);
		assertResult(r, Status.OK, HandshakeStatus.NOT_HANDSHAKING, 10, 32);
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, tls.getHandshakeStatus());
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		r = tls.unwrap(in, out);
		assertInOut(in.limit(), 10);
		assertResult(r, Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position(), 10);
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, tls.getHandshakeStatus());
		assertArrayEquals(data, out());
		
		ssl.closeOutbound();
		assertFalse(ssl.isOutboundDone());
		assertFalse(ssl.isInboundDone());
		clear();
		assertEquals("NOT_HANDSHAKING", ssl.wrap(in, out).getHandshakeStatus().name());
		assertTrue(ssl.isOutboundDone());
		assertFalse(ssl.isInboundDone());
		
		assertFalse(tls.isOutboundDone());
		assertFalse(tls.isInboundDone());
		flip();
		r = tls.unwrap(in, out);
		assertInOut(in.limit(), 0);
		assertResult(r, Status.OK, HandshakeStatus.NEED_WRAP, in.position(), 0);
		assertFalse(tls.isOutboundDone());
		assertTrue(tls.isInboundDone());
		
		clear();
		r = tls.wrap(in, out);
		assertInOut(0, out.position());
		assertResult(r, Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, out.position());
		assertTrue(tls.isOutboundDone());
		assertTrue(tls.isInboundDone());
		
		flip();
		assertEquals("NOT_HANDSHAKING", ssl.unwrap(in, out).getHandshakeStatus().name());
		assertTrue(ssl.isOutboundDone());
		assertTrue(ssl.isInboundDone());
	}

	@Test
	public void testServerMode() throws Exception {
		Assume.assumeTrue(JAVA11);
		SSLContextBuilder builder = SSLContextBuilder.forClient();
		
		builder.protocol("TLSv1.3");
		SSLContext ctx = builder.build();

		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forClient(ctx);
		SSLEngine engine = engineBuilder.build("example.com", 100);
		
		engine.getHandshakeStatus();
		engine.beginHandshake();

		HandshakeEngine eh = new HandshakeEngine(false, new EngineParameters(), handler, handler);
		eh.start();
		
		ByteBuffer out = ByteBuffer.allocate(100000);
		ByteBuffer in = ByteBuffer.allocate(100000);
		in.flip();
		
		engine.wrap(in, out);
		out.flip();
		assertEquals(22, out.get());
		assertEquals(0x0303, out.getShort());
		int remaining = out.getShort();
		
		eh.consume(new ByteBuffer[] {out}, remaining);
		eh.produce();

	}	
}
