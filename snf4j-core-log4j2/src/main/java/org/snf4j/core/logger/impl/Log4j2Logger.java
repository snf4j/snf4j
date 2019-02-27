/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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

import org.apache.logging.log4j.Logger;
import org.snf4j.core.logger.ILogger;

class Log4j2Logger implements ILogger {
	
	private final Logger logger;
	
	Log4j2Logger(Logger logger) {
		this.logger = logger;
	}
	
	@Override
	public boolean isDebugEnabled() {
		return logger.isDebugEnabled();
	}

	@Override
	public boolean isTraceEnabled() {
		return logger.isTraceEnabled();
	}

	@Override
	public void debug(String msg) {
		logger.debug(msg);
	}

	@Override
	public void debug(String msg, Object arg) {
		logger.debug(msg, arg);
	}

	@Override
	public void debug(String msg, Object arg1, Object arg2) {
		logger.debug(msg, arg1, arg2);
	}

	@Override
	public void debug(String msg, Object... args) {
		logger.debug(msg, args);
	}

	@Override
	public void trace(String msg) {
		logger.trace(msg);
	}

	@Override
	public void trace(String msg, Object arg) {
		logger.trace(msg, arg);
	}

	@Override
	public void trace(String msg, Object arg1, Object arg2) {
		logger.trace(msg, arg1, arg2);
	}

	@Override
	public void trace(String msg, Object... args) {
		logger.trace(msg, args);
	}

	@Override
	public void warn(String msg) {
		logger.warn(msg);
	}

	@Override
	public void warn(String msg, Object arg) {
		logger.warn(msg, arg);
	}

	@Override
	public void warn(String msg, Object arg1, Object arg2) {
		logger.warn(msg, arg1, arg2);
	}

	@Override
	public void warn(String msg, Object... args) {
		logger.warn(msg, args);
	}

	@Override
	public void error(String msg) {
		logger.error(msg);
	}

	@Override
	public void error(String msg, Object arg) {
		logger.error(msg, arg);
	}

	@Override
	public void error(String msg, Object arg1, Object arg2) {
		logger.error(msg, arg1, arg2);
	}

	@Override
	public void error(String msg, Object... args) {
		logger.error(msg, args);
	}

	@Override
	public void info(String msg) {
		logger.info(msg);
	}

	@Override
	public void info(String msg, Object arg) {
		logger.info(msg, arg);
	}

	@Override
	public void info(String msg, Object arg1, Object arg2) {
		logger.info(msg, arg1, arg2);
	}

	@Override
	public void info(String msg, Object... args) {
		logger.info(msg, args);
	}

}
