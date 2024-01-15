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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.extension.ISignatureSchemeSpec;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateType;

public class ConsumerUtilTest extends CommonTest {

	@Test
	public void testSign() throws Exception {
		SelectedCertificates certs = new TestCertificateSelector().selectCertificates(
				new CertificateCriteria(true, CertificateType.X509, "", new SignatureScheme[0], null, new SignatureScheme[0]));
		
		Signature s = certs.getAlgorithm().spec().getSignature().createSignature();
		byte[] octets = new byte[64];
		Arrays.fill(octets, (byte)32);
		byte[] server = "TLS 1.3, server CertificateVerify".getBytes(StandardCharsets.US_ASCII);
		byte[] client = "TLS 1.3, client CertificateVerify".getBytes(StandardCharsets.US_ASCII);
		s.initSign(certs.getPrivateKey());
		s.update(cat(octets,server,bytes(0),bytes(1,2,3,4)));
		byte[] signature = s.sign();
		assertArrayEquals(signature, ConsumerUtil.sign(bytes(1,2,3,4), certs.getAlgorithm(), certs.getPrivateKey(), false, RANDOM));

		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certs.getEntries()[0].getData()));
		assertTrue(ConsumerUtil.verify(signature, bytes(1,2,3,4), certs.getAlgorithm(), cert.getPublicKey(), false));
		
		s = certs.getAlgorithm().spec().getSignature().createSignature();
		s.initSign(certs.getPrivateKey());
		s.update(cat(octets,client,bytes(0),bytes(1,2,3,4,5)));
		signature = s.sign();
		assertArrayEquals(signature, ConsumerUtil.sign(bytes(1,2,3,4,5), certs.getAlgorithm(), certs.getPrivateKey(), true, RANDOM));
		assertTrue(ConsumerUtil.verify(signature, bytes(1,2,3,4,5), certs.getAlgorithm(), cert.getPublicKey(), true));
		
		SignatureScheme scheme = new SignatureScheme(100) {
			public ISignatureSchemeSpec spec() {
				throw new RuntimeException();
			}
		};
		try {
			ConsumerUtil.sign(bytes(1,2,3,4,5), scheme, certs.getPrivateKey(), true, RANDOM);
			fail();
		} catch (InternalErrorAlert e) {}
		try {
			ConsumerUtil.verify(signature, bytes(1,2,3,4,5), scheme, cert.getPublicKey(), true);
			fail();
		} catch (InternalErrorAlert e) {}
	}
}
