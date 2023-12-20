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
package org.snf4j.tls;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.snf4j.core.engine.HandshakeStatus.FINISHED;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NOT_HANDSHAKING;
import static org.snf4j.core.engine.Status.CLOSED;
import static org.snf4j.core.engine.Status.OK;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.core.session.ssl.SSLContextBuilder;
import org.snf4j.core.session.ssl.SSLEngineBuilder;
import org.snf4j.tls.alert.CertificateRequiredAlert;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.EngineTest;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.session.ISession;

public class TLSEngineSSLEngineTest extends EngineTest {
	
	SSLContext serverCtx;

	SSLContext clientCtx;
	
	ByteBuffer in;
	
	ByteBuffer out;
	
	ByteBuffer in0;
	
	ByteBuffer out0;
	
	@Override
	public void before() throws Exception {
		super.before();
		in = ByteBuffer.allocate(100000);
		out = ByteBuffer.allocate(100000);
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	void clear() {
		in.clear();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}

	void clear(byte[] bytes) {
		in.clear();
		in.put(bytes);
		in.flip();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	void flip() {
		ByteBuffer tmp = in;
		in = out;
		out = tmp;
		in.flip();
		out.clear();
		in0 = in.duplicate();
		out0 = out.duplicate();
	}
	
	byte[] out() {
		ByteBuffer dup = out.duplicate();
		dup.flip();
		byte[] data = new byte[dup.remaining()];
		dup.get(data);
		return data;
	}

	SSLEngine sslServer(ClientAuth clientAuth, X509Certificate trustCert) throws Exception {
		SSLContextBuilder builder = SSLContextBuilder.forServer(key("EC", "secp256r1"), cert("secp256r1"));
		builder.protocol("TLSv1.3").trustManager(trustCert);
		serverCtx = builder.build();
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forServer(serverCtx);
		engineBuilder.clientAuth(clientAuth);
		return engineBuilder.build();
	}

	SSLEngine sslServer() throws Exception {
		return sslServer(ClientAuth.NONE, cert("rsasha256"));
	}
	
	SSLEngine sslServer(SSLContext ctx) throws Exception {
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forServer(ctx);
		return engineBuilder.build();
	}
	
	SSLEngine sslClient(boolean useKeyManager) throws Exception {
		SSLContextBuilder builder = SSLContextBuilder.forClient();
		builder.protocol("TLSv1.3").trustManager(cert("rsasha256"));
		if (useKeyManager) {
			builder.keyManager(key("EC", "secp256r1"), cert("secp256r1"));
		}
		clientCtx = builder.build();
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forClient(clientCtx);
		return engineBuilder.build("snf4j.org", 100);
	}

	SSLEngine sslClient() throws Exception {
		return sslClient(false);
	}
	
	SSLEngine sslClient(SSLContext ctx) throws Exception {
		SSLEngineBuilder engineBuilder = SSLEngineBuilder.forClient(clientCtx);
		return engineBuilder.build("snf4j.org", 100);
	}
	
	void assertInOut(int inChange, int outChange) {
		if (inChange == 0) {
			assertEquals(in, in0);
		} else if (inChange < 0) {
			assertEquals(in0.limit(), in.limit());
			assertEquals(in0.capacity(), in.capacity());
			assertTrue(in.position()-in0.position() >= -inChange);
		} else {
			assertEquals(in0.limit(), in.limit());
			assertEquals(in0.capacity(), in.capacity());
			assertEquals(inChange, in.position()-in0.position());
			
		}

		if (outChange == 0) {
			assertEquals(out, out0);
		} else if (outChange < 0) {
			assertEquals(out0.limit(), out.limit());
			assertEquals(out0.capacity(), out.capacity());
			assertTrue(out.position()-out0.position() >= -outChange);
		} else  {
			assertEquals(out0.limit(), out.limit());
			assertEquals(out0.capacity(), out.capacity());
			assertEquals(outChange, out.position()-out0.position());
		}
	}
	
	static void assertResult(IEngineResult result, Status s, HandshakeStatus hs, int consumed, int produced) {
		assertSame(s, result.getStatus());
		assertSame(hs, result.getHandshakeStatus());

		if (consumed == -1){
			assertTrue(result.bytesConsumed() > 0);
		}
		else {
			assertEquals(consumed, result.bytesConsumed());
		}
		
		if (produced == -1){
			assertTrue(result.bytesProduced() > 0);
		}
		else {
			assertEquals(produced, result.bytesProduced());
		}
	}

	static void assertResult(SSLEngineResult result, Status s, HandshakeStatus hs) {
		assertEquals(s.name(), result.getStatus().name());
		assertEquals(hs.name(), result.getHandshakeStatus().name());
	}

	static void assertEngine(IEngine engine, HandshakeStatus hs) {
		assertSame(hs, engine.getHandshakeStatus());
		assertFalse(engine.isInboundDone());
		assertFalse(engine.isOutboundDone());
	}
	
	static void assertEngine(SSLEngine engine, HandshakeStatus hs) {
		assertEquals(hs.name(), engine.getHandshakeStatus().name());
		assertFalse(engine.isInboundDone());
		assertFalse(engine.isOutboundDone());
	}

	static void assertEngine(SSLEngine engine, HandshakeStatus hs, boolean inDone, boolean outDone) {
		assertEquals(hs.name(), engine.getHandshakeStatus().name());
		assertEquals(inDone, engine.isInboundDone());
		assertEquals(outDone, engine.isOutboundDone());
	}

	static void assertEngine(IEngine engine, HandshakeStatus hs, boolean inDone, boolean outDone) {
		assertSame(hs, engine.getHandshakeStatus());
		assertEquals(inDone, engine.isInboundDone());
		assertEquals(outDone, engine.isOutboundDone());
	}
	
	static void runTasks(SSLEngine engine) {
		for(;;) {
			Runnable task = engine.getDelegatedTask();
			if (task == null) {
				break;
			}
			task.run();
		}
	}

	static void runTasks(TLSEngine engine) {
		for(;;) {
			Runnable task = engine.getDelegatedTask();
			if (task == null) {
				break;
			}
			task.run();
		}
	}

	static void assertLocalCerts(TLSEngine engine, String... certNames) throws Exception {
		assertCerts(((ISession)engine.getSession()).getLocalCertificates(), certNames);
	}

	static void assertPeerCerts(TLSEngine engine, String... certNames) throws Exception {
		assertCerts(((ISession)engine.getSession()).getPeerCertificates(), certNames);
	}
	
	static void assertLocalCerts(SSLEngine engine, String... certNames) throws Exception {
		assertCerts(engine.getSession().getLocalCertificates(), certNames);
	}

	static void assertPeerCerts(SSLEngine engine, String... certNames) throws Exception {
		Certificate[] certs;
		
		try {
			certs = engine.getSession().getPeerCertificates();
		} catch (SSLPeerUnverifiedException e) {
			certs = null;
		}
		assertCerts(certs, certNames);
	}
	
	static void assertCerts(Certificate[] certs, String... certNames) throws Exception {
		if (certNames.length == 0) {
			assertNull(certs);
		}
		else {
			assertEquals(certNames.length, certs.length);
			for (int i=0; i<certNames.length; ++i) {
				assertEquals(certs[i], cert(certNames[i]));
			}
		}
	}
	
	@Test
	public void testClient() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);

		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_TASK);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, 0);
		
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());

		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		
		//new session ticket ->
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		
		//new session ticket <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);

		flip();
		assertResult(tls.unwrap(in, out), Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//biggest application data
		data = new byte[16384];
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 16384, 16384+5+1+16);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(16384, 16384+5+1+16);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		ssl.wrap(in, out);

		flip();
		assertResult(tls.unwrap(in, out), Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position(), out.position());
		int ipos = in.position();
		int opos = out.position();
		assertResult(tls.unwrap(in, out), Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position()-ipos, out.position()-opos);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), out.position());
		assertArrayEquals(data, out());
		
		//closing
		ssl.closeOutbound();
		assertEngine(ssl, NEED_WRAP, false, false);
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, false, true);
		
		assertEngine(tls, NOT_HANDSHAKING, false, false);
		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NEED_WRAP, in.position(), 0);
		assertEngine(tls, NEED_WRAP, true, false);
		assertInOut(in.limit(), 0);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(0, out.position());

		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);
	}

	@Test
	public void testClientWithCRRequestedNoCert() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer(ClientAuth.REQUESTED, cert("rsasha256"));
		ssl.beginHandshake();
		
		handler.certificateSelector.certNames = new String[0];
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);

		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_TASK);
		assertNull(tls.getDelegatedTask());
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		tls.getDelegatedTask().run();
		assertEngine(tls, NEED_WRAP);

		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		
		//new session ticket ->
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		
		//new session ticket <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
		
		assertLocalCerts(ssl, "secp256r1");
		assertPeerCerts(ssl);
		assertLocalCerts(tls);
		assertPeerCerts(tls, "secp256r1");
	}

	@Test
	public void testClientWithCRRequired() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer(ClientAuth.REQUIRED, cert("rsasha256"));
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);

		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_TASK);
		assertNull(tls.getDelegatedTask());
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		tls.getDelegatedTask().run();
		assertEngine(tls, NEED_WRAP);

		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		
		//new session ticket ->
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		
		//new session ticket <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
		
		assertLocalCerts(ssl, "secp256r1");
		assertPeerCerts(ssl, "rsasha256");
		assertLocalCerts(tls, "rsasha256");
		assertPeerCerts(tls, "secp256r1");
	}

	@Test
	public void testClientWithCRRequiredNoCert() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer(ClientAuth.REQUIRED, cert("rsasha256"));
		ssl.beginHandshake();
		
		handler.certificateSelector.certNames = new String[0];
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);

		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_TASK);
		assertNull(tls.getDelegatedTask());
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		tls.getDelegatedTask().run();
		assertEngine(tls, NEED_WRAP);

		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		
		clear();
		try {
			assertResult(ssl.wrap(in, out), OK, NEED_WRAP);
			fail();
		}
		catch (SSLException e) {}
		assertEngine(ssl, NEED_WRAP, true, false);
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);
		
		flip();
		try {
			assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
			fail();
		}
		catch (TLSException e) {
			assertNotNull(e.getAlert());
		}
		assertEngine(tls, NOT_HANDSHAKING, true, true);
	}
	
	@Test
	public void testServer() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, NEED_TASK);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		
		//ServerHello ->
		task.run();
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);
		
		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		assertNull(((ISession)tls.getSession()).getPeerCertificates());
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		clear();
		assertResult(tls.wrap(array(data,0,4,2), out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, 32);

		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//closing
		tls.closeOutbound();
		assertEngine(tls, NEED_WRAP, false, false);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP, false, true);
		assertInOut(0, out.position());
		
		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NEED_WRAP);
		assertEngine(ssl, NEED_WRAP, true, false);
		
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);

		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NOT_HANDSHAKING, in.limit(), 0);
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(in.limit(), 0);
	}

	@Test
	public void testServerWithCRRequestedNoCert() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.clientAuth(ClientAuth.REQUESTED)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertInOut(in.limit(), 0);
		tls.getDelegatedTask().run();
		//ServerHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);
		
		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);

		assertLocalCerts(ssl);
		assertPeerCerts(ssl, "rsasha256");
		assertLocalCerts(tls, "rsasha256");
		assertPeerCerts(tls);
	}

	@Test
	public void testServerWithCRRequiredNoCert() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.clientAuth(ClientAuth.REQUIRED)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertInOut(in.limit(), 0);
		tls.getDelegatedTask().run();
		//ServerHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		try {
			assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);
			fail();
		}
		catch (TLSException e) {
			assertTrue(e.getAlert() instanceof CertificateRequiredAlert);
		}

		assertEngine(tls, NEED_WRAP, true, false);
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		flip();
		try {
			assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		}
		catch (SSLException e) {			
		}
		assertEngine(ssl, NOT_HANDSHAKING, true, true);
		assertNull(tls.getSession());
	}

	@Test
	public void testServerWithCRRequired() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient(true);
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.clientAuth(ClientAuth.REQUIRED)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertInOut(in.limit(), 0);
		tls.getDelegatedTask().run();
		//ServerHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position()-6, 0);
		tls.getDelegatedTask().run();
		assertEngine(tls, NEED_UNWRAP);
		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		
		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		
		assertLocalCerts(ssl, "secp256r1");
		assertPeerCerts(ssl, "rsasha256");
		assertLocalCerts(tls, "rsasha256");
		assertPeerCerts(tls, "secp256r1");
	}
	
	TLSEngine prepareForServerResumption(SSLEngine ssl) throws Exception {
		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		tls.beginHandshake();

		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, NEED_TASK);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		
		//ServerHello ->
		task.run();
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);
		
		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		
		//closing
		ssl.closeOutbound();
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NEED_WRAP, in.position(), 0);
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NOT_HANDSHAKING);
		
		return tls;
	}
	
	@Test
	public void testServerResumption() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslClient();
		ssl.beginHandshake();
		prepareForServerResumption(ssl);
		

		for (int i=0; i<2; ++i) {
			ssl = sslClient(clientCtx);
			ssl.beginHandshake();

			TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
			tls.beginHandshake();

			//ClientHello ->
			clear();
			assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
			//ClientHello <-
			flip();
			assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
			assertEngine(tls, NEED_WRAP);
			assertInOut(in.limit(), 0);
			//ServerHello ->
			clear();
			assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
			assertInOut(0, out.position());
			//ServerHello <-
			flip();
			assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
			ssl.getDelegatedTask().run();
			assertEngine(ssl, NEED_UNWRAP);
			//EncryptedExtension
			clear();
			assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
			assertInOut(0, out.position());
			flip();
			assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
			ssl.getDelegatedTask().run();
			assertEngine(ssl, NEED_WRAP);
			clear();
			assertResult(ssl.wrap(in, out), OK, FINISHED);
			flip();
			assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
			assertInOut(in.limit(), 0);
			clear();
			assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
			flip();
			assertResult(ssl.unwrap(in, out), OK, FINISHED);
			assertEngine(ssl, NOT_HANDSHAKING);
			assertNull(((ISession)tls.getSession()).getPeerCertificates());
		}
	}

	@Test
	public void testServerResumptionWithHRR() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslClient();
		ssl.beginHandshake();
		Object session = prepareForServerResumption(ssl).getSession();
		
		ssl = sslClient(clientCtx);
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.compatibilityMode(true)
				.namedGroups(NamedGroup.FFDHE2048)
				.build()
				, handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		//HRR ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		//HRR <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		//ServerHello... ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		int pos0 = out.position();
		//EncryptedExtension... ->
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_WRAP);
		assertInOut(in.limit(), 0);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		assertSame(session, tls.getSession());

		ssl = sslClient(clientCtx);
		ssl.beginHandshake();
		
		tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.compatibilityMode(true)
				.namedGroups(NamedGroup.FFDHE2048)
				.build()
				, handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		//HRR ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		//HRR <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		tls.getDelegatedTask().run();
		tls.getDelegatedTask().run();
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		//ServerHello... ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		pos0 = out.position();
		//EncryptedExtension... ->
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		ssl.getDelegatedTask().run();
		assertEngine(ssl, NEED_WRAP);
		assertInOut(in.limit(), 0);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		assertSame(session, tls.getSession());
	}
	
	@Test
	public void testClientWithHRR() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.numberOfOfferedSharedKeys(0)
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.compatibilityMode(true)
				.pskKeyExchangeModes()
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);

		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);

		//HelloRetryRequest ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);;

		//HelloRetryRequest <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task = tls.getDelegatedTask();
		assertEngine(tls, HandshakeStatus.NEED_TASK);
		assertNull(tls.getDelegatedTask());

		clear();
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		
		task.run();
		assertEngine(tls, NEED_WRAP);
		
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, -1);
		
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;

		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(in.limit(), 0);
		
		//change_cipher_spec ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);

		//change_cipher_spec <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		clear();
		runTasks(tls);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, 0);
		
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), Status.OK, HandshakeStatus.NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//closing
		ssl.closeOutbound();
		assertEngine(ssl, NEED_WRAP, false, false);
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, false, true);
		
		assertEngine(tls, NOT_HANDSHAKING, false, false);
		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NEED_WRAP, in.position(), 0);
		assertEngine(tls, NEED_WRAP, true, false);
		assertInOut(in.limit(), 0);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(0, out.position());

		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);
	}

	@Test
	public void testServerWithHRR() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.ALL)
				.compatibilityMode(true)
				.namedGroups(NamedGroup.FFDHE2048)
				.build()
				, handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.limit(), 0);
		assertEngine(tls, NEED_WRAP);
		assertInOut(in.limit(), 0);

		
		//HelloRetryRequest ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, 6);
		assertEngine(tls, NEED_UNWRAP);
		
		//HelloRetryRequest <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		
		//ClientHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(in.limit(), 0);
		
		Runnable task1 = tls.getDelegatedTask();
		assertEngine(tls, NEED_TASK);
		Runnable task2 = tls.getDelegatedTask();
		assertEngine(tls, NEED_TASK);
		assertNull(tls.getDelegatedTask());

		task1.run();
		clear();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		assertResult(tls.wrap(in, out), OK, NEED_TASK, 0, 0);
		assertEngine(tls, NEED_TASK);
		assertInOut(0, 0);
		task2.run();
		
		//ServerHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		
		//EncryptedExtension... ->
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(0, out.position());
		
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		
		//change_cipher_spec <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);

		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		
		//application data
		byte[] data = bytes(1,2,3,4,5,6,7,8,9,0);
		clear(data);
		assertResult(tls.wrap(in, out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(10, 32);
		
		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		clear();
		assertResult(tls.wrap(array(data,0,4,2), out), OK, NOT_HANDSHAKING, 10, 32);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, 32);

		flip();
		ssl.unwrap(in, out);
		assertArrayEquals(data, out());
		clear(data);
		ssl.wrap(in, out);
		
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 10);
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(in.limit(), 10);
		assertArrayEquals(data, out());
		
		//closing
		tls.closeOutbound();
		assertEngine(tls, NEED_WRAP, false, false);
		
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP, false, true);
		assertInOut(0, out.position());
		
		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NEED_WRAP);
		assertEngine(ssl, NEED_WRAP, true, false);
		
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		assertEngine(ssl, NOT_HANDSHAKING, true, true);

		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NOT_HANDSHAKING, in.limit(), 0);
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		assertInOut(in.limit(), 0);
	}
	
	TLSEngine prepareForClientResumption(SSLEngine ssl) throws Exception {
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.peerHost("host")
				.peerPort(8000)
				.build(), 
				handler);
		tls.beginHandshake();

		
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		Runnable task = tls.getDelegatedTask();
		task.run();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		//new session ticket ->
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		//new session ticket <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
		//closing
		ssl.closeOutbound();
		clear();
		assertResult(ssl.wrap(in, out), CLOSED, NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), CLOSED, NEED_WRAP, in.position(), 0);
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING, 0, out.position());
		flip();
		assertResult(ssl.unwrap(in, out), CLOSED, NOT_HANDSHAKING);
		return tls;
	}
	
	@Test
	public void testClientResumption() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = prepareForClientResumption(ssl);

		ssl = sslServer(serverCtx);
		ssl.beginHandshake();
		
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.peerHost("host")
				.peerPort(8000)
				.build(), 
				handler);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position(), 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
	}

	@Test
	public void testClientResumptionFallback() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = prepareForClientResumption(ssl);

		ssl = sslServer();
		ssl.beginHandshake();
		
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.peerHost("host")
				.peerPort(8000)
				.build(), 
				handler);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		tls.getDelegatedTask().run();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
	}
	
	@Test
	public void testClientResumptionWithHRR() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = prepareForClientResumption(ssl);

		ssl = sslServer(serverCtx);
		ssl.beginHandshake();
		
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.numberOfOfferedSharedKeys(0)
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.peerHost("host")
				.peerPort(8000)
				.build(), 
				handler);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//HRR ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);;
		//HRR <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position(), 0);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position(), 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
	}

	@Test
	public void testClientResumptionFallbackWithHRR() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		ssl.beginHandshake();
		
		TLSEngine tls = prepareForClientResumption(ssl);

		ssl = sslServer();
		ssl.beginHandshake();
		
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.numberOfOfferedSharedKeys(0)
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.peerHost("host")
				.peerPort(8000)
				.build(), 
				handler);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//HRR ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);;
		//HRR <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position(), 0);
		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		//EncryptedExtensions... ->
		clear();
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		tls.getDelegatedTask().run();
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_WRAP);
		clear();
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
	}
	
	TLSEngine prepareForSerer(SSLEngine ssl) throws Exception {
		TLSEngine tls = new TLSEngine(false, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, NEED_UNWRAP);
		
		//ClientHello ->
		clear();
		ssl.wrap(in, out);
		
		//ClientHello <-
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_TASK, in.limit(), 0);
		//ServerHello ->
		Runnable task = tls.getDelegatedTask();
		task.run();
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, out.position());
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, out.position());
		int pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_WRAP, 0, 6);
		assertEngine(tls, NEED_WRAP);
		assertInOut(0, pos0 + 6);		
		//EncryptedExtension... ->
		pos0 = out.position();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position()-pos0);
		//ServerHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_UNWRAP);
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP);
		assertResult(ssl.wrap(in, out), OK, FINISHED);
		assertEngine(ssl, NOT_HANDSHAKING);
		flip();
		assertResult(tls.unwrap(in, out), OK, NEED_UNWRAP, 6, 0);
		assertEngine(tls, NEED_UNWRAP);
		assertInOut(6, 0);
		assertResult(tls.unwrap(in, out), OK, NEED_WRAP, in.position()-6, 0);
		//NewSessionTicket ->
		clear();
		assertResult(tls.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(tls, NOT_HANDSHAKING);
		assertInOut(0, out.position());
		//NewSessionTicket <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, FINISHED);
		assertNull(((ISession)tls.getSession()).getPeerCertificates());
		return tls;
	}
	
	@Test
	public void testKeyUpdateFromSSLEngine() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = prepareForSerer(ssl);
		
		//application data
		int keyUpdates = 0;
		
		byte[] data = new byte[1000];
		for (int i=0; i<1000; ++i) {
			clear(data);
			ssl.wrap(in, out);
			flip();
			tls.unwrap(in, out);
			if (tls.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
				++keyUpdates;
				clear();
				tls.wrap(in, out);
				flip();
				ssl.unwrap(in, out);
				continue;
			}
			assertArrayEquals(data, out());
			clear(data);
			tls.wrap(in, out);
			flip();
			ssl.unwrap(in, out);
			assertArrayEquals(data, out());
		}
		assertTrue(keyUpdates > 10);
	}

	@Test
	public void testKeyUpdateFromTLSEngine() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		handler.keyLimit = 16384;
		TLSEngine tls = prepareForSerer(ssl);
		
		//application data
		int keyUpdates = 0;
		
		byte[] data = new byte[1000];
		for (int i=0; i<1000; ++i) {
			clear(data);
			tls.wrap(in, out);
			if (tls.getHandshakeStatus() == NEED_WRAP) {
				tls.wrap(in, out);
				flip();
				ssl.unwrap(in, out);
				ssl.unwrap(in, out);
			} else {
				flip();
				ssl.unwrap(in, out);
			}
			if (ssl.getHandshakeStatus().name().equals("NEED_WRAP")) {
				++keyUpdates;
				clear();
				ssl.wrap(in, out);
				flip();
				tls.unwrap(in, out);
				continue;
			}
			assertArrayEquals(data, out());
			clear(data);
			ssl.wrap(in, out);
			flip();
			tls.unwrap(in, out);
			assertArrayEquals(data, out());
		}
		assertTrue(keyUpdates > 10);
	}
	
	@Test
	public void testCloseInbound() throws Exception {
		Assume.assumeTrue(JAVA11);
		
		SSLEngine ssl = sslClient();
		ssl.beginHandshake();

		TLSEngine tls = prepareForSerer(ssl);
		
		try {
			tls.closeInbound();
			fail();
		}
		catch (SessionIncidentException e) {
		}
		assertEngine(tls, NEED_WRAP, true, false);
		clear();
		assertResult(tls.wrap(in, out), CLOSED, NOT_HANDSHAKING,0, out.position());
		assertEngine(tls, NOT_HANDSHAKING, true, true);
		flip();
		try {
			ssl.unwrap(in, out);
			fail();
		}
		catch (SSLException e) {
		}
	}
	
	static String appicationProtocol(SSLEngine engine) throws Exception {
		Method m = SSLEngine.class.getDeclaredMethod("getApplicationProtocol");
		m.setAccessible(true);
		return (String) m.invoke(engine);
	}
	
	static void appicationProtocol(SSLEngine engine, String... names) throws Exception {
		SSLParameters params = engine.getSSLParameters();
		Method m = SSLParameters.class.getDeclaredMethod("setApplicationProtocols", String[].class);
		m.setAccessible(true);
		m.invoke(params, new Object[] {names});
		engine.setSSLParameters(params);
	}
	
	void prepareForClient(TLSEngine cli, SSLEngine srv) throws Exception {
		srv.beginHandshake();
		assertEngine(cli, NOT_HANDSHAKING);
		cli.beginHandshake();
		assertEngine(cli, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(cli.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		//ClientHello <-
		flip();
		assertResult(srv.unwrap(in, out), OK, NEED_TASK);
		runTasks(srv);
		assertEngine(srv, NEED_WRAP);
		//ServerHello ->
		clear();
		assertResult(srv.wrap(in, out), OK, NEED_WRAP);;
		//ServerHello <-
		flip();
		assertResult(cli.unwrap(in, out), OK, NEED_UNWRAP, in.position(), 0);
		assertEngine(cli, NEED_UNWRAP);
		//EncryptedExtensions... ->
		clear();
		assertResult(srv.wrap(in, out), OK, NEED_UNWRAP);
		flip();
		assertResult(cli.unwrap(in, out), OK, NEED_TASK, in.position(), 0);
		assertEngine(cli, NEED_TASK);
		Runnable task = cli.getDelegatedTask();
		assertEngine(cli, HandshakeStatus.NEED_TASK);
		assertNull(cli.getDelegatedTask());
		clear();
		task.run();
		assertResult(cli.unwrap(in, out), OK, NEED_WRAP, 0, 0);
		assertEngine(cli, NEED_WRAP);
		clear();
		assertResult(cli.wrap(in, out), OK, FINISHED, 0, out.position());
		assertEngine(cli, HandshakeStatus.NOT_HANDSHAKING);
		flip();
		assertResult(srv.unwrap(in, out), OK, NEED_WRAP);
		//new session ticket ->
		clear();
		assertResult(srv.wrap(in, out), OK, FINISHED);
		flip();
		//new session ticket <-
		assertResult(cli.unwrap(in, out), OK, NOT_HANDSHAKING, in.position(), 0);
	}
	
	@Test
	public void testClientWithALPNIgnoredByServer() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.applicationProtocols("xxx")
				.build(), 
				handler);
		prepareForClient(tls, ssl);
		assertEquals("", appicationProtocol(ssl));
		assertEquals("CV|PN(null)|", handler.trace());

		ssl = sslServer();
		appicationProtocol(ssl, "xxx");
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.build(), 
				handler);
		prepareForClient(tls, ssl);
		assertEquals("", appicationProtocol(ssl));
		assertEquals("CV|PN(null)|", handler.trace());
	}

	@Test
	public void testClientWithALPNAcceptedByServer() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		appicationProtocol(ssl, "xxx");
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.applicationProtocols("yyy","xxx")
				.build(), 
				handler);
		prepareForClient(tls, ssl);
		assertEquals("xxx", appicationProtocol(ssl));
		assertEquals("CV|PN(xxx)|", handler.trace());

		ssl = sslServer();
		appicationProtocol(ssl, "xxx");
		tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.applicationProtocols("xxx")
				.build(), 
				handler);
		prepareForClient(tls, ssl);
		assertEquals("xxx", appicationProtocol(ssl));
		assertEquals("CV|PN(xxx)|", handler.trace());
	}

	@Test
	public void testClientWithALPNRejectedByServer() throws Exception {
		Assume.assumeTrue(JAVA11);

		SSLEngine ssl = sslServer();
		appicationProtocol(ssl, "yyy");
		ssl.beginHandshake();
		
		TLSEngine tls = new TLSEngine(true, new EngineParametersBuilder()
				.delegatedTaskMode(DelegatedTaskMode.CERTIFICATES)
				.applicationProtocols("xxx")
				.build(), 
				handler);
		assertEngine(tls, NOT_HANDSHAKING);
		tls.beginHandshake();
		assertEngine(tls, HandshakeStatus.NEED_WRAP);

		//ClientHello ->
		clear();
		assertResult(tls.wrap(in, out), OK, NEED_UNWRAP, 0, out.position());
		assertEngine(tls, NEED_UNWRAP);
		//ClientHello <-
		flip();
		assertResult(ssl.unwrap(in, out), OK, NEED_TASK);
		runTasks(ssl);
		assertEngine(ssl, NEED_WRAP, true, false);
		clear();
		try {
			ssl.wrap(in, out);
			fail();
		}
		catch(SSLException e) {
		}
	}

}
