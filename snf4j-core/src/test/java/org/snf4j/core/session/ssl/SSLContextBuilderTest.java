/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021-2024 SNF4J contributors
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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.DestroyFailedException;

import org.junit.Test;
import org.snf4j.core.SSLSession;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.AbstractSessionFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.IStreamHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;

public class SSLContextBuilderTest {

	static final File KEYSTORE_JKS = file("keystore.jks");

	static final File KEYSTORE_P12 = file("keystore.p12");

	static final File KEYSTORE_PEM = file("keystore.pem");

	static final File KEY_PEM = file("key.pem");

	static final File ENCKEY_PEM = file("enckey.pem");

	static final File KEYSTORE_DSA_PEM = file("keystore_dsa.pem");

	static final File KEY_DSA_PEM = file("key_dsa.pem");

	static final File ENCKEY_DSA_PEM = file("enckey_dsa.pem");

	static final File KEYSTORE_EC_PEM = file("keystore_ec.pem");

	static final File KEY_EC_PEM = file("key_ec.pem");

	static final File ENCKEY_EC_PEM = file("enckey_ec.pem");
	
	static final File CERT_PEM = file("cert.pem");

	static final char[] PASSWORD = "password".toCharArray();
	
	SSLContext c;
	
	SSLContextBuilder b;
	
	static InputStream in(File f) throws IOException {
		return in(f, null, 0);
	}
	
	static InputStream in(File f, byte[] overwrite, int off) throws IOException {
		byte[] buf = new byte[10000];
		int len;
		
		FileInputStream in = new FileInputStream(f);
		try {
			len = in.read(buf);
		}
		finally {
			in.close();
		}
		if (overwrite != null) {
			System.arraycopy(overwrite, 0, buf, off, overwrite.length);
		}
		return new ByteArrayInputStream(buf, 0, len);
	}
	
	static File file(String name) {
		return new File(SSLContextBuilderTest.class.getClassLoader().getResource(name).getFile());
	}
	
	static PrivateKey key(SSLContextBuilder b) throws Exception {
		Field f = SSLContextBuilder.class.getDeclaredField("key");
		
		f.setAccessible(true);
		return (PrivateKey) f.get(b);
	}

	static char[] password(SSLContextBuilder b) throws Exception {
		Field f = SSLContextBuilder.class.getDeclaredField("password");
		
		f.setAccessible(true);
		return (char[]) f.get(b);
	}

	static X509Certificate[] certs(SSLContextBuilder b, boolean trust) throws Exception {
		Field f = SSLContextBuilder.class.getDeclaredField(trust ? "trustCerts" : "keyCerts");
		
		f.setAccessible(true);
		return (X509Certificate[]) f.get(b);
	}

	static Object manager(SSLContextBuilder b, boolean trust) throws Exception {
		Field f = SSLContextBuilder.class.getDeclaredField(trust ? "trustManager" : "keyManager");
		
		f.setAccessible(true);
		return f.get(b);
	}
	
	@Test
	public void testForClient() throws Exception {
		Provider provider;
		
		c = (b = SSLContextBuilder.forClient()).build();
		assertNotNull(c);
		assertTrue(c != SSLContext.getDefault());
		assertEquals("TLS", c.getProtocol());
		assertTrue(b.isForClient());
		assertFalse(b.isForServer());
		
		c = SSLContextBuilder.forClient().protocol(null).build();
		assertNotNull(c);
		assertTrue(c == SSLContext.getDefault());
		
		c = SSLContextBuilder.forClient().protocol("TLS").build();
		assertFalse(c == SSLContext.getDefault());
		assertEquals("TLS", c.getProtocol());
		provider = c.getProvider();
		c = SSLContextBuilder.forClient().protocol("TLS").provider(provider).build();
		assertEquals("TLS", c.getProtocol());
		c = SSLContextBuilder.forClient().protocol("TLS").providerName(provider.getName()).build();
		assertEquals("TLS", c.getProtocol());
		
		c = SSLContextBuilder.forClient().protocol("TLS").sessionTimeout(1234).build();
		assertEquals(1234, c.getClientSessionContext().getSessionTimeout());
		c = SSLContextBuilder.forClient().protocol("TLS").sessionCacheSize(5678).build();
		assertEquals(5678, c.getClientSessionContext().getSessionCacheSize());
		
		try {
			SSLContextBuilder.forClient().protocol("XXXX").build();
			fail();
		}
		catch (SSLContextCreateException e) {
			assertTrue(NoSuchAlgorithmException.class == e.getCause().getClass());
			assertEquals("Building of the SSL context failed", e.getMessage());
		}
	}

