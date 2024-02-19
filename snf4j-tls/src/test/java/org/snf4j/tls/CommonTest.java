/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2024 SNF4J contributors
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.snf4j.core.util.PemUtil;
import org.snf4j.core.util.PemUtil.Label;
import org.snf4j.tls.crypto.ECDSASignatureTest;
import org.snf4j.tls.extension.SignatureScheme;

public class CommonTest {

	protected final ByteBuffer buffer = ByteBuffer.allocate(0x20010);

	protected static final SecureRandom RANDOM = new SecureRandom(); 	
	
	protected final static SignatureScheme[] SIGNATURE_SCHEMES = new SignatureScheme[] {
			SignatureScheme.ECDSA_SECP256R1_SHA256,
			SignatureScheme.ECDSA_SECP384R1_SHA384,
			SignatureScheme.ECDSA_SECP521R1_SHA512,
			SignatureScheme.RSA_PSS_PSS_SHA256,
			SignatureScheme.RSA_PSS_PSS_SHA384,
			SignatureScheme.RSA_PSS_PSS_SHA512,
			SignatureScheme.RSA_PSS_RSAE_SHA256,
			SignatureScheme.RSA_PSS_RSAE_SHA384,
			SignatureScheme.RSA_PSS_RSAE_SHA512,
			SignatureScheme.RSA_PKCS1_SHA256,
			SignatureScheme.RSA_PKCS1_SHA384,
			SignatureScheme.RSA_PKCS1_SHA512,
			SignatureScheme.RSA_PKCS1_SHA1,
			SignatureScheme.ECDSA_SHA1
			};

	public static final boolean JAVA11;

	public static final boolean JAVA8;

	public static final boolean JAVA8_GEQ_U392;
	
	public static final boolean JAVA15;

	static {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		String longerVersion = System.getProperty("java.version");
		
		JAVA8 = version < 9.0;
		JAVA8_GEQ_U392 = JAVA8 && upd(longerVersion) >= 392;
		JAVA11 = version >= 11.0;
		JAVA15 = version >= 15.0;
	}
	
	static int upd(String fullVersion) {
		int i = fullVersion.indexOf('_');
		
		if (i > -1) {
			return Integer.parseInt(fullVersion.substring(i+1));
		}
		return 0;
	}
	
	protected byte[] buffer() {
		ByteBuffer dup = buffer.duplicate();
		dup.flip();
		byte[] bytes = new byte[dup.remaining()];
		dup.get(bytes);
		return bytes;
	}
	
	protected byte[] buffer(int off, int len) {
		byte[] subbytes = new byte[len];
		System.arraycopy(buffer(), off, subbytes, 0, len);
		return subbytes;
	}

	protected static byte[] bytes(int... values) {
		byte[] bytes = new byte[values.length];
		int i = 0;
		
		for (int value: values) {
			bytes[i++] = (byte)value;
		}
		return bytes;
	}
		
	protected static byte[] bytes(String hexString) {
		byte[] bytes = new byte[hexString.length()/2];
		
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] = (byte)Integer.parseInt(hexString.substring(i*2, i*2+2), 16);
		}
		return bytes;
	}
	
	protected static byte[] bytes(byte[] array, int off, int len) {
		byte[] bytes = new byte[len];
		
		System.arraycopy(array, off, bytes, 0, len);
		return bytes;
	}
	
	protected static byte[] bytes(int len, byte first, byte mid, byte last) {
		byte[] bytes = new byte[len];
		
		Arrays.fill(bytes, mid);
		bytes[0] = first;
		bytes[len-1] = last;
		return bytes;
	}
	
	protected static byte[] cat(byte[]... arrays) {
		int len = 0;
		for (byte[] array: arrays) {
			len += array.length;
		}
		byte[] bytes = new byte[len];
		len = 0;
		for (byte[] array: arrays) {
			System.arraycopy(array, 0, bytes, len, array.length);
			len += array.length;
		}
		return bytes;
	}

	protected static byte[][] split(byte[] array, int... sizes) {
		byte[][] splitted = new byte[sizes.length+1][];
		byte[] b;
		int remaining = array.length;
		int i=0;
		int off=0;
		
		
		for (; i<sizes.length; ++i) {
			int size = sizes[i];
			b = new byte[size];
			
			System.arraycopy(array, off, b, 0, size);
			off += size;
			remaining -= size;
			splitted[i] = b;
		}
		b = new byte[remaining];
		System.arraycopy(array, off, b, 0, remaining);
		splitted[i] = b;
		return splitted;
	}
	
	protected static ByteBuffer[] array(byte[] bytes, int off, int... sizes) {
		ByteBuffer[] array = new ByteBuffer[sizes.length + 1];
		int len = bytes.length;
		int i=0;
		
		for (; i<sizes.length; ++i) {
			byte[] a = new byte[sizes[i]];
			System.arraycopy(bytes, off, a, 0, a.length);
			array[i] = ByteBuffer.wrap(a);
			off += sizes[i];
		}
		byte[] a = new byte[len-off];
		System.arraycopy(bytes, off, a, 0, a.length);
		array[i] = ByteBuffer.wrap(a);
		return array;
	}
	
	protected static byte[] random(int len) {
		byte[] data = new byte[len];
		
		RANDOM.nextBytes(data);
		return data;
	}

	public static X509Certificate cert(String name) throws Exception {
		InputStream in = ECDSASignatureTest.class.getResourceAsStream("/certs/" + name + ".crt");
		List<byte[]> certs = PemUtil.read(Label.CERTIFICATE, in);
		in.close();
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certs.get(0)));
	}

	public static PrivateKey key(String algorithm, String name) throws Exception {
		InputStream in = ECDSASignatureTest.class.getResourceAsStream("/certs/" + name + ".key");
		List<byte[]> keys = PemUtil.read(Label.PRIVATE_KEY, in);
		in.close();
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keys.get(0));
		KeyFactory factory = KeyFactory.getInstance(algorithm);
	    return factory.generatePrivate(spec);
	}

	public static void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	@Before
	public void before() throws Exception {
		buffer.clear();
		Security.setProperty("jdk.tls.keyLimits", "AES/GCM/NoPadding KeyUpdate 2^16");
		System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
	}

}
