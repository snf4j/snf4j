package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.DefalutSctpSessionConfig;
import org.snf4j.core.session.ISctpSessionConfig;
import org.snf4j.core.session.ISession;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.NotificationHandler;

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
	
	TestSctpHandler() {	
	}
	
	TestSctpHandler(ICodecExecutor codec) {
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
	public ISession getSession() {
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
		return new DefalutSctpSessionConfig() {
			
			@Override
			public ICodecExecutor createCodecExecutor() {
				return codec;
			}
			
			@Override
			public ICodecExecutor createCodecExecutor(MessageInfo msgInfo) {
				if (codecs == null) {
					return codec;
				}
				Long key = ((long)msgInfo.payloadProtocolID() << 32) | (long)msgInfo.streamNumber();
				return codecs.get(key);
			}
			
		}.setMinSctpStreamNumber(minStreamNumber)
				.setMaxSctpStreamNumber(maxStreamNumber)
				.setMinSctpPayloadProtocolID(minProtoID)
				.setMaxSctpPayloadProtocolID(maxProtoID);
	}

	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		trace("M|"+ msg + "|" + msgInfo.streamNumber() + "|" + msgInfo.payloadProtocolID() + "|");
	}

	@Override
	public Object getNotificationAttachment() {
		return null;
	}

	@Override
	public NotificationHandler<Object> getNotificationHandler() {
		return null;
	}

}
