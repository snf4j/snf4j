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

public class NewSessionTicket extends KnownHandshake implements INewSessionTicket {

	private final static HandshakeType TYPE = HandshakeType.NEW_SESSION_TICKET;
	
	private final long lifetime;
	
	private final long ageAdd;
	
	private final byte[] nonce;
	
	private final byte[] ticket;
	
	private final List<IExtension> extensions;

	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws Alert {
			if (remaining > 9) {
				long lifetime = srcs.getUnsignedInt();
				long ageAdd = srcs.getUnsignedInt();
				int len = srcs.getUnsigned();
				
				remaining -= 9;
				if (remaining > len + 2) {
					byte[] nonce = new byte[len];
					
					srcs.get(nonce);
					remaining -= len + 2;
					len = srcs.getUnsignedShort();
					if (len > 0) {
						if (remaining >= len + 2) {
							byte[] ticket = new byte[len];

							srcs.get(ticket);
							remaining -= len;

							ExtensionsParser parser = new ExtensionsParser(0, 0xffff, decoder);

							parser.parse(TYPE, srcs, remaining);
							if (parser.isComplete() && remaining == parser.getConsumedBytes()) {
								return new NewSessionTicket(ticket, nonce, lifetime, ageAdd, parser.getExtensions());
							}
						}
					}
					else {
						throw decodeError("Empty ticket");
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
		
	};
	
	public NewSessionTicket(byte[] ticket, byte[] nonce, long lifetime, long ageAdd, List<IExtension> extensions) {
		super(TYPE);
		Args.checkMin(ticket, 1, "ticket");
		Args.checkNull(nonce, "nonce");
		Args.checkNull(extensions, "extensions");
		this.ticket = ticket;
		this.nonce = nonce;
		this.lifetime = lifetime;
		this.ageAdd = ageAdd;
		this.extensions = extensions;
	}

	@Override
	public long getLifetime() {
		return lifetime;
	}

	@Override
	public long getAgeAdd() {
		return ageAdd;
	}

	@Override
	public byte[] getNonce() {
		return nonce;
	}

	@Override
	public byte[] getTicket() {
		return ticket;
	}

	@Override
	public List<IExtension> getExtensions() {
		return extensions;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}
	
	@Override
	public int getDataLength() {
		return 4 + 4 
				+ 1 
				+ nonce.length 
				+ 2 
				+ ticket.length 
				+ 2 
				+ ExtensionsUtil.calculateLength(extensions);
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putInt((int) lifetime);
		buffer.putInt((int) ageAdd);
		buffer.put((byte) nonce.length);
		buffer.put(nonce);
		buffer.putShort((short) ticket.length);
		buffer.put(ticket);
		buffer.putShort((short) ExtensionsUtil.calculateLength(extensions));
		for (IExtension e: extensions) {
			e.getBytes(buffer);
		}
	}
	
}
