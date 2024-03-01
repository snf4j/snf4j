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
package org.snf4j.tls.crypto;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import org.snf4j.tls.Args;
import org.snf4j.tls.cipher.ICipherSuiteSpec;
import org.snf4j.tls.cipher.IHashSpec;

/**
 * An abstract key schedule providing the HKDF-Expand-Label function as defined
 * in the TLS1.3 protocol specification.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class AbstractKeySchedule {

	private static final String LABEL_PREFIX = "tls13 ";

	/** Associated HMAC-based Extract-and-Expand Key Derivation Function (HKDF) */
	protected final IHkdf hkdf;

	/** Associated hash function */
	protected final IHashSpec hashSpec;
	
	/** Associated cipher suite  */
	protected ICipherSuiteSpec cipherSuiteSpec;

	/**
	 * Creates a value of the label as defined in the HkdfLabel structure.
	 * 
	 * @param label a value of the label
	 * @return {@code ("tls13 " + label}) encoded into a sequence of bytes using the
	 *         US_ASCII charset.
	 */
	protected static byte[] label(String label) {
		return (LABEL_PREFIX + label).getBytes(StandardCharsets.US_ASCII);
	}
	
	private static ICipherSuiteSpec check(ICipherSuiteSpec cipherSuiteSpec) {
		Args.checkNull(cipherSuiteSpec, "cipherSuiteSpec");
		return cipherSuiteSpec;
	}
	
	/**
	 * Constructs an abstract key schedule for the given HMAC-based
	 * Extract-and-Expand Key Derivation Function (HKDF) and cipher suite.
	 * 
	 * @param hkdf            the HKDF
	 * @param cipherSuiteSpec the cipher suite
	 */
	protected AbstractKeySchedule(IHkdf hkdf, ICipherSuiteSpec cipherSuiteSpec) {
		this(hkdf, check(cipherSuiteSpec).getHashSpec());
		this.cipherSuiteSpec = cipherSuiteSpec;
	}

	/**
	 * Constructs an abstract key schedule for the given HMAC-based
	 * Extract-and-Expand Key Derivation Function (HKDF) and hash function.
	 * 
	 * @param hkdf the HKDF
	 * @param hashSpec the hash function
	 */
	protected AbstractKeySchedule(IHkdf hkdf, IHashSpec hashSpec) {
		Args.checkNull(hkdf, "hkdf");
		Args.checkNull(hashSpec, "hashSpec");
		this.hkdf = hkdf;
		this.hashSpec = hashSpec;
	}

	/**
	 * Gets the associated hash function.
	 * 
	 * @return the associated hash function
	 */
	public IHashSpec getHashSpec() {
		return hashSpec;
	}

	/**
	 * Sets the associated cipher suite.
	 * 
	 * @param cipherSuiteSpec the associated cipher suite
	 */
	public void setCipherSuiteSpec(ICipherSuiteSpec cipherSuiteSpec) {
		Args.checkNull(cipherSuiteSpec, "cipherSuiteSpec");
		this.cipherSuiteSpec = cipherSuiteSpec;
	}
	
	/**
	 * Gets the associated cipher suite.
	 * 
	 * @return the associated cipher suite
	 */
	public ICipherSuiteSpec getCipherSuiteSpec() {
		return cipherSuiteSpec;
	}
	
	/**
	 * The HKDF-Expand-Label function as defined in the TLS1.3 protocol specification
	 * @param secret a secret
	 * @param label a label without the {@code "tls13 "} prefix
	 * @param context a context
	 * @param length the length of the output bytes
	 * @return the output bytes
	 * @throws InvalidKeyException if a key could not be created from the given secret 
	 */
	protected byte[] hkdfExpandLabel(byte[] secret, String label, byte[] context, int length) throws InvalidKeyException {
		return hkdfExpandLabel(secret, label(label), context, length);
	}
	
	/**
	 * The HKDF-Expand-Label function as defined in the TLS1.3 protocol specification
	 * @param secret a secret
	 * @param label a label with the {@code "tls13 "} prefix encoded into a sequence of bytes using the
	 *         US_ASCII charset
	 * @param context a context
	 * @param length the length of the output bytes
	 * @return the output bytes
	 * @throws InvalidKeyException if a key could not be created from the given secret 
	 */
	protected byte[] hkdfExpandLabel(byte[] secret, byte[] label, byte[] context, int length) throws InvalidKeyException {
		byte[] buf = new byte[2 + 1 + label.length + 1 + context.length];
		
		buf[0] = (byte) (length >> 8);
		buf[1] = (byte) length;
		buf[2] = (byte) label.length;
		System.arraycopy(label, 0, buf, 3, label.length);
		buf[3+label.length] = (byte) context.length;
		if (context.length > 0) {
			System.arraycopy(context, 0, buf, buf.length-context.length, context.length);
		}
		return hkdf.expand(secret, buf, length);
	}
	
	/**
	 * Erases all sensitive information and secrets associated with this key schedule
	 */
	public abstract void eraseAll();
}
