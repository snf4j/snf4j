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

import java.nio.ByteBuffer;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class SignatureAlgorithmsExtension extends KnownExtension {

	private final static ExtensionType TYPE = ExtensionType.SIGNATURE_ALGORITHMS;

	private final SignatureScheme[] schemes;
	
	private final static AbstractExtensionParser PARSER = new Parser(TYPE);
	
	static class Parser extends AbstractExtensionParser {
		
		private final ExtensionType type;
		
		Parser(ExtensionType type) {
			this.type = type;
		}
		
		@Override
		public ExtensionType getType() {
			return type;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws AlertException {
			if (remaining >= 4) {
				int len = srcs.getUnsignedShort();
				
				if ((len & 1) != 0 || len == 0) {
					throw decodeError("Incorrect length");
				}
				remaining -= 2;
				if (len == remaining) {
					SignatureScheme[] schemes = new SignatureScheme[len/2];
					
					for (int i=0; i<schemes.length; ++i) {
						schemes[i] = SignatureScheme.of(srcs.getUnsignedShort());
					}
					return new SignatureAlgorithmsExtension(schemes);
				}
			}
			throw decodeError("Inconsistent length");
		}
		
	}
	
	SignatureAlgorithmsExtension(ExtensionType type, SignatureScheme... schemes) {
		super(type);
		Args.checkMin(schemes, 1, "schemes");
		this.schemes = schemes;
	}

	public SignatureAlgorithmsExtension(SignatureScheme... schemes) {
		this(TYPE, schemes);
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}

	public SignatureScheme[] getSchemes() {
		return schemes;
	}
	
	@Override
	public int getDataLength() {
		return 2 + schemes.length*2;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short) (schemes.length*2));
		for (SignatureScheme scheme: schemes) {
			buffer.putShort((short)scheme.value());
		}
	}
}
