/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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
package org.snf4j.example.chat;

import java.util.HashMap;
import java.util.Map;

import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.IStreamSession;

public class ChatServerHandler extends AbstractStreamHandler {

	private static Integer USERID = 0;
	
	private static String YOUID = "[you]";
	
	static final Map<Long, IStreamSession> sessions = new HashMap<Long, IStreamSession>();
	
	@Override
	public void read(Object msg) {
		String s = new String((byte[])msg);
		
		send(s);
		if ("bye".equalsIgnoreCase(s)) {
			getSession().close();
		}
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			sessions.put(getSession().getId(), getSession());
			getSession().getAttributes().put(USERID, "["+getSession().getRemoteAddress()+"]");
			send("{connected}");
			break;
			
		case CLOSED:
			sessions.remove(getSession().getId());
			send("{disconnected}");
			break;
		}
	}
	
	private void send(String message) {
		long youId = getSession().getId();
		String userId = (String) getSession().getAttributes().get(USERID);
		
		for (IStreamSession session: sessions.values()) {
			session.write(((session.getId() == youId ? YOUID : userId) + ' ' + message).getBytes());
		}
	}
	
}
