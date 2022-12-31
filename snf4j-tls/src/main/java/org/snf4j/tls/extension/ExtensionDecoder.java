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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.handshake.HandshakeType;

public class ExtensionDecoder implements IExtensionDecoder {

	public final static IExtensionDecoder DEFAULT;
	
	static {
		ExtensionDecoder decoder = new ExtensionDecoder();
		
		decoder.addParser(ServerNameExtension.getParser());
		decoder.addParser(SupportedVersionsExtension.getParser());
		decoder.addParser(SupportedGroupsExtension.getParser());
		decoder.addParser(KeyShareExtension.getParser());
		decoder.addParser(SignatureAlgorithmsExtension.getParser());
		decoder.addParser(SignatureAlgorithmsCertExtension.getParser());
		DEFAULT = decoder;
	}
	
	private final Map<ExtensionType,IExtensionParser> parsers = new HashMap<>();
	
	protected ExtensionType getType(int type) {
		return ExtensionType.of(type);
	}
	
	public void addParser(IExtensionParser parser) {
		parsers.put(parser.getType(), parser);
	}
	
	public IExtensionParser removeParser(ExtensionType type) {
		return parsers.remove(type);
	}
	
	public boolean hasParser(ExtensionType type) {
		return parsers.containsKey(type);
	}
	
	public List<IExtensionParser> getParsers() {
		return new ArrayList<IExtensionParser>(parsers.values());
	}
	
	public void clearParsers() {
		parsers.clear();
	}
	
	@Override
	public IExtension decode(HandshakeType handshakeType, ByteBuffer[] srcs, int remaining) throws AlertException {
		return decode(handshakeType, ByteBufferArray.wrap(srcs), remaining);
	}
	
	@Override
	public IExtension decode(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws AlertException {
		if (remaining >= 4) {
			ExtensionType type = getType(srcs.getUnsignedShort());
			int len = srcs.getUnsignedShort();

			remaining -= 4;
			if (len <= remaining) {
				IExtensionParser parser = parsers.get(type);

				if (parser != null) {
					return parser.parse(handshakeType, srcs, len);
				}
				byte[] data = new byte[len];
				srcs.get(data);
				return new UnknownExtension(type, data);
			}
			throw new DecodeErrorAlertException("Extension '" + type.name() + "' parsing failure: Data underflow");
		}
		throw new DecodeErrorAlertException("Extension parsing failure: Data underflow");
	}

	
}
