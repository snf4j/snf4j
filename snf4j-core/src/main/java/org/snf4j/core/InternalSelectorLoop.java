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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.core.factory.DefaultThreadFactory;
import org.snf4j.core.factory.IStreamSessionFactory;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.session.ISession;

abstract class InternalSelectorLoop extends IdentifiableObject {

	final ILogger logger;
	
	final IExceptionLogger elogger = ExceptionLogger.getInstance();

	private final static AtomicLong nextId = new AtomicLong(0);

	volatile Thread thread;
	
	ThreadFactory threadFactory = DefaultThreadFactory.DEFAULT;
	
	volatile Selector selector;

	AtomicBoolean stoppingRequested = new AtomicBoolean(false);
	
	volatile boolean stopping;
	
	volatile boolean quickStopping;
	
	private boolean closeWhenEmpty;

	ConcurrentLinkedQueue<PendingRegistration> registrations = new ConcurrentLinkedQueue<PendingRegistration>();
	
	private Set<SelectionKey> invalidatedKeys = new HashSet<SelectionKey>();
	
	boolean debugEnabled;
	
	boolean traceEnabled;

	/**
	 * Constructs a internal selector loop
	 * 
	 * @param name
	 *            the name for this selector loop, or <code>null</code> if the name should 
	 *            be auto generated
	 * @param logger
	 *            the logger that should be used by this selector loop to log messages
	 * @throws IOException
	 *             if the {@link java.nio.channels.Selector Selector} associated with this 
	 *             selector loop could not be opened
	 */
	InternalSelectorLoop(String name, ILogger logger) throws IOException {
		super("SelectorLoop-", nextId.incrementAndGet(), name);
		this.logger = logger;
		selector = Selector.open();
	}

	final void elogWarnOrError(ILogger log, String msg, Object... args) {
		int last = args.length - 1;
		
		if (args[last] instanceof RuntimeException) {
			elogger.error(log, msg, args);
		}
		else {
			elogger.warn(log, msg, args);
		}
	}
	
	/**
	 * Sets the thread factory used to create a thread for this selector loop. To take effect
	 * it have to be set before execution of the <code>start</code> method. 
	 * 
	 * @param threadFactory the current thread factory
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		this.threadFactory = threadFactory;
	}
	
	/**
	 * Returns the thread factory used to create a thread for this selector loop.
	 * 
	 * @return the current thread factory
	 */
	public ThreadFactory getThreadFactory() {
		return threadFactory;
	}
	
	/**
	 * Returns the size of the selector's key set associated with the selector loop
	 * 
	 * @return the size of the selector's key set
	 * @throws ClosedSelectorException
	 *             if the associated selector is closed
	 */
	public int getSize() {
		return selector.keys().size();
	}

	/**
	 * Tells if the selector associated with this selector loop is open.
	 * 
	 * @return <code>true</code> if the associated selector is open
	 */
	public boolean isOpen() {
		return selector.isOpen();
	}
	
	/**
	 * Starts the selector loop in a new thread or in the current one.
	 * 
	 * @param inCurrentThread
	 *            <code>true</code> if the selector loop should run in the
	 *            current thread
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 */
	public void start(boolean inCurrentThread) {
		if (!selector.isOpen()) throw new ClosedSelectorException();
		
		if (thread == null) {
			if (inCurrentThread) {
				thread = Thread.currentThread();
				loop();
			}
			else {
				thread = threadFactory.newThread(new Loop());
				thread.start();
			}
		}
	}

	/**
	 * Starts the selector loop in a new thread.
	 * 
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 */
	public void start() {
		start(false);
	}

	/**
	 * Wakes up the associated selector.
	 * 
	 * @see java.nio.channels.Selector#wakeup() Selector.wakeup()
	 */
	public void wakeup() {
		selector.wakeup();
	}
	
	final boolean inLoop() {
		return thread == Thread.currentThread();
	}
	
