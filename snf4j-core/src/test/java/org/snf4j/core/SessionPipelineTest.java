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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.SessionPipeline.Item;
import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;

public class SessionPipelineTest {

	StreamSession s1,s2,s3,s4;

	@Before
	public void before() {
		s1 = new StreamSession(new TestHandler("1"));
		s2 = new StreamSession(new TestHandler("2"));
		s3 = new StreamSession(new TestHandler("3"));
		s4 = new StreamSession(new TestHandler("4"));
	}
	
	void assertPipeline(ISessionPipeline<IStreamSession> p, StreamSession... sessions) {
		assertPipeline((StreamSessionPipeline)p, sessions);
	}

	void assertPipeline(StreamSessionPipeline p, StreamSession... sessions) {
		StreamSession s = p.first();
		
		if (sessions.length == 0) {
			assertNull(s);
		}
		else {
			for (StreamSession session: sessions) {
				assertTrue(s == session);
				s = (StreamSession) s.pipelineItem.next();
			}
			assertTrue(s1 == s);
		}
	}
	
	@Test
	public void testFirst() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		assertPipeline(p);
		assertTrue(s1.pipeline == p);
		assertNull(s1.pipelineItem);
		assertNull(p.first());
		
		p.add("2", s2);
		assertTrue(s1.pipeline == p);
		assertNull(s1.pipelineItem);
		assertTrue(s2.pipeline == p);
		assertNotNull(s2.pipelineItem);
		assertTrue(s2 == p.first());
		assertPipeline(p, s2);
		
		p.add("3", s3);
		assertTrue(s1.pipeline == p);
		assertNull(s1.pipelineItem);
		assertTrue(s2.pipeline == p);
		assertNotNull(s2.pipelineItem);
		assertTrue(s3.pipeline == p);
		assertNotNull(s3.pipelineItem);
		assertTrue(s2 == p.first());
		assertPipeline(p, s2, s3);
		
