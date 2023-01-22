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
package org.snf4j.tls.extension;

import java.nio.ByteBuffer;

import static org.snf4j.tls.handshake.HandshakeType.CLIENT_HELLO;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class SupportedVersionsExtension extends KnownExtension implements ISupportedVersionsExtension {

	private final static ExtensionType TYPE = ExtensionType.SUPPORTED_VERSIONS;
	
	private final Mode mode;
	
	private final int[] versions;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (remaining >= 2) {
				if (handshakeType.value() == CLIENT_HELLO.value()) {
					int len = srcs.getUnsigned();
					
					if ((len & 1) != 0 || len == 0) {
						throw decodeError("Incorrect length");
					}
					--remaining;
					if (len == remaining) {
						int[] versions = new int[len/2];
						
						for (int i=0; i<versions.length; ++i) {
							versions[i] = srcs.getUnsignedShort();
						}
						return new SupportedVersionsExtension(Mode.CLIENT_HELLO, versions);
					}
				}
				else if (remaining == 2) {
					return new SupportedVersionsExtension(Mode.SERVER_HELLO, srcs.getUnsignedShort());
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public SupportedVersionsExtension(Mode mode, int... versions) {
		super(TYPE);
		Args.checkNull(mode, "mode");
		this.mode = mode;
		if (mode == Mode.CLIENT_HELLO) {
			Args.checkMin(versions, 1, "versions");
		}
		else {
			Args.checkFixed(versions, 1, "versions");
		}
		this.versions = versions;
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}
	
	@Override
	public int getDataLength() {
		if (mode == Mode.CLIENT_HELLO) {
			return 1 + versions.length * 2;
		}
		return 2;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		if (mode == Mode.CLIENT_HELLO) {
			buffer.put((byte) (versions.length*2));
			for (int version: versions) {
				buffer.putShort((short) version);
			}
		}
		else {
			buffer.putShort((short) versions[0]);
		}
	}

	@Override
	public int[] getVersions() {
		return versions;
	}

	@Override
	public Mode getMode() {
		return mode;
	}
}
