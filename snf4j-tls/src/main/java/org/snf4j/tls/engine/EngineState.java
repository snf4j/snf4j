/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.handshake.ClientHello;

public class EngineState {
	
	private final IEngineParameters parameters;
	
	private MachineState state;
	
	private ITranscriptHash transcriptHash;
	
	private KeySchedule keySchedule;
	
	private List<ProducedHandshake> produced = new ArrayList<ProducedHandshake>();
	
	private List<KeySharePrivateKey> privateKeys = new ArrayList<KeySharePrivateKey>();
	
	private ClientHello clientHello;
	
	public EngineState(MachineState state, IEngineParameters parameters) {
		this.state = state;
		this.parameters = parameters;
	}
	
	public IEngineParameters getParameters() {
		return parameters;
	}
	
	public MachineState getState() {
		return state;
	}
	
	public void changeState(MachineState machineState) throws AlertException {
		if (this.state.clientMode() != machineState.clientMode()) {
			throw new InternalErrorAlertException("Invalid new machine state");
		}
		this.state = machineState;
	}
	
	public boolean isClientMode() {
		return state.clientMode();
	}
	
	public boolean isInitialized() {
		return keySchedule != null;
	}
	
	public void initialize(KeySchedule keySchedule, ITranscriptHash transcriptHash) {
		this.keySchedule = keySchedule;
		this.transcriptHash = transcriptHash;
	}
	
	public ITranscriptHash getTranscriptHash() {
		return transcriptHash;
	}

	public KeySchedule getKeySchedule() {
		return keySchedule;
	}

	public ClientHello getClientHello() {
		return clientHello;
	}

	public void setClientHello(ClientHello clientHello) {
		this.clientHello = clientHello;
	}

	public void produce(ProducedHandshake handshake) {
		produced.add(handshake);
	}
	
	public ProducedHandshake[] getProduced() {
		int size = produced.size();
		
		if (size > 0) {
			ProducedHandshake[] msgs = produced.toArray(new ProducedHandshake[size]);
			
			produced.clear();
			return msgs;
		}
		return new ProducedHandshake[0];
	}
	
	public void add(NamedGroup group, PrivateKey key) {
		privateKeys.add(new KeySharePrivateKey(group, key));
	}
}
