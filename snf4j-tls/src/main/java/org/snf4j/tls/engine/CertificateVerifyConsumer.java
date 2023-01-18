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
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.DecryptErrorAlertException;
import org.snf4j.tls.alert.UnexpectedMessageAlertException;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificateVerify;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateVerifyConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CERTIFICATE_VERIFY;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR)	throws AlertException {
		if (state.getState() != MachineState.CLI_WAIT_CV) {
			throw new UnexpectedMessageAlertException("Unexpected CertificateVerify");
		}
		
		ICertificateVerify certificateVerify = (ICertificateVerify) handshake;

		boolean verified = ConsumerUtil.verify(certificateVerify.getSignature(), 
				state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, false),
				certificateVerify.getAlgorithm(),
				state.getPublicKey(), 
				false);
		if (!verified) {
			throw new DecryptErrorAlertException("Failed to verify certificate");
		}
		
		ConsumerUtil.updateTranscriptHash(state, handshake.getType(), data);
		state.changeState(MachineState.CLI_WAIT_FINISHED);
	}

}
