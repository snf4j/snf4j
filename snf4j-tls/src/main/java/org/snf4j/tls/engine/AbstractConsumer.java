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
package org.snf4j.tls.engine;

import java.nio.ByteBuffer;

import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.UnknownHandshake;
import org.snf4j.tls.record.RecordType;

abstract public class AbstractConsumer implements IHandshakeConsumer {

	static void updateTranscriptHash(EngineState state, HandshakeType type, ByteBuffer[] message) {
		state.getTranscriptHash().update(type, message);
	}

	static void updateHRRTranscriptHash(EngineState state, ByteBuffer[] message) {
		state.getTranscriptHash().updateHelloRetryRequest(message);
	}
	
	static byte[] produce0(EngineState state, IHandshake handshake, RecordType recordType) {
		byte[] message = new byte[handshake.getLength()];
		
		ByteBuffer buffer = ByteBuffer.wrap(message);
		handshake.getBytes(buffer);
		state.produce(new ProducedHandshake(new UnknownHandshake(handshake.getType(), message), recordType));
		return message;
	}
	
	static void produce(EngineState state, IHandshake handshake, RecordType recordType) {
		state.getTranscriptHash().update(handshake.getType(), produce0(state, handshake, recordType));
	}
	
	static void produceHRR(EngineState state, IHandshake handshake, RecordType recordType) {
		state.getTranscriptHash().updateHelloRetryRequest(produce0(state, handshake, recordType));
	}

}
