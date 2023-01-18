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

import static org.junit.Assert.assertEquals;
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
	public void testClientMode() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLContextBuilder builder = SSLContextBuilder.forServer(key("RSA", "rsa"), cert("rsasha256"));
		builder.protocol("TLSv1.3");
		SSLContext ctx = builder.build();
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forServer(ctx);
		SSLEngine ssl = engineBuilder.build();
		ssl.beginHandshake();
		ByteBuffer sslOut = ByteBuffer.allocate(100000);
		ByteBuffer sslIn = ByteBuffer.allocate(100000);
		
		TLSEngine tls = new TLSEngine(true, new EngineParameters(DelegatedTaskMode.CERTIFICATES), handler);
		tls.beginHandshake();
		ByteBuffer tlsOut = ByteBuffer.allocate(100000);
		ByteBuffer tlsIn = ByteBuffer.allocate(100000);

		//ClientHello ->
		tlsIn.flip();
		tlsOut.clear();
		assertEquals(HandshakeStatus.NEED_WRAP, tls.getHandshakeStatus());
		IEngineResult r = tls.wrap(tlsIn, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, 0, tlsOut.position());
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());

		//ClientHello <-
		tlsOut.flip();
		sslOut.clear();
		ssl.unwrap(tlsOut, sslOut);
		ssl.getDelegatedTask().run();
		assertNull(ssl.getDelegatedTask());

		//ServerHello ->
		sslIn.flip();
		sslOut.clear();
		ssl.wrap(sslIn, sslOut);

		//ServerHello <-
		sslOut.flip();
		tlsOut.clear();
		r = tls.unwrap(sslOut, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, sslOut.position(), 0);
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		
		//EncryptedExtensions... ->
		sslOut.clear();
		ssl.wrap(sslIn, sslOut);
		
		sslOut.flip();
		tlsOut.clear();
		r = tls.unwrap(sslOut, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.NEED_TASK, sslOut.position(), 0);
		assertEquals(HandshakeStatus.NEED_TASK, tls.getHandshakeStatus());
		assertEquals(0, sslOut.remaining());
		
		Runnable task = tls.getDelegatedTask();
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		assertNull(tls.getDelegatedTask());

		sslOut.clear();
		r = tls.unwrap(sslOut, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
		assertEquals(HandshakeStatus.NEED_UNWRAP, tls.getHandshakeStatus());
		
		task.run();
		r = tls.unwrap(sslOut, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.NEED_WRAP, 0, 0);
		assertEquals(HandshakeStatus.NEED_WRAP, tls.getHandshakeStatus());
		
		tlsOut.clear();
		r = tls.wrap(sslOut, tlsOut);
		assertResult(r, Status.OK, HandshakeStatus.FINISHED, 0, tlsOut.position());
		assertEquals(HandshakeStatus.NOT_HANDSHAKING, tls.getHandshakeStatus());
		
		tlsOut.flip();
		sslOut.clear();
		
//		try {
//		ssl.unwrap(tlsOut, sslOut);
//		} catch (Exception e) {};
//		ssl.wrap(tlsOut, sslOut);
//		ssl.unwrap(tlsOut, sslOut);
		
		assertEquals("FINISHED", ssl.unwrap(tlsOut, sslOut).getHandshakeStatus().name());
	}

	@Test
	public void testServerMode() throws Exception {
		Assume.assumeTrue(JAVA11);
		SSLContextBuilder builder = SSLContextBuilder.forClient();
		
		builder.protocol("TLSv1.3");
		SSLContext ctx = builder.build();

		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forClient(ctx);
		SSLEngine engine = engineBuilder.build("example.com", 100);
		
		engine.beginHandshake();

		HandshakeEngine eh = new HandshakeEngine(false, new EngineParameters(), handler, handler);

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
