package org.snf4j.example.sctp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.snf4j.core.SctpRegistrator;
import org.snf4j.core.SelectorLoop;

import com.sun.nio.sctp.SctpChannel;

public class SctpClient {
	static final String PREFIX = "org.snf4j.";
	static final String HOST = System.getProperty(PREFIX+"Host", "127.0.0.1");
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	static final int SIZE = Integer.getInteger(PREFIX+"Size", 512);

	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();
			
			// Initialize the connection
			SctpChannel channel = SctpChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(InetAddress.getByName(HOST), PORT));
			
			// Register the channel
			SctpRegistrator.register(loop, channel, new SctpClientHandler());
			
			// Wait till the loop ends
			loop.join();
		}
		finally {

			// Gently stop the loop
			loop.stop();
		}
	}
}
