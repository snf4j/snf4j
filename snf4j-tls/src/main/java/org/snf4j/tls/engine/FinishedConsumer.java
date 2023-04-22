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
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.Certificate;
import org.snf4j.tls.handshake.CertificateVerify;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IFinished;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.NewSessionTicket;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISession;

public class FinishedConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.FINISHED;
	}

	private void consumeServer(EngineState state, IFinished finished, ByteBuffer[] data) throws Alert {
		state.getListener().onReceivingTraficKey(RecordType.APPLICATION);
		
		byte[] verifyData;
		
		try {
			ConsumerUtil.updateTranscriptHash(state, finished.getType(), data);
			state.getKeySchedule().deriveResumptionMasterSecret();
			verifyData = state.getKeySchedule().computeClientVerifyData();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify server verify data");
		}
		
		ISession session = state.getSession();
		
		if (session == null) {
			session = state.getHandler().getSessionManager().newSession(state.getSessionInfo()
					.cipher(state.getCipherSuite()));
			state.setSession(session);
		}
		
		if (session.isValid() && state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE)) {
			try {
				NewSessionTicket ticket = state.getHandler().getSessionManager().newTicket(state);
				
				state.produce(new ProducedHandshake(ticket, RecordType.APPLICATION, RecordType.APPLICATION));
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to create new session ticket", e);
			}
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
		
		ConsumerUtil.updateTranscriptHash(state, finished.getType(), data);
		
		DelegatedTaskMode taskMode = state.getParameters().getDelegatedTaskMode();
		CertificateCriteria criteria = state.getCertCryteria();
		CertificateTask task;
		if (criteria != null) {
			task = new CertificateTask(
					state.getHandler().getCertificateSelector(),
					criteria);
		}
		else {
			task = new CertificateTask();
			taskMode = DelegatedTaskMode.NONE;
		}
		
		if (taskMode.certificates()) {
			state.changeState(MachineState.CLI_WAIT_TASK);
			state.addTask(task);
		}
		else {
			task.run(state);
		}
	}
	
	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR)	throws Alert {
		switch (state.getState()) {
		case CLI_WAIT_FINISHED:
			consumeClient(state, (IFinished) handshake, data);
			break;
			
		case SRV_WAIT_FINISHED:
			consumeServer(state, (IFinished) handshake, data);
			break;
			
		default:
			throw new UnexpectedMessageAlert("Unexpected Finished");
		}
	}
	
	static class CertificateTask extends AbstractCertificateTask {

		CertificateTask(ICertificateSelector selector, CertificateCriteria criteria) {
			super(selector, criteria);
		}

		CertificateTask() {
			super();
		}
		
		@Override
		public void finish(EngineState state) throws Alert {
			IEngineParameters params = state.getParameters();
			
			//TODO: skip if early data is offered
			if (params.isCompatibilityMode() && !state.hadState(MachineState.CLI_WAIT_2_SH)) {
				state.getListener().prepareChangeCipherSpec(state);
			}
			
			if (certificates != null) {
				Certificate certificate = new Certificate(new byte[0], certificates.getEntries());
				ConsumerUtil.prepare(state, certificate, RecordType.HANDSHAKE);	

				if (certificate.getEntries().length > 0) {
					state.getSessionInfo().localCerts(certificates.getCertificates());
					byte[] signature = ConsumerUtil.sign(state.getTranscriptHash().getHash(HandshakeType.CERTIFICATE, true), 
							certificates.getAlgorithm(), 
							certificates.getPrivateKey(), 
							true,
							params.getSecureRandom());
					CertificateVerify certificateVerify = new CertificateVerify(certificates.getAlgorithm(), signature);
					ConsumerUtil.prepare(state, certificateVerify, RecordType.HANDSHAKE);
				}
			}
			
			try {
				state.getKeySchedule().deriveMasterSecret();
				state.getKeySchedule().deriveApplicationTrafficSecrets();
				state.getListener().onApplicationTrafficSecrets(state);
				state.getListener().onReceivingTraficKey(RecordType.APPLICATION);
				Finished finished = new Finished(state.getKeySchedule().computeClientVerifyData());
				ConsumerUtil.prepare(state, finished, RecordType.HANDSHAKE, RecordType.APPLICATION);
				state.getKeySchedule().deriveResumptionMasterSecret();
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to compute server verify data", e);
			}
			
			ISession session = state.getSession();
			
			if (session == null) {
				session = state.getHandler().getSessionManager().newSession(state.getSessionInfo()
						.peerHost(params.getPeerHost())
						.peerPort(params.getPeerPort())
						.cipher(state.getCipherSuite()));
				state.setSession(session);
			}
			
			state.changeState(MachineState.CLI_CONNECTED);
		}
	}
}