	@Test
	public void testForServer() throws Exception {
		X509Certificate[] certs;
		PrivateKey key;
		
		c = (b = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, CERT_PEM).protocol("TLS")).build();
		assertEquals("TLS", c.getProtocol());
		c = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM).protocol("TLS").sessionTimeout(1233).build();
		assertEquals(1233, c.getServerSessionContext().getSessionTimeout());
		c = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM).protocol("TLS").sessionCacheSize(4566).build();
		assertEquals(4566, c.getServerSessionContext().getSessionCacheSize());
		assertFalse(b.isForClient());
		assertTrue(b.isForServer());

		FileInputStream keyIn = new FileInputStream(ENCKEY_PEM);
		try {
			FileInputStream certIn = new FileInputStream(CERT_PEM);
			try {
				c = SSLContextBuilder.forServer(keyIn, PASSWORD, certIn).protocol("TLS").build();
				assertEquals("TLS", c.getProtocol());
			}
			finally {
				certIn.close();
			}
		}
		finally {
			keyIn.close();
		}
		
		keyIn = new FileInputStream(KEY_PEM);
		try {
			FileInputStream certIn = new FileInputStream(CERT_PEM);
			try {
				c = SSLContextBuilder.forServer(keyIn, certIn).protocol("TLS").build();
				assertEquals("TLS", c.getProtocol());
			}
			finally {
				certIn.close();
			}
		}
		finally {
			keyIn.close();
		}
		
		SSLContextBuilder b = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, CERT_PEM);
		key = key(b);
		certs = certs(b, false);
		c = SSLContextBuilder.forServer(key, PASSWORD, certs).protocol("TLS").build();
		assertEquals("TLS", c.getProtocol());
		b = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM);
		key = key(b);
		certs = certs(b, false);
		c = SSLContextBuilder.forServer(key, certs).protocol("TLS").build();
		assertEquals("TLS", c.getProtocol());
		
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream ksIn = new FileInputStream(KEYSTORE_JKS);
		try {
			ks.load(ksIn, PASSWORD);
		}
		finally {
			ksIn.close();
		}
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, PASSWORD);
		
		c = SSLContextBuilder.forServer(kmf).protocol("TLS").build();
		assertEquals("TLS", c.getProtocol());
		
		b = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, CERT_PEM);
		assertNotNull(key(b));
		assertNotNull(certs(b,false));
		assertNotNull(password(b));
		assertNull(manager(b,false));
		b.keyManager(kmf);
		assertNull(key(b));
		assertNull(certs(b,false));
		assertNull(password(b));
		assertTrue(kmf == manager(b,false));
		b.keyManager(ENCKEY_PEM, PASSWORD, CERT_PEM);
		assertNotNull(key(b));
		assertNotNull(certs(b,false));
		assertNotNull(password(b));
		assertNull(manager(b,false));
	}
	
	@Test
	public void testTrustManger() throws Exception {
		b = SSLContextBuilder.forClient().protocol("TLS");
		assertEquals("TLS", b.build().getProtocol());
		assertNull(certs(b,true));
		b.trustManager(CERT_PEM);
		assertNotNull(certs(b,true));
		assertEquals("TLS", b.build().getProtocol());
		b.trustManager((X509Certificate[])null);
		assertNull(certs(b,true));
		b.trustManager(in(CERT_PEM));
		assertNotNull(certs(b,true));

		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream ksIn = new FileInputStream(KEYSTORE_JKS);
		try {
			ks.load(ksIn, PASSWORD);
		}
		finally {
			ksIn.close();
		}
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        
        b.trustManager(tmf);
        assertTrue(tmf == manager(b, true));
		assertNull(certs(b,true));
		assertEquals("TLS", b.build().getProtocol());
		b.trustManager(CERT_PEM);
		assertNotNull(certs(b,true));
        assertNull(manager(b, true));
	}
	
	@Test
	public void testPrivateKeyAlgos() throws Exception {
		b = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, KEYSTORE_PEM);
		assertEquals("RSA", key(b).getAlgorithm());
		b = SSLContextBuilder.forServer(KEY_PEM, KEYSTORE_PEM);
		assertEquals("RSA", key(b).getAlgorithm());
		b = SSLContextBuilder.forServer(ENCKEY_DSA_PEM, PASSWORD, KEYSTORE_DSA_PEM);
		assertEquals("DSA", key(b).getAlgorithm());
		b = SSLContextBuilder.forServer(KEY_DSA_PEM, KEYSTORE_DSA_PEM);
		assertEquals("DSA", key(b).getAlgorithm());
		b = SSLContextBuilder.forServer(ENCKEY_EC_PEM, PASSWORD, KEYSTORE_EC_PEM);
		assertEquals("EC", key(b).getAlgorithm());
		b = SSLContextBuilder.forServer(KEY_EC_PEM, KEYSTORE_EC_PEM);
		assertEquals("EC", key(b).getAlgorithm());
		
		Field f = SSLContextBuilder.class.getDeclaredField("KEY_ALGOS");
		f.setAccessible(true);
		String[] algos = (String[]) f.get(null);
		algos[2] = "XX";
		try {
			b = SSLContextBuilder.forServer(KEY_EC_PEM, KEYSTORE_EC_PEM);
			fail();
		}
		catch (KeyException e) {
			assertEquals("Generation of private key failed: none of [RSA, DSA, XX] worked", e.getMessage());
		}	
	}
	
	@Test
	public void testFileNotFoundException() throws Exception {
		File f = new File(KEYSTORE_JKS.getAbsolutePath()+"XXX");
		
		assertFalse(f.exists());
		try {
			SSLContextBuilder.forServer(f, PASSWORD, KEYSTORE_PEM);
			fail();
		} catch (FileNotFoundException e) {}
		try {
			SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, f);
			fail();
		} catch (FileNotFoundException e) {}
		try {
			SSLContextBuilder.forServer(f, KEYSTORE_PEM);
			fail();
		} catch (FileNotFoundException e) {}
		try {
			SSLContextBuilder.forServer(KEY_PEM, f);
			fail();
		} catch (FileNotFoundException e) {}
		try {
			SSLContextBuilder.forClient().trustManager(f);
			fail();
		} catch (FileNotFoundException e) {}
	}

	@Test
	public void testIOException() throws Exception {
		InputStream in = new InputStream() {

			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};

		try {
			SSLContextBuilder.forServer(in, PASSWORD, in(KEYSTORE_PEM));
			fail();
		} catch (IOException e) {}
		try {
			SSLContextBuilder.forServer(in(ENCKEY_PEM), PASSWORD, in);
			fail();
		} catch (IOException e) {}
		try {
			SSLContextBuilder.forServer(in, in(KEYSTORE_PEM));
			fail();
		} catch (IOException e) {}
		try {
			SSLContextBuilder.forServer(in(KEY_PEM), in);
			fail();
		} catch (IOException e) {}
		try {
			SSLContextBuilder.forClient().trustManager(in);
			fail();
		} catch (IOException e) {}
	}
	
	@Test
	public void testIllegalCerts() throws Exception {
		b = SSLContextBuilder.forServer(KEY_PEM, KEYSTORE_PEM);
		PrivateKey k = key(b);
		X509Certificate[] certs = certs(b, false);
		
		try {
			b.keyManager(k, (X509Certificate[])null);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("keyCerts is null", e.getMessage());
		}
		try {
			b.keyManager(k, new X509Certificate[0]);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("keyCerts is empty", e.getMessage());
		}
		try {
			b.keyManager(k, new X509Certificate[] {certs[0], null});
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("keyCerts contains null entry", e.getMessage());
		}
		b.trustManager((X509Certificate[])null);
		b.trustManager(new X509Certificate[0]);
		try {
			b.trustManager(new X509Certificate[] {certs[0], null});
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("trustCerts contains null entry", e.getMessage());
		}
		
		try {
			b.keyManager(KEY_PEM, KEY_PEM);
			fail();
		}
		catch (CertificateException e) {
			assertEquals("No certificate found", e.getMessage());
		}
		try {
			b.keyManager(in(KEY_PEM), in(CERT_PEM, new byte[] {'='}, 54));
			fail();
		}
		catch (CertificateException e) {
			assertEquals("Invalid certificate PEM format", e.getMessage());
		}
	}

	@Test
	public void testIllegalKey() throws Exception {
		b = SSLContextBuilder.forServer(KEY_PEM, KEYSTORE_PEM);
		X509Certificate[] certs = certs(b, false);
		
		try {
			b.keyManager(null, certs);
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals("key is null", e.getMessage());
		}

		try {
			b.keyManager(CERT_PEM, KEYSTORE_PEM);
			fail();
		}
		catch (KeyException e) {
			assertEquals("No private key found", e.getMessage());
		}
		try {
			b.keyManager(in(KEY_PEM, new byte[] {'='}, 50), in(CERT_PEM));
			fail();
		}
		catch (KeyException e) {
			assertEquals("Invalid private key PEM format", e.getMessage());
		}
		try {
			b.keyManager(in(ENCKEY_PEM, "ABABABAB".getBytes(), 50), PASSWORD, in(CERT_PEM));
			fail();
		}
		catch (KeyException e) {
			assertEquals("Invalid PKCS8 encoding of password-protected private key", e.getMessage());
		}
	}

	@Test
	public void testDestroy() throws Exception {
		X509Certificate[] certs;
		PrivateKey k;
		char[] p;
		
		b = SSLContextBuilder.forServer(KEY_PEM, KEYSTORE_PEM);
		k = key(b);
		assertFalse(b.isDestroyed());
		try {
			b.destroy();
			assertTrue(k.isDestroyed());
		}
		catch (DestroyFailedException e) {
			assertFalse(k.isDestroyed());
		}
		assertTrue(b.isDestroyed());
		assertNull(key(b));
		assertNull(certs(b,false));
		
		b = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, KEYSTORE_PEM);
		k = new TestKey();
		certs = certs(b, false);
		
		b.keyManager(k, PASSWORD, certs);
		p = password(b);
		assertFalse(b.isDestroyed());
		b.destroy();
		assertTrue(k.isDestroyed());
		assertTrue(b.isDestroyed());
		assertNull(key(b));
		assertNull(certs(b,false));
		assertNull(password(b));
		char[] cleared = new char[PASSWORD.length];
		assertArrayEquals(cleared, p);

		k = new TestKey();
		((TestKey)k).exception = new DestroyFailedException();
		b.keyManager(k, PASSWORD, certs);
		p = password(b);
		assertFalse(b.isDestroyed());
		try {
			b.destroy();
			fail();
		}
		catch (DestroyFailedException e) {
		}
		assertFalse(k.isDestroyed());
		assertTrue(b.isDestroyed());
		assertNull(key(b));
		assertNull(certs(b,false));
		assertNull(password(b));
		assertArrayEquals(cleared, p);
		b.destroy();
	}

	@Test
	public void silentClose() {
		TestCloseable closeable = new TestCloseable();
		
		SSLContextBuilder.silentClose(closeable);
		assertTrue(closeable.closed);
	}
	
	@Test
	public void testEngineBuilder() throws Exception {
		c = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM).protocol("TLS").secureRandom(new SecureRandom()).build();
		SSLEngine e = c.createSSLEngine();
		
		String protocol = "TLSv1.2";
		String cipher = e.getSupportedCipherSuites()[0];
		
		SSLEngineBuilder eb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
			.protocol("TLS")
			.trustManager(CERT_PEM)
			.protocols(protocol)
			.ciphers(cipher)
			.clientAuth(ClientAuth.REQUESTED).engineBuilder();
		
		SSLEngine e2 = eb.build();
		assertArrayEquals(new String[] {protocol}, e2.getSSLParameters().getProtocols());
		assertArrayEquals(new String[] {cipher}, e2.getSSLParameters().getCipherSuites());
		assertTrue(e2.getSSLParameters().getWantClientAuth());
		assertFalse(e2.getSSLParameters().getNeedClientAuth());
		
		eb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.protocol("TLS")
				.trustManager(CERT_PEM)
				.protocols(protocol)
				.ciphers(cipher)
				.clientAuth(ClientAuth.NONE).engineBuilder();
		assertFalse(eb.isForClient());
		assertTrue(eb.isForServer());
		e2 = eb.build();
		assertFalse(e2.getSSLParameters().getWantClientAuth());
		assertFalse(e2.getSSLParameters().getNeedClientAuth());
		
		eb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.protocol("TLS")
				.trustManager(CERT_PEM)
				.protocols(protocol)
				.ciphers(cipher)
				.clientAuth(ClientAuth.REQUIRED).engineBuilder();
		e2 = eb.build();
		assertFalse(e2.getSSLParameters().getWantClientAuth());
		assertTrue(e2.getSSLParameters().getNeedClientAuth());
		
		//useCiphersOrder
		eb = SSLContextBuilder.forClient().engineBuilder();
		assertTrue(eb.isForClient());
		assertFalse(eb.isForServer());
		e2 = eb.build();
		boolean defOrder = e2.getSSLParameters().getUseCipherSuitesOrder();
		eb = SSLContextBuilder.forClient()
				.useCiphersOrder(!defOrder)
				.engineBuilder();
		e2 = eb.build();
		assertEquals(!defOrder, e2.getSSLParameters().getUseCipherSuitesOrder());
		eb = SSLContextBuilder.forClient()
				.useCiphersOrder(defOrder)
				.engineBuilder();
		e2 = eb.build();
		assertEquals(defOrder, e2.getSSLParameters().getUseCipherSuitesOrder());

		//enableRetranssmision
		eb = SSLContextBuilder.forClient().enableRetransmissions(true).engineBuilder();
		e2 = eb.build();
		eb = SSLContextBuilder.forClient().enableRetransmissions(false).engineBuilder();
		e2 = eb.build();

		//maximumPacketSize
		eb = SSLContextBuilder.forClient().maximumPacketSize(100).engineBuilder();
		e2 = eb.build();
		
	}
	
	@Test
	public void testProtocols() throws Exception {
		SSLEngine e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.engineBuilder().build();
		String[] protos = e.getEnabledProtocols();
		assertTrue(protos.length > 1);
		for (String p: protos) {
			assertTrue(p.startsWith("TLS"));
		}
		
		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.protocols((String[])null)
				.engineBuilder().build();
		assertArrayEquals(protos, e.getEnabledProtocols());

		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.protocols()
				.engineBuilder().build();
		assertEquals(0, e.getEnabledProtocols().length);
		
		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.protocols("TLSv1.2")
				.engineBuilder().build();
		assertArrayEquals(new String[] {"TLSv1.2"}, e.getEnabledProtocols());
		
		try {
			SSLContextBuilder.forClient()
				.protocol("TLS")
				.protocols("TLSv1.2", "XXX")
				.engineBuilder().build();
			fail();
		}
		catch (SSLEngineCreateException e2) {
		}

		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.protocols("TLSv1.2", "XXX")
				.protocolFilter(SupportedCipherProtocolFilters.INSATNCE)
				.engineBuilder().build();
		assertArrayEquals(new String[] {"TLSv1.2"}, e.getEnabledProtocols());
	}

	@Test
	public void testCiphers() throws Exception {
		SSLEngine e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.engineBuilder().build();
		String[] ciphers = e.getEnabledCipherSuites();
		assertTrue(ciphers.length >= 4 && ciphers.length <= ProtocolDefaults.CIPHERS.length);
		assertTrue(ciphers.length < ProtocolDefaults.instance(false).supportedCiphers().size());
		
		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.ciphers((String[])null)
				.engineBuilder().build();
		assertArrayEquals(ciphers, e.getEnabledCipherSuites());

		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.ciphers()
				.engineBuilder().build();
		assertEquals(0, e.getEnabledCipherSuites().length);
		
		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.ciphers(ciphers[0])
				.engineBuilder().build();
		assertArrayEquals(new String[] {ciphers[0]}, e.getEnabledCipherSuites());
		
		try {
			SSLContextBuilder.forClient()
				.protocol("TLS")
				.ciphers(ciphers[0], "XXX")
				.engineBuilder().build();
			fail();
		}
		catch (SSLEngineCreateException e2) {
		}

		e = (b = SSLContextBuilder.forClient()
				.protocol("TLS"))
				.ciphers(ciphers[0], "XXX")
				.cipherFilter(SupportedCipherProtocolFilters.INSATNCE)
				.engineBuilder().build();
		assertArrayEquals(new String[] {ciphers[0]}, e.getEnabledCipherSuites());
	}
	
	SelectorLoop loop;
	int PORT = 7777;
	String HOST = "127.0.0.1";
	
	void start(SSLEngineBuilder sbld, SSLEngineBuilder cbld) throws Exception {
		TestSessionConfig c = new TestSessionConfig(sbld, cbld);
		TestFactory f = new TestFactory(c);
		
		loop = new SelectorLoop();
		try {
			loop.start();

			ServerSocketChannel schannel = ServerSocketChannel.open();
			schannel.configureBlocking(false);
			schannel.socket().bind(new InetSocketAddress(PORT));
			loop.register(schannel, f).sync();

			SocketChannel cchannel = SocketChannel.open();
			cchannel.configureBlocking(false);
			cchannel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
			ISession s = loop.register(cchannel, new SSLSession(new TestHandler(c), true)).sync().getSession();

			s.getReadyFuture().sync();
		}
		finally {
			loop.stop();
			loop.join(2000);
		}
	}
	
	@Test
	public void testConnectionWithNoneClientAuth() throws Throwable {
		SSLContextBuilder scb, ccb;
		
		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM);
		ccb = SSLContextBuilder.forClient()
				.trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());

		scb = SSLContextBuilder.forServer(ENCKEY_PEM, PASSWORD, CERT_PEM);
		ccb = SSLContextBuilder.forClient()
				.trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());
		
		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM);
		ccb = SSLContextBuilder.forClient();
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {
			assertTrue(SSLHandshakeException.class == e.getCause().getClass());
		}

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM);
		ccb = SSLContextBuilder.forClient().trustManager();
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {}
	}

	@Test
	public void testConnectionWithRequiredClientAuth() throws Throwable {
		SSLContextBuilder scb, ccb;

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUIRED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM).
				trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.protocols("TLSv1.2")
				.clientAuth(ClientAuth.REQUIRED);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM).
				trustManager(CERT_PEM);
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {}
		
		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.protocols("TLSv1.2")
				.clientAuth(ClientAuth.REQUIRED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				trustManager(CERT_PEM);
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {}
		
		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUIRED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM);
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {
			assertTrue(SSLHandshakeException.class == e.getCause().getClass());
		}
	}

	@Test
	public void testConnectionWithOptionalClientAuth() throws Throwable {
		SSLContextBuilder scb, ccb;

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUESTED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM).
				trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUESTED);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM).
				trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUESTED);
		ccb = SSLContextBuilder.forClient().
				trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());

		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUESTED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				trustManager(CERT_PEM);
		start(scb.engineBuilder(), ccb.engineBuilder());
		
		scb = SSLContextBuilder.forServer(KEY_PEM, CERT_PEM)
				.clientAuth(ClientAuth.REQUESTED)
				.trustManager(CERT_PEM);
		ccb = SSLContextBuilder.forClient().
				keyManager(KEY_PEM, CERT_PEM);
		try {
			start(scb.engineBuilder(), ccb.engineBuilder());
			fail();
		}
		catch (ExecutionException e) {
			assertTrue(SSLHandshakeException.class == e.getCause().getClass());
		}
	}
	
	static class TestFactory extends AbstractSessionFactory {

		private final ISessionConfig config;

		TestFactory(ISessionConfig config) {
			super(true);
			this.config = config;
		}
		
		@Override
		protected IStreamHandler createHandler(SocketChannel channel) {
			return new TestHandler(config);
		}
	}

	static class TestHandler extends AbstractStreamHandler {

		private final ISessionConfig config;
		
		TestHandler(ISessionConfig config) {
			this.config = config;
		}
		
		@Override
		public ISessionConfig getConfig() {
			return config;
		}
		
		@Override
		public void read(Object msg) {
		}
	}
	
	static class TestSessionConfig extends DefaultSessionConfig {
		
		private final SSLEngineBuilder sbuilder;

		private final SSLEngineBuilder cbuilder;
		
		TestSessionConfig(SSLEngineBuilder sbuilder, SSLEngineBuilder cbuilder) {
			this.sbuilder = sbuilder;
			this.cbuilder = cbuilder;
		}
		
		public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
			return clientMode ? cbuilder.build() : sbuilder.build();
		}
	};
	
	static class TestCloseable implements Closeable {

		boolean closed;
		
		@Override
		public void close() throws IOException {
			closed = true;
			throw new IOException();
		}
	}
	
	static class TestKey implements PrivateKey {
		
		private static final long serialVersionUID = 1L;

		boolean destroyed; 
		
		DestroyFailedException exception;
		
		@Override
		public void destroy() throws DestroyFailedException {
			if (exception != null) {
				throw exception;
			}
			destroyed = true;
		}
		
		@Override
		public boolean isDestroyed() {
			return destroyed;
		}
		
		@Override
		public String getAlgorithm() {
			return null;
		}

		@Override
		public String getFormat() {
			return null;
		}

		@Override
		public byte[] getEncoded() {
			return null;
		}
	};

}
