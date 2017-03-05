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
 * Additional logger interface that is used by the API to log messages for
 * caught exceptions. It is provided to allow the user to customize the way
 * exceptions are to be logged.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IExceptionLogger {
	
	/**
	 * Logs a message at the TRACE level with varying number of arguments.
	 * 
	 * @param logger the logger that is currently used by the API
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments with an exception object at the last position
	 */
	void trace(ILogger logger, String msg, Object... args);

	/**
	 * Logs a message at the DEBUG level with varying number of arguments.
	 * 
	 * @param logger the logger that is currently used by the API
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments with an exception object at the last position
	 */
	void debug(ILogger logger, String msg, Object... args);
	
	/**
	 * Logs a message at the INFO level with varying number of arguments.
	 * 
	 * @param logger the logger that is currently used by the API
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments with an exception object at the last position
	 */
	void info(ILogger logger, String msg, Object... args);
	
	/**
	 * Logs a message at the WARN level with varying number of arguments.
	 * 
	 * @param logger the logger that is currently used by the API
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments with an exception object at the last position
	 */
	void warn(ILogger logger, String msg, Object... args);
	
	/**
	 * Logs a message at the ERROR level with varying number of arguments.
	 * 
	 * @param logger the logger that is currently used by the API
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments with an exception object at the last position
	 */
	void error(ILogger logger, String msg, Object... args);
}
