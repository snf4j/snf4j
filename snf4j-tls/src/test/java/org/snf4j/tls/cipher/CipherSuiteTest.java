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
package org.snf4j.tls.cipher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Test;

public class CipherSuiteTest {
	
	final static String ENTRIES = 
			"| TLS_AES_128_GCM_SHA256 | {0x13,0x01} |"+
			"| TLS_AES_256_GCM_SHA384 | {0x13,0x02} |"+
			"| TLS_CHACHA20_POLY1305_SHA256 | {0x13,0x03} |"+
			"| TLS_AES_128_CCM_SHA256 | {0x13,0x04} |"+
			"| TLS_AES_128_CCM_8_SHA256 | {0x13,0x05} |";
	 
	@Test
	public void testValues() throws Exception {
		String[] entries = ENTRIES.split("\\|");
		
		for (int i=0; i<entries.length; i+=3) {
			String name = entries[i+1].trim();
			String value = entries[i+2].replace("{", "").replace("}", "").replace("0x", "").replace(",", "").trim();
			
			Field f = CipherSuite.class.getDeclaredField(name);
			CipherSuite cs = (CipherSuite) f.get(null);
			assertEquals(Integer.parseInt(value, 16), cs.value());
			assertSame(cs, CipherSuite.of(cs.value()));
			assertTrue(cs.isKnown());
		}
	}

	@Test
	public void testOf() throws Exception {
		int[] values = new int[] {0x1300,0x1306,0x130f,0,0xffff};
		
		for (int i=0; i<values.length; ++i) {
			assertEquals(values[i], CipherSuite.of(values[i]).value());
			assertFalse(CipherSuite.of(values[i]).isKnown());
		}
	}	
}
