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
package org.snf4j.websocket.frame;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.websocket.TestWSSession;

public class FrameAggregatorTest {

	@Test
	public void testTypes() {
		FrameAggregator a = new FrameAggregator(100);
		assertTrue(a.getInboundType() == Frame.class);
		assertTrue(a.getOutboundType() == Frame.class);
	}
	
	@Test
	public void testMaxLength() throws Exception {
		FrameAggregator a = new FrameAggregator(100);
		List<Frame> out = new ArrayList<Frame>();
		TestWSSession s = new TestWSSession();
		
		byte[] data = new byte[101];
		Frame f = new BinaryFrame(data);
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		
		out.clear();
		data = new byte[50];
		f = new BinaryFrame(false, 4, data);
		a.decode(s, f, out);
		assertEquals(0, out.size());
		byte[] data2 = new byte[50];
		a.decode(s, new ContinuationFrame(data2), out);
		assertEquals(1, out.size());
		AggregatedBinaryFrame af = (AggregatedBinaryFrame) out.get(0);
		assertEquals(2, af.getFragments().size());
		assertTrue(data == af.getFragments().get(0));
		assertTrue(data2 == af.getFragments().get(1));
		assertEquals(4, af.getRsvBits());
		assertTrue(af.isFinalFragment());
		
		out.clear();
		a.decode(s, new TextFrame(false, 0, data), out);
		assertFalse(s.isClosed());
		assertEquals(0,s.msgs().size());
		try {
			a.decode(s, new ContinuationFrame(new byte[51]), out);
			fail();
		}
		catch (InvalidFrameException e) {
			assertEquals("Too big payload for aggregated frame", e.getMessage());
		}
		assertFalse(s.isClosed());
		assertEquals(1,s.msgs().size());
		assertEquals(1009, ((CloseFrame)s.msgs().get(0)).getStatus());
	}
	
	@Test
	public void testDecode() throws Exception {
		FrameAggregator a = new FrameAggregator(100);
		List<Frame> out = new ArrayList<Frame>();
		TestWSSession s = new TestWSSession();
		
		Frame f = new TextFrame("ABC");
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		
		out.clear();
		f = new PingFrame();
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		
		out.clear();
		a.decode(s, new TextFrame(false,0,"ABC"), out);
		assertEquals(0, out.size());
		a.decode(s, new ContinuationFrame(false,0,"DEF"), out);
		assertEquals(0, out.size());
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		out.clear();
		a.decode(s, new ContinuationFrame(true,0,"GH"), out);
		assertEquals(1, out.size());
		assertEquals("ABCDEFGH", ((AggregatedTextFrame)out.get(0)).getText());
		
		out.clear();
		a.decode(s, new BinaryFrame(false,0,"ABC".getBytes()), out);
		assertEquals(0, out.size());
		a.decode(s, new ContinuationFrame(false,0,"DEF".getBytes()), out);
		assertEquals(0, out.size());
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
		out.clear();
		a.decode(s, new ContinuationFrame(true,0,"GH".getBytes()), out);
		assertEquals(1, out.size());
		assertEquals("ABCDEFGH", new String(((AggregatedBinaryFrame)out.get(0)).getPayload()));
		
		out.clear();
		f = new ContinuationFrame(true,0,"GH".getBytes());
		a.decode(s, f, out);
		assertEquals(1, out.size());
		assertTrue(f == out.get(0));
	}
}
