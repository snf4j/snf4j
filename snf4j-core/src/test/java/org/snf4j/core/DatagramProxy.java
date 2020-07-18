package org.snf4j.core;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;

public class DatagramProxy {
	SelectorLoop loop;
	int port;
	
	volatile DatagramHandler peer1;
	volatile DatagramHandler peer2;
	
	Map<SocketAddress, List<byte[]>> packets = new HashMap<SocketAddress, List<byte[]>>();

	Action DEFAULT_ACTION = new Action();

	volatile Action action = DEFAULT_ACTION;
	
	public DatagramProxy(int port) {
		this.port = port;
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
	
	DatagramHandler peer(SocketAddress remoteAddress) {
		if (peer1.getSession().getLocalAddress().equals(remoteAddress)) {
			return peer2;
		}
		return peer1;
	}
	
	SocketAddress peerAddress(SocketAddress remoteAddress) {
		return peer(remoteAddress).getSession().getLocalAddress();
	}

	public void start(long millis) throws Exception {
		loop = new SelectorLoop();
		loop.start();
		
		DatagramChannel dc = DatagramChannel.open();
		dc.configureBlocking(false);
		dc.socket().bind(new InetSocketAddress("127.0.0.1", port));
		ISession session = loop.register(dc, new Handler()).getSession();
		session.getReadyFuture().sync(millis);
	}
	
	public void stop(long millis) throws InterruptedException {
		loop.stop();
		loop.join(millis);
		if (loop.thread != null) {
			throw new InterruptedException();
		}
	}
	
	class Handler extends AbstractDatagramHandler {
		
		@Override
		public void read(byte[] data) {
		}

		@Override
		public void read(SocketAddress remoteAddress, byte[] datagram) {
			
			if (!peer(remoteAddress).proxyAction) {
				SocketAddress addr = peerAddress(remoteAddress);
				
				if (addr != null) {
					getSession().sendnf(peerAddress(remoteAddress), datagram);
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
