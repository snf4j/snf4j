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
import java.net.SocketAddress;
import java.util.Set;
import java.util.TreeSet;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpSocketOption;

public class TestSctpServerChannel extends SctpServerChannel {

	public Set<SocketAddress> localAddresses = new TreeSet<SocketAddress>(new TestSctpChannel.AddrComparator());
	
	public IOException localAddressesException;
	
	protected TestSctpServerChannel() {
		super(null);
	}

	@Override
	public SctpChannel accept() throws IOException {
		return null;
	}

	@Override
	public SctpServerChannel bind(SocketAddress arg0, int arg1) throws IOException {
		return null;
	}

	@Override
	public SctpServerChannel bindAddress(InetAddress arg0) throws IOException {
		return null;
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
	public <T> SctpServerChannel setOption(SctpSocketOption<T> arg0, T arg1) throws IOException {
		return null;
	}

	@Override
	public Set<SctpSocketOption<?>> supportedOptions() {
		return null;
	}

	@Override
	public SctpServerChannel unbindAddress(InetAddress arg0) throws IOException {
		return null;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
	}

	@Override
	protected void implConfigureBlocking(boolean arg0) throws IOException {
	}

}
