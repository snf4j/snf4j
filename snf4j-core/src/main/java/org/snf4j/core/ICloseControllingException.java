/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
 */package org.snf4j.core;

import org.snf4j.core.handler.IHandler;
import org.snf4j.core.session.ISession;

/**
 * An exception that can change the default (quickClose) close operation.
 * Throwing an exception implementing this interface during processing of
 * session's I/O operations will cause that the session will be closed according
 * to the close type returned by {@link #getCloseType}.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 *
 */
 public interface ICloseControllingException {
	
	/**
	 * Type of the close operation.
	 */
	enum CloseType {
		
		/**
		 * A session should be gently closed.
		 * 
		 * @see ISession#close
		 */
		GENTLE,
		
		/**
		 * A session should be quickly closed (default).
		 * 
		 * @see ISession#quickClose
		 */
		DEFAULT,
		
		/**
		 * A session should not be closed.
		 */
		NONE
	}
	
	/**
	 * Returns the way a session should be closed after throwing this exception
	 * during processing of session's I/O operations.
	 * 
	 * @return the close type
	 */
	CloseType getCloseType();
	
	/**
	 * Returns the cause that triggered the close operation. The return closing
	 * cause will be passed to the {@link IHandler#exception}.
	 * 
	 * @return the cause that triggered the close operation
	 */
	Throwable getClosingCause();

}
