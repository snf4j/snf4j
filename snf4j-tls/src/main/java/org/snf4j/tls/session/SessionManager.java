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
package org.snf4j.tls.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SessionManager implements ISessionManager {

	private int timeout;
	
	private int limit;
	
	private final Map<Long, List<SessionTicket>> tickets = new HashMap<Long, List<SessionTicket>>();
	
	private final Map<String, Session> sessions = new HashMap<String, Session>();
	
	@Override
	public void storeSession(Session session) {
		if (session.getHost() != null) {
			sessions.put(session.getHost() + ':' + session.getPort(), session);
		}
	}

	@Override
	public Session getSession(long sessionId) {
		return null;
	}
	
	@Override
	public Session getSession() {
		return null;
	}
	
	@Override
	public Session getSession(String host, int port) {
		return sessions.get(host + ':' + port);
	}
	
	@Override
	public Session getSession(byte[] identity) {
		return null;
	}
	
	@Override
	public void storeTicket(Session session, SessionTicket ticket) {
		List<SessionTicket> list = tickets.get(session.getId());
		
		if (list == null) {
			list = new LinkedList<SessionTicket>();
			tickets.put(session.getId(), list);
		}
		list.add(ticket);
	}
	
	@Override
	public List<SessionTicket> findTickets(Session session) {
		List<SessionTicket> list = tickets.get(session.getId());
		
		return list == null ? new ArrayList<SessionTicket>(0) : new ArrayList<SessionTicket>(list);
	}
}
