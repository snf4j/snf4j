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
package org.snf4j.core;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;


public class TestSelector extends Selector implements IDelegatingSelector {

	final Selector delegate;
	
	volatile boolean nonBlocking;
	
	volatile boolean closeException;
	
	volatile boolean delegateException;
	
	volatile boolean delegateCloseSelector;
	
	volatile boolean delegateCloseSelectorWithNullPointerException;
	
	volatile int delegateExceptionCounter = -1;
	
	TestSelector() throws IOException {
		delegate = Selector.open();
	}
	
	@Override
	public void close() throws IOException {
		delegate.close();
		if (closeException) {
			throw new IOException();
		}
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public Set<SelectionKey> keys() {
		return delegate.keys();
	}

	@Override
	public SelectorProvider provider() {
		return delegate.provider();
	}

	@Override
	public int select() throws IOException {
		return nonBlocking ? 0 : delegate.select();
	}

	@Override
	public int select(long arg0) throws IOException {
		return nonBlocking ? 0 : delegate.select(arg0);
	}

	@Override
	public int selectNow() throws IOException {
		return delegate.selectNow();
	}

	@Override
	public Set<SelectionKey> selectedKeys() {
		return delegate.selectedKeys();
	}

	@Override
	public Selector wakeup() {
		return delegate.wakeup();
	}

	@Override
	public Selector getDelegate() {
		if (delegateException) {
			if (delegateExceptionCounter != 0) {
				if (delegateExceptionCounter > 0) {
					--delegateExceptionCounter;
				}
				throw new IllegalStateException();
			}
		}
		if (delegateCloseSelector) {
			try {
				delegate.close();
				if (delegateCloseSelectorWithNullPointerException) {
					throw new NullPointerException();
				}
				throw new ClosedSelectorException();
			} catch (IOException e) {
			}
		}
		return delegate;
	}

}
