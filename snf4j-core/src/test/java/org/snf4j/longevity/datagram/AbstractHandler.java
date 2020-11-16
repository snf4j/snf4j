/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.longevity.datagram;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.ITimerTask;
import org.snf4j.longevity.Config;
import org.snf4j.longevity.Packet;
import org.snf4j.longevity.SessionContext;
import org.snf4j.longevity.Statistics;
import org.snf4j.longevity.Utils;

class AbstractHandler extends AbstractDatagramHandler {

	static final Timer timer = new Timer();
	
	static final Object REWRITE_TIMER_EVENT = new Object();
	
	static final Object CLOSE_TIMER_EVENT = new Object();
	
	ITimerTask rewriteTimerTask;
	
	ITimerTask closeTimerTask;
	
	SocketAddress rewriteAddress;
	
	public void cancelAllTimers() {
		if (rewriteTimerTask != null) {
			rewriteTimerTask.cancelTask();
			rewriteTimerTask = null;
		}
		if (closeTimerTask != null) {
			closeTimerTask.cancelTask();
			closeTimerTask = null;
		}		
	}
	
	void write0(SocketAddress remoteAddress, Packet p, boolean optimize) {
		if (optimize) {
			optimize = Config.DATAGRAM_CACHING_ALLOCATOR && Config.DATAGRAM_OPTIMIZE_DATA_COPING;
		}
		ByteBuffer buffer = null;
		
		if (optimize) {
			byte[] bytes = p.getBytes();
			buffer = getSession().allocate(bytes.length);
			buffer.put(bytes);
			buffer.flip();
		}
		
		if (remoteAddress == null) {
			if (buffer != null) {
				getSession().writenf(buffer);
			}
			else {
				getSession().writenf(p.getBytes());
			}
		}
		else {
			if (buffer != null) {
				getSession().sendnf(remoteAddress, buffer);
			}
			else {
				getSession().sendnf(remoteAddress, p.getBytes());
			}
		}
		Statistics.incPackets();
	}
	
	void write(SocketAddress remoteAddress, Packet p) {
		if (rewriteTimerTask != null) {
			rewriteTimerTask.cancelTask();
			rewriteTimerTask = null;
		}
		if (Utils.randomBoolean(Utils.DELAYED_WRITE_RATIO)) {
			int delay = Utils.random.nextInt(Utils.DATAGRAM_MAX_WRITE_DELAY);
			
			timer.schedule(new WriteTask(remoteAddress, p), delay);
			rewriteTimerTask = getSession().getTimer().scheduleEvent(REWRITE_TIMER_EVENT, 500+delay);
		}
		else {
			write0(remoteAddress, p, true);
			Statistics.incPackets();
			rewriteTimerTask = getSession().getTimer().scheduleEvent(REWRITE_TIMER_EVENT, 500);
			rewriteAddress = remoteAddress;
			if (Utils.randomBoolean(Utils.MULTI_PACKET_RATIO)) {
				int count = Utils.random.nextInt(Utils.MAX_MULTI_PACKET) + 1;
				
				for (int i=0; i<count; ++i) {
					write0(remoteAddress, Utils.randomDatagramNopPacket(), false);
				}
			}
		}
	}
	
	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
		Packet p = (Packet)msg;
		
		switch (p.getType()) {
		case ECHO:
			if (!p.isResponse()) {
				if (closeTimerTask != null) {
					closeTimerTask.cancelTask();
				}
				closeTimerTask = getSession().getTimer().scheduleEvent(CLOSE_TIMER_EVENT, 120000);
				p.setResponse(true);
				write0(remoteAddress, p, true);
			}
			else {
				SessionContext ctx = (SessionContext) getSession().getAttributes().get(SessionContext.ATTR_KEY);
				
				if (ctx != null) {
					if (ctx.nextPacket()) {
						write(remoteAddress, Utils.randomDatagramPacket());
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
	public void read(Object msg) {
		read(null, msg);
	}

	@Override
	public void read(byte[] data) {
		read(null, data);
	}

	@Override
	public void read(SocketAddress remoteAddress, byte[] data) {
		read(remoteAddress, new Packet(data));
	}

	@Override
	public void read(ByteBuffer data) {
		read(null, data);
	}

	@Override
	public void read(SocketAddress remoteAddress, ByteBuffer data) {
		byte[] b = new byte[data.remaining()];
		
		data.get(b);
		getSession().release(data);
		read(remoteAddress, new Packet(b));
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
	public ISessionConfig getConfig() {
		return new SessionConfig();
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new SessionStructureFactory();
	}	
	
	@Override
	public void timer(Object event) {
		if (event == REWRITE_TIMER_EVENT) {
			rewriteTimerTask = null;
			write(rewriteAddress, Utils.randomDatagramPacket());
		}
		else if (event == CLOSE_TIMER_EVENT) {
			closeTimerTask = null;
			getSession().dirtyClose();
		}
	}
	
	class WriteTask extends TimerTask {
		Packet p;
		SocketAddress a;
		
		WriteTask(SocketAddress a, Packet p) {
			this.p = p;
			this.a = a;
		}
		
		private void sync(IFuture<Void> f) throws Exception {
			boolean timeout = Utils.randomBoolean(Utils.SYNC_WITH_TIMEOUT_RATIO);

			if (timeout) {
				f.sync(30 * 60 * 1000);
			}
			else { 
				f.sync();
			}		
		}
		
		@Override
		public void run() {
			if (!getSession().isOpen()) return;
			try {
				if (a == null) {
					sync(getSession().write(p.getBytes()));
				}
				else {
					sync(getSession().send(a, p.getBytes()));
				}
			} catch (Exception e) {
				Statistics.incExceptions();
				e.printStackTrace();
			}		
			Statistics.incPackets();
		}
		
	}	
}
