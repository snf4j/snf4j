/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2024 SNF4J contributors
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

import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.SessionInfo;

public class EngineState implements IEngineState, IEngineProducer {
	
	private final static ProducedHandshake[] NONE_PRODUCED = new ProducedHandshake[0];
	
	private final List<ProducedHandshake> produced = new ArrayList<ProducedHandshake>();

	private final List<ProducedHandshake> prepared = new ArrayList<ProducedHandshake>();
	
	private final Queue<IEngineTask> tasks = new LinkedList<IEngineTask>();
	
	private final Queue<IEngineTask> runningTasks = new LinkedList<IEngineTask>();
		
	private final SessionInfo sessionInfo = new SessionInfo();

	private final IEngineParameters parameters;
	
	private final IEngineHandler handler;
	
	private final IEngineStateListener listener;
	
	private MachineState state;
	
	private int stateBits;
	
	private ISession session;
	
	private ITranscriptHash transcriptHash;
	
	private KeySchedule keySchedule;
		
	private IHandshake retained;
		
	private List<KeySharePrivateKey> privateKeys;

	private List<PskContext> psks;
	
	private int pskModes;
	
	private CipherSuite cipherSuite;
	
	private String applicationProtocol;
	
	private NamedGroup namedGroup;
	
	private String hostName;
	
	private int version;
	
	private boolean producingTasks;
	
	private int maxFragmentLength = 16384;
		
	private CertificateCriteria certCryteria;
	
	private IEarlyDataContext earlyDataContext = NoEarlyDataContext.INSTANCE;
	
	public EngineState(MachineState state, IEngineParameters parameters, IEngineHandler handler, IEngineStateListener listener) {
		this.state = state;
		this.stateBits = state.bitMask();
		this.parameters = parameters;
		this.handler = handler;
		this.listener = listener;
	}
	
	@Override
	public IEngineParameters getParameters() {
		return parameters;
	}
	
	@Override
	public IEngineHandler getHandler() {
		return handler;
	}

	public IEngineStateListener getListener() {
		return listener;
	}

	@Override
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
	
	@Override
	public boolean isStarted() {
		return state.isStarted();
	}
	
	@Override
	public boolean isConnected() {
		return state.isConnected();
	}
	
	@Override
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

	public void setTranscriptHash(ITranscriptHash transcriptHash) {
		this.transcriptHash = transcriptHash;
	}
	
	@Override
	public ISession getSession() {
		return session;
	}
	
	public void setSession(ISession session) {
		this.session = session;
	}
	
	public SessionInfo getSessionInfo() {
		return sessionInfo;
	}
	
	@Override
	public KeySchedule getKeySchedule() {
		return keySchedule;
	}

	@Override
	public CipherSuite getCipherSuite() {
		return cipherSuite;
	}
	
	public NamedGroup getNamedGroup() {
		return namedGroup;
	}

	public void setNamedGroup(NamedGroup namedGroup) {
		this.namedGroup = namedGroup;
	}

	@Override
	public String getApplicationProtocol() {
		return applicationProtocol;
	}

	public void setApplicationProtocol(String protocol) {
		applicationProtocol = protocol;
	}
	
	@Override
	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@SuppressWarnings("unchecked")
	public <T extends IHandshake> T getRetainedHandshake() {
		return (T) retained;
	}

	public void retainHandshake(IHandshake handshake) {
		retained = handshake;
	}

	@Override
	public void produce(ProducedHandshake handshake) {
		if (prepared.isEmpty()) {
			produced.add(handshake);
		}
		else {
			prepare(handshake);
		}
	}

	@Override
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
	
	public boolean hasRunningTasks(boolean onlyUndone) {
		boolean has = !runningTasks.isEmpty();
		
		if (has && onlyUndone) {
			for (IEngineTask task: runningTasks) {
				if (!task.isDone()) {
					return true;
				}
			}
			has = false;
		}
		return has;
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
	
	public void addPrivateKey(NamedGroup group, PrivateKey key) {
		if (privateKeys == null) {
			privateKeys = new ArrayList<KeySharePrivateKey>();
		}
		privateKeys.add(new KeySharePrivateKey(group, key));
	}

	public PrivateKey getPrivateKey(NamedGroup group, boolean clearAll) {
		if (privateKeys != null) {
			PrivateKey key = null;
			
			for (KeySharePrivateKey privateKey: privateKeys) {
				if (privateKey.getGroup().equals(group)) {
					key = privateKey.getKey();
					break;
				}
			}
			if (clearAll) {
				clearPrivateKeys();
			}
			return key;
		}
		return null;
	}

	public void clearPrivateKeys() {
		if (privateKeys != null) {
			privateKeys.clear();
			privateKeys = null;
		}
	}
	
	@Override
	public int getMaxFragmentLength() {
		return maxFragmentLength;
	}
	
	@Override
	public IEarlyDataContext getEarlyDataContext() {
		return earlyDataContext;
	}
	
	public void setEarlyDataContext(IEarlyDataContext context) {
		earlyDataContext = context == null ? NoEarlyDataContext.INSTANCE : context;
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
	
	public CertificateCriteria getCertCryteria() {
		return certCryteria;
	}

	public void setCertCryteria(CertificateCriteria certCryteria) {
		this.certCryteria = certCryteria;
	}

	public void cleanup() {
		clearPrivateKeys();
		clearPskContexts();
		if (keySchedule != null) {
			keySchedule.eraseAll();
		}
		listener.onCleanup(this);
		retainHandshake(null);
	}
}
