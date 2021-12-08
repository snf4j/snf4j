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
package org.snf4j.core.session.ssl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import org.junit.Test;
import org.snf4j.core.DTLSTest;
import org.snf4j.core.session.SSLEngineCreateException;

public class SSLEngineBuilderTest {

	static String[] protocols(SSLEngineBuilder b) throws Exception {
		Field f = SSLEngineBuilder.class.getDeclaredField("protocols");
		
		f.setAccessible(true);
		return (String[]) f.get(b);
	}

	static String[] ciphers(SSLEngineBuilder b) throws Exception {
		Field f = SSLEngineBuilder.class.getDeclaredField("ciphers");
		
		f.setAccessible(true);
		return (String[]) f.get(b);
	}
	
	static boolean forServer(SSLEngineBuilder b) throws Exception {
		Field f = SSLEngineBuilder.class.getDeclaredField("forServer");
		
		f.setAccessible(true);
		return f.getBoolean(b);
	}
	
	static ClientAuth clientAuth(SSLEngineBuilder b) throws Exception {
		Field f = SSLEngineBuilder.class.getDeclaredField("clientAuth");
		
		f.setAccessible(true);
		return (ClientAuth) f.get(b);
	}
	
	static String[] strings(String... s) {
		return s;
	}
	
	@Test
	public void testGet() {
		assertNull(SSLEngineBuilder.get("xxx", boolean.class));
		assertNotNull(SSLEngineBuilder.get("setNeedClientAuth", boolean.class));
	}
	
	@Test
	public void testSet() throws Exception {
		Method m = SSLEngineBuilder.get("setNeedClientAuth", boolean.class);
		
		SSLParameters params = new SSLParameters();
		assertFalse(params.getNeedClientAuth());
		SSLEngineBuilder.set(null, params, true);
		assertFalse(params.getNeedClientAuth());
		SSLEngineBuilder.set(m, params, true);
		assertTrue(params.getNeedClientAuth());
	}
	
	@Test
	public void testNoDtls() throws Exception {
		DTLSTest.assumeJava8();

		Field f = SSLEngineBuilder.class.getDeclaredField("ENABLE_RETRANSMISSIONS");
		f.setAccessible(true);
		assertNull(f.get(null));
		f = SSLEngineBuilder.class.getDeclaredField("MAXIMUM_PACKET_SIZE");
		f.setAccessible(true);
		assertNull(f.get(null));
	}

	@Test
	public void testDtls() throws Exception {
		DTLSTest.assumeJava9();

		Field f = SSLEngineBuilder.class.getDeclaredField("ENABLE_RETRANSMISSIONS");
		f.setAccessible(true);
		assertNotNull(f.get(null));
		f = SSLEngineBuilder.class.getDeclaredField("MAXIMUM_PACKET_SIZE");
		f.setAccessible(true);
		assertNotNull(f.get(null));
		
		SSLEngine e = SSLContextBuilder.forClient().enableRetransmissions(true).engineBuilder().build();
		Method m = SSLParameters.class.getDeclaredMethod("getEnableRetransmissions");
		m.setAccessible(true);
		assertTrue((Boolean)m.invoke(e.getSSLParameters()));
		e = SSLContextBuilder.forClient().enableRetransmissions(false).engineBuilder().build();
		assertFalse((Boolean)m.invoke(e.getSSLParameters()));
		e = SSLContextBuilder.forClient().engineBuilder().build();
		assertFalse((Boolean)m.invoke(e.getSSLParameters()));

		e = SSLContextBuilder.forClient().maximumPacketSize(111).engineBuilder().build();
		m = SSLParameters.class.getDeclaredMethod("getMaximumPacketSize");
		m.setAccessible(true);
		assertEquals((Integer)111, (Integer)m.invoke(e.getSSLParameters()));
		e = SSLContextBuilder.forClient().maximumPacketSize(111).maximumPacketSize(-1).engineBuilder().build();
		Integer i = (Integer)m.invoke(e.getSSLParameters());
		e = SSLContextBuilder.forClient().engineBuilder().build();
		assertEquals(i, (Integer)m.invoke(e.getSSLParameters()));
		
	}
	
	@Test
	public void testClone() throws Exception {
		SSLEngineBuilder b1 = SSLEngineBuilder.forClient(SSLContext.getDefault());
		SSLEngineBuilder b2 = b1.clone();
		
		assertTrue(b1 != b2);
		assertTrue(b1.context() == b2.context());
		assertNull(protocols(b1));
		assertNull(protocols(b2));
		assertFalse(forServer(b1));
		assertFalse(forServer(b2));
		
		b1.protocols("P1", "P2");
		b2 = b1.clone();
		assertArrayEquals(strings("P1","P2"), protocols(b1));
		assertArrayEquals(strings("P1","P2"), protocols(b2));
		b2.protocols("P3");
		assertArrayEquals(strings("P1","P2"), protocols(b1));
		assertArrayEquals(strings("P3"), protocols(b2));

		b1 = SSLEngineBuilder.forServer(SSLContext.getDefault());
		b2 = b1.clone();
		assertTrue(forServer(b1));
		assertTrue(forServer(b2));
		
		b1.ciphers("C1", "C2");
		b2 = b1.clone();
		assertArrayEquals(strings("C1","C2"), ciphers(b1));
		assertArrayEquals(strings("C1","C2"), ciphers(b2));
		b2.ciphers("C3");
		assertArrayEquals(strings("C1","C2"), ciphers(b1));
		assertArrayEquals(strings("C3"), ciphers(b2));
		
		assertEquals(ClientAuth.NONE, clientAuth(b1));
		assertEquals(ClientAuth.NONE, clientAuth(b2));
		b1.clientAuth(ClientAuth.REQUESTED);
		b2 = b1.clone();
		assertEquals(ClientAuth.REQUESTED, clientAuth(b1));
		assertEquals(ClientAuth.REQUESTED, clientAuth(b2));
		
		b1 = new SSLEngineBuilder(SSLContext.getDefault(), false) {
			
			@Override
			SSLEngineBuilder superClone() throws CloneNotSupportedException {
				throw new CloneNotSupportedException();
			}			
		};
		try {
			b1.clone();
			fail();
		}
		catch (RuntimeException e) {}
	}
	
	@Test
	public void testBuild() throws Exception {
		SSLEngineBuilder b = SSLEngineBuilder.forClient(SSLContext.getDefault());
		
		SSLEngine e = b.build();
		assertNotNull(e);
		assertNull(e.getPeerHost());
		assertEquals(-1, e.getPeerPort());
		assertTrue(e.getUseClientMode());
		
		try {
			b.protocols("XXX").build();
		}
		catch (SSLEngineCreateException ex) {
			assertEquals("Building of SSL engine failed", ex.getMessage());
		}

		b = SSLEngineBuilder.forServer(SSLContext.getDefault());
		e = b.build("snf4j.org", 777);
		assertNotNull(e);
		assertEquals("snf4j.org", e.getPeerHost());
		assertEquals(777, e.getPeerPort());
		assertFalse(e.getUseClientMode());
		
		try {
			b.protocols("XXX").build("snf4j.org", 777);
		}
		catch (SSLEngineCreateException ex) {
			assertEquals("Building of SSL engine with peer information failed", ex.getMessage());
		}
	}
}
