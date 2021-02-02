package org.snf4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.SctpChannel;

public class SctpClient extends SctpServer {
	
	String ip = "127.0.0.1";

	Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
	
	SctpChannel sc;
	
	boolean loopStart = true;
	
	SctpClient(int port) {
		super(port);
	}
	
	public IFuture<Void> start() throws Exception {
		return start(null);
	}
	
	public IFuture<Void> start(SctpChannel channel) throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
			if (loopStart) {
				loop.start();
			}
		}
	
		if (channel == null) {
			sc = SctpChannel.open();
			sc.configureBlocking(false);
			if (!localAddresses.isEmpty()) {
				Iterator<SocketAddress> i = localAddresses.iterator();
				
				sc.bind(i.next());
				while (i.hasNext()) {
					sc.bindAddress(((InetSocketAddress)i.next()).getAddress());
				}
			}
			sc.connect(new InetSocketAddress(InetAddress.getByName(ip), port));
		}
		else {
			sc = channel;
		}
		return Sctp.register(loop, sc, new SctpSession(new Handler()));
	}
	
}
