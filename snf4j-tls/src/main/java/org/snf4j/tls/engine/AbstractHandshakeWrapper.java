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
import java.util.LinkedList;
import java.util.Queue;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.RecordType;

abstract public class AbstractHandshakeWrapper {
	
	private final Queue<ProducedHandshake> produced = new LinkedList<ProducedHandshake>();
	
	protected final IHandshakeEngine handshaker;
	
	private final IEncryptorHolder encryptors;
	
	private final IEngineStateListener listener;
	
	private ByteBuffer pending;
	
	private Encryptor pendingEncryptor;
	
	public AbstractHandshakeWrapper(IHandshakeEngine handshaker, IEncryptorHolder encryptors, IEngineStateListener listener) {
		this.handshaker = handshaker;
		this.encryptors = encryptors;
		this.listener = listener;
	}

	void clear() {
		produced.clear();
	}
	
	boolean isPending() {
		return pending != null;
	}
	
	boolean needWrap() {
		return !produced.isEmpty() || pending != null;
	}
	
	int wrap(ByteBuffer dst) throws AlertException {
		return wrap(dst, handshaker.getMaxFragmentLength());
	}
	
	int wrap(ByteBuffer dst, int maxFragmentLength) throws AlertException {
		for (ProducedHandshake handshake: handshaker.produce()) {
			produced.add(handshake);
		}
		
		if (produced.isEmpty()) {
			return 0;
		}

		RecordType type = produced.peek().getRecordType();
		Encryptor encryptor = encryptors.getEncryptor(produced.peek().getRecordType());

		return wrap(dst, 
				maxFragmentLength, 
				calculateExpansionLength(encryptor), 
				type, 
				encryptor);
	}
	
	int wrap(ByteBuffer dst, int maxFragmentLength, int expansion, RecordType type, Encryptor encryptor) throws AlertException {
		if (dst.remaining() < maxFragmentLength + expansion) {
			return -1;
		}

		int remaining = maxFragmentLength;
		int length = 0;
		int count = 0;
		int lastLength = -1;
		int pendingLength = -1;
		RecordType nextType = null;
		
		for (ProducedHandshake handshake: produced) {
			if (handshake.getRecordType() != type) {
				break;
			}
			
			int len = handshake.getHandshake().getLength();

			if (len > remaining) {
				pendingLength = len;
				lastLength = remaining;
				len = remaining;		
			}
			remaining -= len;
			length += len;
			if (handshake.getNextRecordType() != null) {
				nextType = handshake.getNextRecordType();
			}
			if (remaining == 0) {
				break;
			}
			++count;
		}
		
		ByteBuffer content = prepareForContent(dst, length, maxFragmentLength, encryptor);
		
		for (int i=0; i<count; ++i) {
			produced.poll().getHandshake().getBytes(content);
		}
		if (pendingLength != -1) {
			pending = ByteBuffer.allocate(pendingLength);
			produced.poll().getHandshake().getBytes(pending);

			ByteBuffer dup = pending.duplicate();

			dup.limit(dup.position()+lastLength);
			pending.put(content);
			pending.position(dup.position());
			pendingEncryptor = encryptor;
		}

		length = wrap(content, length, encryptor, dst);
		if (nextType != null) {
			listener.onSendingTraficKey(nextType);
		}
		return length;
	}
	
	abstract protected int calculateExpansionLength(Encryptor encryptor);
	
	abstract protected ByteBuffer prepareForContent(ByteBuffer dst, int contentLength, int maxFragmentLength, Encryptor encryptor);
	
	abstract protected int wrap(ByteBuffer content, int contentLength, Encryptor encryptor, ByteBuffer dst) throws AlertException;

}
