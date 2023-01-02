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
package org.snf4j.tls.crypto;

import static org.junit.Assert.assertEquals;

import java.security.MessageDigest;

import javax.crypto.Mac;

import org.junit.Test;

public class HashTest {

	@Test
	public void testSha256() throws Exception {
		Mac mac = Hash.SHA256.createMac();
		MessageDigest md = Hash.SHA256.createMessageDigest();
		
		assertEquals("HmacSHA256", mac.getAlgorithm());
		assertEquals("SHA-256", md.getAlgorithm());
	}

	@Test
	public void testSha384() throws Exception {
		Mac mac = Hash.SHA384.createMac();
		MessageDigest md = Hash.SHA384.createMessageDigest();
		
		assertEquals("HmacSHA384", mac.getAlgorithm());
		assertEquals("SHA-384", md.getAlgorithm());
	}
}