	void loop() {

		if (logger.isDebugEnabled()) {
			logger.debug("Starting main loop");
		}
		
		Set<SelectionKey> stoppingKeys = null;
		int prevKeysSize = 0;
		
		for (;;) {
			debugEnabled = logger.isDebugEnabled();
			traceEnabled = debugEnabled ? logger.isTraceEnabled() : false;
			
			try {
				
				if (traceEnabled) {
					logger.trace("Selecting");
				}
				
				int numOfKeys = selector.select();
				
				if (trackSizeChanges()) {
					int keysSize = selector.keys().size();
					
					if (keysSize != prevKeysSize) {
						sizeChanged(keysSize, prevKeysSize);
						prevKeysSize = keysSize;
					}
					if (closeWhenEmpty && keysSize == 0) {
						quickStop();
					}
				}
				else if (closeWhenEmpty && selector.keys().isEmpty()) {
					quickStop();
				}

				//Handle stopping request
				if (stoppingRequested.compareAndSet(true, false)) {
					stoppingKeys = new HashSet<SelectionKey>();
					for (SelectionKey key: selector.keys()) {
						if (key.attachment() instanceof ISession) {
							ISession session = (ISession) key.attachment();
							
							stoppingKeys.add(key);
							if (quickStopping) {
								session.quickClose();
							}
							else {
								session.close();
							}
						}
						else if (key.channel() instanceof ServerSocketChannel) {
							try {
								if (debugEnabled) {
									logger.debug("Closing of channel {}", toString(key.channel()));
								}
								key.channel().close();
								if (key.attachment() instanceof IStreamSessionFactory) {
									((IStreamSessionFactory)key.attachment()).closed((ServerSocketChannel) key.channel());
								}
							}
							catch (Exception e) {
								elogWarnOrError(logger, "Closing of channel {} failed: {}", toString(key.channel()), e);
							}
						}
					}
				}
				
				//Handle keys that was invalidated outside the selector's thread
				SelectionKey[] keyArray = getInvalidatedKeysToFinish();
				if (keyArray != null) {
					int len = keyArray.length;
					
					for (int i=0; i<len; ++i) {
						SelectionKey key = keyArray[i];
						
						handleInvalidKey(key, stoppingKeys);
					}
				}
				
				if (numOfKeys > 0) {
					Set<SelectionKey> keys = selector.selectedKeys();
					
					for (SelectionKey key: keys) {
						try {
							handleSelectedKey(key);
						}
						catch (CancelledKeyException e) {
							//Ignore
						}
						catch (Exception e) {
							elogger.error(logger, "Processing of selected key for {} failed: {}", key.attachment(), e);
						}

						try {
							if (!key.isValid()) {
								handleInvalidKey(key, stoppingKeys);
							}
						}
						catch (Exception e) {
							elogger.error(logger, "Processing of invalidated key for {} failed: {}", key.attachment(), e);
						}
					}
					keys.clear();
				}
				else {
					selector.selectedKeys().clear();
				}
				
				//Handle keys invalidated during stopping of the selector loop
				if (stoppingKeys != null) {
					for (Iterator<SelectionKey> it = stoppingKeys.iterator(); it.hasNext();) {
						SelectionKey key = it.next();
						
						if (!key.isValid()) {
							try {
								it.remove();
								handleInvalidKey(key, null);
							}
							catch (Exception e) {
								elogger.error(logger, "Processing of not handled invalidated key for {} failed: {}", key.attachment(), e);
							}
						}
					}
				}
				
				//Handle pending registration
				PendingRegistration reg;
				while((reg = registrations.poll()) != null) {
					SelectableChannel channel = reg.channel; 
					
					if (!channel.isRegistered()) {
						try {
							channel.configureBlocking(false);
							
							//Abort pending registrations when stopping is in progress
							if (stopping) {

								if (debugEnabled) {
									logger.debug("Aborting pending registration for channel {}", toString(channel));
								}
								
								try {
									if (channel instanceof DatagramChannel) {
										((DatagramChannel)channel).disconnect();
									}
									channel.close();
								}
								catch (IOException e) {
									elogger.warn(logger, "Closing of channel {} during aborting registration failed: {}", toString(channel), e);
								}
								
								if (reg.attachement instanceof InternalSession) {
									InternalSession session = (InternalSession) reg.attachement;
									
									if (session.isCreated()) {
										fireEndingEvent(session);
									}
								}
							}
							else {
								SelectionKey key = channel.register(selector, reg.ops, reg.attachement);
								
								if (debugEnabled) {
									logger.debug("Channel {} registered with options {}", toString(channel), reg.ops);
								}
								if (reg.attachement instanceof InternalSession) {
									handleRegisteredKey(key, channel, (InternalSession)reg.attachement);
								}
								else if (reg.attachement instanceof IStreamSessionFactory) {
									if (channel instanceof ServerSocketChannel) {
										((IStreamSessionFactory)reg.attachement).registered((ServerSocketChannel) channel);
									}
								}
							}
						}
						catch (ClosedSelectorException e) {
							throw e;
						}
						catch (Exception e) {
							elogger.error(logger, "Registering of channel {} failed: {}", toString(channel), e);
						}
					}
				}
				
				if (stoppingKeys != null) {
					if (stoppingKeys.isEmpty()) {
						selector.close();
						break;
					}
				}
				
			}
			catch (ClosedSelectorException e) {
				break;
			}
			catch (Exception e) {
				elogger.error(logger, "Unexpected exception thrown in main loop: {}", e);
			}
		}

		thread = null;
		if (logger.isDebugEnabled()) {
			logger.debug("Stopping main loop");
		}
	}
	
