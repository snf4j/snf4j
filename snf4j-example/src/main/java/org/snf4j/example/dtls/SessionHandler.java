/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2020 SNF4J contributors
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
package org.snf4j.example.dtls;

import java.net.SocketAddress;

import org.snf4j.core.EndingAction;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.ITimerTask;

public class SessionHandler extends AbstractDatagramHandler {

	final static Object RETRY_EVENT = new Object();
	
	final static int RETRY_COUNT = 5;
	
	final static int RETRY_TIMEOUT = 1000;
	
	final static Object IDLE_EVENT = new Object();
	
	final static int IDLE_TIMEOUT = 30000;
	
	final boolean clientMode;
	
	long sequence;

	long retries;
	
	ITimerTask retryTimer;
	
	ITimerTask idleTimer;
	
	int retryLeft;
	
	SessionHandler(boolean clientMode) {
		this.clientMode = clientMode;
	}
	
	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
	}

	@Override
	public void read(Object msg) {
		if (clientMode) {
			handleClient((Packet)msg);
		}
		else {
			handleServer((Packet)msg);
		}
	}	
	
	void setRetryTimer(boolean resetCounter) {
		if (resetCounter) {
			retryLeft = RETRY_COUNT;
		}
		if (retryTimer != null) {
			retryTimer.cancelTask();
		}
		retryTimer = getSession().getTimer().scheduleEvent(RETRY_EVENT, RETRY_TIMEOUT);
	}
	
	void setIdleTimer() {
		idleTimer = getSession().getTimer().scheduleEvent(IDLE_EVENT, 1000, 5000);
	}
	
	void cancelTimers() {
		if (retryTimer != null) {
			retryTimer.cancelTask();
		}
		if (idleTimer != null) {
			idleTimer.cancelTask();
		}
	}
	
	void handleClient(Packet packet) {
		if (packet.getSequence() == sequence) {
			getSession().writenf(new Packet(++sequence, SessionConfig.MAX_APPLICATION_DATA_SIZE));
			setRetryTimer(true);
			stats();
		}
	}
	
	void handleServer(Packet packet) {
		if (packet.getSequence() == sequence+1) {
			getSession().writenf(new Packet(++sequence, SessionConfig.MAX_APPLICATION_DATA_SIZE));
		}
	}
	
	@Override
	public void timer(Object event) {
		if (event == RETRY_EVENT) {
			++retries;
			retryTimer = null;
			if (retryLeft > 0) {
				--retryLeft;
				log("retry " + (RETRY_COUNT - retryLeft));
			}
			else {
				getSession().close();
				return;
			}
			getSession().writenf(new Packet(sequence, SessionConfig.MAX_APPLICATION_DATA_SIZE));
			setRetryTimer(false);
		}
		else if (event == IDLE_EVENT) {
			if (System.currentTimeMillis() - getSession().getLastReadTime() > IDLE_TIMEOUT) {
				log("idle");
				getSession().close();
			}
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			log("open");
			break;
			
		case READY:
			log("ready");
			if (clientMode) {
				getSession().writenf(new Packet(++sequence, SessionConfig.MAX_APPLICATION_DATA_SIZE));
				setRetryTimer(true);
			}
			else {
				setIdleTimer();
			}
			break;
			
		case CLOSED:
			log("closed");
			break;
			
		case ENDING:
			cancelTimers();
		}
	}
	
	@Override
	public void exception(Throwable e) {
		err(e);
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		err(incident + ": " + t);
		return true;
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return SessionStructureFactory.INSTANCE;
	}	
	
	@Override
	public ISessionConfig getConfig() {
		return new SessionConfig()
			.setOptimizeDataCopying(true)
			.setEndingAction(clientMode ? EndingAction.STOP : EndingAction.DEFAULT);
	}
	
	void log(Object s) {
		if (clientMode) {
			System.out.println("[INF] " + s);
		}
		else {
			System.out.println("[INF] " + getSession().getName() + ": "+ s);
		}
	}
	
	void err(Object s) {
		if (clientMode) {
			System.out.println("[ERR] " + s);
		}
		else {
			System.out.println("[ERR] " + getSession().getName() + ": "+ s);
		}
	}
	
	void stats() {
		if (sequence % 10000 == 0) {
			StringBuilder sb = new StringBuilder(100);
			
			sb.append("packets:");
			sb.append(sequence/1000);
			sb.append("K retries: ");
			sb.append(retries);
			sb.append(" bytes: ");
			sb.append(getSession().getReadBytes()/(1024*1024));
			sb.append("M speed: ");
			sb.append((long)getSession().getReadBytesThroughput()/1024);
			sb.append(" [KB/s]");
			log(sb.toString());
		}
	}
}
