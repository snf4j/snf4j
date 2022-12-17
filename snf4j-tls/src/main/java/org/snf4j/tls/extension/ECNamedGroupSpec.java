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
import java.security.spec.InvalidParameterSpecException;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.crypto.ECKeyExchange;
import org.snf4j.tls.crypto.IECKeyExchange;
import org.snf4j.tls.crypto.IKeyExchange;

public class ECNamedGroupSpec extends AbstractNamedGroupSpec {

	public final static ECNamedGroupSpec SECP256R1 = new ECNamedGroupSpec(ECKeyExchange.SECP256R1,32);
	
	public final static ECNamedGroupSpec SECP384R1 = new ECNamedGroupSpec(ECKeyExchange.SECP384R1,48);

	public final static ECNamedGroupSpec SECP521R1 = new ECNamedGroupSpec(ECKeyExchange.SECP521R1,66);
	
	private final int coordinateLength;
	
	private final IECKeyExchange keyExchange;
	
	public ECNamedGroupSpec(IECKeyExchange keyExchange, int coordinateLength) {
		this.coordinateLength = coordinateLength;
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
			throw decodeError("EC key exchange unexpected size");
		}
		if (srcs.get() != 4) {
			throw decodeError("EC key exchange unexpected legacy form");
		}

		byte[] x = new byte[coordinateLength];
		byte[] y = new byte[coordinateLength];
		
		srcs.get(x);
		srcs.get(y);
		return new ECParsedKey(x,y);
	}

	@Override
	public PublicKey generateKey(ParsedKey key) throws AlertException {
		ECParsedKey ecKey = (ECParsedKey) key;
		
        try {
        	return keyExchange.generatePublicKey(new BigInteger(1,ecKey.getX()), new BigInteger(1,ecKey.getY()));
		} catch (NoSuchAlgorithmException e) {
			throw internalError("No EC algorithm", e);
		} catch (InvalidKeySpecException e) {
			throw internalError("Invalid EC key specification", e);
		} catch (InvalidParameterSpecException e) {
			throw internalError("Invalid EC parameter specification", e);
		} catch (Exception e) {
			throw internalError("EC key generation failure", e);
		}
	}
	
	@Override
	public int getDataLength() {
		return 1 + coordinateLength*2;
	}
	
	@Override
	public void getData(ByteBuffer buffer, PublicKey key) {
		getData(buffer, keyExchange.getX(key).toByteArray(), keyExchange.getY(key).toByteArray());
	}

	@Override
	public void getData(ByteBuffer buffer, ParsedKey key) {
		ECParsedKey ecKey = (ECParsedKey) key;

		getData(buffer, ecKey.getX(), ecKey.getY());
	}

	void getData(ByteBuffer buffer, byte[] x, byte[] y) {
		buffer.put((byte) 4);
		getDataWithLeftPadding(buffer, x, coordinateLength);
		getDataWithLeftPadding(buffer, y, coordinateLength);
	}

	private class ECParsedKey implements ParsedKey {
		
		private final byte[] x;
		
		private final byte[] y;
		
		ECParsedKey(byte[] x, byte[] y) {
			this.x = x;
			this.y = y;
		}

		public byte[] getX() {
			return x;
		}

		public byte[] getY() {
			return y;
		}
	}
}
