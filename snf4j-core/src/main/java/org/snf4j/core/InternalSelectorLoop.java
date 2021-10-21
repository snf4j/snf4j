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

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.snf4j.core.SessionPipeline.Item;
import org.snf4j.core.factory.DefaultSelectorLoopStructureFactory;
import org.snf4j.core.factory.DefaultThreadFactory;
import org.snf4j.core.factory.ISelectorLoopStructureFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.future.IFutureExecutor;
import org.snf4j.core.future.RegisterFuture;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;

abstract class InternalSelectorLoop extends IdentifiableObject implements IFutureExecutor {

	final ILogger logger;
	
	final IExceptionLogger elogger = ExceptionLogger.getInstance();

	private final static AtomicLong nextId = new AtomicLong(0);

	volatile Thread thread;
	
	ThreadFactory threadFactory = DefaultThreadFactory.DEFAULT;
	
	volatile Executor executor = DefaultExecutor.DEFAULT;
	
	final ISelectorLoopStructureFactory factory;
	
	volatile Selector selector;
	
	private final long selectTimeout;
	
	private long selectBeginTime;
	
	private long selectEndTime;
	
	private volatile long totalWorkTime;
	
	private volatile long totalWaitTime;
	
	private AtomicBoolean wakenup = new AtomicBoolean(false);
	
	private int selectCounter;
	
	private volatile int size;
	
	private int prevSize;

	private final static int SELECTOR_REBUILD_THRESHOLD = Integer.getInteger(Constants.SELECTOR_REBUILD_THRESHOLD_SYSTEM_PROPERY, 512);
	
	private AtomicBoolean rebuildRequested = new AtomicBoolean(false);
	
	private AtomicBoolean stoppingRequested = new AtomicBoolean(false);
	
	final AtomicReference<StoppingType> stopping = new AtomicReference<StoppingType>(); 
	
	private Set<SelectionKey> stoppingKeys;
	
	private boolean closeWhenEmpty;
	
	private boolean ending;
	
	private boolean inTask;

	private final ConcurrentLinkedQueue<PendingRegistration> registrations = new ConcurrentLinkedQueue<PendingRegistration>();
	
	private final ConcurrentLinkedQueue<Task> tasks = new ConcurrentLinkedQueue<Task>();

	private final Object registrationLock = new Object();
	
	private Set<SelectionKey> invalidatedKeys = new HashSet<SelectionKey>();
	
	boolean areSwitchings;
	
	final List<InternalSession> switchings = new LinkedList<InternalSession>();
	
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
	InternalSelectorLoop(String name, ILogger logger, ISelectorLoopStructureFactory factory) throws IOException {
		super("selector-loop-", nextId.incrementAndGet(), name);
		this.logger = logger;
		this.factory = factory == null ? DefaultSelectorLoopStructureFactory.DEFAULT : factory;
		selector = this.factory.openSelector();
		selectTimeout = Math.max(0, Long.getLong(Constants.SELECTOR_SELECT_TIMEOUT, 1000));
	}

	/**
	 * Returns the total time in nanoseconds this selector loop spent waiting
	 * for I/O operations
	 * 
	 * @return the total time in nanoseconds
	 */
	public long getTotalWaitTime() {
		return totalWaitTime;
	}
	
	/**
	 * Returns the total time in nanoseconds this selector loop spent processing
	 * I/O operations
	 * 
	 * @return the total time in nanoseconds
	 */
	public long getTotalWorkTime() {
		return totalWorkTime;
	}
	
	/**
	 * Rebuilds the associated selector by replacing it with newly created one. All valid 
	 * selection keys registered with the current selector will be re-registered to the 
	 * new selector. 
	 * <p>
	 * This operation is asynchronous.
	 */
	public void rebuild() {
		rebuildRequested.compareAndSet(false, true);
		wakeup();
	}

