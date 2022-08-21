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
package org.snf4j.tls.handshake;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionsParser;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class ClientHello extends AbstractHello implements IClientHello {

	private final static HandshakeType TYPE = HandshakeType.CLIENT_HELLO;

	private final List<CipherSuite> cipherSuites;
	
	private final byte[] legacyCompressionMethods;

	private final List<IExtension> extensions;
	
	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws DecodeErrorAlertException {
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
						if (remaining >= 4) {
							len = srcs.getUnsignedShort();
							remaining -= 2;
							if ((len & 1) == 0) {
								if (remaining >= len) {
									remaining -= len;
									len /= 2;
									List<CipherSuite> cipherSuites = new ArrayList<CipherSuite>(len);
									
									while (len > 0) {
										cipherSuites.add(CipherSuite.of(srcs.getUnsignedShort()));
										--len;
									}
									if (remaining >= 2) {
										len = srcs.getUnsigned();
										--remaining;
										if (remaining >= len) {
											byte[] legacyCompressionMethods = new byte[len];

											srcs.get(legacyCompressionMethods);
											remaining -= len;
											if (remaining >= 2) {
												ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);

												parser.parse(srcs, remaining);
												if (parser.isComplete() && remaining == parser.getConsumedBytes()) {
													return new ClientHello(
															legacyVersion,
															random,
															legacySessionId,
															cipherSuites,
															legacyCompressionMethods,
															parser.getExtensions()
															);
												}
											}
										}
									}
								}
							}
							else {
								throw decodeError("Cipher suites invalid length");
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
	
	public ClientHello(int legacyVersion, byte[] random, byte[] legacySessionId, List<CipherSuite> cipherSuites, byte[] legacyCompressionMethods, List<IExtension> extensions) {
		super(TYPE, legacyVersion, random, legacySessionId);
		Args.checkNull(cipherSuites, "cipherSuites");
		Args.checkNull(extensions, "extensions");
		Args.checkMax(legacyCompressionMethods, 255, "legacyCompressionMethods");
		this.cipherSuites = cipherSuites;
		this.legacyCompressionMethods = legacyCompressionMethods;
		this.extensions = extensions;
	}
	
	@Override
	public List<CipherSuite> getCipherSuites() {
		return cipherSuites;
	}

	@Override
	public byte[] getLegacyCompressionMethods() {
		return legacyCompressionMethods;
	}

	@Override
	public List<IExtension> getExtensioins() {
		return extensions;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}
	
	@Override
	public int getDataLength() {
		return super.getDataLength() 
				+ 2 
				+ cipherSuites.size() * 2 
				+ 1 
				+ legacyCompressionMethods.length
				+ 2
				+ ExtensionsUtil.calculateLength(extensions);
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		super.getData(buffer);
		int size = cipherSuites.size();
		buffer.putShort((short) (size*2));
		for (CipherSuite cs: cipherSuites) {
			buffer.putShort((short) cs.value());
		}
		buffer.put((byte) legacyCompressionMethods.length);
		buffer.put(legacyCompressionMethods);
		buffer.putShort((short) ExtensionsUtil.calculateLength(extensions));
		for (IExtension e: extensions) {
			e.getBytes(buffer);
		}
	}
	
}
