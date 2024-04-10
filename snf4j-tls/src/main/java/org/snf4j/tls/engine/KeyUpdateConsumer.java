/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.IKeyUpdate;
import org.snf4j.tls.handshake.KeyUpdate;
import org.snf4j.tls.handshake.KeyUpdateRequest;
import org.snf4j.tls.record.RecordType;

public class KeyUpdateConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.KEY_UPDATE;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		if (!state.getState().isConnected()) {
			throw new UnexpectedMessageAlert("Unexpected KeyUpdate");
		}
		
		IKeyUpdate ku = (IKeyUpdate) handshake;
		
		state.getListener().onKeyUpdate(state, ku.getRequest());
		if (!ku.getRequest().isKnown()) {
			throw new IllegalParameterAlert("Unknown KeyUpdateRequest");
		}
		
		state.getListener().onNewReceivingTraficKey(state, RecordType.NEXT_GEN);
		if (ku.getRequest() == KeyUpdateRequest.UPDATE_REQUESTED) {
			KeyUpdate keyUpdate = new KeyUpdate(false);
			state.getListener().onHandshakeCreate(state, keyUpdate, false);
			state.produce(new ProducedHandshake(keyUpdate, RecordType.APPLICATION, RecordType.NEXT_GEN));
		}
	}

}
