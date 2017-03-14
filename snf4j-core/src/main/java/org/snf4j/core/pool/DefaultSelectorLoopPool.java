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
package org.snf4j.core.pool;

import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.IdentifiableObject;
import org.snf4j.core.SelectorLoop;
import org.snf4j.core.factory.DefaultSelectorFactory;
import org.snf4j.core.factory.DefaultThreadFactory;
import org.snf4j.core.factory.ISelectorFactory;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;

/**
 * Default implementation for selector loop pool that is backed by an fixed-size array of selector loops.
 * <p>
 * This implementation is not thread safe.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DefaultSelectorLoopPool extends IdentifiableObject implements ISelectorLoopPool {
	
	private final static ILogger logger = LoggerFactory.getLogger(DefaultSelectorLoopPool.class);
	
	private final IExceptionLogger elogger = ExceptionLogger.getInstance();

	private static AtomicLong nextId = new AtomicLong(0);
	
	/** The backing array for this selector-pool */
	protected final SelectorLoop[] pool;
	
	/** A factory used to create threads for all selector loops in this pool */
	protected final ThreadFactory threadFactory;
	
	/** A factory used to create selectors for all selector loops in this pool */
	protected final ISelectorFactory selectorFactory;
	
	/** The current size of this pool */
	protected int size;
	
	/**
	 * Constructs a named selector loop pool with given capacity and thread
	 * factory.
	 * 
	 * @param name
	 *            the name of the pool or <code>null</code> if the name should
	 *            be auto generated
	 * @param capacity
	 *            the capacity of this pool
	 * @param threadFactory
	 *            a factory used to create threads for all selector loops in
	 *            this pool, or <code>null</code> to use the default factory
	 * @param selectorFactory
	 *            a selector factory used to create selectors for all selector
	 *            loops in this pool, or <code>null</code> to use the default
	 *            factory
	 * 
	 *            that will be used by this selector loop to open its selector,
	 *            or <code>null</code> if default factory should be used
	 */
	public DefaultSelectorLoopPool(String name, int capacity, ThreadFactory threadFactory, ISelectorFactory selectorFactory) {
		super("SelectorPool-", nextId.incrementAndGet(), name);
		this.threadFactory = threadFactory != null ? threadFactory : DefaultThreadFactory.DEFAULT;
		this.selectorFactory = selectorFactory != null ? selectorFactory: DefaultSelectorFactory.DEFAULT;
		pool = new SelectorLoop[capacity];
	}

	/**
	 * Constructs a named selector loop pool with given capacity.
	 * 
	 * @param name
	 *            the name of the pool or <code>null</code> if the name should
	 *            be auto generated
	 * @param capacity
	 *            the capacity of this pool
	 */
	public DefaultSelectorLoopPool(String name, int capacity) {
		this(name, capacity, null, null);
	}

	/**
	 * Constructs a unnamed selector loop pool with given capacity.
	 * 
	 * @param capacity
	 *            the capacity of this pool
	 */
	public DefaultSelectorLoopPool(int capacity) {
		this(null, capacity, null, null);
	}
	
	/**
	 * Gets an array of all selector loops created in this pool.
	 * 
	 * @return the array of selector loops. 
	 */
	public SelectorLoop[] getPool() {
		if (size == pool.length) {
			return pool.clone();
		}
		return Arrays.copyOf(pool, size);
	}

	/**
	 * Gets the capacity of this pool.
	 * 
	 * @return the capacity of this pool
	 */
	public int getCapacity() {
		return pool.length;
	}
	
	/**
	 * Gets the current size of this pool. It is the number of selector loops
	 * created in this pool.
	 * 
	 * @return the current size
	 */
	public int getSize() {
		return size;
	}
	
	SelectorLoop createLoop(String name) throws Exception {
		return new SelectorLoop(name, this, selectorFactory);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @return the selector loop with the smallest number of the registered channels
	 * @see org.snf4j.core.SelectorLoop AbstractSelectorLoop#getSize
	 */
	@Override
	public SelectorLoop getLoop(SelectableChannel channel) {
		int minSize = Integer.MAX_VALUE;
		SelectorLoop minLoop = null;
		
		for (int i = 0; i < size; ++i) {
			try {
				int size = pool[i].getSize();

				if (size < minSize) {
					minSize = size;
					minLoop = pool[i];
				}
			}
			catch (ClosedSelectorException e) {
				logger.error("Stopped selector loop {} detected in pool {}", pool[i], this);
			}
		}
		
		if (minSize > 0 && size < pool.length) {
			try {
				SelectorLoop newLoop = createLoop(getName() + "-" +(size+1)); 
				
				newLoop.setThreadFactory(threadFactory);
				newLoop.start();
				minLoop = newLoop;
			} catch (Exception e) {
				elogger.error(logger, "Creation of new selector loop from pool {} failed: {}", this, e);
				return minLoop;
			}
			pool[size++] = minLoop;
		}
		return minLoop;		
	}
	
	@Override
	public void stop() {
		for (int i = 0; i < size; ++i) {
			pool[i].stop();
		}
	}
	
	@Override
	public void quickStop() {
		for (int i = 0; i < size; ++i) {
			pool[i].quickStop();
		}
	}

	@Override
	public void update(SelectorLoop loop, int newSize, int prevSize) {
	}
	
	@Override
	public boolean join(long millis) throws InterruptedException {
		long endMillis = System.currentTimeMillis() + millis;
		
		for (int i = 0; i < size; ++i) {
			if (pool[i].join(millis)) {
				millis = endMillis - System.currentTimeMillis();
				if (millis <= 0) {
					millis = 1;
				}
			}
			else {
				return false;
			}
		}
		return true;
	}

	@Override
	public void join() throws InterruptedException {
		for (int i = 0; i < size; ++i) {
			pool[i].join();
		}
	}
	
}
