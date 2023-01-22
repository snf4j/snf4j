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

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR)	throws Alert {
		if (state.getState() != MachineState.CLI_WAIT_FINISHED) {
			throw new UnexpectedMessageAlert("Unexpected Finished");
		}
		state.getListener().onSendingTraficKey(RecordType.HANDSHAKE);
		
		IFinished finished = (IFinished) handshake;
		byte[] verifyData;
		
		try {
			verifyData = state.getKeySchedule().computeServerVerifyData();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify server verify data");
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
}
