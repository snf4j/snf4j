/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2022 SNF4J contributors
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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.snf4j.core.ByteBufferHolder;
import org.snf4j.core.IByteBufferHolder;
import org.snf4j.core.SessionTest;
import org.snf4j.core.StreamSession;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;

abstract public class AbstractHandler extends AbstractStreamHandler {

	static final Timer timer = new Timer();
	
	Object allocateBufferIfNeeded(Packet p, boolean optimize) {
		if (optimize) {
			optimize = SessionTest.isOptimized((StreamSession) getSession());
		}
		ByteBuffer buffer = null;
		
		optimize = optimize && Utils.randomBoolean(Utils.WRITE_ALLOCATED_BUFFER_RATIO);
		
		if (optimize) {
			byte[] bytes = p.getBytes();

			if (Utils.randomBoolean(Utils.BUFFER_HOLDER_VS_BUFFER_RATIO)) {
				boolean single = Utils.randomBoolean(Utils.SINGLE_BUFFER_HOLDER_RATIO);
				ByteBufferHolder holder = new ByteBufferHolder();
				byte[][] data;
				
				if (single) {
					data = new byte[][] {bytes};
				}
				else {
					data = new byte[2][];
					int firstSize = data.length/2;
					
					data[0] = Arrays.copyOfRange(bytes, 0, firstSize);
					data[1] = Arrays.copyOfRange(bytes, firstSize, bytes.length);
				}
				for (byte[] d: data) {
					buffer = getSession().allocate(d.length);
					buffer.put(d);
					buffer.flip();
					holder.add(buffer);
				}
				return holder;
			}
			else {
				buffer = getSession().allocate(bytes.length);
				buffer.put(bytes);
				buffer.flip();
			}
		}
		return buffer;
	}
	
	void write0(Packet p, boolean optimize) {
		Object buffer = allocateBufferIfNeeded(p, optimize);
		
		if (buffer instanceof ByteBuffer) {
			getSession().writenf((ByteBuffer)buffer);
		}
		else if (buffer instanceof IByteBufferHolder) {
			getSession().writenf((IByteBufferHolder)buffer);
		}
		else {
			getSession().writenf(p.getBytes());
		}
		Statistics.incPackets();
	}
	
	void write(Packet p) {
		if (Utils.randomBoolean(Utils.DELAYED_WRITE_RATIO)) {
			timer.schedule(new WriteTask(p), Utils.random.nextInt(Utils.MAX_WRITE_DELAY));
		}
		else {
			write0(p, true);
			Statistics.incPackets();
			if (Utils.randomBoolean(Utils.MULTI_PACKET_RATIO)) {
				int count = Utils.random.nextInt(Utils.MAX_MULTI_PACKET) + 1;
				
				for (int i=0; i<count; ++i) {
					write0(Utils.randomNopPacket(), true);
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
				write0(p, true);
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
		return new SessionConfig().setMaxSSLApplicationBufferSizeRatio(200);
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
					ByteBuffer bb;
					
					for (int i=0; i<count-1; ++i) {
						if (Utils.SPLIT_PACKET_WITH_BUFFER_ALLOCATION) {
							bb = getSession().allocate(size);
							bb.put(b, off, size).flip();
							sync(getSession().write(bb));
						}
						else {
							sync(getSession().write(b, off, size));
						}
						off += size;
					}
					if (Utils.SPLIT_PACKET_WITH_BUFFER_ALLOCATION) {
						bb = getSession().allocate(b.length-off);
						bb.put(b, off, b.length-off).flip();
						sync(getSession().write(bb));
					}
					else {
						sync(getSession().write(b, off, b.length-off));
					}
				}
				else {
					Object buffer = allocateBufferIfNeeded(p, true);
					
					if (buffer instanceof ByteBuffer) {
						sync(getSession().write((ByteBuffer)buffer));
					}
					else if (buffer instanceof IByteBufferHolder) {
						sync(getSession().write((IByteBufferHolder)buffer));
					}
					else {
						sync(getSession().write(p.getBytes()));
					}
				}
			} catch (Exception e) {
				Statistics.incExceptions();
				e.printStackTrace();
			}		
			Statistics.incPackets();
		}
		
	}
}
