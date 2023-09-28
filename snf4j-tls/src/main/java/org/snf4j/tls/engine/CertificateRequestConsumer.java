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

import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.MissingExtensionAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.ISignatureAlgorithmsExtension;
import org.snf4j.tls.handshake.CertificateType;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateRequestConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CERTIFICATE_REQUEST;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		switch (state.getState()) {
		case CLI_WAIT_CERT_CR:
			break;
			
		default:
			throw new UnexpectedMessageAlert("Unexpected CertificateRequest");
		}
		
		ISignatureAlgorithmsExtension signAlgorithms = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS);
		if (signAlgorithms == null) {
			throw new MissingExtensionAlert("Missing signature_algorithms extension in CertificateRequest");
		}
		ISignatureAlgorithmsExtension signAlgorithmsCert = find(handshake, ExtensionType.SIGNATURE_ALGORITHMS_CERT);

		
		state.getTranscriptHash().update(handshake.getType(), data);
		
		state.setCertCryteria(new CertificateCriteria(
				CertificateType.X509,
				state.getSessionInfo().peerHost(),
				signAlgorithms.getSchemes(),
				signAlgorithmsCert == null ? null : signAlgorithmsCert.getSchemes()
				));
		
		state.changeState(MachineState.CLI_WAIT_CERT);
	}

}
