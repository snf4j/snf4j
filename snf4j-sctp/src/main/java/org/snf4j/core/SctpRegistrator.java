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
 */
package org.snf4j.core;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;

import org.snf4j.core.factory.ISctpSessionFactory;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpMultiChannel;
import com.sun.nio.sctp.SctpServerChannel;

/**
 * An utility class providing methods for registration of SCTP channels with
 * {@link SelectorLoop}
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SctpRegistrator {
	
	private SctpRegistrator() {
	}
	
	/**
	 * Registers a SCTP channel with the specified selector loop. The method
	 * only adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param loop    the selector loop the SCTP channel should be registered with
	 * @param channel
	 *            the SCTP channel to register with the specified selector
	 *            loop
	 * @param handler
	 *            the handler that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException
	 *             if the channel is closed
	 * @throws SelectorLoopStoppingException
	 *             if selector loop is in the process of stopping
	 * @throws ClosedSelectorException
	 *             if the internal selector is closed
	 * @throws IllegalArgumentException
	 *             if a bit in ops does not correspond to an operation that is
	 *             supported by the channel
	 */
	public static IFuture<Void> register(SelectorLoop loop, SctpChannel channel, ISctpHandler handler) 
			throws ClosedChannelException {
		SctpSession session = new SctpSession(handler);
		
		return loop.register(channel, 0, new SctpChannelContext(session));
	}
	
	/**
	 * Registers a SCTP channel with the specified selector loop. The method only
	 * adds the channel to the selector-loop's pending registration queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param loop    the selector loop the SCTP channel should be registered with
	 * @param channel the SCTP channel to register with the specified selector
	 *                loop
	 * @param session the session that will be associated with the channel
	 * @return the future associated with this registration
	 * @throws ClosedChannelException        if the channel is closed
	 * @throws SelectorLoopStoppingException if selector loop is in the process of
	 *                                       stopping
	 * @throws ClosedSelectorException       if the internal selector is closed
	 * @throws IllegalArgumentException      if a bit in ops does not correspond to
	 *                                       an operation that is supported by the
	 *                                       channel
	 * @throws IllegalArgumentException      if the session argument is
	 *                                       <code>null</code> or the session object
	 *                                       is reused (was already registered with
	 *                                       some selector loop)
	 */
	public static IFuture<Void> register(SelectorLoop loop, SctpChannel channel, SctpSession session)
			throws ClosedChannelException {
		if (session == null) {
			throw new IllegalArgumentException("session is null");
		}
		return loop.register(channel, 0, new SctpChannelContext(session));
	}
	
	/**
	 * Registers a listening SCTP channel with the specified selector loop. The
	 * method only adds the channel to the selector-loop's pending registration
	 * queue.
	 * <p>
	 * This method is asynchronous.
	 * 
	 * @param loop    the selector loop the SCTP channel should be registered with
	 * @param channel the listening SCTP channel to register with the specified
	 *                selector loop
	 * @param factory the factory that will be associated with the channel. It will
	 *                be used to create sessions for newly accepted channels
	 * @return the future associated with this registration
	 * @throws ClosedChannelException        if the channel is closed
	 * @throws SelectorLoopStoppingException if selector loop is in the process of
	 *                                       stopping
	 * @throws ClosedSelectorException       if the internal selector is closed
	 * @throws IllegalArgumentException      if a bit in ops does not correspond to
	 *                                       an operation that is supported by the
	 *                                       channel
	 * @throws IllegalArgumentException      if the factory argument is
	 *                                       <code>null</code>
	 */
	public static IFuture<Void> register(SelectorLoop loop, SctpServerChannel channel, ISctpSessionFactory factory) 
			throws ClosedChannelException {
		if (factory == null) { 
			throw new IllegalArgumentException("factory is null");
		}		
		return loop.register(channel, SelectionKey.OP_ACCEPT, new SctpServerChannelContext(factory));
	}
	
	public static IFuture<Void> register(SelectorLoop loop, SctpMultiChannel channel, ISctpHandler handler) 
			throws ClosedChannelException {
		SctpMultiSession session = new SctpMultiSession(handler);
		
		return loop.register(channel, SelectionKey.OP_READ, new SctpMultiChannelContext(session));
	}	
	
	public static IFuture<Void> register(SelectorLoop loop, SctpMultiChannel channel, SctpMultiSession session)
			throws ClosedChannelException {
		if (session == null) {
			throw new IllegalArgumentException("session is null");
		}
		return loop.register(channel, SelectionKey.OP_READ, new SctpMultiChannelContext(session));
	}
	
}
