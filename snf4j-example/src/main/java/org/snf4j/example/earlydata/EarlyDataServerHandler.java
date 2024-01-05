package org.snf4j.example.earlydata;

import java.time.Instant;

import org.snf4j.core.handler.SessionEvent;

public class EarlyDataServerHandler extends EarlyDataHandler {

	private String cmd;
	
	@Override
	public void read(Object msg) {
		Logger.inf("cmd: " + msg);
		cmd = (String) msg;
		
		if (getSession().getReadyFuture().isSuccessful()) {
			processCmd(true);
			cmd = null;
		}
	}
	
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		super.event(event);
		
		switch(event) {
		case READY:
			if (cmd != null) {
				processCmd(true);
			}
		}
	}
	
	private void processCmd(boolean close) {
		String response;

		if ("time".equals(cmd)) {
			response = Instant.now().toString();
		}
		else {
			response = "unknown cmd";
		}
		cmd = null;
		getSession().writenf(response);
		if (close) {
			getSession().close();
		}
	}
}
