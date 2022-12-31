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

import org.snf4j.tls.crypto.Hash;
import org.snf4j.tls.crypto.IHash;

public class HashSpec implements IHashSpec {
	
	public final static IHashSpec SHA256;
	
	public final static IHashSpec SHA384;
	
	static {
		SHA256 = new HashSpec("SHA-256", 32, Hash.SHA256,
			new byte[] {(byte)0xe3,(byte)0xb0,(byte)0xc4,0x42,(byte)0x98,(byte)0xfc,0x1c,0x14,
					(byte)0x9a,(byte)0xfb,(byte)0xf4,(byte)0xc8,(byte)0x99,0x6f,(byte)0xb9,0x24,
					0x27,(byte)0xae,0x41,(byte)0xe4,0x64,(byte)0x9b,(byte)0x93,0x4c,(byte)0xa4,
					(byte)0x95,(byte)0x99,0x1b,0x78,0x52,(byte)0xb8,0x55}
		);
		SHA384 = new HashSpec("SHA-348", 48, Hash.SHA384,
			new byte[] {0x38,(byte)0xb0,0x60,(byte)0xa7,0x51,(byte)0xac,(byte)0x96,0x38,0x4c,
					(byte)0xd9,0x32,0x7e,(byte)0xb1,(byte)0xb1,(byte)0xe3,0x6a,0x21,(byte)0xfd,
					(byte)0xb7,0x11,0x14,(byte)0xbe,0x07,0x43,0x4c,0x0c,(byte)0xc7,(byte)0xbf,
					0x63,(byte)0xf6,(byte)0xe1,(byte)0xda,0x27,0x4e,(byte)0xde,(byte)0xbf,(byte)0xe7,
					0x6f,0x65,(byte)0xfb,(byte)0xd5,0x1a,(byte)0xd2,(byte)0xf1,0x48,(byte)0x98,
					(byte)0xb9,0x5b}
		);
	}
	
	private final String algorithm;
	
	private final int hashLength;
	
	private final byte[] emptyHash;
	
	private final IHash hash;

	public HashSpec(String algorithm, int hashLength, IHash hash, byte[] emptyHash) {
		super();
		this.algorithm = algorithm;
		this.hashLength = hashLength;
		this.emptyHash = emptyHash;
		this.hash = hash;
	}
	
	@Override
	public String getAlgorithm() {
		return algorithm;
	}
	
	@Override
	public int getHashLength() {
		return hashLength;
	}
	
	@Override
	public byte[] getEmptyHash() {
		return emptyHash.clone();
	}

	@Override
	public IHash getHash() {
		return hash;
	}
}
