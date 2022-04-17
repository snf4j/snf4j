/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2022 SNF4J contributors
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

import org.snf4j.core.future.IDelegatingFuture;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.SessionFuturesController;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

class EncodeTask implements Runnable {
	
	private final static ILogger LOGGER = LoggerFactory.getLogger(EncodeTask.class);	
	
	private final static int BYTES = 0;
	
	private final static int BUFFER = 1;
	
	private final static int HOLDER = 2;
	
	final IExceptionLogger elogger = ExceptionLogger.getInstance();

	final InternalSession session;
	
	IDelegatingFuture<Void> future;

	byte[] bytes;
	
	ByteBuffer buffer;
	
	IByteBufferHolder holder;
	
	Object msg;
	
	SocketAddress remoteAddress;
	
	int length;
	
	private IEncodeTaskWriter writer;	
	
	private final void init(final byte[] bytes) {
		if (session.optimizeCopying) {
			this.bytes = bytes;
		}
		else {
			this.bytes = bytes.clone();
		}
	}
	
	private final void init(final ByteBuffer buffer) {
		if (session.optimizeCopying) {
			this.buffer = buffer;
		}
		else {
			bytes = new byte[buffer.remaining()];
			buffer.get(bytes);
		}
	}

	private final void init(final IByteBufferHolder holder) {
		if (session.optimizeCopying) {
			this.holder = holder;
		}
		else {
			ByteBuffer[] buffers = holder.toArray();
			int len, off = 0;
			
			bytes = new byte[holder.remaining()];
			if (buffers.length == 1) {
				buffers[0].get(bytes);
			}
			else {
				for (ByteBuffer buffer: buffers) {
					len = buffer.remaining();
					buffer.get(bytes, off, len);
					off += len;
				}
			}
		}
	}
	
	static EncodeTask simple(InternalSession session, byte[] bytes) {
		EncodeTask task = new EncodeTask(session);
		
		task.bytes = bytes;
		task.length = bytes.length;
		return task;
	}
	
	static EncodeTask simple(InternalSession session, ByteBuffer buffer) {
		EncodeTask task = new EncodeTask(session);
		
		task.buffer = buffer;
		task.length = buffer.remaining();
		return task;
	}

	static EncodeTask simple(InternalSession session, IByteBufferHolder holder) {
		EncodeTask task = new EncodeTask(session);
		
		task.holder = holder;
		task.length = holder.remaining();
		return task;
	}
	
	private EncodeTask(InternalSession session) {
		this.session = session;
	}
	
	EncodeTask(InternalSession session, byte[] bytes) {
		this.session = session;
		this.length = bytes.length;
		init(bytes);
	}
	
	EncodeTask(InternalSession session, byte[] bytes, int offset, int length) {
		this.session = session;
		this.length = length;
		if (session.optimizeCopying && bytes.length == length) {
			this.bytes = bytes;
		}
		else {
			this.bytes = new byte[length];
			System.arraycopy(bytes, offset, this.bytes, 0, length);
		}
	}
	
	EncodeTask(InternalSession session, ByteBuffer buffer) {
		this.session = session;
		this.length = buffer.remaining();
		init(buffer);
	}

	EncodeTask(InternalSession session, ByteBuffer buffer, int length) {
		this.session = session;
		this.length = length;
		if (session.optimizeCopying && length == buffer.remaining()) {
			this.buffer = buffer;
		}
		else {
			bytes = new byte[length];
			buffer.get(bytes);
		}
	}

	EncodeTask(InternalSession session, IByteBufferHolder holder) {
		this.session = session;
		if (holder.isMessage()) {
			length = -1;
			msg = holder;
		}
		else {
			length = holder.remaining();
			init(holder);
		}
	}
	
	EncodeTask(InternalSession session, Object msg) {
		this.session = session;
		this.length = -1;
		if (msg.getClass() == byte[].class) {
			init((byte[])msg);
		}
		else if (msg instanceof ByteBuffer) {
			init((ByteBuffer)msg);
		}
		else if (msg instanceof IByteBufferHolder) {
			if (((IByteBufferHolder)msg).isMessage()) {
				this.msg = msg;
			}
			else {
				init((IByteBufferHolder)msg);
			}
		}
		else {
			this.msg = msg;
		}
	}
	
	final void registernf(SocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		registernf();
	}

	final void registernf() {
		InternalSession.checkKey(session.key);
		
		InternalSelectorLoop loop = session.loop;
		
		if (loop.inLoop()) {
			run();
		}
		else {
			loop.executenf(this);
		}
	}
	
	final IFuture<Void> register(SocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
		return register();
	}
	
