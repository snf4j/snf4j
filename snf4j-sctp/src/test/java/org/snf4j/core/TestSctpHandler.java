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
import java.util.Map;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SctpNotificationType;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.DefaultSctpSessionConfig;
import org.snf4j.core.session.ISctpSession;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;

public class TestSctpHandler implements ISctpHandler {

	ICodecExecutor codec;
	
	Map<Long, ICodecExecutor> codecs;
	
	int minStreamNumber = 0;
	int maxStreamNumber = 10;
	int minProtoID = 0;
	int maxProtoID = 100;
	
	StringBuilder trace = new StringBuilder();
	
	void trace(String s) {
		trace.append(s);
	}
	
	String getTrace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	public TestSctpHandler() {	
	}
	
	public TestSctpHandler(ICodecExecutor codec) {
		this.codec = codec;
	}
	
	void addCodec(MessageInfo msgInfo, ICodecExecutor codec) {
		if (codecs == null) {
			codecs = new HashMap<Long, ICodecExecutor>();
		}
		Long key = ((long)msgInfo.payloadProtocolID() << 32) | (long)msgInfo.streamNumber();
		codecs.put(key, codec);
	}
	
	@Override
	public void setSession(ISession session) {
	}

	@Override
	public ISctpSession getSession() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void read(byte[] data) {
	}

	@Override
	public void read(ByteBuffer data) {
	}

	@Override
	public void read(Object msg) {
	}

	@Override
	public void event(SessionEvent event) {
	}

	@Override
	public void event(DataEvent event, long length) {
	}

	@Override
	public void exception(Throwable t) {
	}

	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		return false;
	}

	@Override
	public void timer(Object event) {
	}

	@Override
	public void timer(Runnable task) {
	}

	@Override
	public ISessionStructureFactory getFactory() {
		return DefaultSessionStructureFactory.DEFAULT;
	}

	@Override
	public void read(byte[] msg, MessageInfo msgInfo) {
		trace("B|"+ new String(msg) + "|" + msgInfo.streamNumber() + "|" + msgInfo.payloadProtocolID() + "|");
	}

	@Override
	public void read(ByteBuffer msg, MessageInfo msgInfo) {
		byte[] data = new byte[msg.remaining()];
		msg.get(data);
		trace("BB|"+ new String(data) + "|" + msgInfo.streamNumber() + "|" + msgInfo.payloadProtocolID() + "|");
	}

	@Override
	public ISctpSessionConfig getConfig() {
		return new DefaultSctpSessionConfig() {
			
			@Override
			public ICodecExecutor createCodecExecutor() {
				return codec;
			}
			
			@Override
			public Object getCodecExecutorIdentifier(MessageInfo msgInfo) {
				int streamNum = msgInfo.streamNumber();
				int protoID = msgInfo.payloadProtocolID();
				
				if (streamNum < minStreamNumber || streamNum > maxStreamNumber || protoID < minProtoID || protoID > maxProtoID) {
					return DEFAULT_CODEC_EXECUTOR_IDENTIFIER;
				}
				if (codecs == null) {
					return DEFAULT_CODEC_EXECUTOR_IDENTIFIER;
				}
				return ((long)msgInfo.payloadProtocolID() << 32) | (long)msgInfo.streamNumber();
			}

			@Override
			public ICodecExecutor createCodecExecutor(Object type) {
				return codecs.get(type);
			}
			
		};
	}

	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		trace("M|"+ msg + "|" + msgInfo.streamNumber() + "|" + msgInfo.payloadProtocolID() + "|");
	}

	@Override
	public HandlerResult notification(Notification notification, SctpNotificationType type) {
		trace("N|" + notification.getClass().getSimpleName()+"|"+type+"|");
		return null;
	}

}
