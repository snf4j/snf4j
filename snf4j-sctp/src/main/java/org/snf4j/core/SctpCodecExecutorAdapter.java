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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;

class SctpCodecExecutorAdapter extends CodecExecutorAdapter implements ISctpReader {

	private Map<Long, ICodecExecutor> executors;
	
	private final int minStreamNum;
	
	private final int maxStreamNum;
	
	private final int minProtoID;
	
	private final int maxProtoID;
	
	private final ISctpSessionConfig config;
	
	SctpCodecExecutorAdapter(ICodecExecutor executor, ISctpHandler handler) {
		super(executor);
		config = handler.getConfig();
		minStreamNum = config.getMinSctpStreamNumber();
		maxStreamNum = config.getMaxSctpStreamNumber();
		minProtoID = config.getMinSctpPayloadProtocolID();
		maxProtoID = config.getMaxSctpPayloadProtocolID();
	}
	
	void setSession(ISctpSession session) {
		this.session = session;
	}

	final static Long key(int streamNum, int protoID) {
		return new Long(((long)streamNum << 32) | ((long)protoID & 0xffffffffL));
	}
	
	ICodecExecutor getExecutor(MessageInfo msgInfo) {
		int streamNum = msgInfo.streamNumber();
		int protoID = msgInfo.payloadProtocolID();
		
		if (streamNum < minStreamNum || streamNum > maxStreamNum || protoID < minProtoID || protoID > maxProtoID) {
			return executor;
		}

		Long key = key(streamNum, protoID);
		ICodecExecutor executor;
		
		if (executors == null) {
			executors = new HashMap<Long, ICodecExecutor>();
			executor = null;
		}
		else {
			executor = executors.get(key);
		}
		
		if (executor == null) {
			executor = config.createCodecExecutor(msgInfo);
			if (executor == null) {
				executor = SctpNopCodecExecutor.INSTANCE;
			}
			else {
				this.executor.addChild(session, executor);
			}
			executors.put(key, executor);
		}
		return executor;
	}
	
	final List<Object> encode(ByteBuffer data, MessageInfo msgInfo) throws Exception {
		ICodecExecutor executor = getExecutor(msgInfo);
		
		executor.syncEncoders();
		return executor.encode(session, data);
	}
	
	final List<Object> encode(byte[] data, MessageInfo msgInfo) throws Exception {	
		ICodecExecutor executor = getExecutor(msgInfo);
		
		executor.syncEncoders();
		return executor.encode(session, data);
	}
	
	final List<Object> encode(Object msg, MessageInfo msgInfo) throws Exception {	
		ICodecExecutor executor = getExecutor(msgInfo);
		
		executor.syncEncoders();
		return executor.encode(session, msg);
	}
	
	@Override
	public void read(byte[] msg, MessageInfo msgInfo) {
		ICodecExecutor executor = getExecutor(msgInfo);
		List<Object> out;	
		
		executor.syncDecoders();
		try {
			out = executor.decode(session, msg);
		}
		catch (Exception e) {
			throw new PipelineDecodeException((InternalSession) session, e);
		}
		
		ISctpHandler handler = (ISctpHandler) session.getHandler();
		
		if (out == null) {
			handler.read(msg, msgInfo);
			return;
		}
		read(msgInfo, out, handler);
	}

	@Override
	public void read(ByteBuffer msg, MessageInfo msgInfo) {
		ICodecExecutor executor = getExecutor(msgInfo);
		List<Object> out;	
		
		executor.syncDecoders();
		try {
			out = executor.decode(session, msg);
		}
		catch (Exception e) {
			throw new PipelineDecodeException((InternalSession) session, e);
		}
		
		ISctpHandler handler = (ISctpHandler) session.getHandler();
		
		if (out == null) {
			handler.read(msg, msgInfo);
			return;
		}
		read(msgInfo, out, handler);
	}

	private void read(MessageInfo msgInfo, List<Object> out, ISctpHandler handler) {
		if (out.isEmpty()) {
			return;
		}
		
		Iterator<Object> i = out.iterator();
		Object o = i.next();
		
		if (o.getClass() == byte[].class) {
			handler.read((byte[])o, msgInfo);
			while (i.hasNext()) {
				handler.read((byte[])i.next(), msgInfo);
			}
		}
		else if (o instanceof ByteBuffer) {
			handler.read((ByteBuffer)o, msgInfo);
			while (i.hasNext()) {
				handler.read((ByteBuffer)i.next(), msgInfo);
			}
		}
		else {
			handler.read(o, msgInfo);
			while (i.hasNext()) {
				handler.read(i.next(), msgInfo);
			}
		}		
	}
	
}
