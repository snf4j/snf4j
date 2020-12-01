/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class TestSocketChannel extends SocketChannel {

	protected TestSocketChannel() {
		super(null);
	}

	@Override
	public <T> T getOption(SocketOption<T> name) throws IOException {
		return null;
	}

	@Override
	public Set<SocketOption<?>> supportedOptions() {
		return null;
	}

	@Override
	public SocketChannel bind(SocketAddress local) throws IOException {
		return null;
	}

	@Override
	public <T> SocketChannel setOption(SocketOption<T> name, T value) throws IOException {
		return null;
	}

	@Override
	public SocketChannel shutdownInput() throws IOException {
		return null;
	}

	@Override
	public SocketChannel shutdownOutput() throws IOException {
		return null;
	}

	@Override
	public Socket socket() {
		return null;
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	public boolean isConnectionPending() {
		return false;
	}

	@Override
	public boolean connect(SocketAddress remote) throws IOException {
		return false;
	}

	@Override
	public boolean finishConnect() throws IOException {
		return false;
	}

	@Override
	public SocketAddress getRemoteAddress() throws IOException {
		return null;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return 0;
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return 0;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return 0;
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return 0;
	}

	@Override
	public SocketAddress getLocalAddress() throws IOException {
		return null;
	}

	@Override
	protected void implCloseSelectableChannel() throws IOException {
	}

	@Override
	protected void implConfigureBlocking(boolean block) throws IOException {
	}

}
