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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import java.security.cert.X509Certificate;

import org.snf4j.core.util.PemUtil;
import org.snf4j.core.util.PemUtil.Label;
import org.snf4j.tls.CommonTest;

public class SignatureTest extends CommonTest {

	X509Certificate cert(String name) throws Exception {
		InputStream in = ECDSASignatureTest.class.getResourceAsStream("/certs/" + name + ".crt");
		List<byte[]> certs = PemUtil.read(Label.CERTIFICATE, in);
		in.close();
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certs.get(0)));
	}

	PrivateKey key(String algorithm, String name) throws Exception {
		InputStream in = ECDSASignatureTest.class.getResourceAsStream("/certs/" + name + ".key");
		List<byte[]> keys = PemUtil.read(Label.PRIVATE_KEY, in);
		in.close();
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keys.get(0));
		KeyFactory factory = KeyFactory.getInstance(algorithm);
	    return factory.generatePrivate(spec);
	}
	
	PrivateKey ecKey(String name) throws Exception {
		return key("EC", name);
	}

	PrivateKey rsaKey(String name) throws Exception {
		return key("RSA", name);
	}

	void assertVerify(ISignature signature, PrivateKey privKey, PublicKey pubKey) throws Exception {
		Signature s = signature.createSignature();

		byte[] data = random(100);
		
		s.initSign(privKey);
		s.update(data);
		byte[] sign = s.sign();
		
		s = signature.createSignature();
		s.initVerify(pubKey);
		s.update(data);
		assertTrue(s.verify(sign));	
	}

	void assertX509Encoding(String algorithm, PublicKey key) throws Exception {
		assertEquals("X.509", key.getFormat());
		byte[] x509Encoded = key.getEncoded();
		PublicKey publicKey = KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(x509Encoded));         
		assertEquals(key, publicKey);	
	}
}
