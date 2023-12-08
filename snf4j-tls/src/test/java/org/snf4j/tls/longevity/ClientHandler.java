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
package org.snf4j.tls.longevity;

import java.nio.ByteBuffer;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionTimer;

public class ClientHandler extends ServerHandler {
	
	private long remaining = SESSION_SIZE;
		
	private static final Object DELAYED_WRITE = new Object();
	
	private int writeSize;
	
	public ClientHandler() {
		super(true);
	}
	
	private boolean delayedWrite(int size) {
		ISessionTimer timer = getSession().getTimer();
		
		if (timer.isSupported()) {
			writeSize = size;
			timer.scheduleEvent(DELAYED_WRITE, CLIENT_RESPONSE_DELAY);
			return true;
		}
		return false;
	}
	
	@Override
	public void read(byte[] data) {
		if (remaining > 0) {
			remaining -= data.length;
			if (!delayedWrite(data.length)) {
				super.read(data);
			}
		}
		else {
			getSession().close();
		}
	}
	
	@Override
	public void read(ByteBuffer data) {
		if (remaining > 0) {
			remaining -= data.remaining();
			if (delayedWrite(data.remaining())) {
				getSession().release(data);
			}
			else {
				super.read(data);
			}
		}
		else {
			getSession().release(data);
			getSession().close();
		}
	}
	
	@Override
	public void read(Object msg) {
		getSession().writenf(msg);
	}

	@Override
	public void timer(Object event) {
		ByteBuffer data = getSession().allocate(writeSize);
		
		data.position(writeSize).flip();
		getSession().writenf(data);
	}
	
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			ByteBuffer data = getSession().allocate(PACKET_SIZE);
			
			for (int i=0; i<PACKET_SIZE; ++i) {
				data.put((byte)i);
			}
			data.flip();
			getSession().writenf(data);
			break;

		default:
			super.event(event);
		}
	}	
	
	@Override
	public void exception(Throwable t) {
		Metric.incExceptions();
		t.printStackTrace();
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		Metric.incIncidents();
		t.printStackTrace();
		return super.incident(incident, t);
	}
	
	class DelayedWrite implements Runnable {

		final int size;
		
		DelayedWrite(int size) {
			this.size = size;
		}
		
		@Override
		public void run() {
			ByteBuffer data = getSession().allocate(size);
			
			data.position(size).flip();
			getSession().writenf(data);
		}
		
	}
}
