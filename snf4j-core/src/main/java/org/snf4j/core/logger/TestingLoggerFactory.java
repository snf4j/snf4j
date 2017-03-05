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
package org.snf4j.core.logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class TestingLoggerFactory implements ILoggerFactory {

	private final ConcurrentMap<String, ILogger> map = new ConcurrentHashMap<String, ILogger>();

	@Override
	public ILogger getLogger(String name) {
		ILogger logger = map.get(name);
		
		System.err.println("TEST1: " + logger);
		if (logger == null) {
			System.err.println("TEST2: " + logger);
			logger = new TestingLogger(name);
			System.err.println("TEST3: " + logger);
			ILogger prevLogger = map.putIfAbsent(name, logger);
			System.err.println("TEST4: " + prevLogger);
			if (prevLogger != null) {
				System.err.println("TEST5: " + prevLogger);
				return prevLogger;
			}
		}
		System.err.println("TEST6: " + logger);
		return logger;
	}

}