	final void rebuildSelector() {
		Selector selector = this.selector;
		Selector newSelector;
		
		if (selector == null) {
			return;
		}
		
		try {
			newSelector = factory.openSelector();
		}
		catch (Exception e) {
			elogger.error(logger, "Failed to create new selector during rebuilding process: {}", e);
			return;
		}
		
		for (SelectionKey key: selector.keys()) {
			SelectableChannel channel = key.channel();
			
			if (key.isValid() && channel.keyFor(newSelector) == null) {
				ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
				
				try {
					if (ctx.isSession()) {
						InternalSession session = ctx.getSession();
						
						synchronized (session.writeLock) {
							int ops = key.interestOps();
							
							key.cancel();
							SelectionKey newKey = channel.register(getUnderlyingSelector(newSelector), ops, ctx);
							session.setSelectionKey(newKey);
						}
					}
					else {
						int ops = key.interestOps();
						
						key.cancel();
						channel.register(getUnderlyingSelector(newSelector), ops, ctx);
					}
					
				}
				catch (Exception e) {
					elogger.error(logger, "Failed to re-register channel {} to new selector during rebuilding process: {}" , ctx.toString(channel), e);
					try {
						if (ctx.isServer()) {
							fireException(key, e);
							channel.close();
							ctx.postClose(channel);
						}
						else {
							handleInvalidKey(key, stoppingKeys, true);
						}
					}
					catch (Exception e2) {
						elogger.error(logger, "Failed to close channel {} during rebuilding process: {}", ctx.toString(channel), e2);
					}
				}
			}
		}
		
		this.selector = newSelector;
		
		try {
			selector.close();
		}
		catch (Exception e) {
			elogger.error(logger, "Failed to close old selector during rebuilding process: {}" , e);
		}
		
		logger.info("Rebuilding of new selector completed");
	}
	
	final void notifySizeChange(final boolean notify) {
		if (notify) {
			if (size != prevSize) {
				notifyAboutLoopSizeChange(size, prevSize);
				prevSize = size;
			}
		}
	}
	
