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
package org.snf4j.example.sctp;

import java.nio.ByteBuffer;

import org.snf4j.core.EndingAction;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;

public class SctpClientHandler extends SctpServerHandler {
	
	private int statsCount;
	
	private int msgCounter;
	
	private int[] msgCounters = new int[3];
	
	private final static String[] streams = new String[] {"compressed", "encoded(unordered)", "other"};
	
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
		msgCounter++;
		if (msgInfo.streamNumber() == SessionConfig.CODEC_STREAM_NUMBER) {
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

			sb.append("messages: ");
			sb.append("total=");
			sb.append(msgCounter/1000);
			sb.append("K\t");
			for (int i=0; i<streams.length; ++i) {
				sb.append(streams[i]);
				sb.append("=");
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
