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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.extension.ExtensionDecoder;
import org.snf4j.tls.extension.IExtensionDecoder;

public class HandshakeDecoder implements IHandshakeDecoder {

	public final static IHandshakeDecoder DEFAULT;
	
	static {
		HandshakeDecoder decoder = new HandshakeDecoder(ExtensionDecoder.DEFAULT);
		
		decoder.addParser(ClientHello.getParser());
		decoder.addParser(ServerHello.getParser());
		decoder.addParser(EncryptedExtensions.getParser());
		decoder.addParser(Certificate.getParser());
		decoder.addParser(CertificateVerify.getParser());
		decoder.addParser(CertificateRequest.getParser());
		decoder.addParser(Finished.getParser());
		decoder.addParser(NewSessionTicket.getParser());
		decoder.addParser(KeyUpdate.getParser());
		DEFAULT = decoder;
	}
	
	private final Map<HandshakeType,IHandshakeParser> parsers = new HashMap<HandshakeType,IHandshakeParser>();
	
	private IExtensionDecoder extensionDecoder;
	
	public HandshakeDecoder(IExtensionDecoder extensionDecoder) {
		this.extensionDecoder = extensionDecoder;
	}
	
	protected HandshakeType getType(int type) {
		return HandshakeType.of(type);
	}
	
	public void addParser(IHandshakeParser parser) {
		parsers.put(parser.getType(), parser);
	}
	
	public IHandshakeParser removeParser(HandshakeType type) {
		return parsers.remove(type);
	}
	
	public boolean hasParser(HandshakeType type) {
		return parsers.containsKey(type);
	}
	
	public List<IHandshakeParser> getParsers() {
		return new ArrayList<IHandshakeParser>(parsers.values());
	}
	
	public void clearParsers() {
		parsers.clear();
	}
	
	@Override
	public IHandshake decode(ByteBuffer[] srcs, int remaining) throws Alert {
		return decode(ByteBufferArray.wrap(srcs), remaining);
	}

	@Override
	public IHandshake decode(ByteBufferArray srcs, int remaining) throws Alert {
		if (remaining >= 4) {
			HandshakeType type = getType(srcs.getUnsigned());
			int len = srcs.getUnsigned() << 16;
			
			len |= srcs.getUnsignedShort();
			remaining -= 4;
			if (len == remaining) {
				IHandshakeParser parser = parsers.get(type);
				
				if (parser != null) {
					return parser.parse(srcs, len, extensionDecoder);
				}
				byte[] data = new byte[len];
				srcs.get(data);
				return new UnknownHandshake(type, data);
			}
			else if (len > remaining) {
				throw new DecodeErrorAlert("Handshake message '" + type.name() + "' parsing failure: Data underflow");
			}
			else {
				throw new DecodeErrorAlert("Handshake message '" + type.name() + "' parsing failure: Inconsistent length");
			}
		}
		throw new DecodeErrorAlert("Handshake message parsing failure: Data underflow");
	}

}
