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

import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;

public class TLSEngineTest extends EngineTest {
	
	@Test
	public void testClientMode() throws Exception {
		Assume.assumeTrue(JAVA11);
		SSLContextBuilder builder = SSLContextBuilder.forServer(key("RSA", "rsa"), cert("rsasha256"));
		
		builder.protocol("TLSv1.3");
		SSLContext ctx = builder.build();
		
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forServer(ctx);
		SSLEngine engine = engineBuilder.build();
		
		engine.beginHandshake();
		
		HandshakeEngine eh = new HandshakeEngine(true, new EngineParameters(), handler);
		eh.start();
		ProducedHandshake[] produced = eh.produce();
		produced[0].getHandshake().getBytes(buffer);
		buffer.flip();
		ByteBuffer record = ByteBuffer.allocate(100000);
		record.put((byte) 22);
		record.putShort((short) 0x0303);
		record.putShort((short) buffer.remaining());
		record.put(buffer);
		record.flip();
		
		ByteBuffer out = ByteBuffer.allocate(100000);
		ByteBuffer in = ByteBuffer.allocate(100000);
		in.flip();
		
		engine.unwrap(record, out);
		Runnable task = engine.getDelegatedTask();
		task.run();
		task = engine.getDelegatedTask();
		
		engine.wrap(in, out);
		out.flip();
		assertEquals(22, out.get());
		assertEquals(0x0303, out.getShort());
		int remaining = out.getShort();
		
		eh.consume(new ByteBuffer[] {out}, remaining);
		
		out.clear();
		engine.wrap(in, out);
		out.flip();
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

		HandshakeEngine eh = new HandshakeEngine(false, new EngineParameters(), handler);

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