		p.remove("2");
		assertTrue(s3 == p.first());
		assertPipeline(p, s3);
		p.remove("3");
		assertNull(p.first());
		assertPipeline(p);
	}
	
	@Test
	public void testAddFirst() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		try {
			p.addFirst(null, s2);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		p.addFirst("2", s2);
		assertTrue(s2 == p.first());
		assertPipeline(p, s2);
		try {
			p.addFirst("2", s3);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '2' already exists", e.getMessage());
		}
		p.addFirst("3", s3);
		assertTrue(s3 == p.first());
		assertPipeline(p, s3, s2);
		try {
			p.addFirst("x", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.addFirst("x", s2);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		assertPipeline(p, s3, s2);
	}
	
	@Test
	public void testAddLast() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		try {
			p.add(null, s2);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		p.add("2", s2);
		assertTrue(s2 == p.first());
		assertPipeline(p, s2);
		try {
			p.add("2", s3);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '2' already exists", e.getMessage());
		}
		p.add("3", s3);
		assertTrue(s2 == p.first());
		assertPipeline(p, s2, s3);
		try {
			p.add("x", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.add("x", s2);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		assertPipeline(p, s2, s3);
	}
	
	@Test
	public void testAddAfter() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		p.add("2", s2);
		p.add("3", s3);
		try {
			p.addAfter(null, "4", s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.addAfter("2", null, s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		p.first();
		assertPipeline(p,s2,s3);
		p.addAfter("2", "4", s4);
		assertPipeline(p,s2,s4,s3);
		p.remove("4");
		assertPipeline(p,s2,s3);
		try {
			p.addAfter("22", "4", s4);
		}
		catch (NoSuchElementException e) {
			assertEquals("key '22' does not exist", e.getMessage());
		}
		try {
			p.addAfter("2", "3", s4);
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '3' already exists", e.getMessage());
		}
		try {
			p.addAfter("2", "2", s4);
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '2' already exists", e.getMessage());
		}
		p.addAfter("3", "4", s4);
		assertPipeline(p,s2,s3,s4);
		try {
			p.addAfter("4", "x", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.addAfter("4", "x", s2);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		assertPipeline(p,s2,s3,s4);
	}

	@Test
	public void testAddBefore() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		p.add("2", s2);
		p.add("3", s3);
		try {
			p.addBefore(null, "4", s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("baseKey is null", e.getMessage());
		}
		try {
			p.addBefore("2", null, s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		assertPipeline(p,s2,s3);
		p.addBefore("3", "4", s4);
		assertPipeline(p,s2,s4,s3);
		p.remove("4");
		assertPipeline(p,s2,s3);
		try {
			p.addBefore("22", "4", s4);
		}
		catch (NoSuchElementException e) {
			assertEquals("key '22' does not exist", e.getMessage());
		}
		try {
			p.addBefore("2", "3", s4);
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '3' already exists", e.getMessage());
		}
		try {
			p.addBefore("2", "2", s4);
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '2' already exists", e.getMessage());
		}
		p.addBefore("2", "4", s4);
		assertPipeline(p,s4,s2,s3);
		try {
			p.addBefore("4", "x", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.addBefore("4", "x", s2);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		assertPipeline(p,s4,s2,s3);
	}
	
	@Test
	public void testReplace() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		p.add("2", s2);
		p.add("3", s3);
		try {
			p.replace(null, "4", s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("oldKey is null", e.getMessage());
		}
		try {
			p.replace("4", null, s4);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		assertPipeline(p,s2,s3);
		try {
			p.replace("22", "4", s4);
			fail();
		}
		catch (NoSuchElementException e) {
			assertEquals("key '22' does not exist", e.getMessage());
		}
		try {
			p.replace("2", "3", s4);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("key '3' already exists", e.getMessage());
		}
		assertTrue(s2 == p.replace("2", "2", s4));
		assertPipeline(p,s4,s3);
		assertTrue(s4 == p.replace("2", "2", s2));
		assertPipeline(p,s2,s3);
		assertTrue(s3 == p.replace("3", "4", s4));
		assertPipeline(p,s2,s4);
		p.remove("4");
		assertPipeline(p,s2);
		p.add("3", s3);
		assertPipeline(p,s2,s3);
		try {
			p.replace("2", "x", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.replace("2", "2", s1);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is owner", e.getMessage());
		}
		try {
			p.replace("2", "x", s3);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		try {
			p.replace("2", "2", s3);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session already exists", e.getMessage());
		}
		assertPipeline(p,s2,s3);
		p.replace("2", "2", s2);
		assertTrue(s2 == p.get("2"));
		assertPipeline(p,s2,s3);
		
		
		p.replace("2", "x", s2);
		assertTrue(s2 == p.get("x"));
		assertPipeline(p,s2,s3);
	}
	
	@Test
	public void testRemove() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		try {
			p.remove(null);
			fail();
		}
		catch (NullPointerException e) {
			assertEquals("key is null", e.getMessage());
		}
		assertNull(p.remove("2"));
		p.add("2", s2);
		p.add("3", s3);
		assertPipeline(p,s2,s3);
		assertNull(p.remove("4"));
		assertPipeline(p,s2,s3);
		assertTrue(s2 == p.remove("2"));
		assertPipeline(p,s3);
	}
	
	@Test
	public void testGet() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();
		
		try {
			p.get(null);
			fail();
		}
		catch (NullPointerException e) {
		}
		p.add("2", s2);
		p.add("3", s3);
		assertNull(p.get("4"));
		assertTrue(s2 == p.get("2"));
		assertTrue(s3 == p.get("3"));
	}
	
	@Test
	public void testInterface() {
		ISessionPipeline<IStreamSession> p = s1.getPipeline();
		
		p.add("2", s2);
		p.add("3", s3);
		assertPipeline(p,s2,s3);
		p.addFirst("4", s4);
		assertPipeline(p,s4,s2,s3);
		assertTrue(s2 == p.remove("2"));
		assertPipeline(p,s4,s3);
		p.addAfter("4", "2", s2);
		assertPipeline(p,s4,s2,s3);
		p.remove("4");
		assertPipeline(p,s2,s3);
		p.addBefore("2", "4", s4);
		assertPipeline(p,s4,s2,s3);
		p.remove("2");
		assertPipeline(p,s4,s3);
		assertTrue(s3 == p.replace("3", "2", s2));
		assertPipeline(p,s4,s2);
		assertTrue(s4 == p.get("4"));
		
		try {
			p.add("5", new TestInternalSession("", new TestHandler(""), null));
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("session is not an instance of StreamSession class", e.getMessage());
		}
		try {
			p.addFirst("5", new TestInternalSession("", new TestHandler(""), null));
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			p.addBefore("5", "6", new TestInternalSession("", new TestHandler(""), null));
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			p.addAfter("5", "6", new TestInternalSession("", new TestHandler(""), null));
			fail();
		}
		catch (IllegalArgumentException e) {
		}
		try {
			p.replace("5", "6", new TestInternalSession("", new TestHandler(""), null));
			fail();
		}
		catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testSync() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		p.add("2", s2);
		p.add("3", s3);
		assertNull(s2.pipelineItem.next);
		assertNull(s3.pipelineItem.prev);
		p.first();
		assertNotNull(s2.pipelineItem.next);
		assertNotNull(s3.pipelineItem.prev);
		p.first();
		
		before();
		p = (StreamSessionPipeline) s1.getPipeline();
		p.add("2", s2);
		p.add("3", s3);
		p.first();
		p.add("4", s4);
		assertNull(s3.pipelineItem.next);
		assertNull(s4.pipelineItem.prev);
		s2.pipelineItem.next();
		assertNotNull(s3.pipelineItem.next);
		assertNotNull(s4.pipelineItem.prev);
		
		before();
		p = (StreamSessionPipeline) s1.getPipeline();
		p.add("2", s2);
		p.add("3", s3);
		p.add("4", s4);
		p.first();
		p.remove("3");
		assertTrue(s2.pipelineItem.next == s3.pipelineItem);
		assertTrue(s4 == s3.pipelineItem.next());
		assertTrue(s2.pipelineItem.next == s3.pipelineItem);
		assertTrue(s4 == s2.pipelineItem.next());
		assertTrue(s2.pipelineItem.next == s4.pipelineItem);
		assertPipeline(p,s2,s4);
	}
	
	@Test
	public void testItem() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		p.add("2", s2);
		p.add("3", s3);
		p.first();
		Item<?> i2 = s2.pipelineItem;
		Item<?> i3 = s3.pipelineItem;
		
		assertTrue(s1 == i2.owner());
		assertTrue(s1 == i3.owner());
		assertNull(i2.cause());
		assertNull(i3.cause());
		assertFalse(i2.canClose());
		assertFalse(i3.canClose());
		Exception e = new Exception("");
		i2.cause(e);
		assertTrue(i2.cause() == e);
		assertTrue(i3.cause() == e);
		assertTrue(i2.canClose());
		assertTrue(i3.canClose());
		i2.cause(null);
		assertNull(i2.cause());
		assertNull(i3.cause());
		assertFalse(i2.canClose());
		assertFalse(i3.canClose());
		i2.markEos();
		assertTrue(i2.canClose());
		assertTrue(i3.canClose());
		i2.cause(e);
		assertTrue(i2.canClose());
		assertTrue(i3.canClose());		
	}
	
	void assertUnlinked(StreamSession s) {
		assertNull(s.pipeline);
		assertNull(s.pipelineItem);
	}
	
	@Test
	public void testSessionLink() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		p.addFirst("2", s2);
		assertTrue(p == s2.getPipeline());
		assertFalse(p == s3.getPipeline());
		p.add("3", s3);
		assertTrue(p == s3.getPipeline());
		assertPipeline(p,s2,s3);
		p.remove("3");
		assertPipeline(p,s2);
		assertUnlinked(s3);
		p.replace("2", "4", s4);
		assertTrue(p == s4.getPipeline());
		assertPipeline(p,s4);
		assertUnlinked(s2);
		p.addAfter("4", "2", s2);
		assertTrue(p == s2.getPipeline());
		p.addBefore("4", "3", s3);
		assertTrue(p == s3.getPipeline());
	}
	
	@Test
	public void testGetOwner() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		assertTrue(p.getOwner() == s1);
		p.add("2", s2);
		assertTrue(s2.getPipeline().getOwner() == s1);
	}
	
	@Test
	public void testGetKeys() {
		StreamSessionPipeline p = (StreamSessionPipeline) s1.getPipeline();

		assertEquals(0, p.getKeys().size());
		p.add("2", s2);
		assertEquals(1, p.getKeys().size());
		assertEquals("2", p.getKeys().get(0));
		p.addFirst("3", s3);
		assertEquals(2, p.getKeys().size());
		assertEquals("3", p.getKeys().get(0));
		assertEquals("2", p.getKeys().get(1));
	}
}
