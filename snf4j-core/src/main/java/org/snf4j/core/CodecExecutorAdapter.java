/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.handler.IHandler;
import org.snf4j.core.session.IDatagramSession;
import org.snf4j.core.session.ISession;

class CodecExecutorAdapter implements IStreamReader, IDatagramReader {
	
	private final ICodecExecutor executor;
	
	private final ISession session;
	
	private final boolean datagram;
	
	CodecExecutorAdapter(ICodecExecutor executor, ISession session) {
		this.executor = executor;
		this.session = session;
		datagram = session instanceof IDatagramSession;
	}
	
	final List<Object> encode(ByteBuffer data) throws Exception {
		executor.syncEncoders();
		return executor.encode(session, data);
	}

	final List<Object> encode(byte[] data) throws Exception {
		executor.syncEncoders();
		return executor.encode(session, data);
	}

	final List<Object> encode(Object msg) throws Exception {
		executor.syncEncoders();
		return executor.encode(session, msg);
	}
	
	final ICodecExecutor getExecutor() {
		return executor;
	}
	
	@Override
	public int available(ByteBuffer buffer, boolean flipped) {
		executor.syncDecoders();

		IBaseDecoder<?> base = executor.getBaseDecoder();
		
		if (base != null) {
			return base.available(session, buffer, flipped);
		}
		if (executor.hasDecoders()) {
			return flipped ? buffer.remaining() : buffer.position();
		}
		return ((IStreamReader)session.getHandler()).available(buffer, flipped);
	}

	@Override
	public int available(byte[] buffer, int off, int len) {
		executor.syncDecoders();
		
		IBaseDecoder<?> base = executor.getBaseDecoder();
		
		if (base != null) {
			return base.available(session, buffer, off, len);
		}
		if (executor.hasDecoders()) {
			return len;
		}
		return ((IStreamReader)session.getHandler()).available(buffer, off, len);
	}


	@Override
	public void read(byte[] data) throws PipelineDecodeException {
		List<Object> out;
		
		if (datagram) {
			executor.syncDecoders();
		}
		try {
			out = executor.decode(session, data);
		}
		catch (Exception e) {
			throw new PipelineDecodeException((InternalSession) session, e);
		}
		
		IHandler handler = session.getHandler();
		
		if (out == null) {
			handler.read(data);
			return;
		}
		if (out.isEmpty()) {
			return;
		}
		
		Iterator<Object> i = out.iterator();
		Object o = i.next();
		
		if (o.getClass() == byte[].class) {
			handler.read((byte[])o);
			for (;i.hasNext(); o = i.next()) {
				handler.read((byte[])o);
			}
		}
		else {
			handler.read(o);
			for (;i.hasNext(); o = i.next()) {
				handler.read(o);
			}
		}
	}

	@Override
	public void read(SocketAddress remoteAddress, byte[] datagram) throws PipelineDecodeException {
		List<Object> out;
		
		executor.syncDecoders();
		try {
			out = executor.decode(session, datagram);
		}
		catch (Exception e) {
			throw new PipelineDecodeException((InternalSession) session, e);
		}
		
		IDatagramHandler handler = (IDatagramHandler) session.getHandler();
		
		if (out == null) {
			handler.read(remoteAddress, datagram);
			return;
		}
		if (out.isEmpty()) {
			return;
		}
		
		Iterator<Object> i = out.iterator();
		Object o = i.next();
		
		if (o.getClass() == byte[].class) {
			handler.read(remoteAddress, (byte[])o);
			for (;i.hasNext(); o = i.next()) {
				handler.read(remoteAddress, (byte[])o);
			}
		}
		else {
			handler.read(remoteAddress, o);
			for (;i.hasNext(); o = i.next()) {
				handler.read(remoteAddress, o);
			}
		}	
	}	
}
