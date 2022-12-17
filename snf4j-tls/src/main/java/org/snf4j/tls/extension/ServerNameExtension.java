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
import java.nio.charset.StandardCharsets;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class ServerNameExtension extends KnownExtension implements IServerNameExtension {

	private final static ExtensionType TYPE = ExtensionType.SERVER_NAME;
	
	private final static ServerNameExtension EMPTY = new ServerNameExtension();
	
	private final byte[] hostName;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws AlertException {
			
			if (remaining > 0) {
				if (remaining >= 2) {
					int len = srcs.getUnsignedShort();
					
					remaining -= 2;
					if (len == remaining && len >= 1+2) {
						int type = srcs.get();
						byte[] hostName = null;
						
						--remaining;
						switch (type) {
						case 0:
							hostName = parseName(srcs, remaining);
							remaining -= hostName.length + 2;
							break;
							
						default:
							throw decodeError("Invalid name type");
						}
						
						if (remaining == 0) {
							return new ServerNameExtension(hostName);
						}
					}
				}
			}
			else {
				return EMPTY;
			}
			throw decodeError("Inconsistent length");
		}
		
		private byte[] parseName(ByteBufferArray array, int remaining) throws AlertException {
			int len = array.getUnsignedShort();
			
			if (len == 0) {
				throw decodeError("Empty name");
			}
			if (len <= remaining-2) {
				byte[] data = new byte[len];
				
				array.get(data);
				return data;
			}
			throw decodeError("Inconsistent name length");
		}
	};
	
	public ServerNameExtension(String hostName) {
		super(TYPE);
		this.hostName = hostName.getBytes(StandardCharsets.US_ASCII);
		Args.checkRange(this.hostName.length, 1, 0xffff-5, "hostName length");
	}

	public ServerNameExtension(byte[] hostName) {
		super(TYPE);
		Args.checkRange(hostName.length, 1, 0xffff-5, "hostName length");
		this.hostName = hostName;
	}

	public ServerNameExtension() {
		super(TYPE);
		this.hostName = null;
	}
	
	@Override
	public String getHostName() { return hostName == null ? "" : new String(hostName, StandardCharsets.US_ASCII); }
	
	public static IExtensionParser getParser() {
		return PARSER;
	}

	@Override
	public int getDataLength() {
		return hostName == null ? 0 : 2 + 1 + 2 + hostName.length;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		if (hostName != null) {
			buffer.putShort((short) (hostName.length + 1 + 2));
			buffer.put((byte) 0);
			buffer.putShort((short) hostName.length);
			buffer.put(hostName);
		}
	}
}
