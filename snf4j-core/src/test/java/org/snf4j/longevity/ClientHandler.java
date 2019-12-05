/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.longevity;

import org.snf4j.core.handler.SessionEvent;

public class ClientHandler extends AbstractHandler {

	final static String NO_CONNECTION_KEY = "NoConnectionKey";
	
	@Override
	public void read(byte[] data) {
		read(new Packet(data));
	}

	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case CREATED:
			Utils.sessionCreated(getSession());
			getSession().getAttributes().put(SessionContext.ATTR_KEY, new SessionContext());
			break;
			
		case READY:
			write(Utils.randomPacket());
			break;
			
		case ENDING:
			Utils.sessionEnding(getSession());
			break;
		
		default:
		}
	}
	
	@Override
	public void exception(Throwable t) {
		if (!getSession().isOpen() && getSession().getAttributes().containsKey(NO_CONNECTION_KEY)) {
			Statistics.incNotConnected();
			return;
		}
		super.exception(t);
	}
}
