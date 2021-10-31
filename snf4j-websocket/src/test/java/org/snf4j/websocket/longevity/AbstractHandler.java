/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket.longevity;

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.websocket.AbstractWebSocketHandler;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.frame.BinaryFrame;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.Opcode;
import org.snf4j.websocket.frame.TextFrame;

abstract public class AbstractHandler extends AbstractWebSocketHandler {
	
	AbstractHandler(IWebSocketSessionConfig config) {
		super(config);
	}

	void write0(Frame f) {
		getSession().writenf(f);
		Statistics.incPackets();
	}
	
	void write0(Packet p, Opcode opcode) {
		Frame f;
		
		if (opcode == Opcode.BINARY) {
			f = new BinaryFrame(p.getBytes());
		}
		else {
			f = new TextFrame(p.getBytes());
		}
		getSession().writenf(f);
		Statistics.incPackets();
	}

	void write(Frame f) {
		write0(f);
		Statistics.incPackets();
	}

	
	@Override
	public void read(Object msg) {
		Frame f = (Frame) msg;
		Packet p = new Packet(f.getPayload());
		
		switch (p.getType()) {
		case ECHO:
			if (!p.isResponse()) {
				p.setResponse(true);
				write0(p, f.getOpcode());
			}
			else {
				SessionContext ctx = (SessionContext) getSession().getAttributes().get(SessionContext.ATTR_KEY);
				
				if (ctx != null) {
					if (ctx.nextPacket()) {
						write(Utils.randomFrame());
					}
					else {
						getSession().close();
					}
				}
			}
			break;
			
		case NOP:
			Statistics.incNopPackets();
			break;
			
		default:
		}
	}
	
	
	@Override
	public void exception(Throwable t) {
		Statistics.incExceptions();
		System.err.println(t.getMessage());
		t.printStackTrace();
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		Statistics.incIncidents();
		System.err.println(t.getMessage());
		return super.incident(incident, t);
		
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new SessionStructureFactory();
	}
	
}
