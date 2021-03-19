/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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
	
	final ICodecExecutor executor;
	
	ISession session;
	
	private final boolean datagram;
	
	CodecExecutorAdapter(ICodecExecutor executor, ISession session) {
		this.executor = executor;
		this.session = session;
		datagram = session instanceof IDatagramSession;
	}
	
	protected CodecExecutorAdapter(ICodecExecutor executor) {
		this.executor = executor;
		datagram = false;
	}
	
	final List<Object> encode(ByteBuffer data) throws Exception {
		executor.syncEncoders(session);
		return executor.encode(session, data);
	}

	final List<Object> encode(byte[] data) throws Exception {
		executor.syncEncoders(session);
		return executor.encode(session, data);
	}

	final List<Object> encode(Object msg) throws Exception {
		executor.syncEncoders(session);
		return executor.encode(session, msg);
	}
	
	final ICodecExecutor getExecutor() {
		return executor;
	}
	
	@Override
	public int available(ByteBuffer buffer, boolean flipped) {
		executor.syncDecoders(session);

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
		executor.syncDecoders(session);
		
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
			executor.syncDecoders(session);
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
		read(out, handler);
	}
	
	@Override
	public void read(ByteBuffer data) {
		List<Object> out;
		
		executor.syncDecoders(session);
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
		read(out, handler);
	}
	
	@Override
	public void read(SocketAddress remoteAddress, byte[] datagram) throws PipelineDecodeException {
		List<Object> out;
		
		executor.syncDecoders(session);
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
		read(remoteAddress, out, handler);
	}	
    
	@Override
	public void read(SocketAddress remoteAddress, ByteBuffer datagram) {
		List<Object> out;
		
		executor.syncDecoders(session);
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
		read(remoteAddress, out, handler);
	}	
	
	private void read(SocketAddress remoteAddress, List<Object> out, IDatagramHandler handler) {
		if (out.isEmpty()) {
			return;
		}
		
		Iterator<Object> i = out.iterator();
		Object o = i.next();
		
		if (o.getClass() == byte[].class) {
			handler.read(remoteAddress, (byte[])o);
			while (i.hasNext()) {
				handler.read(remoteAddress, (byte[])i.next());
			}
		}
		else if (o instanceof ByteBuffer) {
			handler.read(remoteAddress, (ByteBuffer)o);
			while (i.hasNext()) {
				handler.read(remoteAddress, (ByteBuffer)i.next());
			}
		}
		else {
			handler.read(remoteAddress, o);
			while (i.hasNext()) {
				handler.read(remoteAddress, i.next());
			}
		}	
    }
    
    private void read(List<Object> out, IHandler handler) {
		if (out.isEmpty()) {
			return;
		}
		
		Iterator<Object> i = out.iterator();
		Object o = i.next();
		
		if (o.getClass() == byte[].class) {
			handler.read((byte[])o);
			while (i.hasNext()) {
				handler.read((byte[])i.next());
			}
		}
		else if (o instanceof ByteBuffer) {
			handler.read((ByteBuffer)o);
			while (i.hasNext()) {
				handler.read((ByteBuffer)i.next());
			}
		}
		else {
			handler.read(o);
			while (i.hasNext()) {
				handler.read(i.next());
			}
		}
    }
}
