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
import static org.snf4j.core.engine.HandshakeStatus.FINISHED;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NOT_HANDSHAKING;
import static org.snf4j.core.engine.Status.CLOSED;
import static org.snf4j.core.engine.Status.OK;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
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

	SSLEngine sslClient() throws Exception {
		SSLContextBuilder builder = SSLContextBuilder.forClient();
		builder.protocol("TLSv1.3");
		builder.trustManager(cert("rsasha256"));
		SSLContext ctx = builder.build();
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forClient(ctx);
		return engineBuilder.build("snf4j.org", 100);
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

	static void assertResult(SSLEngineResult result, Status s, HandshakeStatus hs) {
		assertEquals(s.name(), result.getStatus().name());
		assertEquals(hs.name(), result.getHandshakeStatus().name());
	}

	static void assertEngine(IEngine engine, HandshakeStatus hs) {
		assertSame(hs, engine.getHandshakeStatus());
		assertFalse(engine.isInboundDone());
		assertFalse(engine.isOutboundDone());
	}
	
	static void assertEngine(SSLEngine engine, HandshakeStatus hs) {
		assertEquals(hs.name(), engine.getHandshakeStatus().name());
		assertFalse(engine.isInboundDone());
		assertFalse(engine.isOutboundDone());
	}

	static void assertEngine(SSLEngine engine, HandshakeStatus hs, boolean inDone, boolean outDone) {
		assertEquals(hs.name(), engine.getHandshakeStatus().name());
		assertEquals(inDone, engine.isInboundDone());
		assertEquals(outDone, engine.isOutboundDone());
	}

	static void assertEngine(IEngine engine, HandshakeStatus hs, boolean inDone, boolean outDone) {
		assertSame(hs, engine.getHandshakeStatus());
		assertEquals(inDone, engine.isInboundDone());
		assertEquals(outDone, engine.isOutboundDone());
	}
	
	static void runTasks(SSLEngine engine) {
		for(;;) {
			Runnable task = engine.getDelegatedTask();
			if (task == null) {
				break;
			}
			task.run();
		}
	}
	
	@Test
	public void testClient() throws Exception {
		Assume.assumeTrue(JAVA11);

		System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
		
		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParameters(DelegatedTaskMode.CERTIFICATES), handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		ssl.unwrap(in, out);
		runTasks(ssl);

		//ServerHello ->
		clear();
		ssl.wrap(in, out);

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//EncryptedExtensions... ->
		clear();
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_UNWRAP);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 0, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, 0);
		
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, 0);
		
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//closing
		ssl.closeOutbound();
		assertEngine(ssl, NEED_WRAP, false, false);
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, false, true);
		
		assertEngine(tls, NOT_HANDSHAKING, false, false);
		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NEED_WRAP, in.position(), 0);
		assertEngine(tls, NEED_WRAP, true, false);
		assertInOut(in.limit(), 0);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(0, out.position());

		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);
	}

	@Test
	public void testServer() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParameters(DelegatedTaskMode.CERTIFICATES), handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, NEED_WRAP);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, 0);
		
		//ServerHello ->
		task.run();
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		
		//EncryptedExtension... ->
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, FINISHED, in.position()-6, 0);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.position(), 0);
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		clear();
		assertResult(tls.wrap(array(data,0,4,2), out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, 32);

		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//closing
		tls.closeOutbound();
		assertEngine(tls, NEED_WRAP, false, false);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP, false, true);
		assertInOut(0, out.position());
		
		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NEED_WRAP);
		assertEngine(ssl, NEED_WRAP, true, false);
		
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);

		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NOT_HANDSHAKING, in.limit(), 0);
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(in.limit(), 0);
	}	
}
