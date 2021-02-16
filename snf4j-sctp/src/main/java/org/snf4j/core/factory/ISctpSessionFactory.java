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
package org.snf4j.core.factory;

import org.snf4j.core.SctpSession;

import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

/**
 * Factory used to create a SCTP session for a newly accepted
 * connection. This interface is associated with the
 * {@link com.sun.nio.sctp.SctpServerChannel} being registered with the
 * selector loop.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpSessionFactory {

	/**
	 * Creates a SCTP session for a newly accepted connection. This
	 * method can be also used to configure the newly accepted channel.
	 * 
	 * @param channel
	 *            the SCTP channel associated with the accepted connection.
	 * @return a SCTP session that will be associated with the
	 *         accepted connection
	 * @throws Exception
	 *             when a SCTP session could not be created
	 */
	SctpSession create(SctpChannel channel) throws Exception;
	
	/**
	 * Notifies about registration of a listening channel. Once it is called
	 * the associated selector loop is ready to accept new connections form the
	 * channel.
	 * 
	 * @param channel
	 *            the listening channel that has been registered
	 */
	void registered(SctpServerChannel channel);
	
	/**
	 * Notifies about closing and unregistering of a listening channel. It is
	 * only called when the channel is closed by the associated selector loop.
	 * 
	 * @param channel
	 *            the listening channel that has been closed
	 */
	void closed(SctpServerChannel channel);
	
	/**
	 * Notifies about an exception caught during processing of a listening
	 * channel.
	 * 
	 * @param channel
	 *            the listening channel for witch the exception was caught
	 * @param exception
	 *            the exception that was caught
	 */
	void exception(SctpServerChannel channel, Throwable exception);
}
