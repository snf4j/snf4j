package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.MessageInfo;

public class SctpEncodeTask extends EncodeTask {
	
	MessageInfo msgInfo;
	
	private ISctpEncodeTaskWriter writer;
	
	SctpEncodeTask(InternalSession session, byte[] bytes) {
		super(session, bytes);
	}	
	
	SctpEncodeTask(InternalSession session, byte[] bytes, int offset, int length) {
		super(session, bytes, offset, length);
	}
	
	SctpEncodeTask(InternalSession session, ByteBuffer buffer) {
		super(session, buffer);
	}
	
	SctpEncodeTask(InternalSession session, ByteBuffer buffer, int length) {
		super(session, buffer, length);
	}
	
	SctpEncodeTask(InternalSession session, Object msg) {
		super(session, msg);
	}
	
	final void registernf(MessageInfo msgInfo) {
		this.msgInfo = msgInfo;
		registernf();
	}
	
	final IFuture<Void> register(MessageInfo msgInfo) {
		this.msgInfo = msgInfo;
		return register();
	}
	
	protected void setWriter(IEncodeTaskWriter writer) {
		this.writer = (ISctpEncodeTaskWriter) writer;
	}
	
	@Override
	protected IFuture<Void> write(byte[] data, boolean withFuture) {
		return writer.write(msgInfo, data, withFuture);
	}
	
	@Override
	protected IFuture<Void> write(ByteBuffer data, boolean withFuture) {
		return writer.write(msgInfo, data, withFuture);
	}
	
	@Override
	protected List<Object> encode(Object msg) throws Exception {
		return ((SctpCodecExecutorAdapter)session.codec).encode(msg, msgInfo);
	}
	
	@Override
	protected List<Object> encode(byte[] bytes) throws Exception {
		return ((SctpCodecExecutorAdapter)session.codec).encode(bytes, msgInfo);
	}
	
	@Override
	protected List<Object> encode(ByteBuffer buffer) throws Exception {
		return ((SctpCodecExecutorAdapter)session.codec).encode(buffer, msgInfo);
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
		if (msgInfo != null) {
			sb.append(" stream=");
			sb.append(msgInfo.streamNumber());
			sb.append(" protocol=");
			sb.append(msgInfo.payloadProtocolID());
		}
		sb.append(']');
		return sb.toString();
	}
	
}
