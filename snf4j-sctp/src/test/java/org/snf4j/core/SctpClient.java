package org.snf4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.sun.nio.sctp.SctpChannel;

public class SctpClient extends SctpServer {
	
	String ip = "127.0.0.1";

	SctpChannel sc;
	
	boolean loopStart = true;
	
	SctpClient(int port) {
		super(port);
	}
	
	public void start() throws Exception {
		start(null);
	}
	
	public void start(SctpChannel channel) throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
			if (loopStart) {
				loop.start();
			}
		}
	
		if (channel == null) {
			sc = SctpChannel.open();
			sc.configureBlocking(false);
			sc.connect(new InetSocketAddress(InetAddress.getByName(ip), port));
		}
		else {
			sc = channel;
		}
		Sctp.register(loop, sc, new SctpSession(new Handler()));
	}
	
}
