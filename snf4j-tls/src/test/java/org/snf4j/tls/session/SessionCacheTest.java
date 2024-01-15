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
package org.snf4j.tls.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.ref.ReferenceQueue;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.snf4j.tls.session.SessionCache.CacheEntry;
import org.snf4j.tls.session.SessionCache.SoftCacheEntry;

public class SessionCacheTest {

	static final long MAX_MEMORY_SIZE = 1000000000000L; 
	
	@SuppressWarnings("unchecked")
	static Map<Long,CacheEntry<Long>> map(SessionCache<Long> cache) throws Exception {
		Field f = SessionCache.class.getDeclaredField("cache");
		
		f.setAccessible(true);
		return (Map<Long, CacheEntry<Long>>) f.get(cache);
	}
	
	static int size(SessionCache<Long> cache) throws Exception {
		return map(cache).size();
	}
	
	@SuppressWarnings("unchecked")
	static ReferenceQueue<ISession> queue(SessionCache<Long> cache) throws Exception {
		Field f = SessionCache.class.getDeclaredField("queue");
		
		f.setAccessible(true);
		return (ReferenceQueue<ISession>) f.get(cache);
	}
	
	static void waitFor(long ms) throws Exception {
		Thread.sleep(ms);
	}
	
	@Test
	public void testPut() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(10000000, 1000000);

		int sessionSize = 10000000;
		int count = (int) (MAX_MEMORY_SIZE / sessionSize);
		int maxSize1 = 0;
		int size;

		for (int i=0; i<count; ++i) {
			ISession s = new TestSession(i, sessionSize);
			c.put(s.getId(), s);
			
			if (i % 2 == 0) {
				Map<Long, CacheEntry<Long>> map = map(c);
				
				Entry<Long, CacheEntry<Long>> entry = map.entrySet().iterator().next();
				Field f = SoftCacheEntry.class.getDeclaredField("key");
				f.setAccessible(true);
				f.set(entry.getValue(), null);
			}
			
			size = size(c);
			if (size > maxSize1) {
				maxSize1 = size;
			}
			else if (maxSize1 > 0 && size < maxSize1) {
				break;
			}
		}
		assertTrue(size(c) < count);
		
		int maxSize2 = 0;

		c = new SessionCache<Long>(10000000, 1000000);
		sessionSize = 1000000;
		count = (int) (MAX_MEMORY_SIZE / sessionSize);
		
