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
package org.snf4j.core.logger.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.LoggerFactory;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.ILoggerFactory;

/**
 * A factory for the SLF4J logger.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
 public class Slf4jLoggerFactory implements ILoggerFactory {

	private final ConcurrentMap<String, ILogger> map = new ConcurrentHashMap<String, ILogger>();
			
	@Override
	public ILogger getLogger(String name) {
		ILogger logger = map.get(name);
		
		if (logger == null) {
			logger = new Slf4jLogger(LoggerFactory.getLogger(name));
			ILogger prevLogger = map.putIfAbsent(name, logger);
			if (prevLogger != null) {
				return prevLogger;
			}
		}
		return logger;
	}

}
