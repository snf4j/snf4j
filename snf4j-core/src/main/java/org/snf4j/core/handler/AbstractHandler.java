/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
package org.snf4j.core.handler;

import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;

/**
 * Base implementation of the {@link IHandler} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public abstract class AbstractHandler implements IHandler {

	private ISession session;
	
	private String name;
	
	private ISessionConfig config = new DefaultSessionConfig();
	
	/**
	 * Default constructor creating an unnamed handler.
	 */
	protected AbstractHandler() {
	}
	
	/**
	 * Constructor creating a named handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractHandler(String name) {
		this.name = name;
	}
	
	@Override
	public void setSession(ISession session) {
		this.session = session;
	}
	
	@Override
	public ISession getSession() {
		return session;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public void event(SessionEvent event) {
	}
	
	@Override
	public void event(DataEvent event, long length) {
	}

	@Override
	public void exception(Throwable t) {
	}

	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		return false;
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return DefaultSessionStructureFactory.DEFAULT;
	}

	@Override
	public ISessionConfig getConfig() {
		return config;
	}
	
	@Override
	public void read(Object msg) {
	}

}
