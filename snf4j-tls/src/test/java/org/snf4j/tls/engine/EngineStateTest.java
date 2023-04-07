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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.PublicKey;
import java.util.ArrayList;
import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.ServerHello;
import org.snf4j.tls.record.RecordType;

public class EngineStateTest {

	static ServerHello serverHello(int version) {
		return new ServerHello(
				version, 
				new byte[32], 
				new byte[0], 
				CipherSuite.TLS_AES_128_GCM_SHA256, 
				(byte)0, 
				new ArrayList<IExtension>());
	}
	
	static ProducedHandshake handshake(int id) {
		return new ProducedHandshake(serverHello(id), RecordType.INITIAL);
	}
	
	static int id(ProducedHandshake handshake) {
		return ((ServerHello)handshake.getHandshake()).getLegacyVersion();
	}
	
	@Test
	public void testSetPskModes() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE});
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));

		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_DHE_KE});
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));
	
		state.setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE, PskKeyExchangeMode.PSK_DHE_KE});
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertTrue(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));

		state.setPskModes(new PskKeyExchangeMode[] {});
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_KE));
		assertFalse(state.hasPskMode(PskKeyExchangeMode.PSK_DHE_KE));
	}
	
	@Test
	public void testChangeState() throws Exception {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertTrue(state.hadState(MachineState.CLI_INIT));
		assertFalse(state.hadState(MachineState.CLI_WAIT_1_SH));
		state.changeState(MachineState.CLI_WAIT_1_SH);
		assertTrue(state.hadState(MachineState.CLI_WAIT_1_SH));
		assertFalse(state.hadState(MachineState.CLI_WAIT_2_SH));
		state.changeState(MachineState.CLI_WAIT_2_SH);
		assertTrue(state.hadState(MachineState.CLI_WAIT_2_SH));
		assertTrue(state.hadState(MachineState.CLI_INIT));
		assertTrue(state.hadState(MachineState.CLI_WAIT_1_SH));
		assertFalse(state.hadState(MachineState.CLI_WAIT_EE));
		try {
			state.changeState(MachineState.SRV_INIT);
			fail();
		} catch (InternalErrorAlert e) {}
	}
	
	@Test
	public void testGetMaxFragmentLength() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		assertEquals(16384, state.getMaxFragmentLength());
	}
	
	@Test
	public void testStorePublicKey() {
		EngineState state = new EngineState(
				MachineState.CLI_INIT, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());

		PublicKey key = new PublicKey() {

			private static final long serialVersionUID = 1L;

			@Override
			public String getAlgorithm() {
				return null;
			}

			@Override
			public String getFormat() {
				return null;
			}

			@Override
			public byte[] getEncoded() {
				return null;
			}
		};
		
		assertNull(state.getPublicKey());
		state.storePublicKey(key);
		assertSame(key, state.getPublicKey());
		state.clearPublicKeys();
		assertNull(state.getPublicKey());
	}
	
	@Test
	public void testGetProduced() throws Exception {
		EngineState state = new EngineState(
				MachineState.SRV_WAIT_1_CH, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertEquals(0, state.getProduced().length);
		assertFalse(state.hasProduced());
		state.produce(handshake(1));
		assertTrue(state.hasProduced());
		ProducedHandshake[] produced = state.getProduced();
		assertFalse(state.hasProduced());
		assertEquals(1, produced.length);
		assertEquals(1, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		
		state.produce(handshake(2));
		state.produce(handshake(3));
		produced = state.getProduced();
		assertEquals(2, produced.length);
		assertEquals(2, id(produced[0]));
		assertEquals(3, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		
		state.prepare(handshake(4));
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(4, id(produced[0]));
		assertEquals(0, state.getProduced().length);

		state.prepare(handshake(5));
		state.prepare(handshake(6));
		produced = state.getProduced();
		assertEquals(2, produced.length);
		assertEquals(5, id(produced[0]));
		assertEquals(6, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		
		state.produce(handshake(7));
		state.prepare(handshake(8));
		assertFalse(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		state.addTask(new Task(9));
		assertTrue(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(7, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		Runnable task = state.getTask();
		assertFalse(state.hasTasks());
		assertTrue(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		assertEquals(0, state.getProduced().length);
		task.run();
		assertFalse(state.hasTasks());
		assertTrue(state.hasRunningTasks());
		assertTrue(state.hasProducingTasks());
		produced = state.getProduced();
		assertFalse(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		assertEquals(2, produced.length);
		assertEquals(8, id(produced[0]));
		assertEquals(9, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		assertNull(state.getTask());
		
		state.produce(handshake(10));
		state.prepare(handshake(11));
		state.addTask(new Task(false,12));
		assertTrue(state.hasTasks());
		assertFalse(state.hasRunningTasks());
		assertFalse(state.hasProducingTasks());
		state.addTask(new Task(13));
		task = state.getTask();
		Runnable task2 = state.getTask();
		assertNull(state.getTask());
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(10, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		task2.run();
		assertEquals(0, state.getProduced().length);
		task.run();
		produced = state.getProduced();
		assertEquals(3, produced.length);
		assertEquals(11, id(produced[0]));
		assertEquals(12, id(produced[1]));
		assertEquals(13, id(produced[2]));
		assertEquals(0, state.getProduced().length);

		Exception e = new Exception();
		state.addTask(new Task(e, 14));
		assertEquals(0, state.getProduced().length);
		task = state.getTask();
		assertEquals(0, state.getProduced().length);
		task.run();
		try {
			state.getProduced();
			fail();
		} catch (InternalErrorAlert e2) {
			assertSame(e, e2.getCause());
		}
		try {
			task.run();
			fail();
		} catch (IllegalStateException e2) {}
	}
	
	static class Task extends AbstractEngineTask {

		int[] ids;
		
		Exception e;
		
		boolean producing = true;
		
		Task(int... ids) {
			this.ids = ids;
		}

		Task(boolean producing, int... ids) {
			this.producing = producing;
			this.ids = ids;
		}
		
		Task(Exception e, int... ids) {
			this.e = e;
			this.ids = ids;
		}
		
		@Override
		public boolean isProducing() {
			return producing;
		}

		@Override
		public void finish(EngineState state) throws Alert {
			for (int id: ids) {
				state.prepare(handshake(id));
			}
		}

		@Override
		void execute() throws Exception {
			if (e != null) {
				throw e;
			}
		}

		@Override
		public String name() {
			return "Test";
		}
	}
}
