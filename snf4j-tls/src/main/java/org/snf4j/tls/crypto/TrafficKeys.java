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

import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TrafficKeys {
	
	private final IAead aead;
	
	private SecretKey clientKey;
	
	private SecretKey serverKey;
	
	private byte[] clientIv;

	private byte[] serverIv;

	public TrafficKeys(IAead aead, SecretKey clientKey, byte[] clientIv, SecretKey serverKey, byte[] serverIv) {
		this.aead = aead;
		this.clientKey = clientKey;
		this.clientIv = clientIv;
		this.serverKey = serverKey;
		this.serverIv = serverIv;
	}

	public TrafficKeys(IAead aead, SecretKey clientKey, byte[] clientIv) {
		this.aead = aead;
		this.clientKey = clientKey;
		this.clientIv = clientIv;
	}
	
	public IAeadDecrypt getAeadDecrypt(boolean client) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return new AeadDecrypt(getKey(client), aead);
	}

	public IAeadEncrypt getAeadEncrypt(boolean client) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return new AeadEncrypt(getKey(client), aead);
	}
	
	public SecretKey getKey(boolean client) {
		return client ? clientKey : serverKey;
	}
	
	public byte[] getIv(boolean client) {
		return client ? clientIv : serverIv;
	}
	
	public void clear() {
		clientKey = null;
		serverKey = null;
		clientIv = null;
		serverIv = null;
	}
}
