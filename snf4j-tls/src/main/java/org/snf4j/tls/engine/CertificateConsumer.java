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
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.CertificateRequiredAlert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificate;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CERTIFICATE;
	}

	private void consumeClient(EngineState state, ICertificate certificate, ByteBuffer[] data) throws Alert {
		if (certificate.getEntries().length == 0) {
			throw new DecodeErrorAlert("Empty server certificate message");
		}
		
		state.getTranscriptHash().update(certificate.getType(), data);
		state.retainHandshake(certificate);
		state.changeState(MachineState.CLI_WAIT_CV);
	}

	private void consumeServer(EngineState state, ICertificate certificate, ByteBuffer[] data) throws Alert {
		state.getTranscriptHash().update(certificate.getType(), data);

		if (certificate.getEntries().length == 0) {
			if (state.getParameters().getClientAuth() == ClientAuth.REQUIRED) {
				throw new CertificateRequiredAlert("Empty client certificate message");
			}
			state.changeState(MachineState.SRV_WAIT_FINISHED);
			return;
		}
		state.retainHandshake(certificate);
		state.changeState(MachineState.SRV_WAIT_CV);
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		switch (state.getState()) {
		case CLI_WAIT_CERT_CR:
		case CLI_WAIT_CERT:
			consumeClient(state, (ICertificate) handshake, data);
			break;
			
		case SRV_WAIT_CERT:
			consumeServer(state, (ICertificate) handshake, data);
			break;
			
		default:
			throw new UnexpectedMessageAlert("Unexpected Certificate");
		}
	}
}
