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

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.snf4j.tls.Args;

public class Hkdf implements IHkdf {

	private final Mac mac;
	
	public Hkdf(Mac mac) {
		Args.checkNull(mac, "mac");
		this.mac = mac;
	}
	
	@Override
	public byte[] extract(Key salt, byte[] ikm) throws InvalidKeyException {
		mac.init(salt);
		return mac.doFinal(ikm);
	}
	
	@Override
	public byte[] extract(byte[] salt, byte[] ikm)  throws InvalidKeyException {
		return extract(new SecretKeySpec(salt, mac.getAlgorithm()), ikm);
	}

	@Override
	public byte[] extract(byte[] ikm)  throws InvalidKeyException {
		return extract(new byte[getMacLength()], ikm);
	}

	@Override
	public String getAlgorithm() {
		return mac.getAlgorithm();
	}

	@Override
	public Mac getMacFuntion() {
		try {
			Mac mac = (Mac) this.mac.clone();
			
			mac.reset();
			return mac;
		} catch (CloneNotSupportedException e) {
			try {
				return Mac.getInstance(mac.getAlgorithm(), mac.getProvider());
			} catch (NoSuchAlgorithmException e1) {
				throw new UnsupportedOperationException();
			}
		}
	}
	
	@Override
	public int getMacLength() {
		return mac.getMacLength();
	}

	@Override
	public byte[] expand(Key prk, byte[] info, int length) throws InvalidKeyException {
		mac.init(prk);
		return expand(info, length);
	}

	@Override
	public byte[] expand(byte[] prk, byte[] info, int length) throws InvalidKeyException {
		return expand(new SecretKeySpec(prk, mac.getAlgorithm()), info, length);
	}

	@Override
	public byte[] expand(byte[] info, int length) {
		int hashLen = getMacLength();
		Args.checkRange(length, 1, hashLen*255, "length");
		
		byte[] t = new byte[0];
		byte[] okm = new byte[length];
		int n = (length+hashLen-1)/hashLen;
		int off = 0;
		
		for (int i=1; i<=n; ++i) {
			mac.reset();
			mac.update(t);
			mac.update(info);
			mac.update((byte)i);
			t = mac.doFinal();
			if (length >= hashLen) {
				System.arraycopy(t, 0, okm, off, hashLen);
				off += hashLen;
				length -= hashLen;
			}
			else {
				System.arraycopy(t, 0, okm, off, length);
				break;
			}
		}
		return okm;
	}

}
