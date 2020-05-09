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
package org.snf4j.example.heartbeat;

import java.net.SocketAddress;

import org.snf4j.core.EndingAction;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.timer.ITimer;
import org.snf4j.core.timer.ITimerTask;

public class HeartbeatHandler extends AbstractDatagramHandler {

	private ITimerTask beatTimer;
	
	private ITimerTask downTimer;
	
	private final ITimer timer;
	
	private State state;
	
	private final long beatPeriod;
	
	private final long downPeriod;
	
	private final SocketAddress remoteAddress;
	
	enum State {UNKNOWN, UP, DOWN}
	
	public HeartbeatHandler(ITimer timer, long beatPeriod, long downPeriod) {
		this(null, timer, beatPeriod, downPeriod);
	}
	
	public HeartbeatHandler(SocketAddress remoteAddress, ITimer timer, long beatPeriod, long downPeriod) {
		this.timer = timer;
		this.beatPeriod = beatPeriod;
		this.downPeriod = downPeriod;
		this.remoteAddress = remoteAddress;
	}
	
	@Override
	public void read(byte[] data) {
	}
	
	@Override
	public void read(SocketAddress remoteAddress, byte[] datagram) {
	}

	@Override
	public void read(Object msg) {
		up();
	}
	
	@Override
	public void read(SocketAddress remoteAddress, Object msg) {
		if (remoteAddress.equals(this.remoteAddress)) {
			up();
		}
	}
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case OPENED:
			up();
			break;
			
		case READY:
			beatTimer = getSession().getTimer().scheduleTask(
				new Runnable() {

					@Override
					public void run() {
						beat();
					}
					
				}, 0, beatPeriod, true);
			break;
			
		case CLOSED:
			if (beatTimer != null) {
				beatTimer.cancelTask();
			}
			if (downTimer != null) {
				downTimer.cancelTask();
			}
			break;
		}
	}

	@Override
	public void timer(Runnable task) {
		task.run();
	}
	
	@Override
	public ISessionConfig getConfig() {
		return new SessionConfig().setEndingAction(EndingAction.STOP);
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new DefaultSessionStructureFactory() {
			
			@Override
			public ITimer getTimer() {
				return timer;
			}			
		};
	}
	
	private void beat() {
		if (remoteAddress != null) {
			getSession().send(remoteAddress, Packet.INSTANCE);
		}
		else {
			getSession().write(Packet.INSTANCE);
		}
	}
	
	private void up() {
		if (downTimer != null) {
			downTimer.cancelTask();
			update(state, State.UP);
		}
		else if (remoteAddress != null){
			update(state, State.UNKNOWN);
		}
		downTimer = getSession().getTimer().scheduleTask(
			new Runnable() {

				@Override
				public void run() {
					down();
				}
				
			}, downPeriod, true);
	}
	
	private void down() {
		if (getSession().getParent() != null) {
			getSession().close();
		}
		update(state, State.DOWN);
	}
	
	private void update(State oldState, State newState) {
		if (oldState != newState) {
			SocketAddress address = remoteAddress == null ? getSession().getRemoteAddress() : remoteAddress;
			System.out.println("[" + address +"] " + newState);
			state = newState;
		}
	}

}
