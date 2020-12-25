/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.core.thread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

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
	
	int index(FastThreadLocal<?> local) throws Exception {
		Field f = FastThreadLocal.class.getDeclaredField("index");
		
		f.setAccessible(true);
		return f.getInt(local);
	}
	
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
		assertEquals("V1", t.getFastThreadLocal(index(V1)));
		assertEquals("V2", v2);
		assertEquals("V2", t.getFastThreadLocal(index(V2)));
		
		t = new FastThreadLocalThread() {
			
			@Override
			public void run() {
				v1 = V1.get();
				v2 = V2.get();
			}
		};
		t.setFastThreadLocal(index(V1), "V1C");
		t.setFastThreadLocal(index(V2), "V2C");
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
		assertEquals("V1S", t.getFastThreadLocal(index(V1)));
		assertEquals("V2S", t.getFastThreadLocal(index(V2)));
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
		assertEquals("V1", t.getFastThreadLocal(index(V1)));
		assertEquals("V1", v1);
		assertEquals("V2", t.getFastThreadLocal(index(V2)));
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
