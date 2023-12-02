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
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificateVerify;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateVerifyConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CERTIFICATE_VERIFY;
	}

	private void consumeClient(EngineState state, ICertificateVerify certificateVerify, ByteBuffer[] data) throws Alert {
		boolean verified = ConsumerUtil.verify(certificateVerify.getSignature(), 
				state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, false),
				certificateVerify.getAlgorithm(),
				state.getSessionInfo().peerCerts()[0].getPublicKey(), 
				false);
		if (!verified) {
			throw new DecryptErrorAlert("Failed to verify certificate");
		}
		
		state.getTranscriptHash().update(certificateVerify.getType(), data);
		state.changeState(MachineState.CLI_WAIT_FINISHED);
	}

	private void consumeServer(EngineState state, ICertificateVerify certificateVerify, ByteBuffer[] data) throws Alert {
		boolean verified = ConsumerUtil.verify(certificateVerify.getSignature(), 
				state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, true),
				certificateVerify.getAlgorithm(),
				state.getSessionInfo().peerCerts()[0].getPublicKey(), 
				true);
		if (!verified) {
			throw new DecryptErrorAlert("Failed to verify certificate");
		}
		
		state.getTranscriptHash().update(certificateVerify.getType(), data);
		state.changeState(MachineState.SRV_WAIT_FINISHED);
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR)	throws Alert {
		switch (state.getState()) {
		case CLI_WAIT_CV:
			consumeClient(state, (ICertificateVerify) handshake, data);
			break;
			
		case SRV_WAIT_CV:
			consumeServer(state, (ICertificateVerify) handshake, data);
			break;
			
		default:
			throw new UnexpectedMessageAlert("Unexpected CertificateVerify");
		}
	}

}