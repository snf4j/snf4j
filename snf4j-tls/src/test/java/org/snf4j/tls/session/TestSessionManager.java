package org.snf4j.tls.session;

import org.snf4j.tls.cipher.IHashSpec;
import org.snf4j.tls.extension.OfferedPsk;

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
public class TestSessionManager extends SessionManager {
	
	public Integer selectedIdentity;

	public int selectedIdentityCounter;
	
	public SessionTicket sessionTicket;

	public int sessionTicketCounter;
	
	public TestSessionManager(int lifetime, int limit) {
		super(lifetime, limit);
	}
	
	public TestSessionManager() {
		this(86400, 20480);
	}
	
	@Override
	public UsedSession useSession(OfferedPsk[] psks, IHashSpec hashSpec, long currentTime) {
		UsedSession used = super.useSession(psks, hashSpec, currentTime);
		if (used != null) {
			boolean change = false;
			int identity = used.getSelectedIdentity();
			SessionTicket ticket = used.getTicket();
			ISession session = used.getSession();

			if (selectedIdentity != null) {
				change = true;
			}
			if (sessionTicket != null) {
				change = true;
			}
			if (change) {
				return new TestUsedSession(
						session, 
						ticket, 
						identity, 
						selectedIdentity == null ? identity : selectedIdentity, 
						selectedIdentityCounter, 
						sessionTicket, 
						sessionTicketCounter);
			}
		}
		return used;
	}
	
	static class TestUsedSession extends UsedSession {

		public int otherIdentity;

		public int otherIdentityCounter;

		public SessionTicket otherTicket;
		
		public int otherTicketCounter;
		
		public TestUsedSession(ISession session, SessionTicket ticket, int selectedIdentity, int otherIdentity, int otherIdentityCounter, SessionTicket otherTicket, int otherTicketCounter) {
			super(session, ticket, selectedIdentity);
			this.otherIdentity = otherIdentity;
			this.otherIdentityCounter = otherIdentityCounter;
			this.otherTicket = otherTicket;
			this.otherTicketCounter = otherTicketCounter;
		}
		
		@Override
		public int getSelectedIdentity() {
			if (--otherIdentityCounter == 0) {
				return otherIdentity;
			}
			return super.getSelectedIdentity();
		}
		
		@Override
		public SessionTicket getTicket() {
			if (--otherTicketCounter == 0) {
				return otherTicket;
			}
			return super.getTicket();
		}
	}
}
