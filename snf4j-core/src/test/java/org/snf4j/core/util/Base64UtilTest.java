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
package org.snf4j.core.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Test;

public class Base64UtilTest {
	
	@Test
	public void testIsClass() throws Exception {
		Field f = Base64Util.class.getDeclaredField("JAVA_UTIL_BASE64");
		
		f.setAccessible(true);
		assertEquals(java.util.Base64.class.getName(), f.get(null));
		assertTrue(Base64Util.isClass(java.util.Base64.class.getName()));
		assertFalse(Base64Util.isClass("java.util.xxxx"));	
	}
	
	@Test
	public void testNoJdk() throws Exception {
		Field f = Base64Util.class.getDeclaredField("USE_JDK");
		
		f.setAccessible(true);
		assertTrue(f.getBoolean(null));

		byte[] encoded = Base64Util.encode(new byte[10], false);
		assertArrayEquals(encoded, Base64Util.encode(new byte[10]));
		assertArrayEquals(new byte[10], Base64Util.decode(encoded, false));
		assertArrayEquals(new byte[10], Base64Util.decode(encoded));
		try {
			Base64Util.decode("????".getBytes(StandardCharsets.US_ASCII), false);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals("data is not in valid Base64 scheme", e.getMessage());
		}
		try {
			Base64Util.decode("????".getBytes(StandardCharsets.US_ASCII));
			fail();
		}
		catch (Exception e) {
			assertNotEquals("data is not in valid Base64 scheme", e.getMessage());
		}
	}
	
	String[] testVectors = new String[] {
			"","",
			"f","Zg==",
			"fo","Zm8=",
			"foo","Zm9v",
			"foob","Zm9vYg==",
			"fooba","Zm9vYmE=",
			"foobar","Zm9vYmFy"};

	@Test
	public void testEncode() throws Exception {
		for (int i=0; i<testVectors.length; i+=2) {
			assertEquals(testVectors[i+1], Base64Util.encode0(testVectors[i].getBytes(), StandardCharsets.US_ASCII));
			assertEquals(testVectors[i+1], Base64Util.encode(testVectors[i].getBytes(), StandardCharsets.US_ASCII));
		}
		
		String s,e;
		
		for (int size=0; size < 6; size++) {
			byte[] b = new byte[size];

			for (int i=0; i<256; ++i) {
				for (int j=0; j<size; ++j) {
					b[j] = (byte)i;
				}
				s = Base64Util.encode0(b, StandardCharsets.US_ASCII);
				e = java.util.Base64.getEncoder().encodeToString(b);
				assertEquals(Arrays.toString(b), e, s);
				s = Base64Util.encode(b, StandardCharsets.US_ASCII);
				assertEquals(Arrays.toString(b), e, s);
			}
		}
	}

	@Test
	public void testDecode() {
		for (int i=0; i<testVectors.length; i+=2) {
			assertArrayEquals(testVectors[i].getBytes(), Base64Util.decode0(testVectors[i+1], StandardCharsets.US_ASCII));
			assertArrayEquals(testVectors[i].getBytes(), Base64Util.decode(testVectors[i+1], StandardCharsets.US_ASCII));
		}

		String[] failingVectors = new String[] {
				"A", "?", "A?", "A===", "A==", "A=",
				"AA===","AAAAA","AAAAA===","AAAAA==","AAAAA=","AAA?" 
		};
		for (int i=0; i<failingVectors.length; ++i) {
			assertNull(Base64Util.decode0(failingVectors[i], StandardCharsets.US_ASCII));
			try {
				Base64Util.decode(failingVectors[i], StandardCharsets.US_ASCII);
				fail();
			}
			catch (IllegalArgumentException e) {
			}
		}

		String[] okVectors = new String[] {
				"==","AA==","AA","AAA","AAAA",
				"AAAABB","AAAABB=","AAAABB==","AZaz09+/" 
		};
		for (int i=0; i<okVectors.length; ++i) {
			assertNotNull(Base64Util.decode0(okVectors[i], StandardCharsets.US_ASCII));
		}
		
		String chars = " AZaz09+/=?";
		int charsLen = chars.length();
		char c;
		StringBuilder sb = new StringBuilder();
		byte[] b;
		String s;
		
		for (int i0=0; i0<charsLen; ++i0) {
			for (int i1=0; i1<charsLen; ++i1) {
				for (int i2=0; i2<charsLen; ++i2) {
					for (int i3=0; i3<charsLen; ++i3) {
						sb.setLength(0);
						c = chars.charAt(i0);
						sb.append(c == ' ' ? "" : c);
						c = chars.charAt(i1);
						sb.append(c == ' ' ? "" : c);
						c = chars.charAt(i2);
						sb.append(c == ' ' ? "" : c);
						c = chars.charAt(i3);
						sb.append(c == ' ' ? "" : c);
						s = sb.toString();
						
						for (int i=0; i<3; ++i) {
							try {
								b = java.util.Base64.getDecoder().decode(s);
							}
							catch (Exception e) {
								b = null;
							}

							if (b == null) {
								if (null != Base64Util.decode0(s, StandardCharsets.US_ASCII)) {
									if (s.endsWith("==")) {
										b = java.util.Base64.getDecoder().decode(s.substring(0, s.length()-2));
									}
									else if (s.endsWith("=")) {
										b = java.util.Base64.getDecoder().decode(s.substring(0, s.length()-1));
									}
									else {
										fail(s);
									}
								}
							}

							if (b != null) {
								assertArrayEquals(b, Base64Util.decode0(s, StandardCharsets.US_ASCII));
							}
							s = "ABCD" + s;
						}
					}
				}
			}
		}
	}
	
}
