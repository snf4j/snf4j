package org.snf4j.example.sctp;

import java.nio.ByteBuffer;

import org.snf4j.core.EndingAction;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;

public class SctpClientHandler extends SctpServerHandler {
	
	int statsCount;
	
	long totalSize;
	
	int msgCounter;
	
	int[] msgCounters = new int[3];
	
	String[] streams = new String[] {"compressed", "unordered", "other"};
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			getSession().writenf(msg(), randomStream());
			break;
			
		}
	}
	@Override
	public void read(Object msg, MessageInfo msgInfo) {
		stats(((ByteBuffer) msg).remaining(), msgInfo);
		super.read(msg, msgInfo);
	}
	
	void stats(int msgSize, MessageInfo msgInfo) {
		totalSize += msgSize;
		msgCounter++;
		if (msgInfo.streamNumber() == COMPRESSING.streamNumber()) {
			if (msgInfo.isUnordered()) {
				msgCounters[1]++;
			}
			else {
				msgCounters[0]++;
			}
		}
		else {
			msgCounters[2]++;
		}
		if (statsCount++ > 10000) {
			StringBuilder sb = new StringBuilder();

			sb.append("total msgs:");
			sb.append(msgCounter/1000);
			sb.append("K\t");
			for (int i=0; i<streams.length; ++i) {
				sb.append(streams[i]);
				sb.append(" msgs:");
				sb.append(msgCounters[i]/1000);
				sb.append("K\t");
			}
			System.out.println(sb);
			statsCount = 0;
		}
	}
	
	@Override
	public ISctpSessionConfig getConfig() {
		return (ISctpSessionConfig) new SessionConfig()
				.setEndingAction(EndingAction.STOP)
				.setOptimizeDataCopying(true)
				.setMinOutBufferCapacity(SctpClient.SIZE << 1);
	}
}
