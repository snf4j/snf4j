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
import java.util.ArrayList;
import java.util.List;

import static org.snf4j.tls.handshake.HandshakeType.CLIENT_HELLO;
import static org.snf4j.tls.handshake.HandshakeType.SERVER_HELLO;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class KeyShareExtension extends KnownExtension implements IKeyShareExtension {
	
	private final static ExtensionType TYPE = ExtensionType.KEY_SHARE;

	private final Mode mode;
	
	private final KeyShareEntry[] entries;
	
	private final NamedGroup namedGroup;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws AlertException {
			boolean serverHello = handshakeType.value() == SERVER_HELLO.value();
			boolean clientHello = handshakeType.value() == CLIENT_HELLO.value();
			
			if (!serverHello && !clientHello) {
				throw unsupportedExtension(handshakeType);
			}
			if (remaining >= 2) {
				if (remaining == 2) {
					int val = srcs.getUnsignedShort();
					
					
					if (val == 0) {
						if (clientHello) {
							return new KeyShareExtension(Mode.CLIENT_HELLO);
						}
					}
					else if (serverHello) {
						return new KeyShareExtension(NamedGroup.of(val));
					}
				}
				else if (serverHello) {
					if (remaining > 4) {
						NamedGroup namedGrp = NamedGroup.of(srcs.getUnsignedShort());
						int keyLen = srcs.getUnsignedShort();
						
						remaining -= 4;
						if (keyLen == remaining) {
							return new KeyShareExtension(Mode.SERVER_HELLO, parseEntry(srcs, namedGrp, keyLen));
						}
					}
				}
				else {
					int len = srcs.getUnsignedShort();
					
					remaining -= 2;
					if (len == remaining) {
						List<KeyShareEntry> entries = new ArrayList<KeyShareEntry>();
						
						while (remaining > 4) {
							NamedGroup namedGrp = NamedGroup.of(srcs.getUnsignedShort());
							int keyLen = srcs.getUnsignedShort();

							remaining -= 4;
							if (keyLen <= remaining) {
								entries.add(parseEntry(srcs, namedGrp, keyLen));
								remaining -= keyLen;
								if (remaining == 0) {
									return new KeyShareExtension(Mode.CLIENT_HELLO, entries.toArray(new KeyShareEntry[entries.size()]));
								}
							}
							else {
								break;
							}
						}
					}
				}
			}
			throw decodeError("Inconsistent length");
		}		
		
		private KeyShareEntry parseEntry(ByteBufferArray srcs, NamedGroup namedGrp, int keyLen) throws AlertException {
			INamedGroupSpec spec = namedGrp.spec();

			if (spec != null) {
				if (spec.isImplemented()) {
					return new KeyShareEntry(namedGrp, spec.parse(srcs, keyLen));
				}
				if (keyLen != spec.getDataLength()) {
					throw decodeError("Unexpected key_exchange length");
				}
			}
			
			byte[] keyData = new byte[keyLen];
			
			srcs.get(keyData);
			return new KeyShareEntry(namedGrp, keyData);
		}
	};
	
	@SuppressWarnings("incomplete-switch")
	public KeyShareExtension(Mode mode, KeyShareEntry... entries) {
		super(TYPE);
		Args.checkNull(mode, "mode");
		switch (mode) {
		case HELLO_RETRY_REQUEST:
			throw new IllegalArgumentException("type has illegal value");
		
		case SERVER_HELLO:
			Args.checkFixed(entries, 1, "entries");
			break;
		}
		this.mode = mode;
		this.entries = entries;
		namedGroup = null;
	}

	public KeyShareExtension(NamedGroup namedGroup) {
		super(TYPE);
		Args.checkNull(namedGroup, "namedGroup");
		this.mode = Mode.HELLO_RETRY_REQUEST;
		this.namedGroup = namedGroup;
		entries = null;
	}
	
	@Override
	public int getDataLength() {
		switch (mode) {
		case CLIENT_HELLO:
			return 2 + entriesLength();
			
		case SERVER_HELLO:
			return entries[0].getDataLength();
			
		default:
			return 2; 
		}
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		switch (mode) {
		case CLIENT_HELLO:
			buffer.putShort((short) entriesLength());
			for (KeyShareEntry entry: entries) {
				entry.getData(buffer);
			}
			break;
			
		case SERVER_HELLO:
			entries[0].getData(buffer);
			break;
			
		default:
			buffer.putShort((short) namedGroup.value());
		}	
	}
	
	public static IExtensionParser getParser() {
		return PARSER;
	}
	
	@Override
	public NamedGroup getNamedGroup() {
		return namedGroup;
	}
	
	@Override
	public KeyShareEntry[] getEntries() {
		return entries;
	}
	
	@Override
	public Mode getMode() {
		return mode;
	}
	
	private int entriesLength() {
		int sum = 0;
		
		for (KeyShareEntry entry: entries) {
			sum += entry.getDataLength();
		}
		return sum;
	}

}
