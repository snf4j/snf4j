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
package org.snf4j.tls.handshake;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.extension.ExtensionsParser;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class EncryptedExtensions extends KnownHandshake implements IEncryptedExtensions {

	private final static HandshakeType TYPE = HandshakeType.ENCRYPTED_EXTENSIONS;
	
	private final List<IExtension> extensions;
	
	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws AlertException {
			if (remaining >= 2) {
				ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);

				parser.parse(TYPE, srcs, remaining);
				if (parser.isComplete() && remaining == parser.getConsumedBytes()) {
					return new EncryptedExtensions(parser.getExtensions());
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public EncryptedExtensions(List<IExtension> extensions) {
		super(TYPE);
		Args.checkNull(extensions, "extensions");
		this.extensions = extensions;
	}

	@Override
	public int getDataLength() {
		return 2 + ExtensionsUtil.calculateLength(extensions);
	}

	@Override
	public List<IExtension> getExtensioins() {
		return extensions;
	}
	
	public static IHandshakeParser getParser() {
		return PARSER;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short) ExtensionsUtil.calculateLength(extensions));
		for (IExtension e: extensions) {
			e.getBytes(buffer);
		}
	}

}
