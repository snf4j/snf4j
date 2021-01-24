package org.snf4j.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

import org.junit.Test;
import org.snf4j.core.allocator.CachingAllocator;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.DefaultAllocatorMetric;
import org.snf4j.core.allocator.TestAllocator;

public class SctpFragmentsTest {

	TestAllocator allocator;
	
	TestAllocator allocator2;
	
	ByteBuffer buffer(String data, int capacity) {
		ByteBuffer b = allocator2.allocate(capacity);
		b.put(data.getBytes());
		return b;
	}
	
	ByteBuffer buffer(String data) {
		return buffer(data, 16);
	}
	
	SctpFragments prepare(boolean releasable, boolean optimize) {
		allocator = new TestAllocator(false, releasable);
		allocator2 = new TestAllocator(false, releasable);
		return new SctpFragments(allocator, 16, 32, optimize);
	}
	
	void assertBuffer(String data, int capacity, ByteBuffer buffer) {
		assertEquals(capacity, buffer.capacity());
		assertEquals(data.length(), buffer.position());
		assertEquals(capacity, buffer.limit());
		ByteBuffer dup = buffer.duplicate();
		dup.flip();
		byte[] array = new byte[dup.remaining()];
		dup.get(array);
		assertEquals(data, new String(array));
	}
	
	void assertInternals(SctpFragments o, SctpFragments.State state, long streamNum, ByteBuffer fragment, Integer fragments) throws Exception {
		Field f = SctpFragments.class.getDeclaredField("state");
		f.setAccessible(true);
		assertTrue(state == f.get(o));
		f = SctpFragments.class.getDeclaredField("fragmentKey");
		f.setAccessible(true);
		assertEquals(streamNum, f.getLong(o));
		f = SctpFragments.class.getDeclaredField("fragment");
		f.setAccessible(true);
		assertTrue(fragment == f.get(o));
		f = SctpFragments.class.getDeclaredField("fragments");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		Map<Long, ByteBuffer> map = (Map<Long, ByteBuffer>) f.get(o);
		if (fragments == null) {
			assertNull(map);
		}
		else {
			assertEquals(fragments.intValue(), map.size());
		}
	}
	
	void assertAllocator(int allocated, int released) {
		assertEquals(allocated, allocator.getAllocatedCount());
		assertEquals(released, allocator.getReleasedCount());
	}
	
	ByteBuffer allocated(int i) {
		return allocator.getAllocated().get(i);
	}
	
	ByteBuffer released(int i) {
		return allocator.getReleased().get(i);
	}
	
	
	@Test
	public void testStoreWhenEmpty() throws Exception {
		
		//optimize = true
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b;
		
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		assertNull(f.store(1, b1));
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertAllocator(0,0);
		b = f.complete(0, b2);
		assertTrue(b == b2);
		assertBuffer("AA", 16, b);
		b = f.complete(1, buffer("45"));
		assertTrue(b == b1);
		assertBuffer("12345", 16, b);
		b = f.complete(1, b2);
		assertTrue(b == b2);
		assertBuffer("AA", 16, b);
		
		//optimize = false
		f = prepare(true, false);
		b1 = buffer("1233");
		b2 = buffer("WW");
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		assertTrue(b1 == f.store(1, b1));
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		assertAllocator(1,0);
		b = f.complete(0, b2);
		assertTrue(b == b2);
		assertBuffer("WW", 16, b);
		assertBuffer("", 16, b1);
		b = f.complete(1, buffer("45"));
		assertTrue(b == allocated(0));
		assertBuffer("123345", 16, b);
		b = f.complete(1, b2);
		assertTrue(b == b2);
		assertBuffer("WW", 16, b);
	}
	