	/**
	 * Gently stops this selector loop. As a result, all associated sessions
	 * will be gently closed by calling the {@link org.snf4j.core.session.ISession#close() close} method.
	 * <p>
	 * This method is asynchronous.
	 */
	public void stop() {
		if (selector.isOpen()) {
			stoppingRequested.getAndSet(true);
			stopping = true;
			selector.wakeup();
			if (isStopped()) {
				try {
					selector.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * Quickly stops this selector loop. As a result, all associated sessions
	 * will be quickly closed by calling the {@link org.snf4j.core.session.ISession#quickClose() quickClose} method.
	 * <p>
	 * This method is asynchronous.
	 */
	public void quickStop() {
		quickStopping = true;
		stop();
	}
	
	/**
	 * Tells if this selector is in the process of stopping. 
	 * 
	 * @return <code>true</code> if stopping is in progress or this selector loop is already stopped 
	 */
	public boolean isStopping() {
		return stopping;
	}
	
	/**
	 * Tells if this selector loop is stopped. In other words, it means that
	 * the associated thread has stopped running.
	 * 
	 * @return <code>true</code> if this selector loop is stopped
	 */
	public boolean isStopped() {
		return thread == null;
	}
	
	/**
	 * Waits at most <code>millis</code> milliseconds for this selector loop's
	 * thread to die.
	 * 
	 * @param millis
	 *            the time to wait in milliseconds
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread
	 * @throws IllegalArgumentException
	 *             if the value of <code>millis</code> is negative
	 * @return <code>true</code> if the selector loop's thread died
	 */
	public final boolean join(long millis) throws InterruptedException {
		Thread thread = this.thread;
		
		if (thread != null) {
			thread.join(millis);
		}
		return this.thread == null;
	}
	 
	/**
	 * Waits for this selector loop's thread of to die.
	 * 
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread
	 */
	public final void join() throws InterruptedException {
		join(0);
	}

	/**
	 * Adds the channel to the pending registration queue.
	 * 
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by this channel
	 */
	void register(SelectableChannel channel, int ops, Object attachement) {
		if (channel == null) throw new IllegalArgumentException("channel is null");
		if ((ops & ~channel.validOps()) != 0 ) throw new IllegalArgumentException("invalid options " + ops); 
		if (!selector.isOpen()) throw new ClosedSelectorException();
		if (stopping) throw new SelectorLoopStoppingException();
		
		PendingRegistration reg = new PendingRegistration();

		if (logger.isDebugEnabled()) {
			logger.debug("Registering channel {} with options {}", toString(channel), ops);
		}
		
		reg.channel = channel;
		reg.ops = ops;
		reg.attachement = attachement;
		registrations.add(reg);
		selector.wakeup();
	}
	
	/**
	 * Returns a string representation of a channel object. It is provided to
	 * customize the way channel objects are formatted in the messages logged by
	 * the selector loop objects.
	 * 
	 * @param channel
	 *            the channel
	 * @return a string representation of a channel object
	 */
	protected String toString(SelectableChannel channel) {
		if (channel instanceof DatagramChannel) {
			StringBuilder sb = new StringBuilder(100);
			
			sb.append(channel.getClass().getName());
			sb.append("[local=");
			try {
				sb.append(((DatagramChannel)channel).getLocalAddress().toString());
			}
			catch (Exception e) {
				sb.append("unknown");
			}
			if (((DatagramChannel)channel).isConnected()) {
				sb.append(",remote=");
				try {
					sb.append(((DatagramChannel)channel).getRemoteAddress().toString());
				}
				catch (Exception e) {
					sb.append("unknown");
				}
			}
			sb.append("]");
			return sb.toString();
		}
		return channel != null ? channel.toString() : null;
	}
	
	/**
	 * Registers the invalidated key for finishing. The method does nothing if
	 * run in the selector's thread.
	 * 
	 * @param key
	 *            the key that was invalidated
	 */
	final void finishInvalidatedKey(SelectionKey key) {
		if (Thread.currentThread() != thread) {
			synchronized (invalidatedKeys) {
				invalidatedKeys.add(key);
			}
			selector.wakeup();
		}
	}
	
	/**
	 * Gets the new invalidated keys.
	 * 
	 * @return the new invalidated keys or null if there were no new invalidated
	 *         keys.
	 */
	final SelectionKey[] getInvalidatedKeysToFinish() {
		synchronized (invalidatedKeys) {
			int size = invalidatedKeys.size();
			
			if (size != 0) {
				SelectionKey[] keys = new SelectionKey[size];
				
				invalidatedKeys.toArray(keys);
				invalidatedKeys.clear();
				return keys;
			}
		}
		return null;
	}
	
	final void fireEndingEvent(final InternalSession session) {
		fireEvent(session, SessionEvent.ENDING);

		switch (session.getConfig().getClosingAction()) {
		case STOP:
			stop();
			break;

		case QUICK_STOP:
			quickStop();
			break;

		case STOP_WHEN_EMPTY:
			closeWhenEmpty = true;
			break;
			
		default:
		}
		
		if (closeWhenEmpty) {
			boolean empty = true;

			for (SelectionKey key: selector.keys()) {
				if (key != session.key) {
					empty = false;
					break;
				}
			}
			if (empty) {
				quickStop();
			}
		}
	}
	
	final void fireEvent(final InternalSession session, SessionEvent event) {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", event.type(), session);
		}
		session.event(event);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", event.type(), session);
		}
	}

	final void fireEvent(final InternalSession session, DataEvent event, long length) {
		if (traceEnabled) {
			logger.trace("Firing event {} for {}", event.type(), session);
		}
		session.event(event, length);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", event.type(), session);
		}
	}

	final void fireException(final InternalSession session, Throwable t) {
		if (debugEnabled) {
			logger.debug("Firing event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
		session.exception(t);
		if (traceEnabled) {
			logger.trace("Ending event {} for {}", EventType.EXCEPTION_CAUGHT, session);
		}
	}
	
	final void handleInvalidKey(SelectionKey key, Set<SelectionKey> stoppingKeys) throws IOException {
		if (stoppingKeys != null) {
			stoppingKeys.remove(key);
		}
		if (key.attachment() instanceof InternalSession) {
			InternalSession session = (InternalSession) key.attachment();

			try {
				if (key.channel() instanceof DatagramChannel) {
					((DatagramChannel)key.channel()).disconnect();
				}
				key.channel().close();
				fireEvent(session, SessionEvent.CLOSED);
			}
			finally {
				fireEndingEvent(session);
			}
		}
	}

	abstract void handleRegisteredKey(SelectionKey key, SelectableChannel channel, InternalSession session);
	
	abstract void handleSelectedKey(SelectionKey key);
	
	abstract void sizeChanged(int newSize, int prevSize);
	
	abstract boolean trackSizeChanges();
	
	static final class PendingRegistration {
		SelectableChannel channel;
		int ops;
		Object attachement;
	}

	class Loop implements Runnable {
		
		@Override
		public void run() {
			loop();
		}
		
		@Override
		public String toString() {
			return InternalSelectorLoop.this.toString();
		}
		
	}
}
