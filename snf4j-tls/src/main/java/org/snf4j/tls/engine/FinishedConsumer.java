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
import java.util.Arrays;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IFinished;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class FinishedConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.FINISHED;
	}

	private void consumeServer(EngineState state, IFinished finished, ByteBuffer[] data) throws Alert {
		state.getListener().onReceivingTraficKey(RecordType.APPLICATION);
		
		byte[] verifyData;
		
		try {
			verifyData = state.getKeySchedule().computeClientVerifyData();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify server verify data");
		}
		state.changeState(MachineState.SRV_CONNECTED);
	}

	private void consumeClient(EngineState state, IFinished finished, ByteBuffer[] data) throws Alert {
		state.getListener().onSendingTraficKey(RecordType.HANDSHAKE);
		
		byte[] verifyData;
		
		try {
			verifyData = state.getKeySchedule().computeServerVerifyData();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify server verify data");
		}
				
		//TODO: skip if early data is offered
		if (state.getParameters().isCompatibilityMode() && !state.hadState(MachineState.CLI_WAIT_2_SH)) {
			state.getListener().produceChangeCipherSpec(state);
		}
		
		try {
			ConsumerUtil.updateTranscriptHash(state, finished.getType(), data);
			state.getKeySchedule().deriveMasterSecret();
			state.getKeySchedule().deriveApplicationTrafficSecrets();
			state.getListener().onApplicationTrafficSecrets(state);
			state.getListener().onReceivingTraficKey(RecordType.APPLICATION);
			finished = new Finished(state.getKeySchedule().computeClientVerifyData());
			ConsumerUtil.produce(state, finished, RecordType.HANDSHAKE, RecordType.APPLICATION);
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		state.changeState(MachineState.CLI_CONNECTED);
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR)	throws Alert {
		if (state.getState() == MachineState.CLI_WAIT_FINISHED) {
			consumeClient(state, (IFinished) handshake, data);
		}
		else if (state.getState() == MachineState.SRV_WAIT_FINISHED) {
			consumeServer(state, (IFinished) handshake, data);
		}
		else {
			throw new UnexpectedMessageAlert("Unexpected Finished");
		}
	}
}
