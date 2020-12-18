package org.snf4j.core.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FastThreadLocalTest {

	final static FastThreadLocal<String> V1 = new FastThreadLocal<String>() {
		
		@Override
		protected String initialValue() {
			return "V1";
		}
	};
	
	final static FastThreadLocal<String> V2 = new FastThreadLocal<String>(false) {
		
		@Override
		protected String initialValue() {
			return "V2";
		}
	};

	final static FastThreadLocal<String> V3 = new FastThreadLocal<String>() {
		
		@Override
		protected String initialValue() {
			return "V3";
		}
	};
	
	final static FastThreadLocal<String> V4 = new FastThreadLocal<String>(false) {
		
		@Override
		protected String initialValue() {
			return "V4";
		}
	};

	final static FastThreadLocal<String> V5 = new FastThreadLocal<String>() {
		
		@Override
		protected String initialValue() {
			return "V5";
		}
	};
	
	final static FastThreadLocal<String> V6 = new FastThreadLocal<String>(false) {
		
		@Override
		protected String initialValue() {
			return "V6";
		}
	};
	
	volatile String v1;
	volatile String v2;
	
	@Test
	public void testGet() throws Exception {		
		FastThreadLocalThread t = new FastThreadLocalThread() {
			
			@Override
			public void run() {
				v1 = V1.get();
				v2 = V2.get();
			}
		};
		
		assertTrue(V1.isForAllThreads());
		assertFalse(V2.isForAllThreads());
		assertEquals("V1", V1.get());
		assertNull(V2.get());
		t.start();
		t.join(1000);
		assertEquals("V1", v1);
		assertEquals("V1", t.getFastThreadLocal(0));
		assertEquals("V2", v2);
		assertEquals("V2", t.getFastThreadLocal(1));
		
		t = new FastThreadLocalThread() {
			
			@Override
			public void run() {
				v1 = V1.get();
				v2 = V2.get();
			}
		};
		t.setFastThreadLocal(0, "V1C");
		t.setFastThreadLocal(1, "V2C");
		t.start();
		t.join(1000);
		assertEquals("V1C", v1);
		assertEquals("V2C", v2);
		assertEquals("V1", V1.get());
		assertNull(V2.get());
	}
	
	@Test
	public void testSet() throws Exception {
		FastThreadLocalThread t = new FastThreadLocalThread() {
			
			@Override
			public void run() {
				V1.set("V1S");
				V2.set("V2S");
				v1 = V1.get();
				v2 = V2.get();
			}
		};
		
		assertTrue(V3.isForAllThreads());
		assertFalse(V4.isForAllThreads());
		t.start();
		t.join(1000);
		assertEquals("V1S", t.getFastThreadLocal(0));
		assertEquals("V2S", t.getFastThreadLocal(1));
		assertEquals("V1S", v1);
		assertEquals("V2S", v2);		
		assertEquals("V3", V3.get());
		assertNull(V4.get());
		V3.set("V3S");
		assertEquals("V3S", V3.get());
		assertNull(V4.get());
		V4.set("V4S");
		assertNull(V4.get());
	}
	
	@Test
	public void testRemove() throws Exception {
		FastThreadLocalThread t = new FastThreadLocalThread() {
			
			@Override
			public void run() {
				V1.set("V1R");
				V2.set("V2R");
				V1.remove();
				V2.remove();
				v1 = V1.get();
				v2 = V2.get();
			}
		};
		
		assertTrue(V5.isForAllThreads());
		assertFalse(V6.isForAllThreads());
		V5.set("V5R");
		V6.set("V6R");
		t.start();
		t.join();
		assertEquals("V1", t.getFastThreadLocal(0));
		assertEquals("V1", v1);
		assertEquals("V2", t.getFastThreadLocal(1));
		assertEquals("V2", v2);
		assertEquals("V5R", V5.get());
		assertNull(V6.get());
		V5.remove();
		assertEquals("V5", V5.get());
		assertNull(V6.get());
		V6.remove();
		assertNull(V6.get());
		
	}
}