	final IFuture<Void> register() {
		InternalSession.checkKey(session.key);
		future = session.futuresController.getDelegatingFuture();
		
		InternalSelectorLoop loop = session.loop;
		
		if (loop.inLoop()) {
			run();
		}
		else {
			loop.executenf(this);
		}
		return future;
	}
	
	protected void setWriter(IEncodeTaskWriter writer) {
		this.writer = writer;
	}
	
	protected IFuture<Void> write(byte[] data, boolean withFuture) {
		return writer.write(remoteAddress, data, withFuture);
	}

	protected IFuture<Void> write(ByteBuffer data, boolean withFuture) {
		return writer.write(remoteAddress, data, withFuture);
	}

	protected IFuture<Void> write(IByteBufferHolder data, boolean withFuture) {
		return writer.write(remoteAddress, data, withFuture);
	}
	
	private final IFuture<Void> write(final List<Object> out, boolean withFuture) {
		Iterator<Object> i = out.iterator();
		Object data = i.next();
	
		if (data.getClass() == byte[].class) {
			for (;;) {
				if (i.hasNext()) {
					write((byte[])data, false);
					data = i.next();
				}
				else {
					return write((byte[])data, withFuture);
				}
			}
		}
		else if (data instanceof ByteBuffer) {
			for (;;) {
				if (i.hasNext()) {
					write((ByteBuffer)data, false);
					data = i.next();
				}
				else {
					return write((ByteBuffer)data, withFuture);
				}
			}
		}
		else {
			for (;;) {
				if (i.hasNext()) {
					write((IByteBufferHolder)data, false);
					data = i.next();
				}
				else {
					return write((IByteBufferHolder)data, withFuture);
				}
			}
		}
	}
	
	/**
	 * Returns {@code null} if there is no encoder
	 */
	protected List<Object> encode(Object msg) throws Exception {
		return session.codec.encode(msg);
	}
	
	/**
	 * Returns {@code null} if there is no encoder
	 */
	protected List<Object> encode(byte[] bytes) throws Exception {
		return session.codec.encode(bytes);
	}
	
	/**
	 * Returns {@code null} if there is no encoder
	 */
	protected List<Object> encode(ByteBuffer buffer) throws Exception {
		return session.codec.encode(buffer);
	}

	/**
	 * Returns {@code null} if there is no encoder
	 */
	protected List<Object> encode(IByteBufferHolder holder) throws Exception {
		return session.codec.encode(holder);
	}
	
	@Override
	public void run() {
		SessionFuturesController futures = session.futuresController;
		boolean withFuture = future != null;
		List<Object> out;
		int isBuffer = BYTES;
		
		try  {
			if (msg != null) {
				out = encode(msg);
				if (out == null) {
					if (withFuture) {
						future.setDelegate(futures.getCancelledFuture());
					}
					return;
				}
			}
			else if (bytes != null) {
				out = encode(bytes);
				isBuffer = BYTES;
			}
			else if (buffer != null) {
				out = encode(buffer);
				isBuffer = BUFFER;
			}
			else {
				out = encode(holder);
				isBuffer = HOLDER;
			}
		}
		catch (Exception e) {
			SessionIncident incident = SessionIncident.ENCODING_PIPELINE_FAILURE;

			if (withFuture) {
				future.setDelegate(futures.getFailedFuture(e));
			}
			if (!session.incident(incident, e)) {
				elogger.error(LOGGER, incident.defaultMessage(), session, e);
			}
			return;
		}

		setWriter(session.getEncodeTaskWriter());
		IFuture<Void> tmpFuture;
		
		try {
			if (out != null) {
				if (out.isEmpty()) {
					tmpFuture = withFuture ? futures.getSuccessfulFuture() : null;
				}
				else {
					tmpFuture = write(out, withFuture);
				}
			}
			else {
				switch (isBuffer) {
				case BUFFER:
					tmpFuture = write(buffer, withFuture);
					break;
				case BYTES:
					tmpFuture = write(bytes, withFuture);
					break;
				default:
					tmpFuture = write(holder, withFuture);
				}
			}
		}
		catch (Exception e) {
			tmpFuture = withFuture ? futures.getFailedFuture(e) : null;
		}

		if (withFuture) {
			future.setDelegate(tmpFuture);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(50);
		
		sb.append(getClass().getName());
		sb.append("[session=");
		sb.append(session);
		if (length == -1) {
			sb.append(" message");
		}
		else {
			sb.append(" length=");
			sb.append(length);
		}
		if (future != null) {
			sb.append(" future");
		}
		if (remoteAddress != null) {
			sb.append(" remote=");
			sb.append(remoteAddress.toString());
		}
		sb.append(']');
		return sb.toString();
	}
}
