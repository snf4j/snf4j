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

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


class TestingLogger implements ILogger {

	private static final int TRACE = 4; 
	private static final int DEBUG = 3;
	private static final int INFO = 2;
	private static final int WARN = 1;
	private static final int ERROR = 0;
	
	private static final String[] levels = new String[] {"ERROR"," WARN"," INFO","DEBUG","TRACE"};
	
	private final String name;
	
	private PrintStream out = System.err;
	
	private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	
	TestingLogger(String name) {
		this.name = name;
	}
	
	private final void log0(int level, String msg, Throwable t) {
		StringBuilder sb = new StringBuilder(msg.length() + 50);
		String time;
		
		synchronized (dateFormatter) {
			time = dateFormatter.format(new Date());
		}
		sb.append(time);
		sb.append(" [");
		sb.append(Thread.currentThread().getName());
		sb.append("] ");
		sb.append(levels[level]);
		sb.append(" ");
		sb.append(name);
		sb.append(" - ");
		sb.append(msg);
		out.println(sb.toString());
	}

	private final static String format(String msg, Object[] args) {
		StringBuilder sb = new StringBuilder(msg.length() + 50);
		
		sb.append(msg);
		for (int i=0; i<args.length; ++i) {
			Object arg = args[i];
			sb.append(" [");
			sb.append(arg == null ? null : arg.toString());
			sb.append(']');
		}
		return sb.toString();
	}
	
	private void log(int level, String msg, Object arg) {
		log0(level, format(msg, new Object[] {arg}), null);
	}

	private void log(int level, String msg, Object arg1, Object arg2) {
		log0(level, format(msg, new Object[] {arg1, arg2}), null);
	}
	
	private void log(int level, String msg, Object... args) {
		log0(level, format(msg, args), null);
	}
	
	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		return true;
	}
	
	@Override
	public void debug(String msg) {
		log0(DEBUG, msg, null);
	}

	@Override
	public void debug(String msg, Object arg) {
		log(DEBUG, msg, arg);
	}

	@Override
	public void debug(String msg, Object arg1, Object arg2) {
		log(DEBUG, msg, arg1, arg2);
	}

	@Override
	public void debug(String msg, Object... args) {
		log(DEBUG, msg, args);
	}

	@Override
	public void trace(String msg) {
		log0(TRACE, msg, null);
	}

	@Override
	public void trace(String msg, Object arg) {
		log(TRACE, msg, arg);
	}

	@Override
	public void trace(String msg, Object arg1, Object arg2) {
		log(TRACE, msg, arg1, arg2);
	}

	@Override
	public void trace(String msg, Object... args) {
		log(TRACE, msg, args);
	}
	
	@Override
	public void warn(String msg) {
		log0(WARN, msg, null);
	}

	@Override
	public void warn(String msg, Object arg) {
		log(WARN, msg, arg);
	}

	@Override
	public void warn(String msg, Object arg1, Object arg2) {
		log(WARN, msg, arg1, arg2);
	}

	@Override
	public void warn(String msg, Object... args) {
		log(WARN, msg, args);
	}

	@Override
	public void error(String msg) {
		log0(ERROR, msg, null);
	}

	@Override
	public void error(String msg, Object arg) {
		log(ERROR, msg, arg);
	}

	@Override
	public void error(String msg, Object arg1, Object arg2) {
		log(ERROR, msg, arg1, arg2);
	}

	@Override
	public void error(String msg, Object... args) {
		log(ERROR, msg, args);
	}

	@Override
	public void info(String msg) {
		log0(INFO, msg, null);
	}

	@Override
	public void info(String msg, Object arg) {
		log(INFO, msg, arg);
	}

	@Override
	public void info(String msg, Object arg1, Object arg2) {
		log(INFO, msg, arg1, arg2);
	}

	@Override
	public void info(String msg, Object... args) {
		log(INFO, msg, args);
	}

}
