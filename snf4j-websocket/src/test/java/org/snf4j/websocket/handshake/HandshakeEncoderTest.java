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
package org.snf4j.websocket.handshake;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.TestSession;
import org.snf4j.core.allocator.TestAllocator;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.websocket.DefaultWebSocketSessionConfig;
import org.snf4j.websocket.TestWSSession;

public class HandshakeEncoderTest {

	@Test
	public void testTypes() {
		HandshakeEncoder e = new HandshakeEncoder(true);

		assertTrue(e.getInboundType() == HandshakeFrame.class);
		assertTrue(e.getOutboundType() == ByteBuffer.class);
	}
	
	@Test
	public void testEncode() throws Exception {
		HandshakeEncoder e = new HandshakeEncoder(true);
		HandshakeDecoder d = new HandshakeDecoder(false);
		HandshakeFrame f = new HandshakeRequest("/uri");
		TestAllocator a = new TestAllocator(false, true);
		TestWSSession s = new TestWSSession(a);
		List<ByteBuffer> out = new ArrayList<ByteBuffer>();
		List<HandshakeFrame> out2 = new ArrayList<HandshakeFrame>();
		byte[] b;
		ByteBuffer bb;
		
		e.encode(s, f, out);
		assertEquals(1, out.size());
		bb = out.get(0);
		b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(b.length, d.available(s, b, 0, b.length));
		d.decode(s, b, out2);
		assertEquals(1, out2.size());
		HandshakeRequest r = (HandshakeRequest) out2.get(0);
		assertEquals("/uri", r.getUri());
		assertEquals(1, a.getAllocatedCount());
		assertEquals(0, a.getReleasedCount());
		assertEquals(1, a.getSize());
		
		s.incCapacity = -1;
		out.clear();
		e.encode(s, f, out);
		assertEquals(1, out.size());
		bb = out.get(0);
		b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(b.length, d.available(s, b, 0, b.length));
		out2.clear();
		d.decode(s, b, out2);
		assertEquals(1, out2.size());
		r = (HandshakeRequest) out2.get(0);
		assertEquals("/uri", r.getUri());
		assertEquals(3, a.getAllocatedCount());
		assertEquals(1, a.getReleasedCount());
		assertEquals(2, a.getSize());
		
		s.incCapacity = -1000;
		out.clear();
		try {
			e.encode(s, f, out);
			fail();
		}
		catch (InvalidHandshakeException ex) {
		}
		assertEquals(5, a.getAllocatedCount());
		assertEquals(3, a.getReleasedCount());
		assertEquals(2, a.getSize());
		
		try {
			e.encode(s, new HandshakeResponse(200, "Ok"), out);
			fail();
		}
		catch (InvalidHandshakeException ex) {
		}
		assertEquals(6, a.getAllocatedCount());
		assertEquals(4, a.getReleasedCount());

		try {
			e.encode(new TestSession(), f, out);
			fail();
		}
		catch (InvalidHandshakeException ex) {
		}
		assertEquals(6, a.getAllocatedCount());
		assertEquals(4, a.getReleasedCount());
		
		e = new HandshakeEncoder(false);
		d = new HandshakeDecoder(true);
		f = new HandshakeResponse(400, "Bad Request");
		out.clear();
		s.incCapacity = 0;
		e.encode(s, f, out);
		assertEquals(1, out.size());
		bb = out.get(0);
		b = new byte[bb.remaining()];
		bb.get(b);
		assertEquals(b.length, d.available(s, b, 0, b.length));
		out2.clear();
		d.decode(s, b, out2);
		assertEquals(1, out2.size());
		HandshakeResponse res = (HandshakeResponse) out2.get(0);
		assertEquals(400, res.getStatus());
		assertEquals("Bad Request", res.getReason());
		assertEquals(7, a.getAllocatedCount());
		assertEquals(4, a.getReleasedCount());
		
		try {
			e.encode(s, new HandshakeRequest("/uri"), out);
			fail();
		}
		catch (InvalidHandshakeException ex) {
		}
		assertEquals(8, a.getAllocatedCount());
		assertEquals(5, a.getReleasedCount());
	}
	
	@Test
	public void testEncodeWithProtocolSwitching() throws Exception {
		HandshakeEncoder e = new HandshakeEncoder(false);
		HandshakeFrame f = new HandshakeResponse(400, "Bad Request");
		TestWSSession s = new TestWSSession(new TestAllocator(false, true));
		List<ByteBuffer> out = new ArrayList<ByteBuffer>();
		final ICodecPipeline[] pipelines = new ICodecPipeline[1];
		ICodecPipeline pipeline = new DefaultCodecExecutor().getPipeline();
		
		DefaultWebSocketSessionConfig config = new DefaultWebSocketSessionConfig() {
			@Override
			public void switchEncoders(ICodecPipeline pipeline, boolean allowExtensions) {
				pipelines[0] = pipeline;
			}
		};
		s.config = config;
		
		e.added(s, pipeline);
		e.encode(s, f, out);
		assertEquals(1, out.size());
		assertNull(pipelines[0]);
		f = new HandshakeResponse(101, "Switching Protocols");
		out.clear();
		e.encode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(pipelines[0] == pipeline);
		e.removed(s, pipeline);
		out.clear();
		e.encode(s, f, out);
		assertEquals(1, out.size());
		assertNull(pipelines[0]);
		e.event(s, null);
	}
	
}
