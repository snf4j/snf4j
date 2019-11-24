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
package org.snf4j.core.logger.impl;

import org.snf4j.core.logger.ILoggerFactory;

/**
 * The binder class used by the {@link org.snf4j.core.logger.LoggerFactory
 * LoggerFactory} utility as a way to produce an actual instance of the
 * {@link org.snf4j.core.logger.ILoggerFactory ILoggerFactory} interface that
 * will be used by the API to create the logger.
 * <p>
 * It is provided to allow the user to choose the logger provider that should by
 * used by the API for logging purposes.
 * <p>
 * This class is not implemented by the core API.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class LoggerFactoryBinder {
	
	private final static LoggerFactoryBinder INSTANCE = new LoggerFactoryBinder();
	
	/**
	 * Gets the singleton of this class.
	 * 
	 * @return the singleton of this class
	 */
	public final static LoggerFactoryBinder getInstance() {
		return INSTANCE;
	}
	
	private LoggerFactoryBinder() {
	}
	
	/**
	 * Gets the actual implementation of the
	 * {@link org.snf4j.core.logger.ILoggerFactory ILoggerFactory} interface
	 * that should be used by the API for logging purposes.
	 * 
	 * @return the logger factory
	 */
	public ILoggerFactory getFactory() {
		throw new UnsupportedOperationException("This code should have never been incorporated into the API");
	}
}
