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
package org.snf4j.longevity.sctp;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.snf4j.core.ImmutableSctpMessageInfo;
import org.snf4j.core.SctpSession;
import org.snf4j.core.SctpTest;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractSctpHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.longevity.Packet;
import org.snf4j.longevity.Statistics;

import com.sun.nio.sctp.MessageInfo;

public class AbstractHandler extends AbstractSctpHandler {

	static final Timer TIMER = new Timer();
	
	ByteBuffer allocateIfNeeded(Packet p, boolean optimize) {
		if (optimize) {
			optimize = SctpTest.isOptimized((SctpSession) getSession());
		}
		ByteBuffer buffer = null;
		
		optimize = optimize && Utils.randomBoolean(Utils.WRITE_ALLOCATED_BUFFER_RATIO);
		if (optimize) {
			byte[] b = p.getBytes();
			buffer = getSession().allocate(b.length);
			buffer.put(b).flip();
		}
		return buffer;
	}
	
	ImmutableSctpMessageInfo msgInfo(MessageInfo msgInfo) {
		msgInfo = Utils.MSG_INFOS[Utils.RANDOM.nextInt(Utils.MSG_INFOS.length)];
		
		if (msgInfo != null) {
			return ImmutableSctpMessageInfo.create(msgInfo);
		}
		return ImmutableSctpMessageInfo.create(0);
	}
	
	void write0(Packet p, ImmutableSctpMessageInfo msgInfo, boolean optimize) {
		ByteBuffer msg = allocateIfNeeded(p, optimize);
		
		if (msg != null) {
			getSession().writenf(msg, msgInfo);
		}
		else {
			getSession().writenf(p.getBytes(), msgInfo);
		}
		Statistics.incPackets();
	}
	
	void write(Packet p, ImmutableSctpMessageInfo msgInfo) {
		if (Utils.randomBoolean(Utils.DELAYED_WRITE_RATIO)) {
			TIMER.schedule(new WriteTask(p, msgInfo), Utils.RANDOM.nextInt(Utils.MAX_WRITE_DELAY));
		}
		else {
			write0(p, msgInfo, true);
			Statistics.incPackets();
			if (Utils.randomBoolean(Utils.MULTI_PACKET_RATIO)) {
				int count = Utils.RANDOM.nextInt(Utils.MAX_MULTI_PACKET) + 1;
				
				for (int i=0; i<count; ++i) {
					write0(Utils.randomNopPacket(), msgInfo, true);
				}
			}
		}
	}
	
	@Override
	public void read(ByteBuffer msg, MessageInfo msgInfo) {
		byte[] b = new byte[msg.remaining()];
		
		msg.get(b);
		getSession().release(msg);
		read(b, msgInfo);
	}
	
	@Override
	public void read(byte[] msg, MessageInfo msgInfo) {
		read(new Packet(msg), msgInfo);
	}
	
	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		Packet p = (Packet) msg;
		
		switch (p.getType()) {
		case ECHO:
			if (!p.isResponse()) {
				p.setResponse(true);
				write0(p, msgInfo(msgInfo), true);
			}
			else {
				SessionContext ctx = (SessionContext) getSession().getAttributes().get(SessionContext.ATTR_KEY);
			
				if (ctx != null) {
					if (ctx.nextPacket()) {
						write(Utils.randomPacket(), msgInfo(msgInfo));
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
	public ISctpSessionConfig getConfig() {
		return new SessionConfig();
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new SessionStructureFactory();
	}
	
	class WriteTask extends TimerTask {

		Packet p;
		
		ImmutableSctpMessageInfo msgInfo;
		
		WriteTask(Packet p, ImmutableSctpMessageInfo msgInfo) {
			this.p = p;
			this.msgInfo = msgInfo;
		}
		
		void sync(IFuture<Void> f) throws Exception {
			boolean timeout = Utils.randomBoolean(Utils.SYNC_WITH_TIMEOUT_RATIO);
			
			if (timeout) {
				f.sync(30 * 60 * 10000);
			}
			else {
				f.sync();
			}
		}
		
		@Override
		public void run() {
			try {
				ByteBuffer bb = allocateIfNeeded(p, true);
				
				if (bb != null) {
					sync(getSession().write(bb, msgInfo));
				}
				else {
					sync(getSession().write(p.getBytes(), msgInfo));	
				}
			}
			catch (Exception e) {
				Statistics.incExceptions();
				e.printStackTrace();
			}
			Statistics.incPackets();
		}
		
	}
}
