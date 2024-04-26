/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.junit.Before;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.core.util.PemUtil;
import org.snf4j.core.util.PemUtil.Label;

public class CommonTest {

	final static char[] PASSWORD = "password".toCharArray();

	public static final boolean JAVA11;

	protected ByteBuffer buffer = ByteBuffer.allocate(128000);
	
	static {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		
		JAVA11 = version >= 11.0;
	}

	@Before
	public void before() throws Exception {
		buffer.clear();
	}
	
	protected byte[] bytes() {
		ByteBuffer dup = buffer.duplicate();
		
		dup.flip();
		byte[] bytes = new byte[dup.remaining()];
		dup.get(bytes);
		return bytes;
	}
	
	protected byte[] bytes(ByteBuffer buf) {
		ByteBuffer dup = buf.duplicate();

		byte[] bytes = new byte[dup.remaining()];
		dup.get(bytes);
		return bytes;
	}
	
	protected byte[] bytesAndClear() {
		buffer.flip();
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		buffer.clear();
		return bytes;
	}
	
	protected ByteBuffer buffer(String hexString) {
		buffer.clear();
		buffer.put(bytes(hexString)).flip();
		return buffer;
	}

	protected ByteBuffer buffer(byte[] bytes) {
		return ByteBuffer.wrap(bytes);
	}
	
	protected static byte[] bytes(String hexString) {
		hexString = hexString.replace(" ", "");
		byte[] bytes = new byte[hexString.length()/2];
		
		for (int i=0; i<bytes.length; ++i) {
			bytes[i] = (byte)Integer.parseInt(hexString.substring(i*2, i*2+2), 16);
		}
		return bytes;
	}
	
	protected static byte[] bytes(int count) {
		byte[] bytes = new byte[count];
		
		for (int i=0; i<count; ++i) {
			bytes[i] = (byte) i;
		}
		return bytes;
	}
	
	protected static String hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for(byte b: bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
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
	
	protected static ByteBuffer[] split(ByteBuffer buf, int... sizes) {
		byte[] bytes = new byte[buf.remaining()];
		
		buf.duplicate().get(bytes);
		byte[][] splittedBytes = split(bytes, sizes);
		ByteBuffer[] splitted = new ByteBuffer[splittedBytes.length];
		for (int i=0; i<splitted.length; ++i) {
			splitted[i] = ByteBuffer.wrap(splittedBytes[i]);
		}
		return splitted;
	}

	protected static ByteBuffer cat(ByteBuffer... bufs) {
		ByteBufferArray bufArray = ByteBufferArray.wrap(bufs);
		byte[] bytes = new byte[(int) bufArray.remaining()];
		bufArray.get(bytes);
		return ByteBuffer.wrap(bytes);
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
	
	protected X509Certificate cert(String name) throws Exception {
		InputStream in = CommonTest.class.getResourceAsStream("/certs/" + name + ".crt");
		List<byte[]> certs = PemUtil.read(Label.CERTIFICATE, in);
		in.close();
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certs.get(0)));
	}

	protected PrivateKey key(String algorithm, String name) throws Exception {
		InputStream in = CommonTest.class.getResourceAsStream("/certs/" + name + ".key");
		List<byte[]> keys = PemUtil.read(Label.PRIVATE_KEY, in);
		in.close();
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keys.get(0));
		KeyFactory factory = KeyFactory.getInstance(algorithm);
	    return factory.generatePrivate(spec);
	}

	protected X509KeyManager km() throws Exception {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setKeyEntry("key", key("EC", "secp256r1"), PASSWORD, new X509Certificate[] {cert("secp256r1")});
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks,PASSWORD);
		return (X509KeyManager) kmf.getKeyManagers()[0];
	}
	
	protected X509TrustManager tm() throws Exception {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setCertificateEntry("ca", cert("secp256r1"));
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		return (X509TrustManager) tmf.getTrustManagers()[0];
	}
}
