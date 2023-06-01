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

import static org.snf4j.tls.extension.ExtensionsUtil.find;

import java.nio.ByteBuffer;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IEarlyDataExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.handshake.INewSessionTicket;
import org.snf4j.tls.session.ISession;
import org.snf4j.tls.session.SessionTicket;

public class NewSessionTicketConsumer implements IHandshakeConsumer {

	@Override
	public HandshakeType getType() {
		return HandshakeType.NEW_SESSION_TICKET;
	}

	@Override
	public void consume(EngineState state, IHandshake handshake, ByteBuffer[] data, boolean isHRR) throws Alert {
		if (state.getState() != MachineState.CLI_CONNECTED) {
			throw new UnexpectedMessageAlert("Unexpected NewSessionTicket");
		}
		
		INewSessionTicket nst = (INewSessionTicket) handshake;
		long maxEarlyDataSize;
		
		IEarlyDataExtension earlyData = find(handshake, ExtensionType.EARLY_DATA);
		if (earlyData != null) {
			maxEarlyDataSize = earlyData.getMaxSize();
		}
		else {
			maxEarlyDataSize = -1;
		}
		
		try {
			ISession session = state.getSession();
			
			if (session.isValid()) {
				session.getManager().putTicket(session, 
						new SessionTicket(
								state.getCipherSuite(),
								state.getKeySchedule().computePsk(nst.getNonce()), 
								nst.getTicket(), 
								nst.getLifetime(), 
								nst.getAgeAdd(),
								maxEarlyDataSize));
			}
		} catch (Exception e) {
			throw new InternalErrorAlert("Failed to compute PSK", e);
		}
	}

}
