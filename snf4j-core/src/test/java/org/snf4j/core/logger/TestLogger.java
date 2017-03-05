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

import java.util.Arrays;

public class TestLogger implements ILogger {

	StringBuilder sb = new StringBuilder();

	public void append(String level, String msg, Object... args) {
		sb.append(level);
		sb.append('|');
		sb.append(msg);
		sb.append('|');
		sb.append(Arrays.toString(args));
		sb.append('|');
	}
	
	public String getLog() {
		String s = sb.toString();
		sb.setLength(0);
		return s;
	}
	
	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public void debug(String msg) {
	}

	@Override
	public void debug(String msg, Object arg) {
	}

	@Override
	public void debug(String msg, Object arg1, Object arg2) {
	}

	@Override
	public void debug(String msg, Object... args) {
		append("D", msg, args);
	}

	@Override
	public void trace(String msg) {
	}

	@Override
	public void trace(String msg, Object arg) {
	}

	@Override
	public void trace(String msg, Object arg1, Object arg2) {
	}

	@Override
	public void trace(String msg, Object... args) {
		append("T", msg, args);
	}

	@Override
	public void warn(String msg) {
	}

	@Override
	public void warn(String msg, Object arg) {
	}

	@Override
	public void warn(String msg, Object arg1, Object arg2) {
	}

	@Override
	public void warn(String msg, Object... args) {
		append("W", msg, args);
	}

	@Override
	public void error(String msg) {
	}

	@Override
	public void error(String msg, Object arg) {
	}

	@Override
	public void error(String msg, Object arg1, Object arg2) {
	}

	@Override
	public void error(String msg, Object... args) {
		append("E", msg, args);
	}

	@Override
	public void info(String msg) {
	}

	@Override
	public void info(String msg, Object arg) {
	}

	@Override
	public void info(String msg, Object arg1, Object arg2) {
	}

	@Override
	public void info(String msg, Object... args) {
		append("I", msg, args);
	}

}
