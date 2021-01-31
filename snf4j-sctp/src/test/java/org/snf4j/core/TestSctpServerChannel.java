package org.snf4j.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpSocketOption;

public class TestSctpServerChannel extends SctpServerChannel {

	public Set<SocketAddress> localAddresses = new HashSet<SocketAddress>();
	
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