		for (int i=0; i<count; ++i) {
			ISession s = new TestSession(i, sessionSize);
			c.put(s.getId(), s);
			
			size = size(c);
			if (size > maxSize2) {
				maxSize2 = size;
			}
			else if (maxSize2 > 0 && size < maxSize2) {
				break;
			}
		}
		assertTrue(size(c) < count);
		assertTrue(maxSize1 * 10 / 5 < maxSize2);
	}
	
	@Test
	public void testPutAndInvalidateOld() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(10000000, 1000000);
		
		TestSession s1 = new TestSession(1, 100);
		TestSession s2 = new TestSession(1, 100);

		c.put(1L, s1);
		assertEquals(1, size(c));
		CacheEntry<Long> entry1 = map(c).get(1L);
		assertNotNull(entry1.getKey());
		assertSame(s1, c.get(1L));
		
		c.put(1L,  s2);
		assertEquals(1, size(c));
		CacheEntry<Long> entry2 = map(c).get(1L);
		assertNotNull(entry2.getKey());
		assertSame(s2, c.get(1L));
		
		assertNull(entry1.getKey());
	}
	
	@Test
	public void testPutAndExpirationTime() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(100, 0);
		
		c.put(1L, new TestSession(1, 100), 1000);
		assertEquals(1, size(c));
		assertNotNull(c.get(1L, 0));
		assertNotNull(c.get(1L, 1000));
		assertNotNull(c.get(1L, 10000000));
		assertEquals(1, size(c));

		c = new SessionCache<Long>(100, 1);
		c.put(1L, new TestSession(1, 100), 1000);
		assertEquals(1, size(c));
		assertNotNull(c.get(1L, 0));
		assertNotNull(c.get(1L, 1000));
		assertNotNull(c.get(1L, 1001));
		assertEquals(1, size(c));
		assertNull(c.get(1L, 1002));
		assertEquals(0, size(c));
	}
	
	@Test
	public void testPutAndLimit() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(0, 1000);
		
		c.put(1L, new TestSession(1, 100), 1000);
		assertEquals(1, size(c));
		c.put(2L, new TestSession(1, 100), 1000);
		assertEquals(2, size(c));
		
		TestSession s1 = new TestSession(1, 100);
		TestSession s2 = new TestSession(1, 100);

		c = new SessionCache<Long>(1, 1000);
		c.put(1L, s1, 1000);
		assertEquals(1, size(c));
		c.put(2L, s2, 1000);
		assertEquals(1, size(c));
		assertSame(s2, c.get(2L, 1000));
		c.put(1L, s1, 2001);
		assertEquals(1, size(c));
		assertSame(s1, c.get(1L, 2001));
	}

	@Test
	public void testPutAndLifetime() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(3, 0);

		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1000);
		c.put(3L, new TestSession(1, 100), 1000);
		assertEquals(3, size(c));
		c.put(4L, new TestSession(1, 100), 1000000);
		assertEquals(3, size(c));
		assertNull(c.get(1L));
		
		c = new SessionCache<Long>(3, 1);
		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1000);
		c.put(3L, new TestSession(1, 100), 1001);
		assertEquals(3, size(c));
		c.put(4L, new TestSession(1, 100), 1002);
		assertEquals(2, size(c));
		assertNull(c.get(1L));
		assertNull(c.get(2L));
		c.put(5L, new TestSession(1, 100), 1004);
		assertEquals(3, size(c));
		c.put(6L, new TestSession(1, 100), 1006);
		assertEquals(1, size(c));
		
		c = new SessionCache<Long>(2, 50);
		c.put(1L, new TestSession(1, 100));
		c.put(2L, new TestSession(1, 100));
		assertEquals(2, size(c));
		waitFor(100);
		c.put(3L, new TestSession(1, 100));
		assertEquals(1, size(c));
	}
	
	@Test
	public void testSize() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(3, 50);
		
		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1030);
		c.put(3L, new TestSession(1, 100), 1050);
		assertEquals(3, c.size(1050));
		assertEquals(3, c.size(1050));
		assertEquals(3, size(c));
		assertEquals(2, c.size(1051));
		assertEquals(2, size(c));
		assertNull(c.get(1L, 1051));
		assertEquals(2, c.size(1051));
		assertEquals(2, size(c));
		assertEquals(0, c.size(1101));
		assertEquals(0, size(c));
		
		c = new SessionCache<Long>(3, 50);
		c.put(1L, new TestSession(1, 100));
		c.put(2L, new TestSession(1, 100));
		c.put(3L, new TestSession(1, 100));
		assertEquals(3, c.size());
		waitFor(100);
		assertEquals(0, c.size());
	}
	
	@Test
	public void testClear() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(3, 50);
		
		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1000);
		c.put(3L, new TestSession(1, 100), 1000);
		assertEquals(3, c.size(1000));
		c.clear();
		assertEquals(0, c.size(1000));
		
		c = new SessionCache<Long>(10000000, 50);
		ReferenceQueue<ISession> q = queue(c);
		for (int i=0; i<10000000; ++i) {
			c.put((long)i, new TestSession(i, 100000));
			if (q.poll() != null) {
				break;
			}
		}
		assertTrue(size(c) > 0);
		c.clear();
		assertEquals(0, c.size(1000));
	}
	
	@Test
	public void testGet() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(3, 50);
		
		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1025);
		c.put(3L, new TestSession(1, 100), 1050);
		assertEquals(3, size(c));
		assertNull(c.get(4L,1050));
		assertNotNull(c.get(1L, 1050));
		assertNull(c.get(1L, 1051));
		assertEquals(2, size(c));

		c = new SessionCache<Long>(3, 0);
		c.put(1L, new TestSession(1, 100), 1000);
		c.put(2L, new TestSession(1, 100), 1025);
		c.put(3L, new TestSession(1, 100), 1050);
		assertEquals(3, size(c));
		assertNotNull(c.get(1L, 1050000));

		c = new SessionCache<Long>(10000000, 50);
		ReferenceQueue<ISession> q = queue(c);
		for (int i=0; i<10000000; ++i) {
			c.put((long)i, new TestSession(i, 100000));
			if (q.poll() != null) {
				break;
			}
		}
		int size = size(c);
		assertTrue(size > 0);
		assertNull(c.get(-1L));
		assertTrue(size > size(c));
		
		c = new SessionCache<Long>(3, 50);
		c.put(1L, new TestSession(1, 100));
		c.put(2L, new TestSession(1, 100));
		c.put(3L, new TestSession(1, 100));
		assertNotNull(c.get(1L));
		waitFor(100);
		assertNull(c.get(1L));	
	}
	
	@Test
	public void testRemove() throws Exception {
		SessionCache<Long> c = new SessionCache<Long>(3, 50);

		c.put(1L, new TestSession(1, 100));
		c.put(2L, new TestSession(1, 100));
		c.put(3L, new TestSession(1, 100));
		assertEquals(3, size(c));
		c.remove(1L);
		assertEquals(2, size(c));
		assertNull(c.get(1L));
		assertEquals(2, size(c));
		assertNotNull(c.get(2L));		
		c.remove(4L);
		assertEquals(2, size(c));
		
		c = new SessionCache<Long>(10000000, 50);
		ReferenceQueue<ISession> q = queue(c);
		for (int i=0; i<10000000; ++i) {
			c.put((long)i, new TestSession(i, 100000));
			if (q.poll() != null) {
				break;
			}
		}
		int size = size(c);
		assertTrue(size > 0);
		c.remove(-1L);
		assertTrue(size > size(c));	
	}
	
	@Test
	public void testEntry() {
		TestSession s1 = new TestSession(1, 100);
		ReferenceQueue<ISession> queue = new ReferenceQueue<ISession>();
		SoftCacheEntry<Long> entry = new SoftCacheEntry<Long>(1L, s1, 2000, queue);
		
		assertEquals(1L, entry.getKey().longValue());
		assertSame(s1, entry.getSession());
		assertSame(s1, entry.get());
		assertTrue(entry.isValid(2000));
		assertFalse(entry.isValid(2001));
		assertNull(entry.getKey());
		assertNull(entry.getSession());
		assertNull(entry.get());
		
		entry = new SoftCacheEntry<Long>(1L, s1, 2000, queue);
		assertTrue(entry.isValid(2000));
		entry.clear();
		assertEquals(1L, entry.getKey().longValue());
		assertNull(entry.getSession());
		assertNull(entry.get());
		assertFalse(entry.isValid(2000));
		assertNull(entry.getKey());
		assertNull(entry.getSession());
		assertNull(entry.get());
		
	}
}
