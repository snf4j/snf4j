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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.engine.ProducedHandshake.Type;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.RecordType;

abstract public class AbstractHandshakeFragmenter {
	
	private final Queue<ProducedHandshake> produced = new LinkedList<ProducedHandshake>();
	
	protected final IHandshakeEngine handshaker;
	
	private final IEncryptorHolder encryptors;
	
	private final IEngineStateListener listener;
	
	private ByteBuffer pending;

	private ProducedHandshake.Type pendingType;
	
	private Encryptor pendingEncryptor;
	
	public AbstractHandshakeFragmenter(IHandshakeEngine handshaker, IEncryptorHolder encryptors, IEngineStateListener listener) {
		this.handshaker = handshaker;
		this.encryptors = encryptors;
		this.listener = listener;
	}

	public void clear() {
		produced.clear();
	}
	
	public boolean isPending() {
		return pending != null;
	}
	
	public boolean needWrap() {
		return !produced.isEmpty() || isPending();
	}
	
	public int wrap(ByteBuffer dst) throws Alert {
		if (pending != null) {
			return wrapPending(dst, handshaker.getState().getMaxFragmentLength());
		}
		return wrap(dst, handshaker.getState().getMaxFragmentLength());
	}
	
	private int wrap(ByteBuffer dst, int maxFragmentLength) throws Alert {
		for (ProducedHandshake handshake: handshaker.produce()) {
			produced.add(handshake);
		}
		
		if (produced.isEmpty()) {
			return 0;
		}

		ProducedHandshake firstHandshake = produced.peek();
		RecordType recordType = firstHandshake.getRecordType();
		
		if (recordType == null) {
			int length = firstHandshake.getHandshake().getLength();
			
			if (dst.remaining() < length) {
				return -1;
			}
			produced.poll().getHandshake().getBytes(dst);
			return length;
		}
		
		Encryptor encryptor = encryptors.getEncryptor(recordType);
		int remaining = maxFragmentLength;
		int length = 0;
		int count = 0;
		int lastLength = -1;
		int pendingLength = -1;
		RecordType nextRecordType = null;
		Type type = firstHandshake.getType();
		
		for (ProducedHandshake handshake: produced) {
			if (handshake.getRecordType() != recordType || handshake.getType() != type) {
				break;
			}
			
			int len = handshake.getHandshake().getLength();

			if (len > remaining) {
				pendingLength = len;
				lastLength = remaining;
				len = remaining;		
			}
			else {
				++count;
			}
			remaining -= len;
			length += len;
			if (handshake.getNextRecordType() != null) {
				nextRecordType = handshake.getNextRecordType();
				break;
			}
			if (remaining == 0) {
				break;
			}
		}

		if (dst.remaining() < length + calculateExpansionLength(encryptor)) {
			return -1;
		}
		
		ByteBuffer content = prepareForContent(dst, length, maxFragmentLength, type, encryptor);
		
		for (int i=0; i<count; ++i) {
			produced.poll().getHandshake().getBytes(content);
		}
		if (pendingLength != -1) {
			pending = ByteBuffer.allocate(pendingLength);
			produced.poll().getHandshake().getBytes(pending);
			pending.flip();
			
			ByteBuffer dup = pending.duplicate();

			dup.limit(lastLength);
			content.put(dup);
			pending.position(dup.position());
			pendingType = type;
			pendingEncryptor = encryptor;
		}

		length = wrap(content, length, type, encryptor, dst);
		if (nextRecordType != null) {
			listener.onNewSendingTraficKey(handshaker.getState(), nextRecordType);
		}
		return length;
	}

	private int wrapPending(ByteBuffer dst, int maxFragmentLength) throws Alert {
		Encryptor encryptor = pendingEncryptor;
		Type type = pendingType;
		int expansion = calculateExpansionLength(pendingEncryptor);
		int remaining = dst.remaining();
		int length = Math.min(pending.remaining(), maxFragmentLength) ;
		boolean keepPending;
		
		if (remaining < length + expansion) {
			//if (length < maxFragmentLength) {
				return -1;
			//}
			//length = remaining - expansion;
			//keepPending = true;
		}
		else {
			keepPending = pending.remaining() > length;
		}
		
		ByteBuffer content = prepareForContent(dst, length, maxFragmentLength, type, encryptor);
		
		if (keepPending) {
			ByteBuffer dup = pending.duplicate();

			dup.limit(dup.position() + length);
			content.put(dup);
			pending.position(dup.position());
		}
		else {
			content.put(pending);
			pending = null;
			pendingType = null;
			pendingEncryptor = null;
		}
		return wrap(content, length, type, encryptor, dst);
	}
		
	abstract protected int calculateExpansionLength(Encryptor encryptor);
	
	abstract protected ByteBuffer prepareForContent(ByteBuffer dst, int contentLength, int maxFragmentLength, Type type, Encryptor encryptor);
	
	abstract protected int wrap(ByteBuffer content, int contentLength, Type type, Encryptor encryptor, ByteBuffer dst) throws Alert;

}
