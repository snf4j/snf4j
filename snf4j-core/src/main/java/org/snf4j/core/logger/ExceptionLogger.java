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

/**
 * Utility class used by the API for producing exception logger based on
 * the runtime configuration. By default it produces the {@link DefaultExceptionLogger}
 * implementation but it can be changed by the user in the following way:  
 * <p>
 * Adding the system property <code>org.snf4j.ExceptionLogger</code> which
 * should point the full class name of a User's implementation of the
 * {@link IExceptionLogger} interface.
 *
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ExceptionLogger {

	private final static ILogger log = LoggerFactory.getLogger(ExceptionLogger.class);

	private static volatile IExceptionLogger instance;
	
	private ExceptionLogger() {
	}

	/**
	 * Gets the instance of the currently configured exception logger.
	 * 
	 * @return the instance of the exception logger
	 */
	public static IExceptionLogger getInstance() {
		if (instance == null) {
			synchronized (LoggerFactory.class) {
				if (instance == null) {
					instance = load();
				}
			}
		}
		return instance;
	}
	
	static IExceptionLogger load() {
		String className = System.getProperty(Constants.EXCEPTION_LOGGER_SYSTEM_PROERTY);
		IExceptionLogger logger = null;
		
		if (className != null) {
			try {
				logger = (IExceptionLogger) Class.forName(className).newInstance();
			} catch (Throwable e) {
				log.error("Unable to load external exception logger: {}", e.getMessage());
			}
		}
		return logger != null ? logger : new DefaultExceptionLogger();
	}

}
