package org.snf4j.example.sctp;

import java.net.InetSocketAddress;

import org.snf4j.core.SctpRegistrator;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.AbstractSctpSessionFactory;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

public class SctpServer {
	static final String PREFIX = "org.snf4j.";
	static final int PORT = Integer.getInteger(PREFIX+"Port", 8001);
	
	public static void main(String[] args) throws Exception {
		SelectorLoop loop = new SelectorLoop();

		try {
			loop.start();

			// Initialize the listener
			SctpServerChannel channel = SctpServerChannel.open();
			channel.configureBlocking(false);
			channel.bind(new InetSocketAddress(PORT));
			
			// Register the listener
			SctpRegistrator.register(loop, channel, new AbstractSctpSessionFactory() {

				@Override
				protected ISctpHandler createHandler(SctpChannel channel) {
					return new SctpServerHandler();
				}
			});
			
			// Wait till the loop ends
			loop.join();
		}
		finally {
			
			// Gently stop the loop
			loop.stop();
		}
	}
}
