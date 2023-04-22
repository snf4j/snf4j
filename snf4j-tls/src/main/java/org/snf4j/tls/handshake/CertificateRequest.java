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
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.extension.ExtensionsParser;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class CertificateRequest extends KnownHandshake implements ICertificateRequest {

	private final static HandshakeType TYPE = HandshakeType.CERTIFICATE_REQUEST;

	private final static byte[] EMPTY = new byte[0];
	
	private final byte[] context;
	
	private final List<IExtension> extensions;
	
	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws Alert {
			if (remaining >= 1) {
				int len = srcs.getUnsigned();
				
				--remaining;
				if (remaining >= len + 2) {
					byte[] context = new byte[len];
					
					srcs.get(context);
					remaining -= len;

					ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);

					parser.parse(TYPE, srcs, remaining);
					if (parser.isComplete() && remaining == parser.getConsumedBytes()) {
						return new CertificateRequest(context, parser.getExtensions());
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public CertificateRequest(List<IExtension> extensions) {
		super(TYPE);
		Args.checkNull(extensions, "extensions");
		context = EMPTY;
		this.extensions = extensions;
	}

	public CertificateRequest(byte[] context, List<IExtension> extensions) {
		super(TYPE);
		Args.checkMax(context, 255, "context");
		Args.checkNull(extensions, "extensions");
		this.context = context;
		this.extensions = extensions;
	}

	@Override
	public int getDataLength() {
		return 1 + context.length + 2 + ExtensionsUtil.calculateLength(extensions);
	}

	@Override
	public List<IExtension> getExtensions() {
		return extensions;
	}

	@Override
	public byte[] getContext() {
		return context;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}
	
	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.put((byte) context.length);
		buffer.put(context);
		buffer.putShort((short) ExtensionsUtil.calculateLength(extensions));
		for (IExtension e: extensions) {
			e.getBytes(buffer);
		}
	}

}
