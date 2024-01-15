/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.longevity;

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.session.ISessionConfig;

public class ServerHandler extends AbstractStreamHandler implements Config {

	private final boolean client;
	
	protected ServerHandler(boolean client) {
		this.client = client;
	}
	
	public ServerHandler() {
		this(false);
	}
	
	@SuppressWarnings("unused")
	@Override
	public void read(Object msg) {
		if (SERVER_SLEEP_TIME > 0) {
			try {
				Thread.sleep(SERVER_SLEEP_TIME);
			} catch (InterruptedException e) {
			}
		}
		getSession().writenf(msg);
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case CREATED:
			Controller.sessionCreated(getSession(), client);
			break;
			
		case ENDING:
			Controller.sessionEnding(getSession(), client);
			break;			
		}
	}	
	
	@Override
	public void event(DataEvent event, long length) {
		switch (event) {
		case RECEIVED:
			Metric.dataReceived(getSession(), length);
			break;
			
		case SENT:
			Metric.dataSent(getSession(), length);
			break;
		}
	}
	
	@Override
	public ISessionConfig getConfig() {
		return new SessionConfig().setOptimizeDataCopying(true);
	}	
	
	@Override
	public ISessionStructureFactory getFactory() {
		return new SessionStructureFactory(client);
	}
	
	@Override
	public void exception(Throwable t) {
		Metric.incExceptions();
		t.printStackTrace();
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		Metric.incIncidents();
		t.printStackTrace();
		return super.incident(incident, t);
	}

}
