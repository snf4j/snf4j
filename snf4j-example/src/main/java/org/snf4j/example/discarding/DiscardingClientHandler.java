/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.example.discarding;

import org.snf4j.core.ClosingAction;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISessionConfig;

public class DiscardingClientHandler extends AbstractStreamHandler {

	private byte[] data = new byte[DiscardingClient.SIZE];
	
	@Override
	public void read(byte[] data) {
		//Discarding all read bytes
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			getSession().write(data);
			break;
			
		case ENDING:
			System.out.println("Max throughput [bytes/secs]: " + (long)maxThroughput);
			break;
		}
	}
	
	long dataSent;
	long totalDataSent;
	double maxThroughput;
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(DataEvent event, long size) {
		switch (event) {
		case SENT:
			double throughput = getSession().getWrittenBytesThroughput();
			
			if (throughput > maxThroughput) {
				maxThroughput = throughput;
			}
			
			totalDataSent += size;
			if (totalDataSent >= DiscardingClient.TOTAL_SIZE) {
				getSession().close();
			}
			else {
				dataSent += size;
				if (size == data.length) {
					dataSent = 0;
					getSession().write(data);
				}
			}
			break;
		}
	}

	@Override
	public ISessionConfig getConfig() {
		return new DefaultSessionConfig()
				.setClosingAction(ClosingAction.STOP);
	}
}
