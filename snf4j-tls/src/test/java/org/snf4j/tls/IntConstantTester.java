/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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
package org.snf4j.tls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class IntConstantTester<C extends IntConstant> {
	
	final String entries;
	final Class<C> clazz;
	final Class<C[]> arrayClazz;
	
	public IntConstantTester(String entries, Class<C> clazz, Class<C[]> arrayClazz) {
		this.entries = entries;
		this.clazz = clazz;
		this.arrayClazz = arrayClazz;
	}
	
	@SuppressWarnings("unchecked")
	public void assertValues() throws Exception {
		Field[] fields = clazz.getDeclaredFields();
		C[] known = null;
		String entries = this.entries;
		int count = 0;
	
		for (Field field: fields) {
			field.setAccessible(true);
			Object value = field.get(null);
			
			if (arrayClazz == value.getClass()) {
				known = (C[]) value;
				assertEquals("KNOWN", field.getName());
				continue;
			}
			if (clazz != value.getClass()) {
				continue;
			}
			
			assertSame(clazz, value.getClass());
			
			C type = (C) value;
			
			assertEquals(field.getName().toLowerCase(), type.name());
			assertEquals(field.getName().toUpperCase(), field.getName());
			
			String entry = "|"+type.name()+"("+type.value()+")";
			assertTrue(entries.indexOf(entry) != -1);
			entries = entries.replace(entry, "");
			count++;
		}
		assertTrue(entries.indexOf("|") == -1);
		assertNotNull(known);
		
		for (int i=0; i<known.length; ++i) {
			C type = known[i];
			
			if (type != null) {
				assertEquals(i, type.value());
				--count;
			}
		}
		assertEquals(0, count);
	}
	
	@SuppressWarnings("unchecked")
	public void assertOf() throws Exception {
		Field f = clazz.getDeclaredField("KNOWN");
		f.setAccessible(true);
		C[] known = (C[]) f.get(null);
		Method of = clazz.getDeclaredMethod("of", int.class);
		
		for (int i=0; i<known.length; ++i) {
			C type = known[i];
			of.invoke(null, i);
			if (type != null) {
				assertSame(type, of.invoke(null, i));
				assertTrue(((C)of.invoke(null, i)).isKnown());
				assertSame(of.invoke(null, i), of.invoke(null, i));
			}
			else {
				assertEquals(i, ((C)of.invoke(null, i)).value());
				assertFalse(((C)of.invoke(null, i)).isKnown());
				assertNotSame(of.invoke(null, i), of.invoke(null, i));
			}
		}
		assertEquals(known.length, ((C)of.invoke(null, known.length)).value());
		assertFalse(((C)of.invoke(null, known.length)).isKnown());
		assertNotSame(of.invoke(null, known.length), of.invoke(null, known.length));

		assertEquals(-1, ((C)of.invoke(null, -1)).value());
		assertFalse(((C)of.invoke(null, -1)).isKnown());
		assertNotSame(of.invoke(null, -1), of.invoke(null, -1));
	}
}
