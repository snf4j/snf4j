/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import org.junit.Assume;
import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.crypto.ECDSASignature;
import org.snf4j.tls.crypto.EdDSASignature;
import org.snf4j.tls.crypto.RSAPKCS1Signature;
import org.snf4j.tls.crypto.RSASSAPSSSignature;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateType;

public class X509KeyManagerCertificateSelectorTest extends CommonTest {
	
	final static char[] PASSWORD = "password".toCharArray();
	
	KeyStore ks;
	
	@Override
	public void before() throws Exception {
		super.before();
		ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
	}

	void add(String alias, PrivateKey key, X509Certificate... certs) throws Exception {
		ks.setKeyEntry(alias, key, PASSWORD, certs );
	}

	X509KeyManager keyManager() throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        kmf.init(ks,PASSWORD);
        return (X509KeyManager) kmf.getKeyManagers()[0];
	}
	
	SignatureScheme[] schemes(SignatureScheme... schemes) {
		return schemes;
	}
	
	@Test
	public void testKeyManager() throws Exception {
		Assume.assumeTrue(JAVA15);
		add("key1", key("RSA", "rsa"), cert("rsasha256"));
		add("key2", key("RSA", "rsa2048"), cert("rsapsssha256"));
		add("key3", key("RSASSA-PSS", "rsapsssha256"), cert("rsapsspsssha256"));
		add("key4", key("Ed25519", "ed25519"), cert("ed25519"));
		add("key5", key("Ed448", "ed448"), cert("ed448"));
		add("key6", key("EC", "secp256r1"), cert("secp256r1"));
		X509KeyManager km = keyManager();
		
		String[] aliases = km.getClientAliases(RSAPKCS1Signature.RSA_PKCS1_SHA256.keyAlgorithm(), null);
		Arrays.sort(aliases);
		assertEquals("[key1, key2]", Arrays.toString(aliases));
		aliases = km.getClientAliases(ECDSASignature.ECDSA_SECP256R1_SHA256.keyAlgorithm(), null);
		Arrays.sort(aliases);
		assertEquals("[key6]", Arrays.toString(aliases));
		aliases = km.getClientAliases(RSASSAPSSSignature.RSA_PSS_PSS_SHA256.keyAlgorithm(), null);
		Arrays.sort(aliases);
		assertEquals("[key3]", Arrays.toString(aliases));
		aliases = km.getClientAliases(EdDSASignature.ED25519.keyAlgorithm(), null);
		Arrays.sort(aliases);
		assertEquals("[key4, key5]", Arrays.toString(aliases));
	}
		
	@Test(expected = CertificateSelectorException.class)
	public void testUnknownAlias() throws Exception {
		PrivateKey key = key("RSA", "rsa");
		X509Certificate cert = cert("rsasha256");

		add("key", key, cert);
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager(), "key2");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		EngineDefaults.getDefaulSignatureSchemes(),
        		null,
        		EngineDefaults.getDefaulSignatureSchemes()
        		);
        s.selectCertificates(c);
	}

	@Test(expected = CertificateSelectorException.class)
	public void testAlgorithmNotMatched() throws Exception {
		PrivateKey key = key("RSA", "rsa");
		X509Certificate cert = cert("rsasha256");

		add("key", key, cert);
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager(), "key");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		schemes(SignatureScheme.ECDSA_SECP384R1_SHA384),
        		null,
        		schemes(SignatureScheme.ECDSA_SECP384R1_SHA384)
        		);
        s.selectCertificates(c);
	}

	@Test(expected = CertificateSelectorException.class)
	public void testCertsNotMatched() throws Exception {
		PrivateKey key = key("RSA", "rsa");
		X509Certificate cert = cert("rsasha256");

		add("key", key, cert);
		keyManager().getServerAliases("RSA", null);
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager(), "key");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		schemes(SignatureScheme.RSA_PKCS1_SHA1),
        		null,
        		schemes(SignatureScheme.RSA_PKCS1_SHA1)
        		);
        s.selectCertificates(c);
	}

	@Test(expected = CertificateSelectorException.class)
	public void testUnknownKeyAlias() throws Exception {
		X509Certificate cert = cert("rsasha256");
		X509KeyManager keyManager = new X509KeyManager() {

			@Override
			public String[] getClientAliases(String keyType, Principal[] issuers) {
				return null;
			}

			@Override
			public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
				return null;
			}

			@Override
			public String[] getServerAliases(String keyType, Principal[] issuers) {
				return null;
			}

			@Override
			public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
				return null;
			}

			@Override
			public X509Certificate[] getCertificateChain(String alias) {
				return new X509Certificate[] {cert};
			}

			@Override
			public PrivateKey getPrivateKey(String alias) {
				return null;
			}
		};
		
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager, "key");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		schemes(SignatureScheme.RSA_PKCS1_SHA256),
        		null,
        		schemes(SignatureScheme.RSA_PKCS1_SHA256)
        		);
        s.selectCertificates(c);
	}
	
	@Test
	public void testSelectCertificates() throws Exception {
		PrivateKey key = key("RSA", "rsa");
		X509Certificate cert = cert("rsasha256");

		add("key", key, cert);
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager(), "key");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.ED25519, SignatureScheme.RSA_PKCS1_SHA1, SignatureScheme.RSA_PKCS1_SHA256)
        		);
        SelectedCertificates sc = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA1, sc.getAlgorithm());
        assertEquals(key, sc.getPrivateKey());
        assertEquals(1, sc.getCertificates().length);
        assertEquals(cert, sc.getCertificates()[0]);
	}
	
	@Test
	public void testSelectCertificatesWithoutAlias() throws Exception {
		PrivateKey key1 = key("RSA", "rsa");
		X509Certificate cert1 = cert("rsasha256");
		PrivateKey key2 = key("EC", "secp256r1");
		X509Certificate cert2 = cert("secp256r1");

		add("key1", key1, cert1);
		TestKeyManager km = new TestKeyManager(keyManager());
		X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(km);
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.RSA_PKCS1_SHA1, SignatureScheme.RSA_PKCS1_SHA256)
        		);
        SelectedCertificates scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA1, scheme.getAlgorithm());
        assertEquals(key1, scheme.getPrivateKey());
        assertEquals("S|", km.trace());
        
        c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA256, scheme.getAlgorithm());
        assertEquals(key1, scheme.getPrivateKey());
        assertEquals("S|", km.trace());

        c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA256, scheme.getAlgorithm());
        assertEquals(key1, scheme.getPrivateKey());
        assertEquals("S|S|", km.trace());

        c = new CertificateCriteria(
        		false,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA256, scheme.getAlgorithm());
        assertEquals(key1, scheme.getPrivateKey());
        assertEquals("C|C|", km.trace());
        
		add("key1", key1, cert1, cert2);
        s = new X509KeyManagerCertificateSelector(keyManager());
        c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		schemes(SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1),
        		null,
        		schemes(SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        try {
        	s.selectCertificates(c);
        	fail();
        }
        catch (CertificateSelectorException e) {
        }

		add("key1", key1, cert1);
		add("key2", key2, cert2);
		km = new TestKeyManager(keyManager());
        s = new X509KeyManagerCertificateSelector(km);
        c = new CertificateCriteria(
        		false,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		schemes(SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.ECDSA_SECP256R1_SHA256, scheme.getAlgorithm());
        assertEquals(key2, scheme.getPrivateKey());
        assertEquals("C|", km.trace());

        c = new CertificateCriteria(
        		false,
        		CertificateType.X509,
        		"host",
        		schemes(SignatureScheme.RSA_PKCS1_SHA1, SignatureScheme.RSA_PKCS1_SHA256),
        		null,
        		schemes(SignatureScheme.ECDSA_SECP256R1_SHA256, SignatureScheme.RSA_PKCS1_SHA256, SignatureScheme.RSA_PKCS1_SHA1)
        		);
        scheme = s.selectCertificates(c);
        assertSame(SignatureScheme.RSA_PKCS1_SHA256, scheme.getAlgorithm());
        assertEquals(key1, scheme.getPrivateKey());
	}
	
	void assertCertificate(String keyAlgo, String keyName, String certName, SignatureScheme scheme, SignatureScheme... preferred) throws Exception {
		PrivateKey key = key(keyAlgo, keyName);
		X509Certificate cert = cert(certName);
		
		add("key", key, cert);
        X509KeyManagerCertificateSelector s = new X509KeyManagerCertificateSelector(keyManager(), "key");
        CertificateCriteria c = new CertificateCriteria(
        		true,
        		CertificateType.X509,
        		"host",
        		SIGNATURE_SCHEMES,
        		null,
        		preferred
        		);
        SelectedCertificates sc = s.selectCertificates(c);
        assertSame(scheme, sc.getAlgorithm());
        assertEquals(key, sc.getPrivateKey());
        assertEquals(1, sc.getCertificates().length);
        assertEquals(cert, sc.getCertificates()[0]);
	}
	
	@Test
	public void testRsaPkcs1Sha256() throws Exception {
		assertCertificate("RSA", "rsa", "rsasha256", SignatureScheme.RSA_PKCS1_SHA512,
				SignatureScheme.RSA_PKCS1_SHA512, 
				SignatureScheme.RSA_PKCS1_SHA256
				);
	}

	@Test
	public void testRsaPkcs1Sha384() throws Exception {
		assertCertificate("RSA", "rsa", "rsasha384", SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PKCS1_SHA512
				);
	}

	@Test
	public void testRsaPkcs1Sha384_2() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa", "rsasha384", SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PKCS1_SHA512
				);
	}

	@Test
	public void testRsaPkcs1Sha384_3() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa", "rsasha384", SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PKCS1_SHA512
				);
	}

	@Test
	public void testRsaPkcs1Sha384_4() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa", "rsasha384", SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.RSA_PKCS1_SHA384,
				SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PKCS1_SHA512
				);
	}
	
	@Test
	public void testRsaPkcs1Sha512() throws Exception {
		assertCertificate("RSA", "rsa", "rsasha512", SignatureScheme.RSA_PKCS1_SHA512,
				SignatureScheme.RSA_PKCS1_SHA512
				);
	}

	@Test
	public void testRsaPkcs1Sha1() throws Exception {
		assertCertificate("RSA", "rsa", "rsasha1", SignatureScheme.RSA_PKCS1_SHA1,
				SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.RSA_PKCS1_SHA1
				);
	}

	@Test
	public void testEcdsaSecp256r1Sha256() throws Exception {
		assertCertificate("EC", "secp256r1", "secp256r1", SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.ECDSA_SECP521R1_SHA512,
				SignatureScheme.ECDSA_SECP256R1_SHA256
				);
	}

	@Test
	public void testEcdsaSecp384r1Sha384() throws Exception {
		assertCertificate("EC", "secp384r1", "secp384r1", SignatureScheme.ECDSA_SECP384R1_SHA384,
				SignatureScheme.ECDSA_SECP521R1_SHA512,
				SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.ECDSA_SECP384R1_SHA384
				);
	}

	@Test
	public void testEcdsaSecp521r1Sha512() throws Exception {
		assertCertificate("EC", "secp521r1", "secp521r1", SignatureScheme.ECDSA_SECP521R1_SHA512,
				SignatureScheme.ECDSA_SECP256R1_SHA256,
				SignatureScheme.ECDSA_SECP521R1_SHA512);
	}

	@Test
	public void testEcdsaSha1() throws Exception {
		assertCertificate("EC", "ecdsasha1", "ecdsasha1", SignatureScheme.ECDSA_SHA1,
				SignatureScheme.ECDSA_SHA1,
				SignatureScheme.ECDSA_SECP384R1_SHA384);
	}

	@Test
	public void testRsaPssRsaeSha256() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa2048", "rsapsssha256", SignatureScheme.RSA_PSS_RSAE_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA256, 
				SignatureScheme.RSA_PSS_RSAE_SHA512
				);
	}

	@Test
	public void testRsaPssRsaeSha384() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa2048", "rsapsssha384", SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256 
				);
	}

	@Test
	public void testRsaPssRsaeSha384_2() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa2048", "rsapsssha384", SignatureScheme.RSA_PKCS1_SHA256,
				SignatureScheme.RSA_PKCS1_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256 
				);
	}

	@Test
	public void testRsaPssRsaeSha384_3() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa2048", "rsapsssha384", SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256 
				);
	}
	
	@Test
	public void testRsaPssRsaeSha512() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSA", "rsa2048", "rsapsssha512", SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA512,
				SignatureScheme.RSA_PSS_RSAE_SHA256 
				);
	}

	@Test
	public void testRsaPssPssSha256() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSASSA-PSS", "rsapsssha256", "rsapsspsssha256", SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.RSA_PSS_PSS_SHA512,
				SignatureScheme.RSA_PSS_PSS_SHA256
				);
	}

	@Test
	public void testRsaPssPssSha384() throws Exception {
		Assume.assumeTrue(JAVA11);
		assertCertificate("RSASSA-PSS", "rsapsssha384", "rsapsspsssha384", SignatureScheme.RSA_PSS_PSS_SHA384,
				SignatureScheme.RSA_PSS_PSS_SHA512,
				SignatureScheme.RSA_PSS_PSS_SHA256,
				SignatureScheme.ECDSA_SECP384R1_SHA384,
				SignatureScheme.RSA_PSS_PSS_SHA384
				);
	}
	
	class TestKeyManager implements X509KeyManager {

		X509KeyManager mgr;
		
		StringBuilder trace = new StringBuilder();
		
		void trace(String s) {
			trace.append(s).append('|');
		}
		
		String trace() {
			String s = trace.toString();
			
			trace.setLength(0);
			return s;
		}
		
		TestKeyManager(X509KeyManager mgr) {
			this.mgr = mgr;
		}
		
		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			trace("C");
			return mgr.getClientAliases(keyType, issuers);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return mgr.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			trace("S");
			return mgr.getServerAliases(keyType, issuers);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return mgr.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return mgr.getCertificateChain(alias);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return mgr.getPrivateKey(alias);
		}
		
	}
	
}
