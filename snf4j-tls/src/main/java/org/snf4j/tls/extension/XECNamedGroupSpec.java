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
package org.snf4j.tls.extension;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.crypto.IKeyExchange;
import org.snf4j.tls.crypto.IXDHKeyExchange;
import org.snf4j.tls.crypto.XDHKeyExchange;

public class XECNamedGroupSpec extends AbstractNamedGroupSpec {
	
	public final static XECNamedGroupSpec X25519 = new XECNamedGroupSpec(XDHKeyExchange.X25519, 32);
	
	public final static XECNamedGroupSpec X448 = new XECNamedGroupSpec(XDHKeyExchange.X448, 56);
	
	private final int contentLength;
	
	private final IXDHKeyExchange keyExchange;
	
	@Override
	public boolean isImplemented() {
		return keyExchange.isImplemented();
	}

	public XECNamedGroupSpec(IXDHKeyExchange keyExchange, int contentLength) {
		this.contentLength = contentLength;
		this.keyExchange = keyExchange;
	}

	@Override
	public ParsedKey parse(ByteBufferArray srcs, int remaining) throws AlertException {
		if (remaining != getDataLength()) {
			throw decodeError("XEC key exchange unexpected size");
		}
		
		byte[] u = new byte[contentLength];

		srcs.get(u);
		return new XECParsedKey(u);
	}

	@Override
	public PublicKey generateKey(ParsedKey key) throws AlertException {
		byte[] u = ((XECParsedKey)key).getU().clone();
		
		reverse(u);
		try {
			return keyExchange.generatePublicKey(new BigInteger(1,u));
		} catch (NoSuchAlgorithmException e) {
			throw internalError("No XDH algorithm", e);
		} catch (InvalidKeySpecException e) {
			throw internalError("Invalid XEC key specification", e);
		} catch (Exception e) {
			throw internalError("XEC key generation failure", e);
		}
	}
	
	@Override
	public int getDataLength() {
		return contentLength;
	}

	@Override
	public void getData(ByteBuffer buffer, PublicKey key) {
		byte[] u = keyExchange.getU(key).toByteArray();
		
		reverse(u);
		getData(buffer, u);
	}

	@Override
	public void getData(ByteBuffer buffer, ParsedKey key) {
		getData(buffer, ((XECParsedKey)key).getU());
	}

	void getData(ByteBuffer buffer, byte[] u) {
		getDataWithRightPadding(buffer, u, contentLength);
	}
	
	@Override
	public IKeyExchange getKeyExchange() {
		return keyExchange;
	}

	static void reverse(byte[] data) {
		int i = 0;
		int j = data.length - 1;
		byte tmp;
		
		while(j > i) {
			tmp = data[j];
			data[j] = data[i];
			data[i] = tmp;
			++i;
			--j;
		}
	}
	
	private class XECParsedKey implements ParsedKey {
		
		private final byte[] u;
		
		XECParsedKey(byte[] u) {
			this.u = u;
		}

		public byte[] getU() {
			return u;
		}
	}

}
