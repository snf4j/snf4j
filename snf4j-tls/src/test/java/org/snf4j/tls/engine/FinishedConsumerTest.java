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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.extension.PskKeyExchangeMode;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IFinished;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class FinishedConsumerTest extends EngineTest {
	
	@Override
	public void before() throws Exception {
		super.before();
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
	}

	@Test
	public void testUnexpectedMessage() throws Exception {
		FinishedConsumer c = new FinishedConsumer();
		List<MachineState> validStates = Arrays.asList(
				MachineState.CLI_WAIT_FINISHED,
				MachineState.SRV_WAIT_FINISHED);
		Finished certReq = new Finished(new byte[10]);
		
		for (MachineState s: MachineState.values()) {
			if (validStates.contains(s)) {
				continue;
			}
			try {
				c.consume(new EngineState(s, null, null, null), certReq, new ByteBuffer[0], false);
				fail();
			}
			catch (UnexpectedMessageAlert e) {
				assertEquals("Unexpected Finished", e.getMessage());
			}
		}
	}

	@Test
	public void testFailingServerVerifyData() throws Exception {
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(false, HandshakeType.FINISHED);
		IHandshake h = c.get(false);
		assertNotNull(h);
		assertSame(HandshakeType.FINISHED, h.getType());
		((IFinished)h).getVerifyData()[0] ^= 0xaa;
		h.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals("Failed to verify server verify data", e.getMessage());
		}		
	}
	
	@Test
	public void testServerVerifyDataException() throws Exception {
		EngineState state = new EngineState(MachineState.CLI_WAIT_FINISHED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		
		try {
			new FinishedConsumer().consume(state, new Finished(new byte[10]), new ByteBuffer[0], false);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to compute server verify data", e.getMessage());
		}
	}	

	@Test
	public void testFailingClientVerifyData() throws Exception {
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, HandshakeType.FINISHED);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.FINISHED, h.getType());
		((IFinished)h).getVerifyData()[0] ^= 0xaa;
		h.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals("Failed to verify client verify data", e.getMessage());
		}		
	}

	@Test
	public void testFailingServerCreateVerifyData() throws Exception {
		TestHandshakeHandler handler2 = new TestHandshakeHandler() {
			int count = 1;
			public void onNewReceivingTraficKey(IEngineState state, RecordType recordType) throws Alert {
				if (count-- <= 0) {
					throw new InternalErrorAlert("x");
				}
			}
		};
		HandshakeEngine cli = new HandshakeEngine(true, params, handler2, handler2);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		try {
			c.run(false, null);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("x", e.getMessage());
		}		

		handler2 = new TestHandshakeHandler() {
			int count = 1;
			public void onNewReceivingTraficKey(IEngineState state, RecordType recordType) throws Alert {
				if (count-- <= 0) {
					throw new NullPointerException("y");
				}
			}
		};
		cli = new HandshakeEngine(true, params, handler2, handler2);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		
		try {
			c.run(false, null);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to compute server verify data", e.getMessage());
			assertEquals("y", e.getCause().getMessage());
		}		
		
	}
	
	@Test
	public void testClientVerifyDataException() throws Exception {
		EngineState state = new EngineState(MachineState.SRV_WAIT_FINISHED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		
		try {
			new FinishedConsumer().consume(state, new Finished(new byte[10]), new ByteBuffer[0], false);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to compute client verify data", e.getMessage());
		}
	}	
	
	@Test
	public void testInvalidSession() throws Exception {
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.FINISHED);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.FINISHED, h.getType());

		srv.getState().getSession().invalidate();
		c.run(false, HandshakeType.NEW_SESSION_TICKET);
		assertNull(c.get(false));
	}

	@Test
	public void testWithoutPskDheKe() throws Exception {
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.FINISHED);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.FINISHED, h.getType());

		((EngineState)srv.getState()).setPskModes(new PskKeyExchangeMode[] {PskKeyExchangeMode.PSK_KE});
		c.run(false, HandshakeType.NEW_SESSION_TICKET);
		assertNull(c.get(false));
	}

	@Test
	public void testcreateNewTicketsException() throws Exception {
		params.peerHost = "snf4j.org";
		params.peerPort = 99;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, null);
		assertTrue(cli.getState().isConnected());
		assertTrue(srv.getState().isConnected());

		TestHandshakeHandler handler2 = new TestHandshakeHandler() {
			
			@Override
			public TicketInfo[] createNewTickets() {
				throw new NullPointerException();
			}
		};
		cli = new HandshakeEngine(true, params, handler, handler);
		srv = new HandshakeEngine(false, params, handler2, handler);
		
		c = new HandshakeController(cli, srv);
		c.run(true, HandshakeType.FINISHED);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.FINISHED, h.getType());

		try {
			c.run(false, null);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to create new session ticket", e.getMessage());
		}
	}
	
}
