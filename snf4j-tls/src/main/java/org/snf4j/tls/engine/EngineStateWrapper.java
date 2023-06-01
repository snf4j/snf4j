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

import org.snf4j.tls.Args;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.ITranscriptHash;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.session.ISession;

class EngineStateWrapper implements IEngineState {

	private final IEngineState state;
	
	private final KeySchedule keySchedule;
	
	EngineStateWrapper(IEngineState state, KeySchedule keySchedule) {
		Args.checkNull(state, "state");
		this.state = state;
		this.keySchedule = keySchedule;
	}
	
	@Override
	public IEngineParameters getParameters() {
		return state.getParameters();
	}

	@Override
	public IEngineHandler getHandler() {
		return state.getHandler();
	}

	@Override
	public MachineState getState() {
		return state.getState();
	}

	@Override
	public boolean isClientMode() {
		return state.isClientMode();
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
	public ITranscriptHash getTranscriptHash() {
		return keySchedule != null ? keySchedule.getTranscriptHash() : state.getTranscriptHash();
	}

	@Override
	public ISession getSession() {
		return state.getSession();
	}

	@Override
	public KeySchedule getKeySchedule() {
		return keySchedule != null ? keySchedule : state.getKeySchedule();
	}

	@Override
	public CipherSuite getCipherSuite() {
		return state.getCipherSuite();
	}

	@Override
	public String getHostName() {
		return state.getHostName();
	}

	@Override
	public int getVersion() {
		return state.getVersion();
	}

	@Override
	public int getMaxFragmentLength() {
		return state.getMaxFragmentLength();
	}

	@Override
	public IEarlyDataContext getEarlyDataContext() {
		return state.getEarlyDataContext();
	}

}
