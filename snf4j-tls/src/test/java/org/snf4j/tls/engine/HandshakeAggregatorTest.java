/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.Alert;

public class HandshakeAggregatorTest extends CommonTest {

	StringBuilder trace = new StringBuilder();
	
	String hasTask;
	
	boolean hasProducingTask;
	
	boolean needProduce;

	boolean needProduceAfterUpdateTasks;
	
	@Before
	public void before() throws Exception {
		super.before();
		hasTask = "=F";
		hasProducingTask = false;
		needProduce = false;
		needProduceAfterUpdateTasks = false;
	}

	String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	void trace(String s) {
		trace.append(s).append('|');
	}
	
	ByteBuffer fragments(String... contents) {
		buffer.clear();
		for (String content: contents) {
			byte[] b = content.getBytes(StandardCharsets.US_ASCII);
			
			buffer.putInt(b.length);
			buffer.put(b);
		}
		buffer.flip();
		return buffer;
	}
	
	void assertState(HandshakeAggregator ha, boolean empty, boolean hasRemainig, int remaining) {
		assertEquals(empty, ha.isEmpty());
		assertEquals(hasRemainig, ha.hasRemaining());
		assertEquals(remaining, buffer.remaining());
	}
	
	@Test
	public void testUnwrapCompleteFragments() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		assertTrue(ha.unwrap(fragments("ABC"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);

		assertTrue(ha.unwrap(fragments("aBC","DE"), buffer.remaining()));
		assertEquals("3:aBC|2:DE|", trace());
		assertState(ha, true, false, 0);
		
		assertTrue(ha.unwrap(fragments("ABC","DE","F"), buffer.remaining()));
		assertEquals("3:ABC|2:DE|1:F|", trace());
		assertState(ha, true, false, 0);
	}

	@Test
	public void testUnwrapIncompleteFragment() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		assertTrue(ha.unwrap(fragments("ABC"), buffer.remaining()-1));
		assertEquals("", trace());
		assertState(ha, false, false, 1);
		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);

		assertTrue(ha.unwrap(fragments("ABCDEF"), buffer.remaining()-4));
		assertEquals("", trace());
		assertState(ha, false, false, 4);
		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("6:ABCDEF|", trace());
		assertState(ha, true, false, 0);

