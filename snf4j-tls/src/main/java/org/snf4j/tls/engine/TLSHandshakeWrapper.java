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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.Record;

public class TLSHandshakeWrapper extends AbstractHandshakeWrapper {
	
	public TLSHandshakeWrapper(IHandshakeEngine handshaker, IEncryptorHolder encryptors, IEngineStateListener listener) {
		super(handshaker, encryptors, listener);
	}
	
	@Override
	protected int calculateExpansionLength(Encryptor encryptor) {
		return encryptor == null 
				? Record.HEADER_LENGTH 
				: Record.HEADER_LENGTH + 1 + encryptor.getExapnsion();
	}
	
	@Override
	protected ByteBuffer prepareForContent(ByteBuffer dst, int contentLength, int maxFragmentLength, Encryptor encryptor) {
		if (encryptor == null) {
			Record.header(ContentType.HANDSHAKE, contentLength, dst);
			return dst;
		}
		else {
			int padding = handshaker.getHandler().calculatePadding(ContentType.HANDSHAKE, contentLength);
			
			if (padding > 0) {
				padding = Math.min(padding, maxFragmentLength-contentLength);
			}
			return ByteBuffer.wrap(new byte[contentLength + 1 + padding]);
		}
	}
	
	@Override
	protected int wrap(ByteBuffer content, int contentLength, Encryptor encryptor, ByteBuffer dst) throws Alert {
		if (encryptor == null) {
			return contentLength + Record.HEADER_LENGTH;
		}
		content.put((byte) ContentType.HANDSHAKE.value());
		content.position(0);
		return Record.protect(content, encryptor, dst);
	}
}
