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

import org.snf4j.core.Constants;
import org.snf4j.core.logger.impl.LoggerFactoryBinder;

/**
 * Utility class used by the API for producing an actual logger based on the 
 * runtime configuration. By default it produces the {@link NopLogger} 
 * implementation but it can be changed by the user in the following ways:
 * <p>
 * a) Adding the system property <code>org.snf4j.LoggerFactory</code> which
 * should point the full class name of a User's implementation of the
 * {@link ILoggerFactory} interface.
 * <p>
 * b) Implementing the {@link org.snf4j.core.logger.impl.LoggerFactoryBinder}
 * logger factory binder which can be used to produce the logger implementation.
 * The core API does not implement it.
 *
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class LoggerFactory {
	
	final static String BINDER_CLASS = "org.snf4j.core.logger.impl.LoggerFactoryBinder";
	
	private static volatile ILoggerFactory factory;
	
	private LoggerFactory() {
	}
	
	/**
	 * Returns an instance of the {@link ILogger} interface that is appropriate for
	 * the specified class.
	 * 
	 * @param clazz
	 *            the class for the logger.
	 * @return an instance of the logger
	 */
	public static ILogger getLogger(Class<?> clazz) {
		return getFactory().getLogger(clazz.getName());
	}
	
	private static ILoggerFactory getFactory() {
		if (factory == null) {
			synchronized (LoggerFactory.class) {
				if (factory == null) {
					factory = load();
					report("Factory: " + factory.getClass().getName() + " ", new Exception(""));
					report("Factory: " + factory.getLogger("XXX") + " ", new Exception(""));
					report("Factory: " + System.getProperty(Constants.LOGGER_FACTORY_SYSTEM_PROERTY) + " ", new Exception(""));
				}
			}
		}
		return factory;
	}
	
	static ILoggerFactory load() {
		String className = System.getProperty(Constants.LOGGER_FACTORY_SYSTEM_PROERTY);
		ILoggerFactory logger = null;
		
		if (className != null) {
			try {
				logger = (ILoggerFactory) Class.forName(className).newInstance();
			} catch (Throwable e) {
				report("Unable to load external logger factory: ", e);
			}
		}
		
		if (logger == null) {
			try {
				Class.forName(BINDER_CLASS);
				logger = LoggerFactoryBinder.getInstance().getFactory();
			} catch (ClassNotFoundException e) {
				//Ignore
			} catch (Throwable e) {
				report("Unable to load logger factory binder: ", e);
			}
		}
		
		return logger != null ? logger : new NopLoggerFactory();
	}

	static void report(String msg, Throwable e) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(Constants.SHORT_NAME);
		sb.append(": ");
		sb.append(msg);
		sb.append(e);
		System.err.println(sb.toString());
	}
}