		assertTrue(ha.unwrap(fragments("ABCDEF"), buffer.remaining()-5));
		assertEquals("", trace());
		assertState(ha, false, false, 5);
		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("6:ABCDEF|", trace());
		assertState(ha, true, false, 0);
	}

	@Test
	public void testUnwrapIncompleteFragmentTwoTimes() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		assertTrue(ha.unwrap(fragments("ABCDEF"), buffer.remaining()-4));
		assertEquals("", trace());
		assertState(ha, false, false, 4);
		assertTrue(ha.unwrap(buffer, buffer.remaining()-2));
		assertEquals("", trace());
		assertState(ha, false, false, 2);
		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("6:ABCDEF|", trace());
		assertState(ha, true, false, 0);
	}
	
	@Test
	public void testUnwrapIncompleteHeader() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		assertTrue(ha.unwrap(fragments("ABC"), 0));
		assertEquals("", trace());
		assertState(ha, true, false, 7);
		assertTrue(ha.unwrap(buffer, 0));
		assertEquals("", trace());
		assertState(ha, true, false, 7);
		
		assertTrue(ha.unwrap(buffer, 1));
		assertEquals("", trace());
		assertState(ha, false, false, 6);

		assertTrue(ha.unwrap(buffer, 1));
		assertEquals("", trace());
		assertState(ha, false, false, 5);

		assertTrue(ha.unwrap(buffer, 1));
		assertEquals("", trace());
		assertState(ha, false, false, 4);

		assertTrue(ha.unwrap(buffer, 1));
		assertEquals("", trace());
		assertState(ha, false, false, 3);

		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);

		assertTrue(ha.unwrap(fragments("ABC"), 1));
		assertEquals("", trace());
		assertState(ha, false, false, 6);

		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);

		assertTrue(ha.unwrap(fragments("ABC","DE"), 1));
		assertEquals("", trace());
		assertState(ha, false, false, 12);

		assertTrue(ha.unwrap(buffer, buffer.remaining()));
		assertEquals("3:ABC|2:DE|", trace());
		assertState(ha, true, false, 0);
	}
	
	@Test
	public void testUnwrapWithTask() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		hasTask = "=T";
		assertTrue(ha.unwrap(fragments("ABC", "DE"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, false, true, 0);
		hasTask = "=F";
		assertTrue(ha.unwrapRemaining());
		assertEquals("2:DE|", trace());
		assertState(ha, true, false, 0);
	}

	@Test
	public void testUnwrapWithProducingTask() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		hasTask = "=T";
		hasProducingTask = true;
		assertFalse(ha.unwrap(fragments("ABC", "DE"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, false, true, 0);
		hasTask = "=F";
		hasProducingTask = false;
		assertTrue(ha.unwrapRemaining());
		assertEquals("2:DE|", trace());
		assertState(ha, true, false, 0);
	}
	
	@Test
	public void testUnwrapWithNeedProduce() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());
		
		needProduce = true;
		assertFalse(ha.unwrap(fragments("ABC"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);
		needProduce = false;
		needProduceAfterUpdateTasks = true;
		assertFalse(ha.unwrap(fragments("ABC"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, true, false, 0);
	}
	
	@Test
	public void testUnwrapRemaining() throws Exception {
		HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());

		hasTask = "=T";
		assertTrue(ha.unwrap(fragments("ABC","DE"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, false, true, 0);
		hasTask = "=F";
		assertTrue(ha.unwrapRemaining());
		assertEquals("2:DE|", trace());
		assertState(ha, true, false, 0);
		
		hasTask = "=T";
		assertTrue(ha.unwrap(fragments("ABC","DE","FGH"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, false, true, 0);
		hasProducingTask = true;
		assertFalse(ha.unwrapRemaining());
		assertEquals("2:DE|", trace());
		assertState(ha, false, true, 0);

		hasTask = "=F";
		hasProducingTask = false;
		assertTrue(ha.unwrapRemaining());
		assertEquals("3:FGH|", trace());
		assertState(ha, true, false, 0);
		
		hasTask = "=T";
		assertTrue(ha.unwrap(fragments("ABC","DE","FGH"), buffer.remaining()));
		assertEquals("3:ABC|", trace());
		assertState(ha, false, true, 0);
		assertTrue(ha.unwrapRemaining());
		assertEquals("2:DE|3:FGH|", trace());
		assertState(ha, true, false, 0);
	}
	
	@Test
	public void testUnwrap() throws Exception {
		for (int i=1; i<7; ++i) {
			HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());

			assertTrue(ha.unwrap(fragments("ABC"), i));
			assertEquals("", trace());
			assertState(ha, false, false, 7-i);
			assertTrue(ha.unwrap(buffer, buffer.remaining()));
			assertEquals("3:ABC|", trace());
		}

		for (int i=1; i<13; ++i) {
			HandshakeAggregator ha = new HandshakeAggregator(new TestHandshaker());

			assertTrue(ha.unwrap(fragments("ABC","CD"), i));
			if (i < 7) {
				assertEquals("", trace());
				assertState(ha, false, false, 13-i);
			}
			else {
				assertEquals("3:ABC|", trace());
				assertState(ha, i == 7 ? true : false, false, 13-i);
			}
			assertTrue(ha.unwrap(buffer, buffer.remaining()));
			assertEquals(i < 7 ? "3:ABC|2:CD|" : "2:CD|" , trace());
		}
	}
	
	class TestHandshaker implements IHandshakeEngine {

		@Override
		public IEngineHandler getHandler() {
			return null;
		}

		@Override
		public void consume(ByteBuffer[] srcs, int remaining) throws Alert {
			ByteBufferArray srca = ByteBufferArray.wrap(srcs);
			int i = srca.getInt();
			byte[] b = new byte[remaining-4];
			srca.get(b);
			trace(Integer.toString(i)+":"+new String(b, StandardCharsets.US_ASCII));
		}

		@Override
		public void consume(ByteBufferArray srcs, int remaining) throws Alert {
		}

		@Override
		public boolean needProduce() {
			return needProduce;
		}

		@Override
		public ProducedHandshake[] produce() throws Alert {
			return null;
		}

		@Override
		public boolean updateTasks() throws Alert {
			needProduce = needProduceAfterUpdateTasks;
			return false;
		}

		@Override
		public boolean hasProducingTask() {
			return hasProducingTask;
		}

		@Override
		public boolean hasRunningTask(boolean onlyUndone) {
			return false;
		}

		@Override
		public boolean hasTask() {
			char c = hasTask.charAt(0);
			
			if (c == '=') {
				hasTask = hasTask.substring(1) + hasTask;
				return hasTask();
			}
			else {
				hasTask = hasTask.substring(1);
			}
			return c == 'T';
		}

		@Override
		public Runnable getTask() {
			return null;
		}

		@Override
		public void start() throws Alert {
		}

		@Override
		public IEngineState getState() {
			return null;
		}

		@Override
		public void updateKeys() throws Alert {
		}
		
		@Override
		public void cleanup() {
		}		
	}
}
