/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2021 SNF4J contributors
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

import java.nio.channels.SelectableChannel;

/**
 * A controller that determines behavior of the associated selector loop.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISelectorLoopController {
	
	/**
	 * Called to determine if the calling selector loop is permitted to process
	 * the accepted channel. If not, the accepted channel is immediately closed
	 * without creating associated session.
	 * 
	 * @param channel
	 *            the channel that was accepted
	 * @return <code>true</code> if the accepted channel can be processed
	 */
	boolean processAccepted(SelectableChannel channel);
	
	/**
	 * Called to determine if the calling selector loop is permitted to process
	 * the connecting channel. If not, the connecting channel is immediately
	 * closed without opening associated session witch is immediately ended.
	 * 
	 * @param channel
	 *            the connecting channel
	 * @return <code>true</code> if the connecting channel can be processed
	 */
	boolean processConnection(SelectableChannel channel);
}
