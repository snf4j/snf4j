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
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.extension.ExtensionsParser;
import org.snf4j.tls.extension.ExtensionsUtil;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class Certificate extends KnownHandshake implements ICertificate {
	
	private final static HandshakeType TYPE = HandshakeType.CERTIFICATE;
	
	private final static ICertificateEntry[] EMPTY = new ICertificateEntry[0];
	
	private final byte[] requestContext;
	
	private final ICertificateEntry[] entries;
	
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
				if (len <= remaining+3) {
					byte[] requestContext = new byte[len];
					
					srcs.get(requestContext);
					remaining -= len + 3;
					len = srcs.getUnsigned() << 16;
					len |= srcs.getUnsignedShort();
					if (len == remaining) {
						if (len == 0) {
							return new Certificate(requestContext, EMPTY);
						}
						else {
							List<ICertificateEntry> entries = new ArrayList<ICertificateEntry>();
							
							while (remaining >= 3) {
								len = srcs.getUnsigned() << 16;
								len |= srcs.getUnsignedShort();
								
								remaining -= 3;
								if (len + 2 <= remaining) {
									byte[] data = new byte[len];
									ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);
									
									srcs.get(data);
									remaining -= len;
									parser.parse(TYPE, srcs, remaining);
									if (parser.isComplete()) {
										entries.add(new CertificateEntry(data,parser.getExtensions()));
									}
									else {
										break;
									}
									remaining -= parser.getConsumedBytes();
								}
								else {
									break;
								}
							}
							if (remaining == 0) {
								return new Certificate(requestContext, entries.toArray(new ICertificateEntry[entries.size()]));
							}
						}
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public Certificate(byte[] requestContext, ICertificateEntry[] entries) {
		super(TYPE);
		Args.checkMax(requestContext, 255, "requestContext");
		Args.checkNull(entries, "entries");
		this.requestContext = requestContext;
		this.entries = entries;
	}
	
	@Override
	public List<IExtension> getExtensioins() {
		List<IExtension> extensions = new ArrayList<IExtension>();
		
		for (ICertificateEntry entry: entries) {
			extensions.addAll(entry.getExtensioins());
		}
		return extensions;
	}

	@Override
	public byte[] getRequestContext() {
		return requestContext;
	}
	
	@Override
	public ICertificateEntry[] getEntries() {
		return entries;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}

	static int calculateLength(ICertificateEntry[] entries) {
		int len = 0;
		
		for (ICertificateEntry entry: entries) {
			len += 3 + 2;
			len += entry.getDataLength();
			len += ExtensionsUtil.calculateLength(entry.getExtensioins());
		}
		return len;
	}
	
	@Override
	public int getDataLength() {
		return 1 
			+ requestContext.length 
			+ 3
			+ calculateLength(entries);
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		if (requestContext.length == 0) {
			buffer.put((byte) 0);
		}
		else {
			buffer.put((byte) requestContext.length);
			buffer.put(requestContext);
		}
		
		int len = calculateLength(entries);
		
		buffer.put((byte) (len >> 16));
		buffer.putShort((short) (len & 0xffff));
		for (ICertificateEntry entry: entries) {
			len = entry.getDataLength();
			buffer.put((byte) (len >> 16));
			buffer.putShort((short) (len & 0xffff));
			buffer.put(entry.getData());
			buffer.putShort((short) ExtensionsUtil.calculateLength(entry.getExtensioins()));
			for (IExtension e: entry.getExtensioins()) {
				e.getBytes(buffer);
			}
		}
	}
}
