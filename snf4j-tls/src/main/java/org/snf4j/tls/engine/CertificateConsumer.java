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

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.UnexpectedMessageAlertException;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificate;
import org.snf4j.tls.handshake.ICertificateEntry;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.CERTIFICATE;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws AlertException {
		if (state.getState() != MachineState.CLI_WAIT_CERT) {
			throw new UnexpectedMessageAlertException("Unexpected Certificate");
		}
		ConsumerUtil.updateTranscriptHash(state, handshake.getType(), data);
		
		AbstractEngineTask task = new CertificateTask(((ICertificate)handshake).getEntries());
		if (state.getParameters().getDelegatedTaskMode().certificates()) {
			state.changeState(MachineState.CLI_WAIT_CERT_TASK);
			state.addTask(task);
		}
		else {
			task.run(state);
		}		
	}

	static class CertificateTask extends AbstractEngineTask {

		private final ICertificateEntry[] entries;
		
		private volatile PublicKey publicKey;
		
		CertificateTask(ICertificateEntry[] entries) {
			this.entries = entries;
		}
		
		@Override
		public String name() {
			return "Certificate";
		}

		@Override
		public boolean isProducing() {
			return false;
		}

		@Override
		public void finish(EngineState state) throws AlertException {
			state.storePublicKey(publicKey);
			state.changeState(MachineState.CLI_WAIT_CV);
		}

		@Override
		void execute() throws Exception {
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			X509Certificate[] certs = new X509Certificate[entries.length];
			
			for (int i=0; i<certs.length; ++i) {
				certs[i] = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(entries[i].getData()));
			}
			publicKey = certs[0].getPublicKey();
		}
	}
}
