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
package org.snf4j.tls.extension;

import java.nio.ByteBuffer;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class CookieExtension extends KnownExtension implements ICookieExtension {
	
	private final static ExtensionType TYPE = ExtensionType.COOKIE;
	
	private final byte[] cookie;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws AlertException {
			if (remaining >= 2) {
				int len = srcs.getUnsignedShort();
				
				remaining -= 2;
				if (len == remaining) {
					if (len > 0) {
						byte[] cookie = new byte[len];
						
						srcs.get(cookie);
						return new CookieExtension(cookie);
					}
					else {
						throw decodeError("Cookie is empty");
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public CookieExtension(byte[] cookie) {
		super(TYPE);
		Args.checkMin(cookie, 1, "cookie");
		this.cookie = cookie;
	}

	@Override
	public int getDataLength() {
		return 2 + cookie.length;
	}

	@Override
	public byte[] getCookie() {
		return cookie;
	}
	
	public static IExtensionParser getParser() {
		return PARSER;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short) cookie.length);
		buffer.put(cookie);
	}
}
