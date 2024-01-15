/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;

import javax.crypto.Mac;

import org.junit.Test;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.TestSession;

public class EngineStateWrapperTest {

	@Test
	public void testAll() throws Exception {
		EngineState s = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		TranscriptHash th1 = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		TranscriptHash th2 = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		ISession session = new TestSession(1,100);
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		KeySchedule ks1 = new KeySchedule(h, th1, CipherSuite.TLS_AES_128_GCM_SHA256.spec());
		KeySchedule ks2 = new KeySchedule(h, th2, CipherSuite.TLS_AES_128_GCM_SHA256.spec());
		EarlyDataContext ctx = new EarlyDataContext(CipherSuite.TLS_AES_128_GCM_SHA256,100);
		
		EngineStateWrapper w = new EngineStateWrapper(s, null);
		assertNotNull(s.getParameters());
		assertSame(s.getParameters(), w.getParameters());
		assertNotNull(s.getHandler());
		assertSame(s.getHandler(), w.getHandler());
		assertNotNull(s.getState());
		assertSame(s.getState(), w.getState());
		assertTrue(w.isClientMode());
		assertFalse(w.isStarted());
		assertFalse(w.isConnected());
		s.changeState(MachineState.CLI_WAIT_1_SH);
		assertSame(MachineState.CLI_WAIT_1_SH, w.getState());
		assertTrue(w.isStarted());
		assertFalse(w.isConnected());
		s.changeState(MachineState.CLI_CONNECTED);
		assertSame(MachineState.CLI_CONNECTED, w.getState());
		assertTrue(w.isStarted());
		assertTrue(w.isConnected());
		assertNull(w.getTranscriptHash());
		s.setTranscriptHash(th1);
		assertSame(th1, w.getTranscriptHash());
		assertNull(w.getSession());
		s.setSession(session);
		assertSame(session, w.getSession());
		assertNull(w.getKeySchedule());
		assertNull(w.getCipherSuite());
		assertNull(s.getApplicationProtocol());
		assertNull(w.getApplicationProtocol());
		s.initialize(ks1, CipherSuite.TLS_AES_256_GCM_SHA384);
		s.setApplicationProtocol("proto");
		assertSame(ks1, w.getKeySchedule());
		assertSame(CipherSuite.TLS_AES_256_GCM_SHA384, w.getCipherSuite());
		assertNull(w.getHostName());
		assertEquals("proto", w.getApplicationProtocol());
		assertEquals("proto", s.getApplicationProtocol());
		s.setHostName("host");
		assertEquals("host", w.getHostName());
		assertEquals(0, w.getVersion());
		s.setVersion(100);
		assertEquals(100, w.getVersion());
		assertEquals(16384, w.getMaxFragmentLength());
		assertSame(NoEarlyDataContext.INSTANCE, w.getEarlyDataContext());
		s.setEarlyDataContext(ctx);
		assertSame(ctx, w.getEarlyDataContext());
		
		w = new EngineStateWrapper(s, ks2);
		assertSame(ks2, w.getKeySchedule());
		assertSame(th2, w.getTranscriptHash());
	}
}
