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
package org.snf4j.tls.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

public class Hash implements IHash {
	
	public final static IHash SHA256 = new Hash("SHA-256","HmacSHA256", HashId.SHA_256);

	public final static IHash SHA384 = new Hash("SHA-384","HmacSHA384", HashId.SHA_384);
	
	private final String hashAlgoritm;
	
	private final String macAlgorithm;
	
	private final HashId id;
	
	public Hash(String hashAlgorithm, String macAlgorithm, HashId id) {
		this.hashAlgoritm = hashAlgorithm;
		this.macAlgorithm = macAlgorithm;
		this.id = id;
	}
	
	@Override
	public MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
		return MessageDigest.getInstance(hashAlgoritm);
	}
	
	@Override
	public Mac createMac() throws NoSuchAlgorithmException {
		return Mac.getInstance(macAlgorithm);
	}

	@Override
	public HashId getId() {
		return id;
	}
}
