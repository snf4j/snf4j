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
package org.snf4j.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpMultiChannel;

public class SctpClient extends SctpServer {
	
	String ip = "127.0.0.1";

	Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
	
	SctpChannel sc;
	
	SctpMultiChannel smc;
	
	boolean loopStart = true;
	
	boolean registerHandler;
	
	SctpClient(int port) {
		super(port);
	}
	
	public IFuture<Void> start() throws Exception {
		return start((SctpChannel)null);
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
		if (registerHandler) {
			return SctpRegistrator.register(loop, sc, new Handler());
		}
		return SctpRegistrator.register(loop, sc, new SctpSession(new Handler()));
	}
	
	public IFuture<Void> startMulti() throws Exception {
		return start((SctpMultiChannel)null);
	}
	
	public IFuture<Void> start(SctpMultiChannel channel) throws Exception {
		if (loop == null) {
			loop = new SelectorLoop();
			if (loopStart) {
				loop.start();
			}
		}
	
		if (channel == null) {
			smc = SctpMultiChannel.open();
			smc.configureBlocking(false);
			if (!localAddresses.isEmpty()) {
				Iterator<SocketAddress> i = localAddresses.iterator();
				
				smc.bind(i.next());
				while (i.hasNext()) {
					smc.bindAddress(((InetSocketAddress)i.next()).getAddress());
				}
			}
			else {
				smc.bind(new InetSocketAddress(InetAddress.getByName(ip), port));
			}
		}
		else {
			smc = channel;
		}
		if (registerHandler) {
			return SctpRegistrator.register(loop, smc, new Handler());
		}
		return SctpRegistrator.register(loop, smc, new SctpMultiSession(new Handler()));
	}
	
	
}
