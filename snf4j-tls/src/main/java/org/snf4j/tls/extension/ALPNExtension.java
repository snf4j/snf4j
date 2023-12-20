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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class ALPNExtension extends KnownExtension implements IALPNExtension {

	private final static ExtensionType TYPE = ExtensionType.APPLICATION_LAYER_PROTOCOL_NEGOTIATION;
	
	private final byte[][] protocolNames;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (remaining >= 2) {
				int len = srcs.getUnsignedShort();
				
				remaining -= 2;
				if (len == remaining) {
					List<byte[]> names = new ArrayList<byte[]>();
					
					while (remaining > 0) {
						len = srcs.getUnsigned();

						if (len == 0) {
							throw decodeError("Empty protocol name");
						}
						
						if (len <= remaining-1) {
							byte[] name = new byte[len];
							
							srcs.get(name);
							names.add(name);
							remaining -= len + 1;
						}
						else {
							break;
						}
					}
					if (remaining == 0) {
						if (names.isEmpty()) {
							throw decodeError("No protocol names");
						}
						if (handshakeType != HandshakeType.CLIENT_HELLO && names.size() > 1) {
							throw decodeError("Too many protocol names");
						}
						return new ALPNExtension(names.toArray(new byte[names.size()][]));
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public ALPNExtension(String... protocolNames) {
		super(TYPE);
		Args.checkMin(protocolNames, 1, "protocolNames");
		this.protocolNames = new byte[protocolNames.length][];
		for (int i=0; i<protocolNames.length; ++i) {
			this.protocolNames[i] = protocolNames[i].getBytes(StandardCharsets.US_ASCII);
		}
	}
	
	public ALPNExtension(byte[]... protocolNames) {
		super(TYPE);
		Args.checkMin(protocolNames, 1, "protocolNames");
		for (byte[] protocolName: protocolNames) {
			if (protocolName == null) {
				throw new NullPointerException();
			}
		}
		this.protocolNames = protocolNames;
	}
	
	public static IExtensionParser getParser() {
		return PARSER;
	}
	
	private int protocolNamesLength() {
		int sum = 0;
		
		for (byte[] name: protocolNames) {
			sum += name.length + 1;
		}
		return sum;
	}
	
	@Override
	public int getDataLength() {
		return 2 + protocolNamesLength();
	}

	@Override
	public String[] getProtocolNames() {
		String[] names = new String[protocolNames.length];
		
		for (int i=0; i<protocolNames.length; ++i) {
			names[i] = new String(protocolNames[i], StandardCharsets.US_ASCII);
		}
		return names;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short)protocolNamesLength());
		for (byte[] protocolName: protocolNames) {
			buffer.put((byte) protocolName.length);
			buffer.put(protocolName);
		}
	}

}
