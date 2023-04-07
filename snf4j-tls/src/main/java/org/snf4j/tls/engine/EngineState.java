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
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.SessionInfo;

public class EngineState {
	
	private final static ProducedHandshake[] NONE_PRODUCED = new ProducedHandshake[0];
	
	private final List<ProducedHandshake> produced = new ArrayList<ProducedHandshake>();

	private final List<ProducedHandshake> prepared = new ArrayList<ProducedHandshake>();
	
	private final Queue<IEngineTask> tasks = new LinkedList<IEngineTask>();
	
	private final Queue<IEngineTask> runningTasks = new LinkedList<IEngineTask>();
	
	private List<KeySharePrivateKey> privateKeys = new ArrayList<KeySharePrivateKey>();
	
	private final IEngineParameters parameters;
	
	private final IEngineHandler handler;
	
	private final IEngineStateListener listener;
	
	private MachineState state;
	
	private int stateBits;
	
	private ISession session;
	
	private SessionInfo sessionInfo = new SessionInfo();

	private ITranscriptHash transcriptHash;
	
	private KeySchedule keySchedule;
		
	private IClientHello clientHello;
	
	private List<PskContext> psks;
	
	private int pskModes;
	
	private CipherSuite cipherSuite;
	
	private NamedGroup namedGroup;
	
	private String hostName;
	
	private int version;
	
	private boolean producingTasks;
	
	private PublicKey publicKey;
	
	private int maxFragmentLength = 16384;
		
	public EngineState(MachineState state, IEngineParameters parameters, IEngineHandler handler, IEngineStateListener listener) {
		this.state = state;
		this.stateBits = state.bitMask();
		this.parameters = parameters;
		this.handler = handler;
		this.listener = listener;
	}
	
	public IEngineParameters getParameters() {
		return parameters;
	}
	
	public IEngineHandler getHandler() {
		return handler;
	}

	public IEngineStateListener getListener() {
		return listener;
	}

	public MachineState getState() {
		return state;
	}
	
	public void changeState(MachineState newState) throws Alert {
		if (this.state.clientMode() != newState.clientMode()) {
			throw new InternalErrorAlert("Invalid new machine state");
		}
		this.state = newState;
		this.stateBits |= newState.bitMask();
	}
	
	public boolean hadState(MachineState state) {
		return (stateBits & state.bitMask()) != 0;
	}
	
	public boolean isClientMode() {
		return state.clientMode();
	}
	
	public boolean isInitialized() {
		return keySchedule != null;
	}
	
	public void initialize(KeySchedule keySchedule, CipherSuite cipherSuite) {
		this.keySchedule = keySchedule;
		this.transcriptHash = keySchedule.getTranscriptHash();
		this.cipherSuite = cipherSuite;
	}
	
	public ITranscriptHash getTranscriptHash() {
		return transcriptHash;
	}

	public ISession getSession() {
		return session;
	}
	
	public void setSession(ISession session) {
		this.session = session;
	}
	
	public SessionInfo getSessionInfo() {
		return sessionInfo;
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

	public IClientHello getClientHello() {
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
	
	public boolean hasProduced() {
		return !produced.isEmpty();
	}
	
	public ProducedHandshake[] getProduced() throws Alert {
		updateTasks();
		
		int size = produced.size();
		
		if (size > 0) {
			ProducedHandshake[] msgs = produced.toArray(new ProducedHandshake[size]);
			
			produced.clear();
			return msgs;
		}
		return NONE_PRODUCED;
	}
	
	public boolean updateTasks() throws Alert {
		if (tasks.isEmpty()) {
			if (runningTasks.isEmpty()) {
				if (!prepared.isEmpty()) {
					produced.addAll(prepared);
					prepared.clear();
				}
				return false;
			}
			
			IEngineTask task;
			
			while ((task = runningTasks.peek()) != null) {
				if (task.isDone()) {
					runningTasks.poll();
					if (task.isSuccessful()) {
						task.finish(this);
					}
					else {
						throw new InternalErrorAlert(task.name() + " task failed", task.cause());
					}
				}
				else {
					break;
				}
			}

			if (task == null) {
				producingTasks = false;
				if (!prepared.isEmpty()) {
					produced.addAll(prepared);
					prepared.clear();
				}
				return false;
			}
		}
		return true;
	}

	public boolean hasProducingTasks() {
		return producingTasks;
	}
	
	public boolean hasTasks() {
		return !tasks.isEmpty();
	}
	
	public boolean hasRunningTasks() {
		return !runningTasks.isEmpty();
	}
	
	public Runnable getTask() {
		if (tasks.isEmpty()) {
			return null;
		}
		
		IEngineTask task = tasks.poll();
		
		runningTasks.add(task);
		return task;
	}
	
	public void addTask(IEngineTask task) {
		tasks.add(task);
		if (task.isProducing()) {
			producingTasks = true;
		}
	}
	
	public void storePrivateKey(NamedGroup group, PrivateKey key) {
		privateKeys.add(new KeySharePrivateKey(group, key));
	}

	public PrivateKey getPrivateKey(NamedGroup group) {
		for (KeySharePrivateKey privateKey: privateKeys) {
			if (privateKey.getGroup().equals(group)) {
				return privateKey.getKey();
			}
		}
		return null;
	}

	public void clearPrivateKeys() {
		privateKeys.clear();
	}
	
	public void storePublicKey(PublicKey publicKey) {
		this.publicKey = publicKey;
	}
	
	public PublicKey getPublicKey() {
		return publicKey;
	}

	public void clearPublicKeys() {
		publicKey = null;
	}
	
	public int getMaxFragmentLength() {
		return maxFragmentLength;
	}
	
	public void addPskContext(PskContext psk) {
		if (psks == null) {
			psks = new LinkedList<PskContext>();
		}
		psks.add(psk);
	}
	
	public List<PskContext> getPskContexts() {
		return psks;
	}
		
	public void clearPskContexts() {
		if (psks != null) {
			for (PskContext psk: psks) {
				psk.clear();
			}
			psks = null;
		}
	}
	
	public void setPskModes(PskKeyExchangeMode[] modes) {
		pskModes = 0;
		for (PskKeyExchangeMode mode: modes) {
			pskModes |= 1 << mode.value();
		}
	}
	
	public boolean hasPskMode(PskKeyExchangeMode mode) {
		return (pskModes & (1 << mode.value())) != 0;
	}
	
}
