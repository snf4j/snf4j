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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.IExtension;
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
	public void testGetProduced() throws Exception {
		EngineState state = new EngineState(
				MachineState.SRV_START, 
				new TestParameters(), 
				new TestHandshakeHandler(),
				new TestHandshakeHandler());
		
		assertEquals(0, state.getProduced().length);
		state.produce(handshake(1));
		ProducedHandshake[] produced = state.getProduced();
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
		state.addTask(new Task(9));
		produced = state.getProduced();
		assertEquals(1, produced.length);
		assertEquals(7, id(produced[0]));
		assertEquals(0, state.getProduced().length);
		Runnable task = state.getTask();
		assertEquals(0, state.getProduced().length);
		task.run();
		produced = state.getProduced();
		assertEquals(2, produced.length);
		assertEquals(8, id(produced[0]));
		assertEquals(9, id(produced[1]));
		assertEquals(0, state.getProduced().length);
		assertNull(state.getTask());
		
		state.produce(handshake(10));
		state.prepare(handshake(11));
		state.addTask(new Task(12));
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
		
		Task(int... ids) {
			this.ids = ids;
		}

		Task(Exception e, int... ids) {
			this.e = e;
			this.ids = ids;
		}
		
		@Override
		public boolean isProducing() {
			return true;
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
