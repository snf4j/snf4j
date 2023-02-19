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

public class PskKeyExchangeModesExtension extends KnownExtension implements IPskKeyExchangeModesExtension {

	private final static ExtensionType TYPE = ExtensionType.PSK_KEY_EXCHANGE_MODES;
	
	private final PskKeyExchangeMode[] modes;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (remaining > 1) {
				int len = srcs.getUnsigned();
				
				--remaining;
				if (len == remaining) {
					PskKeyExchangeMode[] modes = new PskKeyExchangeMode[len];
					
					for (int i=0; i<len; ++i) {
						modes[i] = PskKeyExchangeMode.of(srcs.getUnsigned());
					}
					return new PskKeyExchangeModesExtension(modes);
				}
			}
			throw decodeError("Inconsistent length");
		}	
	};
	
	public PskKeyExchangeModesExtension(PskKeyExchangeMode... modes) {
		super(TYPE);
		Args.checkMin(modes, 1, "modes");
		this.modes = modes;
	}

	@Override
	public PskKeyExchangeMode[] getModes() {
		return modes;
	}

	@Override
	public int getDataLength() {
		return 1 + modes.length;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.put((byte) modes.length);
		for (PskKeyExchangeMode mode: modes) {
			buffer.put((byte) mode.value());
		}
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}

}
