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

import java.util.LinkedList;
import java.util.Queue;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.handshake.KeyUpdateRequest;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.TestSessionManager;

public class TestHandshakeHandler implements IEngineHandler, IEngineStateListener {

	private StringBuilder trace = new StringBuilder();
	
	public boolean verifyServerName = true;
	
	public volatile TestCertificateSelector certificateSelector = new TestCertificateSelector();
	
	public RuntimeException certificateSelectorException;
	
	public volatile TestCertificateValidator certificateValidator = new TestCertificateValidator();
	
	public volatile TestSessionManager sessionManager = new TestSessionManager();
	
	public Alert onETSException;

	public Alert onHTSException;
	
	public Alert onATSException;
	
	public final Queue<byte[]> earlyData = new LinkedList<byte[]>();
	
	public int padding;
	
	public RuntimeException paddingException;
	
	public long keyLimit = -1;
	
	public long maxEarlyDataSize = 1000;
	
	public TicketInfo[] ticketInfos = new TicketInfo[] {new TicketInfo()};
	
	public void trace(String msg) {
		synchronized (trace) {
			trace.append(msg).append('|');
		}
	}
	
	public String trace() {
		String s;
		
		synchronized (trace) {
			s = trace.toString();
			trace.setLength(0);
		}
		return s;
	}
	
	@Override
	public ICertificateSelector getCertificateSelector() {
		trace("CS");
		if (certificateSelectorException != null) {
			throw certificateSelectorException;
		}
		return certificateSelector;
	}

	@Override
	public ICertificateValidator getCertificateValidator() {
		trace("CV");
		return certificateValidator;
	}
	
	@Override
	public ISessionManager getSessionManager() {
		return sessionManager;
	}

	@Override
	public boolean verify(IServerNameExtension serverName) {
		trace("VSN(" + serverName.getHostName() +")");
		return verifyServerName;
	}
	
	@Override
	public long getMaxEarlyDataSize() {
		return maxEarlyDataSize;
	}
	
	@Override
	public TicketInfo[] createNewTickets() {
		return ticketInfos;
	}

	@Override
	public boolean hasEarlyData() {
		return !earlyData.isEmpty();
	}

	@Override
	public byte[] nextEarlyData() {
		return earlyData.poll();
	}
	
	@Override
	public void onNewTrafficSecrets(IEngineState state, RecordType recordType) throws Alert {
		switch (recordType) {
		case ZERO_RTT:
			trace("ETS");
			if (onETSException != null) {
				throw onETSException;
			}
			break;
			
		case HANDSHAKE:
			trace("HTS");
			if (onHTSException != null) {
				throw onHTSException;
			}
			break;
			
		case APPLICATION:
			trace("ATS");
			if (onATSException != null) {
				throw onATSException;
			}
			break;
			
		default:
			trace("NTS");
		}
	}

	@Override
	public 	void onNewReceivingTraficKey(IEngineState state, RecordType recordType) throws Alert {
		trace("RTS(" + recordType.name() + ")");
	}
	
	@Override
	public void onNewSendingTraficKey(IEngineState state, RecordType recordType) throws Alert {
		trace("STS(" + recordType.name() + ")");
	}
		
	@Override
	public int calculatePadding(ContentType type, int contentLength) {
		if (paddingException != null) {
			throw paddingException;
		}
		return padding;
	}
	
	@Override
	public long getKeyLimit(CipherSuite cipher, long defaultValue) {
		return keyLimit == -1 ? defaultValue : keyLimit;
	}
	
	@Override
	public void produceChangeCipherSpec(IEngineProducer producer) {
		trace("prodCCS");
	}

	@Override
	public void prepareChangeCipherSpec(IEngineProducer producer) {
		trace("prepCCS");
	}

	@Override
	public void onKeyUpdate(IEngineState state, KeyUpdateRequest request) {
		trace("KU(" + request.name() +")");
	}
	
}
