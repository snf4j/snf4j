/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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

import java.nio.ByteBuffer;

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
	
	private final String name;
	
	private final ISessionConfig config;
	
	/**
	 * Default constructor creating an unnamed handler.
	 */
	protected AbstractHandler() {
		this(new DefaultSessionConfig());
	}
	
	/**
	 * Default constructor creating an unnamed handler with given session
	 * configuration object.
	 * 
	 * @param config the session configuration object, or {@code null} to
	 *               use the default configuration
	 */
	protected AbstractHandler(ISessionConfig config) {
		this.config = config != null ? config : new DefaultSessionConfig();
		this.name = null;
	}
	
	/**
	 * Constructor creating a named handler.
	 * 
	 * @param name
	 *            the name for this handler
	 */
	protected AbstractHandler(String name) {
		this(name, new DefaultSessionConfig());
	}
	
	/**
	 * Constructor creating a named handler with given session configuration object.
	 * 
	 * @param name   the name for this handler
	 * @param config the session configuration object, or {@code null} to
	 *               use the default configuration
	 */
	protected AbstractHandler(String name, ISessionConfig config) {
		this.name = name;
		this.config = config != null ? config : new DefaultSessionConfig();
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
	public void timer(Object event) {	
	}
	
	@Override
	public void timer(Runnable task) {
	}
	
	@Override
	public ISessionStructureFactory getFactory() {
		return DefaultSessionStructureFactory.DEFAULT;
	}

	@Override
	public ISessionConfig getConfig() {
		return config;
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it simply passes the {@code data} value to the
	 * {@link IHandler#read(Object)} method.
	 */
	@Override
	public void read(byte[] data) {
		read((Object)data);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * By default it simply passes the {@code data} value to the
	 * {@link IHandler#read(Object)} method.
	 */
	@Override
	public void read(ByteBuffer data) {
		read((Object)data);
	}
}
