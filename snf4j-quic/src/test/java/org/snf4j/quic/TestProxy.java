/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020-2024 SNF4J contributors
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
package org.snf4j.quic;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snf4j.core.SelectorLoop;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;

public class TestProxy {
	
	final SelectorLoop loop;
	
	final int port;
	
	final InetSocketAddress srvAddress;
	
	volatile TestHandler srv, cli;
	
	Map<SocketAddress, List<byte[]>> packets = new HashMap<SocketAddress, List<byte[]>>();

	Action DEFAULT_ACTION = new Action();

	volatile Action action = DEFAULT_ACTION;
	
	public TestProxy(SelectorLoop loop, int port, int srvPort) {
		this.loop = loop;
		this.port = port;
		srvAddress = new InetSocketAddress("127.0.0.1", srvPort);
	}
	
	List<byte[]> get(SocketAddress remoteAddress) {
		List<byte[]> l = packets.get(remoteAddress);
		
		if (l == null) {
			l = new ArrayList<byte[]>();
			packets.put(remoteAddress, l);
		}
		return l;
	}
	
	boolean alreadyLost(SocketAddress remoteAddress, int length) {
		for (byte[] b: get(remoteAddress)) {
			if (b.length == length) {
				return true;
			}
		}
		return false;
	}
	
	TestHandler peer(SocketAddress remoteAddress) {
		if (cli.getSession().getLocalAddress().equals(remoteAddress)) {
			return srv;
		}
		return cli;
	}
	
	SocketAddress peerAddress(SocketAddress remoteAddress) {
		TestHandler peer = peer(remoteAddress);
		
		if (peer != srv && peer.getSession() != null) {
			return peer.getSession().getLocalAddress();
		}
		return srvAddress;
	}

	public void start(long millis) throws Exception {
		DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		dc.socket().bind(new InetSocketAddress("127.0.0.1", port));
		ISession session = loop.register(dc, new Handler()).getSession();
		session.getReadyFuture().sync(millis);
	}
		
	class Handler extends AbstractDatagramHandler {
		
		@Override
		public void read(Object msg) {
		}
		
		@Override
		public void read(SocketAddress remoteAddress, Object msg) {
		}

		@Override
		public void read(SocketAddress remoteAddress, byte[] datagram) {
			
			if (!peer(remoteAddress).proxyAction) {
				SocketAddress addr = peerAddress(remoteAddress);
				
				if (addr != null) {
					getSession().sendnf(addr, datagram);
				}
			}
			else {
				action.read(getSession(), remoteAddress, datagram);
				get(remoteAddress).add(datagram);
			}
		}
	}
	
	Action WITH_1PPREVIOUS_PACKET_ACTION = new Action() {
		@Override
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			List<byte[]> p = get(remoteAddress);
			
			out.add(datagram);
			if (p != null && !p.isEmpty()) {
				out.add(p.get(p.size()-1));
			}
		}
	};
	
	Action DUPLICATE_ACTION = new Action() {
		@Override
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			out.add(datagram);
			out.add(datagram);
		}
	};
	
	Action LOST_FIRST_3PACKETS_ACTION = new Action() {
		int count = 3;
		@Override
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			if (count-- > 0) {
				return;
			}
			out.add(datagram);
		}
	};
	
	Action LOST_FIRST_1PACKET_ACTION = new Action() {
		int count = 1;
		@Override
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			if (count-- > 0) {
				return;
			}
			out.add(datagram);
		}
	};
	
	Action LOST_EVERY_PACKET_ONCE_ACTION = new Action() {
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			if (alreadyLost(remoteAddress, datagram.length)) {
				out.add(datagram);
			}
		}
	};
	
	Action LOST_EVERY_PACKET_ACTION = new Action() {
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
		}
	};
	
	Action lostAction(boolean... lost) {
		return new LostAction(lost);
	}
	
	class LostAction extends Action {
		
		boolean[] lost;
		
		int count;
		
		LostAction(boolean... lost) {
			this.lost = lost.clone();
		}
		
		@Override
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			if (count < lost.length && lost[count++]) {
				return;
			}
			out.add(datagram);
		}
	}
	
	class Action {
		
		void process(SocketAddress remoteAddress, byte[] datagram, List<byte[]> out) {
			out.add(datagram);
		}
		
		void read(IDatagramSession session, SocketAddress remoteAddress, byte[] datagram) {
			ArrayList<byte[]> out = new ArrayList<byte[]>();
			
			process(remoteAddress, datagram, out);
			for (byte[] d: out) {
				session.sendnf(peerAddress(remoteAddress), d);
			}
		}
	}
}
