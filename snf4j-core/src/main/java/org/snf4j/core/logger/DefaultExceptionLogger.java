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

/**
 * Default implementation of the {@link IExceptionLogger} interface. It only
 * logs the detail message string of the <code>Throwable</code> instance passed
 * as the last argument.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultExceptionLogger implements IExceptionLogger {

	/**
	 * Constructs the exception logger.
	 */
	protected DefaultExceptionLogger() {
	}
	
	/**
	 * Prepares an array of arguments that will be passed to the logger
	 * currently used by the API.
	 * 
	 * @param args
	 *            the original array of the arguments passed by the API. The
	 *            last argument in this array is usually an exception object.
	 * @return the prepared array of arguments
	 */
	protected Object[] prepareArguments(Object[] args) {
		if (args != null && args.length > 0) {
			int last = args.length - 1;
			Object lastArg = args[last];
			
			if (lastArg instanceof Throwable) {
				args = args.clone();
				args[last] = ((Throwable)lastArg).toString();
			}
		}
		return args;
	}
	
	@Override
	public void trace(ILogger loggger, String msg, Object... args) {
		loggger.trace(msg, prepareArguments(args));
	}

	@Override
	public void debug(ILogger logger, String msg, Object... args) {
		logger.debug(msg, prepareArguments(args));
	}

	@Override
	public void info(ILogger logger, String msg, Object... args) {
		logger.info(msg, prepareArguments(args));
	}

	@Override
	public void warn(ILogger logger, String msg, Object... args) {
		logger.warn(msg, prepareArguments(args));
	}

	@Override
	public void error(ILogger logger, String msg, Object... args) {
		logger.error(msg, prepareArguments(args));
	}
}
