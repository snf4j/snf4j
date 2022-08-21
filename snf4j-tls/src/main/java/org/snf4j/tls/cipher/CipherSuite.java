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
package org.snf4j.tls.cipher;

import org.snf4j.tls.IntConstant;

public class CipherSuite extends IntConstant {
	
	public static final CipherSuite TLS_AES_128_GCM_SHA256 = new CipherSuite("TLS_AES_128_GCM_SHA256",0x1301);
	public static final CipherSuite TLS_AES_256_GCM_SHA384 = new CipherSuite("TLS_AES_256_GCM_SHA384",0x1302);
	public static final CipherSuite TLS_CHACHA20_POLY1305_SHA256 = new CipherSuite("TLS_CHACHA20_POLY1305_SHA256",0x1303);
	public static final CipherSuite TLS_AES_128_CCM_SHA256 = new CipherSuite("TLS_AES_128_CCM_SHA256",0x1304);
	public static final CipherSuite TLS_AES_128_CCM_8_SHA256 = new CipherSuite("TLS_AES_128_CCM_8_SHA256",0x1305);
	
	private final static CipherSuite[] KNOWN = new CipherSuite[] {
			null, 
			TLS_AES_128_GCM_SHA256, 
			TLS_AES_256_GCM_SHA384, 
			TLS_CHACHA20_POLY1305_SHA256, 
			TLS_AES_128_CCM_SHA256, 
			TLS_AES_128_CCM_8_SHA256,
			null,null,null,null,null,null,null,null,null,null};
	
	protected CipherSuite(String name, int value) {
		super(name, value);
	}

	protected CipherSuite(int value) {
		super(value);
	}

	public static CipherSuite of(int value) {
		if ((value & 0xfff0) == 0x1300) {
			CipherSuite known = KNOWN[value & 0xf];
			
			if (known != null) {
				return known;
			}
		}
		return new CipherSuite(value);
	}
	
}
