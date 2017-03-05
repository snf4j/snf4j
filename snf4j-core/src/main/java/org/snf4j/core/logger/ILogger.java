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
 * Logger interface that is used by the API to log messages. It separates the
 * core API from any specific logger implementation.	
 * <p>
 * Typical usage pattern:
 * <pre>
 * import org.snf4j.core.logger.ILogger;
 * import org.snf4j.core.logger.LoggerFactory;
 * 
 * class Foo {
 *     private ILogger log = LoggerFactory.getLogger(Foo.class);
 *     
 *     Foo(int arg1, Object arg2) {
 *         log.debug("Foo created with args: {} {}", arg1, arg2);
 *     }
 * }
 * </pre>
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ILogger {
    
	/**
	 * Tells if the logger is enabled for the DEBUG level.
	 * 
	 * @return <code>true</code> if the logger is enabled for the DEBUG level
	 */
	boolean isDebugEnabled();

	/**
	 * Tells if the logger is enabled for the TRACE level.
	 * 
	 * @return <code>true</code> if the logger is enabled for the TRACE level
	 */
    boolean isTraceEnabled();
    
	/**
	 * Logs a message at the DEBUG level.
	 * 
	 * @param msg
	 *            the message to be logged
	 */
    void debug(String msg);
    
	/**
	 * Logs a message at the DEBUG level with one argument.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg
	 *            the argument
	 */
    void debug(String msg, Object arg);
    
	/**
	 * Logs a message at the DEBUG level with two arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg1
	 *            the first argument
	 * @param arg2
	 *            the second argument
	 */
    void debug(String msg, Object arg1, Object arg2);
    
	/**
	 * Logs a message at the DEBUG level with three or more arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments
	 */
    void debug(String msg, Object... args);
    
	/**
	 * Logs a message at the TRACE level.
	 * 
	 * @param msg
	 *            the message to be logged
	 */
    void trace(String msg);
    
	/**
	 * Logs a message at the TRACE level with one argument.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg
	 *            the argument
	 */
    void trace(String msg, Object arg);
    
	/**
	 * Logs a message at the TRACE level with two arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg1
	 *            the first argument
	 * @param arg2
	 *            the second argument
	 */
    void trace(String msg, Object arg1, Object arg2);

	/**
	 * Logs a message at the TRACE level with three or more arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments
	 */
    void trace(String msg, Object... args);
    
	/**
	 * Logs a message at the WARN level.
	 * 
	 * @param msg
	 *            the message to be logged
	 */
    void warn(String msg);
    
	/**
	 * Logs a message at the WARN level with one argument.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg
	 *            the argument
	 */
    void warn(String msg, Object arg);
    
	/**
	 * Logs a message at the WARN level with two arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg1
	 *            the first argument
	 * @param arg2
	 *            the second argument
	 */
    void warn(String msg, Object arg1, Object arg2);

	/**
	 * Logs a message at the WARN level with three or more arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments
	 */
    void warn(String msg, Object... args);

	/**
	 * Logs a message at the ERROR level.
	 * 
	 * @param msg
	 *            the message to be logged
	 */
    void error(String msg);
    
	/**
	 * Logs a message at the ERROR level with one argument.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg
	 *            the argument
	 */
    void error(String msg, Object arg);
    
	/**
	 * Logs a message at the ERROR level with two arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg1
	 *            the first argument
	 * @param arg2
	 *            the second argument
	 */
    void error(String msg, Object arg1, Object arg2);
    
	/**
	 * Logs a message at the ERROR level with three or more arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments
	 */
    void error(String msg, Object... args);

	/**
	 * Logs a message at the INFO level.
	 * 
	 * @param msg
	 *            the message to be logged
	 */
    void info(String msg);
    
	/**
	 * Logs a message at the INFO level with one argument.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg
	 *            the argument
	 */
    void info(String msg, Object arg);
    
	/**
	 * Logs a message at the INFO level with two arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param arg1
	 *            the first argument
	 * @param arg2
	 *            the second argument
	 */
    void info(String msg, Object arg1, Object arg2);
    
	/**
	 * Logs a message at the INFO level with three or more arguments.
	 * 
	 * @param msg
	 *            a pattern of the message to be logged that uses the
	 *            <code>{}</code> characters to indicate the place for the
	 *            appropriate argument
	 * @param args
	 *            the arguments
	 */
    void info(String msg, Object... args);
}
