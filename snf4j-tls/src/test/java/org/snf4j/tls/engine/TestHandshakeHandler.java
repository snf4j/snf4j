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

import org.snf4j.tls.extension.IServerNameExtension;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.RecordType;

public class TestHandshakeHandler implements IHandshakeEngineHandler, IEngineStateListener {

	private StringBuilder trace = new StringBuilder();
	
	public boolean verifyServerName = true;
	
	public volatile TestCertificateSelector certificateSelector = new TestCertificateSelector();
	
	public volatile TestCertificateValidator certificateValidator = new TestCertificateValidator();

	public RuntimeException onETSException;

	public RuntimeException onHTSException;
	
	public RuntimeException onATSException;
	
	public int padding;
	
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
		return certificateSelector;
	}

	@Override
	public ICertificateValidator getCertificateValidator() {
		trace("CV");
		return certificateValidator;
	}
	
	@Override
	public boolean verify(IServerNameExtension serverName) {
		trace("VSN(" + serverName.getHostName() +")");
		return verifyServerName;
	}

	@Override
	public void onEarlyTrafficSecret(EngineState state) throws Exception {
		trace("ETS");
		if (onETSException != null) {
			throw onETSException;
		}
	}

	@Override
	public void onHandshakeTrafficSecrets(EngineState state) throws Exception {
		trace("HTS");
		if (onHTSException != null) {
			throw onHTSException;
		}
	}

	@Override
	public void onApplicationTrafficSecrets(EngineState state) throws Exception {
		trace("ATS");
		if (onATSException != null) {
			throw onATSException;
		}
	}

	@Override
	public 	void onReceivingTraficKey(RecordType recordType) {
		trace("RTS(" + recordType.name() + ")");
	}
	
	@Override
	public void onSendingTraficKey(RecordType recordType) {
		trace("STS(" + recordType.name() + ")");
	}
	
	@Override
	public int calculatePadding(ContentType type, int contentLength) {
		return padding;
	}

	@Override
	public void produceChangeCipherSpec(EngineState state) {
		trace("prodCCS");
	}

	@Override
	public void prepareChangeCipherSpec(EngineState state) {
		trace("prepCCS");
	}
	
}
