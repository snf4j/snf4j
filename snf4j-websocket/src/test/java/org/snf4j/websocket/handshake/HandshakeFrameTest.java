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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;

import org.junit.Test;

public class HandshakeFrameTest extends HandshakeTest {

	void assertLength(HandshakeFrame frame) throws Exception {
		ByteBuffer b = ByteBuffer.allocate(frame.getLength()*2);
		
		HandshakeFactory.getDefault().format(frame, b, frame instanceof HandshakeRequest);
		assertEquals(frame.getLength(), b.position());
	}
	
	@Test
	public void testAddValue() throws Exception {
		HandshakeFrame frame = new HandshakeRequest("/chat");
		
		assertFields(frame, "");
		assertLength(frame);
		frame.addValue("name1", "value1");
		assertFields(frame, "name1:value1;");
		assertLength(frame);
		frame.addValue("name2", "value2");
		assertFields(frame, "name1:value1;name2:value2;");
		assertLength(frame);
		frame.addValue("nAme1", "xxx");
		assertFields(frame, "name1:value1, xxx;name2:value2;");
		assertLength(frame);
		frame.appendValue("yyy");
		assertFields(frame, "name1:value1, xxxyyy;name2:value2;");
		assertLength(frame);
	}
	
	@Test
	public void testAppendValue() throws Exception {
		HandshakeFrame frame = new HandshakeRequest("/chat");

		try {
			frame.appendValue("value");
			fail();
		}
		catch (InvalidHandshakeException e) {
		}
		frame.addValue("n1", "v1");
		assertFields(frame, "n1:v1;");
		assertLength(frame);
		frame.appendValue("xx");
		assertFields(frame, "n1:v1xx;");
		assertLength(frame);
		frame.addValue("n2", "v2");
		assertFields(frame, "n1:v1xx;n2:v2;");
		assertLength(frame);
		frame.appendValue("yy");
		assertFields(frame, "n1:v1xx;n2:v2yy;");
		assertLength(frame);
		frame.addValue("n1", "zz");
		assertFields(frame, "n1:v1xx, zz;n2:v2yy;");
		assertLength(frame);
		frame.appendValue("vv");
		assertFields(frame, "n1:v1xx, zzvv;n2:v2yy;");
		assertLength(frame);
	}
	
	@Test
	public void testGetValue() {
		HandshakeFrame frame = new HandshakeResponse(0, "Reason text");

		assertFalse(frame.hasValue("name"));
		assertNull(frame.getValue("name"));
		
		String[] names = new String[] {"name","NAME","Name","namE"};
		
		frame.addValue("name", "value");
		for (String name: names) {
			assertTrue(frame.hasValue(name));
			assertEquals("value", frame.getValue(name));
		}

		names = new String[] {"name2","NAME2","Name2","namE2"};
		frame.addValue("name2", "value2");
		for (String name: names) {
			assertTrue(frame.hasValue(name));
			assertEquals("value2", frame.getValue(name));
		}
	}
	
	@Test
	public void testPendingName() throws Exception {
		HandshakeFrame frame = new HandshakeResponse(0, "Reason text");

		assertFields(frame, "");
		assertLength(frame);
		assertFalse(frame.pendingName());
		frame.pendingName("na");
		assertTrue(frame.pendingName());
		frame.addValue("me1", "value1");
		assertFields(frame, "name1:value1;");
		assertLength(frame);
		assertFalse(frame.pendingName());
		
		frame.pendingName("na");
		frame.pendingName("m");
		assertTrue(frame.pendingName());
		frame.addValue("e2", "value2");
		assertFields(frame, "name1:value1;name2:value2;");
		assertLength(frame);
		assertFalse(frame.pendingName());
	}
}
