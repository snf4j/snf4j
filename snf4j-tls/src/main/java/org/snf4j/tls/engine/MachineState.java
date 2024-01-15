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

public enum MachineState {
	
	/** Client handshake engine is created but has not been started yet */
	CLI_INIT(true, false, false),

	/** Client handshake engine is waiting for the first ServerHello message */
	CLI_WAIT_1_SH(true),
	
	/** Client handshake engine is waiting for execution of task(s) */
	CLI_WAIT_TASK(true), 

	/** Client handshake engine is waiting for the second ServerHello message */
	CLI_WAIT_2_SH(true),
	
	CLI_WAIT_EE(true), 
	CLI_WAIT_CERT_CR(true), 
	CLI_WAIT_CERT(true), 
	CLI_WAIT_CV(true), 
	CLI_WAIT_FINISHED(true),
	CLI_CONNECTED(true, true),
	
	/** Server handshake engine is created but has not been started yet */
	SRV_INIT(false, false, false),

	/** Server handshake engine is waiting for the first ClientHello message */
	SRV_WAIT_1_CH(false),

	/** Server handshake engine is waiting for the second ClientHello message */
	SRV_WAIT_2_CH(false),
	
	/** Server handshake engine is waiting for execution of task(s) */
	SRV_WAIT_TASK(false),

	SRV_WAIT_EOED(false),
	SRV_WAIT_CERT(false),
	SRV_WAIT_CV(false),
	SRV_WAIT_FINISHED(false),
	SRV_CONNECTED(false, true);
	
	private final boolean clientMode;
	
	private final boolean connected;
	
	private final boolean started;

	private final int bitMask;
	
	MachineState(boolean clientMode, boolean connected, boolean started) {
		this.clientMode = clientMode;
		this.connected = connected;
		this.started = started;
		bitMask = 1 << ordinal();
	}
	
	MachineState(boolean clientMode, boolean connected) {
		this.clientMode = clientMode;
		this.connected = connected;
		started = true;
		bitMask = 1 << ordinal();
	}

	MachineState(boolean clientMode) {
		this(clientMode, false);
	}
	
	public boolean clientMode() {
		return clientMode;
	}

	public boolean isConnected() {
		return connected;
	}
	
	public boolean isStarted() {
		return started;
	}
	
	public int bitMask() {
		return bitMask;
	}
}