	final void select() throws IOException {
		final boolean notify = notifyAboutLoopChanges(); 
		
		if (rebuildRequested.compareAndSet(true, false)) {
			rebuildSelector();
			selectCounter = 0;
		}

		if (traceEnabled && selectTimeout == 0) {
			logger.trace("Selecting");
		}

		//populate selector's key set to properly set the loop size
		int selectedKeys = selector.selectNow();
		size = selector.keys().size();
		notifySizeChange(notify);
		
		if (selectedKeys > 0) {
			wakenup.set(false);
		}
		else {
			long selectBlocked;
			
			selectBeginTime = System.nanoTime();
			if (selectEndTime != 0) {
				totalWorkTime += selectBeginTime - selectEndTime;
			}
			if (!wakenup.compareAndSet(true, false)) {
				selectedKeys = selector.select(selectTimeout);
				selectEndTime = System.nanoTime();
				selectBlocked = selectEndTime - selectBeginTime;
				totalWaitTime += selectBlocked;
				if (selectedKeys == 0) {
					//if the blocking time is greater than 90% of the select timeout
					//then the select returned normally
					if (selectBlocked >= selectTimeout * 900000L) {
						selectCounter = 0;
					}
				}
				size = selector.keys().size();
				notifySizeChange(notify);
			}
			else {
				selectEndTime = selectBeginTime; 
			}
		}
		
		if (selectedKeys > 0) {
			selectCounter = 1;
		}
		else {
			++selectCounter;
			if (selectCounter >= SELECTOR_REBUILD_THRESHOLD) {
				logger.warn("Selector selected nothing {} times in a row and rebuilding will be initiated", selectCounter);
				rebuildSelector();
				selectedKeys = selector.selectNow();
				size = selector.keys().size();
				notifySizeChange(notify);
				selectCounter = 1;
			}
		}
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
	 * Sets the thread factory used to create a thread for this selector loop.
	 * To take effect it have to be set before execution of the
	 * <code>start</code> method.
	 * 
	 * @param threadFactory
	 *            the new thread factory
	 * @throws NullPointerException
	 *             If the <code>threadFactory</code> argument is
	 *             <code>null</code>
	 */
	public void setThreadFactory(ThreadFactory threadFactory) {
		if (threadFactory == null) {
			throw new NullPointerException(); 
		}
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
	 * Sets the executor that will be used to execute delegated tasks required
	 * by engine driven sessions to complete operations that block, or may take
	 * an extended period of time to complete.
	 * 
	 * @param executor
	 *            the new executor
	 * @throws NullPointerException
	 *             If the <code>executor</code> argument is <code>null</code>
	 */
	public void setExecutor(Executor executor) {
		if (executor == null) {
			throw new NullPointerException(); 
		}
		this.executor = executor;
	}
	
	/**
	 * Returns the executor that is used to execute delegated tasks required by
	 * engine driven sessions to complete operations that block, or may take an
	 * extended period of time to complete.
	 * 
	 * @return the current executor
	 */
	public Executor getExecutor() {
		return executor;
	}
	
	/**
	 * Returns the size of the selector's key set associated with the selector loop
	 * 
	 * @return the size of the selector's key set
	 * @throws ClosedSelectorException
	 *             if the associated selector is closed
	 */
	public int getSize() {
		if (selector.isOpen()) {
			return size;
		}
		throw new ClosedSelectorException();
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
		wakenup.compareAndSet(false, true);
		if (!inLoop()) {
			selector.wakeup();
		}
	}
	
	/**
	 * Wakes up the associated selector only if executed from outside this
	 * loop's thread.
	 */
	final void lazyWakeup() {
		if (!inLoop()) {
			wakeup();
		}
	}
	
	/**
	 * Tells if the current {@link Thread} is executed in this selector loop.
	 * 
	 * @return <code>true</code> the current {@link Thread} is executed in this selector loop.
	 */
	final boolean inLoop() {
		return thread == Thread.currentThread();
	}
	
	final boolean inTask() {
		return inTask;
	}
	
	@Override
	public final boolean inExecutor() {
		return inLoop();
	}
	
	final Selector getUnderlyingSelector(Selector selector) {
		return (selector instanceof IDelegatingSelector) ? ((IDelegatingSelector) selector).getDelegate() : selector;
	}
	
	void loop() {

		if (logger.isDebugEnabled()) {
			logger.debug("Starting main loop");
		}
		
		stoppingKeys = null;
		prevSize = 0;
		
		for (;;) {
			debugEnabled = logger.isDebugEnabled();
			traceEnabled = debugEnabled ? logger.isTraceEnabled() : false;
			
			try {
				
				select();
				
				if (closeWhenEmpty && size == 0) {
					quickStop();
				}

				//Handle stopping request
				if (stoppingRequested.compareAndSet(true, false)) {
					stoppingKeys = new HashSet<SelectionKey>();
					for (SelectionKey key: selector.keys()) {
						ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
						
						if (ctx.isSession()) {
							InternalSession session = ctx.getSession();
							
							if (session.pipelineItem != null) {
								session.pipelineItem.markEos();
							}
							stoppingKeys.add(key);
							switch (stopping.get()) {
							case QUICK:
								session.quickClose();
								break;

							case DIRTY:
								session.dirtyClose();
								break;
								
							default:
								session.close();
								break;
							}
						}
						else {
							try {
								if (debugEnabled) {
									logger.debug("Closing of channel {}", ctx.toString(key.channel()));
								}
								key.channel().close();
								ctx.postClose(key.channel());
							}
							catch (Exception e) {
								elogWarnOrError(logger, "Closing of channel {} failed: {}", ctx.toString(key.channel()), e);
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
						
						try {
							handleInvalidKey(key, stoppingKeys);
						}
						catch (Exception e){
							elogger.error(logger, "Processing of invalidated key for {} failed: {}", key.attachment(), e);
						}
					}
				}
				
				Set<SelectionKey> keys = selector.selectedKeys();
				
				if (!keys.isEmpty()) {
					Iterator<SelectionKey> i = keys.iterator();
					
					for (;;) {
						SelectionKey key = i.next();
						i.remove();
						
						try {
							key = handleSelectedKey(key);
						}
						catch (CancelledKeyException e) {
							//Ignore
						}
						catch (PipelineDecodeException e) {
							InternalSession session = e.getSession();
							SessionIncident incident = SessionIncident.DECODING_PIPELINE_FAILURE;
							ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
							
							if (ctx.exceptionOnDecodingFailure()) {
								elogger.error(logger, incident.defaultMessage(), session, e.getCause());
								fireException(session, e.getCause());
							}
							else if (!session.incident(incident, e.getCause())) {
								elogger.error(logger, incident.defaultMessage(), session, e.getCause());
							}
						}
						catch (Exception e) {
							if (key.isValid()) {
								fireException(key, e);
							}
							
							boolean cancel = true;
							
							if (e instanceof ICloseControllingException) {
								switch (((ICloseControllingException)e).getCloseType()) {
								case GENTLE:
								case NONE:
									cancel = false;
									break;
									
								default:
								}
							}
							
							if (cancel) {
								key.cancel();
							}
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
						
						if (!i.hasNext()) {
							break;
						}
					}
				}
				
				handleTasks();	
				
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
					
					if (channel.keyFor(selector) == null) {
						ChannelContext<?> ctx = reg.ctx;
						
						try {
							channel.configureBlocking(false);
							
							//Abort pending registrations when stopping is in progress
							if (stopping.get() != null) {

								if (debugEnabled) {
									logger.debug("Aborting pending registration for channel {}", ctx.toString(channel));
								}
								abortRegistration(reg, true, null);
							}
							else {
								SelectionKey key = channel.register(getUnderlyingSelector(selector), reg.ops, ctx);
								
								if (debugEnabled) {
									logger.debug("Channel {} registered with options {}", ctx.toString(channel), reg.ops);
								}
								handleRegistration(key, reg);
							}
						}
						catch (ClosedSelectorException e) {
							abortRegistration(reg, true, null);
							throw e;
						}
						catch (Exception e) {
							//JDK1.6 does not throw ClosedSelectorException if registering with closed selector
							if (!selector.isOpen()) {
								abortRegistration(reg, true, null);
								throw new ClosedSelectorException();
							}
							elogger.error(logger, "Registering of channel {} failed: {}", ctx.toString(channel), e);
							abortRegistration(reg, true, e);
						}
					}
					else {
						abortRegistration(reg, false, null);
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

		if (stopping.get() == null) {
			//make sure we are not in the middle of adding registration
			synchronized (registrationLock) {
				stopping.compareAndSet(null, StoppingType.DIRTY);
			}
		}
		
		PendingRegistration reg;
		while((reg = registrations.poll()) != null) {
			if (debugEnabled) {
				logger.debug("Aborting pending registration for channel {}", reg.ctx.toString(reg.channel));
			}
			abortRegistration(reg, true, null);
		}
		
		//make sure we are not in the middle of registering task
		synchronized (registrationLock) {
			ending = true;
		}
		
		handleTasks();
		
		thread = null;
		if (logger.isDebugEnabled()) {
			logger.debug("Stopping main loop");
		}
	}
	
	private final void handleTasks() {
		Task task;
		inTask = true;
		while((task = tasks.poll()) != null) {
			handleTask(task);
		}			
		inTask = false;
	}
	
	private final void handleTask(Task task) {
		TaskFuture<Void> future = task.future;
		
		try {
			if (traceEnabled) {
				logger.trace("Starting execution of task {}", task.task);
			}
			task.task.run();
			if (traceEnabled) {
				logger.trace("Finished execution of task {}", task.task);
			}
			if (future != null) {
				future.success();
			}
			if (areSwitchings) {
				handleSwitchings();
			}
		}
		catch (Exception e) {
			elogger.error(logger, "Unexpected exception thrown during execution of task {}: {}", task.task, e);
			if (future != null) {
				future.abort(e);
			}
		}
	}
	
	private final void handleRegistration(SelectionKey key, PendingRegistration reg) throws Exception {
		if (reg.ctx.isSession()) {
			handleRegisteredKey(key, reg);
		}
		else {
			reg.ctx.postRegistration(reg.channel);
		}	
		reg.future.success();
	}
	
	private final void abortRegistration(PendingRegistration reg, boolean closeChannel, Throwable cause) {
		final ChannelContext<?> ctx = reg.ctx;
		
		if (closeChannel) {
			try {
				ctx.close(reg.channel);
			}
			catch (IOException e) {
				elogger.warn(logger, "Closing of channel {} during aborting registration failed: {}", ctx.toString(reg.channel), e);
			}
		}
		
		if (ctx.isSession()) {
			InternalSession session = ctx.getSession();
			
			if (session.isCreated()) {
				fireEndingEvent(session, false);
			}
			else {
				session.abortFutures(cause);

			}
		}
		else {
			ctx.postClose(reg.channel);
		}	
		reg.future.abort(cause);
	}

	void stop(StoppingType type) {
		if (selector.isOpen()) {
			boolean stoppingSet = false;
			for (StoppingType expectedType: type.expect()) {
				if (stopping.compareAndSet(expectedType, type)) {
					stoppingSet = true;
					break;
				}
			}
			if (stoppingSet) {
				stoppingRequested.getAndSet(true);
				wakeup();
				if (isStopped()) {
					try {
						selector.close();
					} catch (IOException e) {
						//Ignore
					}
				}
			}
		}
	}
	
	/**
	 * Gently stops this selector loop. As a result, all associated sessions
	 * will be gently closed by calling the {@link org.snf4j.core.session.ISession#close() close} method.
	 * <p>
	 * This method is asynchronous.
	 */
	public void stop() {
		stop(StoppingType.GENTLE);
	}
	
	/**
	 * Quickly stops this selector loop. As a result, all associated sessions
	 * will be quickly closed by calling the {@link org.snf4j.core.session.ISession#quickClose() quickClose} method.
	 * <p>
	 * This method is asynchronous.
	 */
	public void quickStop() {
		stop(StoppingType.QUICK);
	}

	/**
	 * Quickly stops this selector loop. As a result, all associated sessions
	 * will be quickly closed by calling the {@link org.snf4j.core.session.ISession#quickClose() dirtyClose} method.
	 * <p>
	 * This method is asynchronous.
	 */
	public void dirtyStop() {
		stop(StoppingType.DIRTY);
	}
	
	/**
	 * Tells if this selector is in the process of stopping. 
	 * 
	 * @return <code>true</code> if stopping is in progress or this selector loop is already stopped 
	 */
	public boolean isStopping() {
		return stopping.get() != null;
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
	 * Waits for this selector loop's thread to die.
	 * 
	 * @throws InterruptedException
	 *             if any thread has interrupted the current thread
	 */
	public final void join() throws InterruptedException {
		join(0);
	}

	/**
	 * Adds a channel to the pending registration queue.
	 * @throws ClosedChannelException 
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by this channel
	 */
	IFuture<Void> register(SelectableChannel channel, int ops, ChannelContext<?> ctx) throws ClosedChannelException {
		if (channel == null) {
			throw new IllegalArgumentException("channel is null");
		}
		if (!channel.isOpen()) {
			throw new ClosedChannelException();
		}
		if ((ops & ~channel.validOps()) != 0 ) {
			throw new IllegalArgumentException("invalid options " + ops); 
		}
		if (!selector.isOpen()) {
			throw new ClosedSelectorException();
		}
		if (stopping.get() != null) {
			throw new SelectorLoopStoppingException();
		}
		
		PendingRegistration reg = new PendingRegistration();

		if (logger.isDebugEnabled()) {
			logger.debug("Registering channel {} with options {}", ctx.toString(channel), ops);
		}
		
		reg.channel = channel;
		reg.ops = ops;
		reg.ctx = ctx;
		if (ctx.isSession()) {
			InternalSession session = ctx.getSession(); 
			if (session.isCreated()) {
				throw new IllegalArgumentException("session cannot be reused");
			}
			reg.future = new RegisterFuture<Void>(session);
		}
		else {
			reg.future = new RegisterFuture<Void>(null);
		}
		synchronized (registrationLock) {
			//make sure not to register while stopping
			if (stopping.get() != null) {
				throw new SelectorLoopStoppingException();
			}
			registrations.add(reg);
		}
		wakeup();
		return reg.future;
	}

	/**
	 * Executes a task in the selector-loop's thread. This operation is
	 * asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize
	 * on a future associated with the specified task. This will save some
	 * resources and may improve performance.
	 * 
	 * @param task
	 *            task to be executed in the selector-loop's thread
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws IllegalArgumentException
	 *             if {@code task} is null
	 */
	public void executenf(Runnable task) {
		if (task == null) {
			throw new IllegalArgumentException("task is null");
		}
		
		Task task0 = new Task();
		
		task0.task = task;
		execute0(task0);
	}

	/**
	 * Executes a task in the selector-loop's thread. This operation is
	 * asynchronous.
	 * 
	 * @param task
	 *            task to be executed in the selector-loop's thread
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws IllegalArgumentException
	 *             if {@code task} is null
	 * @return the future associated with the specified task
	 */
	public IFuture<Void> execute(Runnable task) {
		if (task == null) {
			throw new IllegalArgumentException("task is null");
		}
		
		Task task0 = new Task();
		
		task0.task = task;
		task0.future = new TaskFuture<Void>(null);
		execute0(task0);
		return task0.future;
	}
	
	private final void execute0(Task task) {
		synchronized (registrationLock) {
			//make sure not to register while stopping
			if (ending) {
				throw new SelectorLoopStoppingException();
			}
			tasks.add(task);
		}		
		wakeup();
	}
	
	final void execute0(Runnable task) {
		Task task0 = new Task();
		
		task0.task = task;
		synchronized (registrationLock) {
			//make sure not to register while stopping
			if (ending) {
				return;
			}
			tasks.add(task0);
		}		
		wakeup();
	}
	
	/**
	 * Registers the invalidated key for finishing. The method does nothing if
	 * run in the selector's thread that is not currently executing any task.
	 * 
	 * @param key
	 *            the key that was invalidated
	 */
	final void finishInvalidatedKey(SelectionKey key) {
		if (!inLoop() || inTask()) {
			synchronized (invalidatedKeys) {
				invalidatedKeys.add(key);
			}
			wakeup();
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
	
	final boolean fireCreatedEvent(final InternalSession session, SelectableChannel channel) {
		session.setChannel(channel);
		session.setLoop(this);
		session.preCreated();
		fireEvent(session, SessionEvent.CREATED);
		if (session.closeCalled.get() || !channel.isOpen()) {
			session.closeAndFinish(channel);
		}
		return channel.isOpen();
	}
	
	final void fireEndingEvent(final InternalSession session, boolean skipCloseWhenEmpty) {
		fireEvent(session, SessionEvent.ENDING);
		session.postEnding();
		
		Item<?> item = session.pipelineItem;
		
		if (item != null) {
			List<InternalSession> sessions = new LinkedList<InternalSession>();
			InternalSession owner = item.owner();
			Throwable cause = item.cause();

			while ((item = item.next) != null) {
				if (item.session().getConfig().alwaysNotifiedBeingInPipeline()) {
					sessions.add(item.session());
				}
			}
			sessions.add(owner);
			for (Iterator<InternalSession> i = sessions.iterator(); i.hasNext();) {
				InternalSession s = i.next();
				
				fireCreatedEvent(s, session.channel);
				if (cause != null) {
					fireException(s, cause);
				}
				fireEndingEvent(s, skipCloseWhenEmpty);
			}
		}
		
		switch (session.getConfig().getEndingAction()) {
		case STOP:
			stop();
			return;

		case QUICK_STOP:
			quickStop();
			return;
			
		case DIRTY_STOP:
			dirtyStop();
			return;

		case STOP_WHEN_EMPTY:
			closeWhenEmpty = true;
			break;
			
		default:
		}
		
		if (!skipCloseWhenEmpty && closeWhenEmpty) {
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

	final void fireException(final SelectionKey key, Throwable t) {
		final ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
		
		if (ctx.isSession()) {
			fireException(ctx.getSession(), t);
		}
		else if (ctx.isServer()) {
			ctx.exception(key.channel(), t);
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
	
	private final void handleInvalidKey(SelectionKey key, Set<SelectionKey> stoppingKeys, boolean skipCloseWhenEmpty) throws IOException {
		if (stoppingKeys != null) {
			stoppingKeys.remove(key);
		}
		
		ChannelContext<?> ctx = (ChannelContext<?>) key.attachment();
		
		if (ctx.isSession()) {
			InternalSession session = ctx.getSession();

			try {
				session.close(key.channel());
			}
			finally {
				fireEvent(session, SessionEvent.CLOSED);
				fireEndingEvent(session, skipCloseWhenEmpty);
			}
		}
	}
	
	void switchSession(InternalSession session) {
		if (debugEnabled) {
			logger.debug("Switching of {}", session);
		}
		fireEvent(session, SessionEvent.CLOSED);
		fireEvent(session, SessionEvent.ENDING);

		InternalSession newSession = session.pipelineItem.next();
		SelectionKey key = session.key;
		int bytes;

		key.attach(((ChannelContext<?>)key.attachment()).wrap(newSession));
		if (!fireCreatedEvent(newSession, key.channel())) {
			fireEndingEvent(newSession, false);
			return;
		}
		newSession.setSelectionKey(key);
		try {
			key.interestOps(SelectionKey.OP_READ);
			fireEvent(newSession, SessionEvent.OPENED);
			bytes = newSession.copyInBuffer(session);
			session.postEnding();
			if (bytes > 0) {
				long currentTime = System.currentTimeMillis();

				if (traceEnabled) {
					logger.trace("{} byte(s) copied from input buffer in {}", bytes, newSession);
				}
				newSession.calculateThroughput(currentTime, false);
				newSession.incReadBytes(bytes, currentTime);
				fireEvent(newSession, DataEvent.RECEIVED, bytes);
				newSession.consumeInBuffer();
			}
		} catch (Exception e) {
			elogger.error(logger, "Switching from {} to {} failed: {}", session, newSession, e);
			fireException(newSession, e);
		}
	}

	void handleSwitchings() {
		for (InternalSession session: switchings) {
			switchSession(session);
		}
		switchings.clear();
		areSwitchings = false;
	}
	
	private final void handleInvalidKey(SelectionKey key, Set<SelectionKey> stoppingKeys) throws IOException {
		handleInvalidKey(key, stoppingKeys, false);
	}
	
	abstract void handleRegisteredKey(SelectionKey key, PendingRegistration reg) throws Exception;
	
	abstract SelectionKey handleSelectedKey(SelectionKey key);
	
	abstract void notifyAboutLoopSizeChange(int newSize, int prevSize);
	
	abstract boolean notifyAboutLoopChanges();
	
	static final class PendingRegistration {
		SelectableChannel channel;
		int ops;
		ChannelContext<?> ctx;
		RegisterFuture<Void> future;
	}
	
	static final class Task {
		Runnable task;
		TaskFuture<Void> future;
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
