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
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.OfferedPsk;
import org.snf4j.tls.handshake.NewSessionTicket;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.ISessionManager;
import org.snf4j.tls.session.SessionInfo;
import org.snf4j.tls.session.SessionTicket;
import org.snf4j.tls.session.TestSession;
import org.snf4j.tls.session.UsedSession;

public class NewSessionTicketConsumerTest {
	
	@Test
	public void testUnexpectedMessage() throws Exception {
		NewSessionTicketConsumer c = new NewSessionTicketConsumer();
		List<MachineState> validStates = Arrays.asList(
				MachineState.CLI_CONNECTED);
		NewSessionTicket nst = new NewSessionTicket(new byte[10], new byte[3], 1, 1, Collections.emptyList());
		
		for (MachineState s: MachineState.values()) {
			if (validStates.contains(s)) {
				continue;
			}
			try {
				c.consume(new EngineState(s, null, null, null), nst, new ByteBuffer[0], false);
				fail();
			}
			catch (UnexpectedMessageAlert e) {
				assertEquals("Unexpected NewSessionTicket", e.getMessage());
			}
		}
	}

	@Test
	public void testInvalidSession() throws Exception {
		NewSessionTicketConsumer c = new NewSessionTicketConsumer();
		TestSession session = new TestSession(100, 10);
		EngineState state = new EngineState(MachineState.CLI_CONNECTED, null, null, null);
		NewSessionTicket nst = new NewSessionTicket(new byte[10], new byte[3], 1, 1, Collections.emptyList());
		Manager mgr = new Manager();
		
		session.valid = false;
		session.manager = mgr;
		state.setSession(session);
		c.consume(state, nst, new ByteBuffer[0], false);
		assertEquals(0, mgr.tickets);
	}

	@Test
	public void testPSKException() throws Exception {
		NewSessionTicketConsumer c = new NewSessionTicketConsumer();
		TestSession session = new TestSession(100, 10);
		EngineState state = new EngineState(MachineState.CLI_CONNECTED, null, null, null);
		NewSessionTicket nst = new NewSessionTicket(new byte[10], new byte[3], 1, 1, Collections.emptyList());
		Manager mgr = new Manager();

		mgr.putTicketException = new NullPointerException();
		session.manager = mgr;
		state.setSession(session);
		try {
			c.consume(state, nst, new ByteBuffer[0], false);
			fail();
		}
		catch (InternalErrorAlert e) {
			assertEquals("Failed to compute PSK", e.getMessage());
		}
	}
	
	class Manager implements ISessionManager {

		int tickets;
		
		RuntimeException putTicketException;
		
		@Override
		public ISession getSession(long sessionId) {
			return null;
		}

		@Override
		public ISession getSession(String host, int port) {
			return null;
		}

		@Override
		public ISession newSession(SessionInfo info) {
			return null;
		}

		@Override
		public void removeSession(long sessionId) {
		}

		@Override
		public UsedSession useSession(OfferedPsk[] psks, CipherSuite cipher, boolean earlyData, String protocol) {
			return null;
		}

		@Override
		public void putTicket(ISession session, SessionTicket ticket) {
			if (putTicketException != null) {
				throw putTicketException;
			}
			tickets++;
		}

		@Override
		public void removeTicket(ISession session, SessionTicket ticket) {
		}

		@Override
		public SessionTicket[] getTickets(ISession session) {
			return null;
		}

		@Override
		public NewSessionTicket newTicket(IEngineState state, long maxEarlyDataSize) throws Exception {
			return null;
		}
		
	}
}
