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

import org.junit.Test;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.record.RecordType;

public class TLSEngineStateListenerTest extends EngineTest {

	@Test
	public void testOnNewTrafficSecretsWithNextGen() throws Exception {
		new TLSEngineStateListener().onNewTrafficSecrets(null, RecordType.NEXT_GEN);
	}

	@Test(expected = InternalErrorAlert.class)
	public void testOnNewTrafficSecretsException() throws Exception {
		new TLSEngineStateListener().onNewTrafficSecrets(null, RecordType.HANDSHAKE);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testOnNewReceivingTraficKeyException() throws Exception {
		EngineState state = new EngineState(MachineState.SRV_CONNECTED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		new TLSEngineStateListener().onNewReceivingTraficKey(state, RecordType.NEXT_GEN);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testOnNewSendingTraficKeyException() throws Exception {
		EngineState state = new EngineState(MachineState.SRV_CONNECTED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		new TLSEngineStateListener().onNewSendingTraficKey(state, RecordType.NEXT_GEN);
	}
}
