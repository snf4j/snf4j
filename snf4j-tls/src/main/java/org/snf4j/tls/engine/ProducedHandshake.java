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
package org.snf4j.tls.engine;

import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class ProducedHandshake {
	
	public enum Type {HANDSHAKE, APPLICATION_DATA, CHANGE_CIPHER_SPEC};
	
	private final IHandshake handshake;
	
	private final Type type;
	
	private final RecordType recordType;

	private final RecordType nextRecordType;
	
	public ProducedHandshake(IHandshake handshake) {
		this(handshake, null, (RecordType)null);
	}

	public ProducedHandshake(IHandshake handshake, Type type) {
		this(handshake, null, (RecordType)null, type);
	}

	public ProducedHandshake(IHandshake handshake, RecordType recordType) {
		this(handshake, recordType, (RecordType)null);
	}

	public ProducedHandshake(IHandshake handshake, RecordType recordType, Type type) {
		this(handshake, recordType, null, type);
	}

	public ProducedHandshake(IHandshake handshake, RecordType recordType, RecordType nextRecordType) {
		this(handshake, recordType, nextRecordType, handshake.getType() != null ? Type.HANDSHAKE : Type.APPLICATION_DATA);
	}

	public ProducedHandshake(IHandshake handshake, RecordType recordType, RecordType nextRecordType, Type type) {
		this.handshake = handshake;
		this.recordType = recordType;
		this.nextRecordType = nextRecordType;
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
	
	public IHandshake getHandshake() {
		return handshake;
	}

	public RecordType getRecordType() {
		return recordType;
	}

	public RecordType getNextRecordType() {
		return nextRecordType;
	}
	
}
