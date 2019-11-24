/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2019 SNF4J contributors
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
package org.snf4j.core;

import java.nio.channels.Selector;

/**
 * A selector wrapping an original implementation of the abstract {@link java.nio.channels.Selector Selector} class.
 * This interface allows selectors that wrap the original implementation to be properly handled by the selector loop. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDelegatingSelector {

	/**
	 * Returns the underlying original selector that is wrapped by this implementation
	 * of {@link java.nio.channels.Selector Selector}
	 * 
	 * @return the underlying selector
	 */
	Selector getDelegate();
}
