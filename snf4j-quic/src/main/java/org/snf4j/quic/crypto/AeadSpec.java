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
package org.snf4j.quic.crypto;

import org.snf4j.tls.crypto.AeadId;

/**
 * A QUIC specification for the supported AEAD algorithms. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AeadSpec {

	private final static long[] CONFIDENTIALITY_LIMITS;
	
	private final static long[] INTEGRITY_LIMITS;
	
	private final static IHeaderProtection[] HEADER_PROTECTIONS;
	
	static {
	}
	
	static {
		CONFIDENTIALITY_LIMITS = new long[AeadId.values().length];
		CONFIDENTIALITY_LIMITS[AeadId.AES_128_GCM.ordinal()] = 8388608L;
		CONFIDENTIALITY_LIMITS[AeadId.AES_256_GCM.ordinal()] = 8388608L;
		CONFIDENTIALITY_LIMITS[AeadId.CHACHA20_POLY1305.ordinal()] = 4611686018427387904L + 1;

		INTEGRITY_LIMITS = new long[CONFIDENTIALITY_LIMITS.length];
		INTEGRITY_LIMITS[AeadId.AES_128_GCM.ordinal()] = 4503599627370496L;
		INTEGRITY_LIMITS[AeadId.AES_256_GCM.ordinal()] = 4503599627370496L;
		INTEGRITY_LIMITS[AeadId.CHACHA20_POLY1305.ordinal()] = 68719476736L;

		HEADER_PROTECTIONS = new IHeaderProtection[CONFIDENTIALITY_LIMITS.length];		
		HEADER_PROTECTIONS[AeadId.AES_128_GCM.ordinal()] = AESHeaderProtection.HP_AES_128;
		HEADER_PROTECTIONS[AeadId.AES_256_GCM.ordinal()] = AESHeaderProtection.HP_AES_256;
		HEADER_PROTECTIONS[AeadId.CHACHA20_POLY1305.ordinal()] = ChaCha20HeaderProtection.HP_CHACHA20;
	}
	
	private AeadSpec() {}
	
	/**
	 * Returns the confidentiality limit for the given AEAD algorithm. This is the
	 * number of packets that can be encrypted with a given key.
	 * 
	 * @param aeadId the AEAD algorithm
	 * @return the confidentiality limit
	 */
	public static long getConfidentialityLimit(AeadId aeadId) {
		return CONFIDENTIALITY_LIMITS[aeadId.ordinal()];
	}

	/**
	 * Returns the integrity limit for the given AEAD algorithm. This is the number
	 * of invalid packets that can be accepted for a given key.
	 * 
	 * @param aeadId the AEAD algorithm
	 * @return the integrity limit
	 */
	public static long getIntegrityLimit(AeadId aeadId) {
		return INTEGRITY_LIMITS[aeadId.ordinal()];
	}
	
	/**
	 * Returns the header protection algorithm for the given AEAD algorithm.
	 * 
	 * @param aeadId the AEAD algorithm
	 * @return the header protection algorithm
	 */
	public static IHeaderProtection getHeaderProtection(AeadId aeadId) {
		return HEADER_PROTECTIONS[aeadId.ordinal()];
	}

}
