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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpSocketOption;

public class TestSctpChannel extends SctpChannel {

	public Set<SocketAddress> localAddresses = new TreeSet<SocketAddress>(new AddrComparator());

	public Set<SocketAddress> remoteAddresses = new TreeSet<SocketAddress>(new AddrComparator());
	
	IOException remoteAddressesException;

	IOException localAddressesException;
	
	boolean connectionPending;
	
	IOException receiveException;

	IOException sendException;
	
	MessageInfo msgInfo;
	
	static class AddrComparator implements Comparator<SocketAddress> {

		@Override
		public int compare(SocketAddress o1, SocketAddress o2) {
			return ((InetSocketAddress)o1).getPort() - ((InetSocketAddress)o2).getPort();
		}
	}
	
	protected TestSctpChannel() {
		super(null);
	}

	@Override
	public Association association() throws IOException {
		return null;
	}

	@Override
	public SctpChannel bind(SocketAddress arg0) throws IOException {
		return null;
	}

	@Override
	public SctpChannel bindAddress(InetAddress arg0) throws IOException {
		return null;
	}

	@Override
	public boolean connect(SocketAddress arg0) throws IOException {
		return false;
	}

	@Override
	public boolean connect(SocketAddress arg0, int arg1, int arg2) throws IOException {
		return false;
	}

	@Override
	public boolean finishConnect() throws IOException {
		return false;
	}

	@Override
	public Set<SocketAddress> getAllLocalAddresses() throws IOException {
		if (localAddressesException != null) {
			throw localAddressesException;
		}
		return localAddresses;
	}

	@Override
	public <T> T getOption(SctpSocketOption<T> arg0) throws IOException {
		return null;
	}

	@Override
	public Set<SocketAddress> getRemoteAddresses() throws IOException {
		if (remoteAddressesException != null) {
			throw remoteAddressesException;
		}
		return remoteAddresses;
	}

	@Override
	public boolean isConnectionPending() {
		return connectionPending;
	}

	@Override
	public <T> MessageInfo receive(ByteBuffer arg0, T arg1, NotificationHandler<T> arg2) throws IOException {
		if (receiveException != null) {
			throw receiveException;
		}
		return msgInfo;
	}

	@Override
	public int send(ByteBuffer arg0, MessageInfo arg1) throws IOException {
		if (sendException != null) {
			throw sendException;
		}
		return 0;
	}

	@Override
	public <T> SctpChannel setOption(SctpSocketOption<T> arg0, T arg1) throws IOException {
		return null;
	}

	@Override
	public SctpChannel shutdown() throws IOException {
		return null;
	}

	@Override
	public Set<SctpSocketOption<?>> supportedOptions() {
		return null;
	}

	@Override
	public SctpChannel unbindAddress(InetAddress arg0) throws IOException {
		return null;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
	}

	@Override
	protected void implConfigureBlocking(boolean arg0) throws IOException {
	}

}
