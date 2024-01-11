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
import java.util.Arrays;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.EarlyDataExtension;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.Certificate;
import org.snf4j.tls.handshake.CertificateVerify;
import org.snf4j.tls.handshake.EndOfEarlyData;
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
		byte[] verifyData;
		
		state.getListener().onNewReceivingTraficKey(state, RecordType.APPLICATION);
		try {
			state.getTranscriptHash().update(finished.getType(), data);
			state.getKeySchedule().deriveResumptionMasterSecret();
			state.getKeySchedule().eraseMasterSecret();
			verifyData = state.getKeySchedule().computeClientVerifyData();
			state.getKeySchedule().eraseHandshakeTrafficSecrets();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute client verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify client verify data");
		}
		
		ISession session = state.getSession();
		IEngineHandler handler = state.getHandler();
		
		if (session == null) {
			session = handler.getSessionManager().newSession(state.getSessionInfo()
					.cipher(state.getCipherSuite()));
			state.setSession(session);
		}
		
		if (session.isValid() && state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE)) {
			try {
				TicketInfo[] ticketInfos = handler.createNewTickets();
				
				for (TicketInfo ticketInfo: ticketInfos) {
					long maxSize = ticketInfo.getMaxEarlyDataSize();
					NewSessionTicket ticket = handler.getSessionManager().newTicket(state, maxSize);
					
					if (maxSize != -1) {
						ticket.getExtensions().add(new EarlyDataExtension(maxSize));
					}
					state.produce(new ProducedHandshake(
							ticket, 
							RecordType.APPLICATION));
				}
			} catch (Exception e) {
				throw new InternalErrorAlert("Failed to create new session ticket", e);
			}
		}
		
		state.changeState(MachineState.SRV_CONNECTED);
	}

	private void consumeClient(EngineState state, IFinished finished, ByteBuffer[] data) throws Alert {
		byte[] verifyData;
		
		try {
			verifyData = state.getKeySchedule().computeServerVerifyData();
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute server verify data", e);
		}
		if (!Arrays.equals(finished.getVerifyData(), verifyData)) {
			throw new DecryptErrorAlert("Failed to verify server verify data");
		}
		
		state.getTranscriptHash().update(finished.getType(), data);
		
		IEarlyDataContext ctx = state.getEarlyDataContext();
		if (ctx.getState() == EarlyDataState.PROCESSING) {
			ConsumerUtil.prepare(state, new EndOfEarlyData(), RecordType.ZERO_RTT, RecordType.HANDSHAKE);
		}
		else {
			state.getListener().onNewSendingTraficKey(state, RecordType.HANDSHAKE);
		}
		ctx.complete();
		
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
			
			//CSS before encrypted handshake flight (early data not offered)
			if (params.isCompatibilityMode() 
					&& !state.hadState(MachineState.CLI_WAIT_2_SH) 
					&& state.getEarlyDataContext().getState() == EarlyDataState.NONE) {
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
							state.getHandler().getSecureRandom());
					CertificateVerify certificateVerify = new CertificateVerify(certificates.getAlgorithm(), signature);
					ConsumerUtil.prepare(state, certificateVerify, RecordType.HANDSHAKE);
				}
			}
			
			try {
				state.getKeySchedule().deriveMasterSecret();
				state.getKeySchedule().eraseHandshakeSecret();
				state.getKeySchedule().deriveApplicationTrafficSecrets();
				state.getListener().onNewTrafficSecrets(state, RecordType.APPLICATION);
				state.getListener().onNewReceivingTraficKey(state, RecordType.APPLICATION);
				Finished finished = new Finished(state.getKeySchedule().computeClientVerifyData());
				state.getKeySchedule().eraseHandshakeTrafficSecrets();
				ConsumerUtil.prepare(state, finished, RecordType.HANDSHAKE, RecordType.APPLICATION);
				state.getKeySchedule().deriveResumptionMasterSecret();
				state.getKeySchedule().eraseMasterSecret();
			} catch (Alert e) {
				throw e;
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
