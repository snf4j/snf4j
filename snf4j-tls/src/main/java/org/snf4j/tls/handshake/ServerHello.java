/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.handshake;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionsParser;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class ServerHello extends AbstractHello implements IServerHello {
	
	private final static HandshakeType TYPE = HandshakeType.SERVER_HELLO;

	private final CipherSuite cipherSuite;
	
	private final byte legacyCompressionMethod;
	
	private final List<IExtension> extensions;
	
	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws Alert {
			if (remaining > COMMON_PART_MIN_LENGTH) {
				int legacyVersion = srcs.getUnsignedShort();
				byte[] random = new byte[RANDOM_LENGTH]; 
				int len;
				
				srcs.get(random);
				len = srcs.getUnsigned();
				remaining -= COMMON_PART_MIN_LENGTH;
				if (len <= SESSION_ID_MAX_LENGTH) {
					if (remaining >= len) {
						byte[] legacySessionId = new byte[len];

						if (len > 0) {
							srcs.get(legacySessionId);
							remaining -= len;
						}
						if (remaining >= 3) {
							CipherSuite cipherSuite = CipherSuite.of(srcs.getUnsignedShort());
							byte legacyCompressionMethod = srcs.get();
							
							remaining -= 3;
							if (remaining >= 2) {
								ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);

								parser.parse(TYPE, srcs, remaining);
								if (parser.isComplete() && remaining == parser.getConsumedBytes()) {
									return new ServerHello(
											legacyVersion,
											random,
											legacySessionId,
											cipherSuite,
											legacyCompressionMethod,
											parser.getExtensions()
											);
								}
							}
							
						}
					}
				}
				else {
					throw decodeError("Legacy session id is too big");
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public ServerHello(int legacyVersion, byte[] random, byte[] legacySessionId, CipherSuite cipherSuite, byte legacyCompressionMethod, List<IExtension> extensions) {
		super(TYPE, legacyVersion, random, legacySessionId);
		Args.checkNull(cipherSuite, "cipherSuite");
		Args.checkNull(extensions, "extensions");
		this.cipherSuite = cipherSuite;
		this.legacyCompressionMethod = legacyCompressionMethod;
		this.extensions = extensions;
	}

	@Override
	public List<IExtension> getExtensions() {
		return extensions;
	}

	@Override
	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}

	@Override
	public byte getLegacyCompressionMethod() {
		return legacyCompressionMethod;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}

	@Override
	public int getDataLength() {
		return super.getDataLength() 
				+ 2 
				+ 1 
				+ 2
				+ ExtensionsUtil.calculateLength(extensions);
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		super.getData(buffer);
		buffer.putShort((short) cipherSuite.value());
		buffer.put(legacyCompressionMethod);
		buffer.putShort((short) ExtensionsUtil.calculateLength(extensions));
		for (IExtension e: extensions) {
			e.getBytes(buffer);
		}
	}
	
}
