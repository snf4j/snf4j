package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

class SctpNopCodecExecutor implements ICodecExecutor {

	static final ICodecExecutor INSTANCE = new SctpNopCodecExecutor();
	
	private SctpNopCodecExecutor() {		
	}
	
	@Override
	public ICodecPipeline getPipeline() {
		return null;
	}

	@Override
	public void syncDecoders() {
	}

	@Override
	public void syncEncoders() {
	}

	@Override
	public void syncEventDrivenCodecs(ISession session) {
	}

	@Override
	public IBaseDecoder<?> getBaseDecoder() {
		return null;
	}

	@Override
	public boolean hasDecoders() {
		return false;
	}

	@Override
	public List<Object> encode(ISession session, ByteBuffer data) throws Exception {
		return null;
	}

	@Override
	public List<Object> encode(ISession session, byte[] data) throws Exception {
		return null;
	}

	@Override
	public List<Object> encode(ISession session, Object msg) throws Exception {
		return null;
	}

	@Override
	public List<Object> decode(ISession session, byte[] data) throws Exception {
		return null;
	}

	@Override
	public List<Object> decode(ISession session, ByteBuffer data) throws Exception {
		return null;
	}

	@Override
	public void event(ISession session, SessionEvent event) {
	}

}
