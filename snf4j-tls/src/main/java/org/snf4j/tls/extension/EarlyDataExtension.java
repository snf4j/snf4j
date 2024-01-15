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
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class EarlyDataExtension extends KnownExtension implements IEarlyDataExtension {

	private final static ExtensionType TYPE = ExtensionType.EARLY_DATA;
	
	private final long maxSize;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (handshakeType.equals(HandshakeType.NEW_SESSION_TICKET)) {
				if (remaining == 4) {
					return new EarlyDataExtension(srcs.getUnsignedInt());
				}
			}
			else if (remaining == 0) {
				return new EarlyDataExtension();
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public EarlyDataExtension() {
		super(TYPE);
		maxSize = -1;
	}

	public EarlyDataExtension(long maxSize) {
		super(TYPE);
		Args.checkRange(maxSize, 0L, 0xffffffffL, "maxSize");
		this.maxSize = maxSize;
	}

	@Override
	public int getDataLength() {
		return maxSize == -1 ? 0 : 4;
	}

	@Override
	public long getMaxSize() {
		return maxSize;
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}
	
	@Override
	protected void getData(ByteBuffer buffer) {
		if (maxSize != -1) {
			buffer.putInt((int) maxSize);
		}
	}
}
