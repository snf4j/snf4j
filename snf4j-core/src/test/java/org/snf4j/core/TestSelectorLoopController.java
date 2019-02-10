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

import java.nio.channels.SocketChannel;

public class TestSelectorLoopController implements ISelectorLoopController {
	final static int DEFAULT = 0;
	final static int BLOCK = 1;
	final static int EXCEPTION = 2;
	
	volatile int accept = DEFAULT;
	volatile int connect = DEFAULT;
	
	@Override
	public boolean processAccepted(SocketChannel channel) {
		switch (accept) {
		case BLOCK:
			return false;
		case EXCEPTION:
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			throw new IllegalArgumentException("not permitted");
		}
		return true;
	}

	@Override
	public boolean processConnection(SocketChannel channel) {
		switch (connect) {
		case BLOCK:
			return false;
		case EXCEPTION:
			throw new IllegalArgumentException("not permitted");
		}
		return true;
	}

}