	@Test
	public void testStoreWhenSingle() throws Exception {
		
		//optimize = true
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b;
		
		f.store(1, b1);
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertAllocator(0,0);
		assertNull(f.store(1, b2));
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertAllocator(0,1);
		assertTrue(b2 == released(0));
		b2 = buffer("BB");
		b = f.complete(0, b2);
		assertTrue(b == b2);
		assertBuffer("BB", 16, b);
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertAllocator(0,1);
		b = f.complete(1, b2);
		assertTrue(b == b1);
		assertBuffer("123AABB", 16, b);
		assertAllocator(0,2);
		assertTrue(b2 == released(1));
		
		//optimize = false
		f = prepare(true, false);
		b1 = buffer("123");
		b2 = buffer("AA");

		f.store(1, b1);
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		b = f.store(1, b2);
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		assertTrue(b == b2);
		assertBuffer("", 16, b);
		b2 = buffer("BB");
		b = f.complete(0, b2);
		assertTrue(b == b2);
		assertBuffer("BB", 16, b);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		assertAllocator(1,0);
		b = f.complete(1, b2);
		assertTrue(b == allocated(0));
		assertAllocator(1,1);
		assertTrue(b2 == released(0));
		assertBuffer("123AABB", 16, b);
		assertEquals(1, allocator.getSize());
		allocator.release(b);
		assertEquals(0, allocator.getSize());
		
		//buffer reallocation
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, buffer("1234567890"));
		assertAllocator(2,2);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(1), null);
		f.store(1, buffer("abcdef"));
		assertAllocator(2,2);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(1), null);
		assertEquals(1, allocator.getSize());
		f.store(1, buffer("G"));
		assertEquals(1, allocator.getSize());
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocator.get().get(0), null);
		b = f.complete(1, buffer("H"));
		assertBuffer("1234567890abcdefGH", 32, b);
		assertTrue(b == allocator.get().get(0));
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		assertEquals(1, allocator.getSize());
		allocator.release(b);
		assertEquals(0, allocator.getSize());
		
		//buffer exception
		f.store(1, buffer("1234567890"));
		f.store(1, buffer("1234567890"));
		f.store(1, buffer("1234567890AA"));
		try {
			f.store(1, buffer("C"));
			fail("");
		}
		catch (IndexOutOfBoundsException e) {
		}
	}	
	
	@Test
	public void testStoreWhenSingleToMulti() throws Exception {
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b;
		
		f.store(1, b1);
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertAllocator(0,0);
		assertNull(f.store(2,  b2));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		b = f.complete(1, buffer("4"));
		assertTrue(b == b1);
		assertBuffer("1234", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		b = f.complete(2, buffer("7"));
		assertTrue(b == b2);
		assertBuffer("AA7", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 0);	
	}	
	
	@Test
	public void testStoreWhenMulti() throws Exception {
		
		//optimize = true
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b3 = buffer("CC");
		ByteBuffer b;
		
		f.store(1, b1);
		assertNull(f.store(2,  b2));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertAllocator(0,0);
		assertNull(f.store(2, b3));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertAllocator(0,1);
		assertTrue(b3 == released(0));

		b = f.complete(1, buffer("7"));
		assertTrue(b == b1);
		assertBuffer("1237", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		b = f.complete(2, buffer("70"));
		assertTrue(b == b2);
		assertBuffer("AACC70", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 0);
		
		//optimize = false
		f = prepare(true, false);
		b1 = buffer("123");
		b2 = buffer("AA");
		b3 = buffer("CC");
		
		f.store(1, b1);
		b = f.store(2, b2);
		assertTrue(b == b2);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertBuffer("", 16, b);
		assertAllocator(2,0);
		b = f.store(2, b3);
		assertTrue(b == b3);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertBuffer("", 16, b);
		assertAllocator(2,0);

		b = f.complete(1, buffer("7"));
		assertTrue(b == allocated(0));
		assertBuffer("1237", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		b = f.complete(2, buffer("70"));
		assertTrue(b == allocated(1));
		assertBuffer("AACC70", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 0);

		//buffer reallocation
		f = prepare(true, false);
		f.store(1, buffer("1234567890"));
		assertEquals(1, allocator.getSize());
		f.store(2, buffer("1234567890"));
		f.store(2, buffer("ABCDEF"));
		assertEquals(2, allocator.getSize());
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertEquals(16, allocator.get().get(1).capacity());
		f.store(2, buffer("g"));
		assertEquals(2, allocator.getSize());
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertEquals(32, allocator.get().get(1).capacity());
		b = f.complete(2, buffer("h"));
		assertBuffer("1234567890ABCDEFgh", 32, b);
		assertEquals(2, allocator.getSize());
		allocator.release(b);
		assertEquals(1, allocator.getSize());
		b = f.complete(1, buffer("ABCDEF"));
		assertBuffer("1234567890ABCDEF", 16, b);
		allocator.release(b);
		assertEquals(0, allocator.getSize());

		//buffer exception
		f.store(1, buffer("1234567890"));
		f.store(2, buffer("1234567890"));
		f.store(2, buffer("1234567890"));
		f.store(2, buffer("1234567890AA"));
		try {
			f.store(2, buffer("C"));
			fail("");
		}
		catch (IndexOutOfBoundsException e) {
		}
		
	}	
	
	@Test
	public void testCompleteWhenEmpty() {
		SctpFragments f = prepare(true, true);
		ByteBuffer b = buffer("123456");
		
		assertTrue(b == f.complete(1, b));
		assertBuffer("123456", 16, b);
	}
	
	@Test
	public void testCompleteWhenSingle() throws Exception {
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123456");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b;
		
		f.store(1, b1);
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		assertTrue(b2 == f.complete(2, b2));
		assertAllocator(0,0);
		assertBuffer("AA", 16, b2);
		b = f.complete(1, b2);
		assertTrue(b == b1);
		assertBuffer("123456AA", 16, b);
		assertAllocator(0,1);
		assertTrue(b2 == released(0));
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		
		//buffer reallocation
		f.store(1, buffer("0123456789abcde"));
		b = f.complete(1, buffer("f"));
		assertBuffer("0123456789abcdef", 16, b);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, buffer("0123456789abcdef"));
		b = f.complete(1, buffer("g"));
		assertBuffer("0123456789abcdefg", 32, b);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, buffer("0123456789abcdef"));
		b = f.complete(1, buffer("0123456789abcdef"));
		assertBuffer("0123456789abcdef0123456789abcdef", 32, b);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, buffer("0123456789abcdef"));
		f.store(1, buffer("0123456789abcdef"));
		try {
			b = f.complete(1, buffer("g"));
			fail("");
		}
		catch (IndexOutOfBoundsException e) {
		}
		
		
		f = prepare(true, false);
		b1 = buffer("123456");
		b2 = buffer("AA");

		f.store(1, b1);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		assertTrue(b2 == f.complete(2, b2));
		assertAllocator(1,0);
		assertBuffer("AA", 16, b2);
		b = f.complete(1, b2);
		assertTrue(b == allocated(0));
		assertBuffer("123456AA", 16, b);
		assertAllocator(1,1);
		assertTrue(b2 == released(0));
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);

		f = prepare(false, false);
		b1 = buffer("123456");
		b2 = buffer("AA");

		f.store(1, b1);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		assertTrue(b2 == f.complete(2, b2));
		assertAllocator(1,0);
		assertBuffer("AA", 16, b2);
		b = f.complete(1, b2);
		assertTrue(b == allocated(0));
		assertBuffer("123456AA", 16, b);
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.EMPTY, -1, b2, null);
		
	}	
	
	@Test
	public void testCompleteWhenMulti() throws Exception {
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123456");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b3 = buffer("CCC");
		ByteBuffer b;
		
		f.store(1, b1);
		f.store(2, b2);
		assertAllocator(0,0);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertTrue(b3 == f.complete(3, b3));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertBuffer("CCC", 16, b3);
		b = f.complete(2, b3);
		assertBuffer("AACCC", 16, b);
		assertAllocator(0,1);
		assertTrue(b3 == released(0));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		b = f.complete(1, buffer("X"));
		assertBuffer("123456X", 16, b);
		assertAllocator(0,2);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 0);
		
		//buffer reallocation
		f.store(1, buffer("0123456789abcde"));
		f.store(2, buffer("X"));
		b = f.complete(1, buffer("f"));
		assertBuffer("0123456789abcdef", 16, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		f.store(1, buffer("0123456789abcdef"));
		b = f.complete(1, buffer("g"));
		assertBuffer("0123456789abcdefg", 32, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		f.store(1, buffer("0123456789abcdef"));
		b = f.complete(1, buffer("0123456789abcdef"));
		assertBuffer("0123456789abcdef0123456789abcdef", 32, b);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		f.store(1, buffer("0123456789abcdef"));
		f.store(1, buffer("0123456789abcdef"));
		try {
			b = f.complete(1, buffer("g"));
			fail("");
		}
		catch (IndexOutOfBoundsException e) {
		}		
		
		f = prepare(true, false);
		b1 = buffer("123456");
		b2 = buffer("AA");
		b3 = buffer("CCC");
		
		f.store(1, b1);
		f.store(2, b2);
		assertAllocator(2,0);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertTrue(b3 == f.complete(3, b3));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertBuffer("CCC", 16, b3);
		b = f.complete(2, b3);
		assertBuffer("AACCC", 16, b);
		assertAllocator(2,1);
		assertTrue(b3 == released(0));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);

		f = prepare(false, false);
		b1 = buffer("123456");
		b2 = buffer("AA");
		b3 = buffer("CCC");
		
		f.store(1, b1);
		f.store(2, b2);
		assertAllocator(2,0);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertTrue(b3 == f.complete(3, b3));
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertBuffer("CCC", 16, b3);
		b = f.complete(2, b3);
		assertBuffer("AACCC", 16, b);
		assertAllocator(2,0);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		
	}	
	
	@Test
	public void testRelease() throws Exception {
		SctpFragments f = prepare(true, true);
		ByteBuffer b1 = buffer("123456");
		ByteBuffer b2 = buffer("AA");
		ByteBuffer b3 = buffer("CCC");
		
		f.release();
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b1);
		assertAllocator(0,0);
		assertInternals(f, SctpFragments.State.SINGLE, 1, b1, null);
		f.release();
		assertAllocator(0,1);
		assertTrue(b1 == released(0));
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b2);
		f.store(2, b3);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		f.release();
		assertAllocator(0,3);
		assertTrue(b2 == released(1));
		assertTrue(b3 == released(2));
		
		f = prepare(true, false);
		
		f.release();
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b1);
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		f.release();
		assertAllocator(1,1);
		assertTrue(allocated(0) == released(0));
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b2);
		f.store(2, b3);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		f.release();
		assertAllocator(3,3);
		assertTrue(allocated(1) == released(1));
		assertTrue(allocated(2) == released(2));

		f = prepare(false, false);
		
		f.release();
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b1);
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.SINGLE, 1, allocated(0), null);
		f.release();
		assertAllocator(1,0);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
		f.store(1, b2);
		f.store(2, b3);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		f.release();
		assertAllocator(3,0);
		
	}
	
	void put(ByteBuffer b, String data) {
		b.clear();
		b.put(data.getBytes());
	}
	
	@Test
	public void testAllocationWithOptimize() throws Exception {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		CachingAllocator a = new CachingAllocator(false,m);
		SctpFragments f = new SctpFragments(a, 256, 512, true);
		ByteBuffer in = a.allocate(256);

		//EMPTY
		assertEquals(1, m.getAllocatedCount());
		put(in, "123456");
		in = f.complete(0, in);
		assertEquals(1, m.getAllocatedCount());
		assertBuffer("123456", 256, in);
		
		//SINGLE
		assertNull(f.store(1, in));
		in = a.allocate(256);
		assertEquals(2, m.getAllocatedCount());
		put(in, "ABC");
		in = f.complete(2, in);
		in = f.complete(1, in);
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABC", 256, in);
		
		assertNull(f.store(1, in));
		in = a.allocate(256);
		assertEquals(2, m.getAllocatedCount());
		put(in, "D");
		in = f.complete(2, in);
		in = f.complete(1, in);
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABCD", 256, in);
		
		//MULTI
		assertNull(f.store(0, in));
		in = a.allocate(256);
		put(in, "XXX");
		assertNull(f.store(1, in));
		assertEquals(2, m.getAllocatedCount());
		in = a.allocate(256);
		assertEquals(3, m.getAllocatedCount());
		put(in, "Y");
		in = f.complete(2, in);
		in = f.complete(1, in);
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("XXXY", 256, in);
		
		put(in, "ZZZ");
		assertNull(f.store(1, in));
		assertEquals(3, m.getAllocatedCount());
		in = a.allocate(256);
		assertEquals(3, m.getAllocatedCount());
		put(in, "YY");
		assertNull(f.store(1, in));
		in = a.allocate(256);
		assertEquals(3, m.getAllocatedCount());
		put(in, "VV");
		in = f.complete(2, in);
		in = f.complete(1, in);
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("ZZZYYVV", 256, in);
		
	}
	
	@Test
	public void testAllocationWithoutOptimize() throws Exception {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		CachingAllocator a = new CachingAllocator(false,m);
		SctpFragments f = new SctpFragments(a, 256, 512, false);
		ByteBuffer in = a.allocate(256);
		ByteBuffer in0 = in;
		
		//EMPTY
		assertEquals(1, m.getAllocatedCount());
		put(in, "123456");
		in = f.complete(0, in);
		assertTrue(in == in0);
		assertEquals(1, m.getAllocatedCount());
		assertBuffer("123456", 256, in);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);
	
		//SINGLE
		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(2, m.getAllocatedCount());
		put(in, "ABC");
		in = f.complete(2, in);
		assertTrue(in == in0);
		assertBuffer("ABC", 256, in);
		in = f.complete(1, in);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABC", 256, in);

		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(2, m.getAllocatedCount());
		put(in, "D");
		in = f.complete(2, in);
		assertTrue(in == in0);
		assertBuffer("D", 256, in);
		in = f.complete(1, in);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABCD", 256, in);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);

		//MULTI
		assertTrue(in == f.store(0, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		put(in, "XXX");
		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(3, m.getAllocatedCount());
		put(in, "Y");
		in = f.complete(2, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		in = f.complete(1, in);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("XXXY", 256, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);

		put(in, "ZZZ");
		assertTrue(in == f.store(1, in));
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("", 256, in);
		assertTrue(in == in0);
		put(in, "YY");
		assertTrue(in == f.store(1, in));
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("", 256, in);
		assertTrue(in == in0);
		put(in, "VV");
		in = f.complete(2, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertTrue(in == in0);
		assertBuffer("VV", 256, in);
		in = f.complete(1, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		assertFalse(in == in0);
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("ZZZYYVV", 256, in);

	}

	@Test
	public void testAllocationWithoutRelease() throws Exception {
		DefaultAllocatorMetric m = new DefaultAllocatorMetric();
		DefaultAllocator a = new DefaultAllocator(false,m);
		SctpFragments f = new SctpFragments(a, 256, 512, false);
		ByteBuffer in = a.allocate(256);
		ByteBuffer in0 = in;
		
		//EMPTY
		assertEquals(1, m.getAllocatedCount());
		put(in, "123456");
		in = f.complete(0, in);
		assertTrue(in == in0);
		assertEquals(1, m.getAllocatedCount());
		assertBuffer("123456", 256, in);
		assertInternals(f, SctpFragments.State.EMPTY, -1, null, null);

		//SINGLE
		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(2, m.getAllocatedCount());
		put(in, "ABC");
		in = f.complete(2, in);
		assertTrue(in == in0);
		assertBuffer("ABC", 256, in);
		in = f.complete(1, in);
		assertInternals(f, SctpFragments.State.EMPTY, -1, in0, null);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABC", 256, in);

		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(2, m.getAllocatedCount());
		put(in, "D");
		in = f.complete(2, in);
		assertTrue(in == in0);
		assertBuffer("D", 256, in);
		in = f.complete(1, in);
		assertInternals(f, SctpFragments.State.EMPTY, -1, in0, null);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(2, m.getAllocatedCount());
		assertBuffer("123456ABCD", 256, in);

		//MULTI
		assertTrue(in == f.store(0, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		put(in, "XXX");
		assertTrue(in == f.store(1, in));
		assertTrue(in == in0);
		assertBuffer("", 256, in);
		assertEquals(3, m.getAllocatedCount());
		put(in, "Y");
		in = f.complete(2, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		in = f.complete(1, in);
		assertFalse(in == in0);
		in0 = in;
		assertEquals(3, m.getAllocatedCount());
		assertBuffer("XXXY", 256, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);

		put(in, "ZZZ");
		assertTrue(in == f.store(1, in));
		assertEquals(4, m.getAllocatedCount());
		assertBuffer("", 256, in);
		assertTrue(in == in0);
		put(in, "YY");
		assertTrue(in == f.store(1, in));
		assertEquals(4, m.getAllocatedCount());
		assertBuffer("", 256, in);
		assertTrue(in == in0);
		put(in, "VV");
		in = f.complete(2, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 2);
		assertTrue(in == in0);
		assertBuffer("VV", 256, in);
		in = f.complete(1, in);
		assertInternals(f, SctpFragments.State.MULTI, -1, null, 1);
		assertFalse(in == in0);
		assertEquals(4, m.getAllocatedCount());
		assertBuffer("ZZZYYVV", 256, in);
		
	}	
}
