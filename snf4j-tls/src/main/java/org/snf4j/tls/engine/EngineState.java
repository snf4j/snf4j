/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.handshake.ClientHello;

public class EngineState {
	
	private final static ProducedHandshake[] NONE_PRODUCED = new ProducedHandshake[0];
	
	private final IEngineParameters parameters;
	
	private final IEngineHandler handler;
	
	private MachineState state;
	
	private ITranscriptHash transcriptHash;
	
	private KeySchedule keySchedule;
	
	private List<ProducedHandshake> produced = new ArrayList<ProducedHandshake>();

	private List<ProducedHandshake> prepared = new ArrayList<ProducedHandshake>();
	
	private Queue<IEngineTask> pendingTasks = new LinkedList<IEngineTask>();
	
	private Queue<IEngineTask> runningTasks = new LinkedList<IEngineTask>();
	
	private List<KeySharePrivateKey> privateKeys = new ArrayList<KeySharePrivateKey>();
	
	private ClientHello clientHello;
	
	private CipherSuite cipherSuite;
	
	private NamedGroup namedGroup;
	
	private String hostName;
	
	private int version;
	
	public EngineState(MachineState state, IEngineParameters parameters, IEngineHandler handler) {
		this.state = state;
		this.parameters = parameters;
		this.handler = handler;
	}
	
	public IEngineParameters getParameters() {
		return parameters;
	}
	
	public IEngineHandler getHandler() {
		return handler;
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
	
	public void initialize(KeySchedule keySchedule, ITranscriptHash transcriptHash, CipherSuite cipherSuite) {
		this.keySchedule = keySchedule;
		this.transcriptHash = transcriptHash;
		this.cipherSuite = cipherSuite;
	}
	
	public ITranscriptHash getTranscriptHash() {
		return transcriptHash;
	}

	public KeySchedule getKeySchedule() {
		return keySchedule;
	}

	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}
	
	public NamedGroup getNamedGroup() {
		return namedGroup;
	}

	public void setNamedGroup(NamedGroup namedGroup) {
		this.namedGroup = namedGroup;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
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

	public void prepare(ProducedHandshake handshake) {
		prepared.add(handshake);
	}
	
	public ProducedHandshake[] getProduced() throws AlertException {
		if (pendingTasks.isEmpty()) {
			IEngineTask task;
			
			while ((task = runningTasks.peek()) != null) {
				if (task.isDone()) {
					runningTasks.poll();
					if (task.isSuccessful()) {
						task.prepare(this);
					}
					else {
						throw new InternalErrorAlertException(task.name() + " task failed", task.cause());
					}
				}
				else {
					break;
				}
			}

			if (task == null && !prepared.isEmpty()) {
				produced.addAll(prepared);
				prepared.clear();
			}
		}
		
		int size = produced.size();
		
		if (size > 0) {
			ProducedHandshake[] msgs = produced.toArray(new ProducedHandshake[size]);
			
			produced.clear();
			return msgs;
		}
		return NONE_PRODUCED;
	}
	
	public Runnable getDelegatedTask() {
		if (pendingTasks.isEmpty()) {
			return null;
		}
		
		IEngineTask task = pendingTasks.poll();
		
		runningTasks.add(task);
		return task;
	}
	
	public void addDelegatedTask(IEngineTask task) {
		pendingTasks.add(task);
	}
	
	public void storePrivateKey(NamedGroup group, PrivateKey key) {
		privateKeys.add(new KeySharePrivateKey(group, key));
	}
}
