/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.alert;

import org.snf4j.tls.ProtocolException;

public class Alert extends ProtocolException {

	private static final long serialVersionUID = 1L;

	private final AlertLevel level;
	
	private final AlertDescription description;
	
	private final boolean closure;
	
	protected Alert(String message, AlertLevel level, AlertDescription description) {
		super(message);
		this.level = level;
		this.description = description;
		closure = false;
	}

	protected Alert(String message, AlertDescription description) {
		this(message, AlertLevel.FATAL, description);
	}

	protected Alert(String message, AlertLevel level, AlertDescription description, Throwable cause, boolean closure) {
		super(message, cause);
		this.level = level;
		this.description = description;
		this.closure = closure;
	}

	protected Alert(String message, AlertDescription description, Throwable cause) {
		this(message, AlertLevel.FATAL, description, cause, false);
	}
	
	public AlertLevel getLevel() { return level; }
	
	public AlertDescription getDescription() { return description; }

	public boolean isClosure() {
		return closure;
	}
	
}
