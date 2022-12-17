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
import org.snf4j.tls.crypto.DHKeyExchange;
import org.snf4j.tls.crypto.IDHKeyExchange;
import org.snf4j.tls.crypto.IKeyExchange;

public class DHNamedGroupSpec extends AbstractNamedGroupSpec {

	public final static DHNamedGroupSpec FFDHE2048 = new DHNamedGroupSpec(DHKeyExchange.FFDHE2048);

	public final static DHNamedGroupSpec FFDHE3072 = new DHNamedGroupSpec(DHKeyExchange.FFDHE3072);

	public final static DHNamedGroupSpec FFDHE4096 = new DHNamedGroupSpec(DHKeyExchange.FFDHE4096);
	
	public final static DHNamedGroupSpec FFDHE6144 = new DHNamedGroupSpec(DHKeyExchange.FFDHE6144);
	
	public final static DHNamedGroupSpec FFDHE8192 = new DHNamedGroupSpec(DHKeyExchange.FFDHE8192);
	
	private final IDHKeyExchange keyExchange;
	
	public DHNamedGroupSpec(IDHKeyExchange keyExchange) {
		this.keyExchange = keyExchange;
	}
	
	@Override
	public boolean isImplemented() {
		return keyExchange.isImplemented();
	}

	@Override
	public IKeyExchange getKeyExchange() {
		return keyExchange;
	}

	@Override
	public ParsedKey parse(ByteBufferArray srcs, int remaining) throws AlertException {
		if (remaining != getDataLength()) {
			throw decodeError("DH key exchange unexpected size");
		}
		
		byte[] y = new byte[keyExchange.getPLength()];

		srcs.get(y);
		return new DHParsedKey(y);
	}

	@Override
	public PublicKey generateKey(ParsedKey key) throws AlertException {
		try {
			return generatePublicKey(new BigInteger(1, ((DHParsedKey)key).getY()));
		} catch (NoSuchAlgorithmException e) {
			throw internalError("No DH algorithm", e);
		} catch (InvalidKeySpecException e) {
			throw internalError("Invalid DH key specification", e);
		} catch (Exception e) {
			throw internalError("DH key generation failure", e);
		}
	}
	
	protected PublicKey generatePublicKey(BigInteger y) throws NoSuchAlgorithmException, InvalidKeySpecException {
		return keyExchange.generatePublicKey(y);
	}
	
	@Override
	public int getDataLength() {
		return keyExchange.getPLength();
	}

	@Override
	public void getData(ByteBuffer buffer, PublicKey key) {
		getData(buffer, keyExchange.getY(key).toByteArray());
	}

	@Override
	public void getData(ByteBuffer buffer, ParsedKey key) {
		getData(buffer, ((DHParsedKey)key).getY());
	}
	
	void getData(ByteBuffer buffer, byte[] y) {
		getDataWithLeftPadding(buffer, y, keyExchange.getPLength());
	}
	
	private class DHParsedKey implements ParsedKey {
		
		private final byte[] y;
		
		DHParsedKey(byte[] y) {
			this.y = y;
		}

		public byte[] getY() {
			return y;
		}
	}
}
