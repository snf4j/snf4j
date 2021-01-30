package org.snf4j.core;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import com.sun.nio.sctp.Association;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpSocketOption;

public class TestSctpChannel extends SctpChannel {

	IOException remoteAddressesException;
	
	IOException receiveException;

	IOException sendException;
	
	MessageInfo msgInfo;
	
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
		return null;
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
		return null;
	}

	@Override
	public boolean isConnectionPending() {
		return false;
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
