/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;

abstract public class AbstractHandler extends AbstractStreamHandler {

	static final Timer timer = new Timer();
	
	void write0(Packet p) {
		getSession().writenf(p.getBytes());
		Statistics.incPackets();
	}
	
	void write(Packet p) {
		if (Utils.randomBoolean(Utils.DELAYED_WRITE_RATIO)) {
			timer.schedule(new WriteTask(p), Utils.random.nextInt(Utils.MAX_WRITE_DELAY));
		}
		else {
			write0(p);
			Statistics.incPackets();
			if (Utils.randomBoolean(Utils.MULTI_PACKET_RATIO)) {
				int count = Utils.random.nextInt(Utils.MAX_MULTI_PACKET) + 1;
				
				for (int i=0; i<count; ++i) {
					write0(Utils.randomNopPacket());
				}
			}
		}
	}

	ByteBuffer buffered = ByteBuffer.allocate(20000);
	
	@Override
	public void read(ByteBuffer data) {
		buffered.put(data);
		getSession().release(data);

		int i = Packet.available(buffered, false);
		if (i > 0) {
			byte[] b = new byte[i];
			
			buffered.flip();
			buffered.get(b);
			buffered.compact();
			read(new Packet(b));
		}
	}
	
	@Override
	public void read(Object msg) {
		Packet p = (Packet)msg;
		
		switch (p.getType()) {
		case ECHO:
			if (!p.isResponse()) {
				p.setResponse(true);
				write0(p);
			}
			else {
				SessionContext ctx = (SessionContext) getSession().getAttributes().get(SessionContext.ATTR_KEY);
				
				if (ctx != null) {
					if (ctx.nextPacket()) {
						write(Utils.randomPacket());
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
	public int available(ByteBuffer buffer, boolean flipped) {
		return Packet.available(buffer, flipped);
	}
	
	@Override
	public int available(byte[] buffer, int off, int len) {
		return Packet.available(buffer, off, len);
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
		return new SessionConfig().setMaxSSLApplicationBufferSizeRatio(2);
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new SessionStructureFactory();
	}
	
	class WriteTask extends TimerTask {
		Packet p;
		
		WriteTask(Packet p) {
			this.p = p;
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
			try {
				if (Utils.randomBoolean(Utils.SPLIT_PACKET_RATIO)) {
					int count = 2;
					byte[] b = p.getBytes();
					int size = b.length / count;
					int off = 0;
					for (int i=0; i<count-1; ++i) {
						sync(getSession().write(b, off, size));
						off += size;
					}
					sync(getSession().write(b, off, b.length-off));
				}
				else {
					sync(getSession().write(p.getBytes()));
				}
			} catch (Exception e) {
				Statistics.incExceptions();
				e.printStackTrace();
			}		
			Statistics.incPackets();
		}
		
	}
}
