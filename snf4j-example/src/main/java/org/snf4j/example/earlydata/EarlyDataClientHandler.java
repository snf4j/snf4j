package org.snf4j.example.earlydata;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.tls.engine.IEarlyDataHandler;

public class EarlyDataClientHandler extends EarlyDataHandler implements IEarlyDataHandler {

	private final String cmd;

	private Queue<byte[]> earlyData;
	
	private boolean earlyDataAccepted;
	
	public EarlyDataClientHandler(String cmd) {
		this.cmd = cmd;
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		super.event(event);

		if (!earlyDataAccepted) {
			switch (event) {
			case READY:
				getSession().writenf(cmd);
				break;
			}
		}
	}
	
	@Override
	public void read(Object msg) {
		Logger.inf("rsp: " + msg);
	}

	@Override
	public boolean hasEarlyData() {
		return true;
	}

	@Override
	public byte[] nextEarlyData(String protocol) {
		if (earlyData == null) {
			earlyData = new LinkedList<byte[]>();

			ISession session = getSession();

			try {
				ICodecExecutor executor = SessionConfig.createCodecExecutor(Integer.parseInt(protocol.substring(1)));

				executor.syncEncoders(session);
				for (Object data: executor.encode(session, cmd)) {
					ByteBuffer buffer = (ByteBuffer) data;
					byte[] bytes = new byte[buffer.remaining()];

					buffer.get(bytes);
					getSession().release(buffer);
					earlyData.add(bytes);
				}
			} catch (Exception e) {
				Logger.err(e.getMessage());
			}
		}
		return earlyData.poll();
	}

	@Override
	public void acceptedEarlyData() {
		Logger.inf("early data accepted");
		earlyDataAccepted = true;
	}

	@Override
	public void rejectedEarlyData() {
		Logger.inf("early data rejected");
	}
	
}
