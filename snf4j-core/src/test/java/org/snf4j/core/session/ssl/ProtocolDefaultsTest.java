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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.junit.Test;
import org.snf4j.core.DTLSTest;

public class ProtocolDefaultsTest {

	String[] stra(String... strings) {
		return strings;
	}

	Set<String> strs(String... strings) {
		Set<String> s = new HashSet<String>();
		
		for (String str: strings) {
			s.add(str);
		}
		return s;
	}
	
	@Test
	public void testIsDtls() {
		TestEngine e = new TestEngine();
		
		e.supportedProtocols = stra("SSL1","TLSv1");
		assertFalse(ProtocolDefaults.isDtls(e));
		e.supportedProtocols = stra("SSL1","DTLSv1");
		assertTrue(ProtocolDefaults.isDtls(e));
		e.supportedProtocols = new String[0];
		assertFalse(ProtocolDefaults.isDtls(e));		
	}
	
	@Test
	public void testInstanceDtls() {
		DTLSTest.assumeJava9();
		
		ProtocolDefaults pd = ProtocolDefaults.instance(true);
		assertNotNull(pd);
		assertTrue(pd == ProtocolDefaults.instance(true));
		assertArrayEquals(stra("DTLSv1.2", "DTLSv1.0"), pd.defaultProtocols());
	}

	@Test
	public void testInstanceNoDtls() {
		DTLSTest.assumeJava8();
		
		try {
			ProtocolDefaults.instance(true);
			fail();
		}
		catch (Error e) {
			assertEquals("Initialization of SSL context for protocol DTLS failed", e.getMessage());
		}
	}
	
	@Test
	public void testDefaultEngine() throws Exception {
		assertNotNull(ProtocolDefaults.defaultEngine("TLS"));
		try {
			ProtocolDefaults.defaultEngine("XXX");
			fail();
		}
		catch (Error e) {
			assertEquals("Initialization of SSL context for protocol XXX failed", e.getMessage());
		}
	}
	
	@Test
	public void testSupportedCiphers() throws Exception {
		TestEngine e = new TestEngine();
		Set<String> s;
		
		e.supportedCipherSuites = stra("C1","C2");
		s = ProtocolDefaults.supportedCiphers(e);
		assertEquals(2, s.size());
		assertTrue(s.contains("C1"));
		assertTrue(s.contains("C2"));
	}

	@Test
	public void testDefaultCiphers() throws Exception {
		TestEngine e = new TestEngine();
		
		e.enabledCipherSuites = stra("C1","C2");
		assertArrayEquals(stra("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"), 
				ProtocolDefaults.defaultCiphers(e, strs("TLS_AES_128_GCM_SHA256","TLS_AES_256_GCM_SHA384")));
		assertArrayEquals(stra("C1", "C2"), ProtocolDefaults.defaultCiphers(e, strs("XXX","XXX2")));
	}
	
	@Test
	public void testSupportedProtocols() throws Exception {
		TestEngine e = new TestEngine();
		Set<String> s;
		
		e.supportedProtocols = stra("C1","C2");
		s = ProtocolDefaults.supportedProtocols(e);
		assertEquals(2, s.size());
		assertTrue(s.contains("C1"));
		assertTrue(s.contains("C2"));
	}

	@Test
	public void testDefaultProtocols() throws Exception {
		TestEngine e = new TestEngine();
		
		e.enabledProtocols = stra("C1","C2");
		assertArrayEquals(stra("TLSv1.2", "TLSv1"), 
				ProtocolDefaults.defaultProtocols(e, strs("TLSv1","TLSv1.2")));
		assertArrayEquals(stra("C1", "C2"), ProtocolDefaults.defaultProtocols(e, strs("XXX","XXX2")));
	}
	
	static class TestEngine extends SSLEngine {
		
		String[] supportedCipherSuites;
		
		String[] supportedProtocols;
		
		String[] enabledCipherSuites;
		
		String[] enabledProtocols;
		
		@Override
		public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
			return null;
		}

		@Override
		public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
			return null;
		}

		@Override
		public Runnable getDelegatedTask() {
			return null;
		}

		@Override
		public void closeInbound() throws SSLException {
		}

		@Override
		public boolean isInboundDone() {
			return false;
		}

		@Override
		public void closeOutbound() {
		}

		@Override
		public boolean isOutboundDone() {
			return false;
		}

		@Override
		public String[] getSupportedCipherSuites() {
			return supportedCipherSuites;
		}

		@Override
		public String[] getEnabledCipherSuites() {
			return enabledCipherSuites;
		}

		@Override
		public void setEnabledCipherSuites(String[] suites) {
		}

		@Override
		public String[] getSupportedProtocols() {
			return supportedProtocols;
		}

		@Override
		public String[] getEnabledProtocols() {
			return enabledProtocols;
		}

		@Override
		public void setEnabledProtocols(String[] protocols) {
		}

		@Override
		public SSLSession getSession() {
			return null;
		}

		@Override
		public void beginHandshake() throws SSLException {
		}

		@Override
		public HandshakeStatus getHandshakeStatus() {
			return null;
		}

		@Override
		public void setUseClientMode(boolean mode) {
		}

		@Override
		public boolean getUseClientMode() {
			return false;
		}

		@Override
		public void setNeedClientAuth(boolean need) {
		}

		@Override
		public boolean getNeedClientAuth() {
			return false;
		}

		@Override
		public void setWantClientAuth(boolean want) {
		}

		@Override
		public boolean getWantClientAuth() {
			return false;
		}

		@Override
		public void setEnableSessionCreation(boolean flag) {
		}

		@Override
		public boolean getEnableSessionCreation() {
			return false;
		}
		
	}
}
