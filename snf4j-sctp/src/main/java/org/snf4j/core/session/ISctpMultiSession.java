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
package org.snf4j.core.session;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import org.snf4j.core.future.IFuture;

import com.sun.nio.sctp.Association;

/**
 * Extends the {@link ISctpSession} interface to cover SCTP functionalities
 * related to the SCTP multi sessions.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpMultiSession extends ISctpSession {
	
	@Override
	ISctpMultiSession getParent();

	/**
	 * Shutdown an association without closing this session.
	 * 
	 * @param association the association to shutdown
	 * @return the future associated with this shutdown operation
	 */
	IFuture<Void> shutdown(Association association);
	
	/**
	 * Returns the open associations on the SCTP multi-channel's socket associated
	 * with this session.
	 * 
	 * @return the open associations, or an empty {@code Set} if there are none
	 */
	Set<Association> getAssociations();
	
	/**
	 * Returns all of the remote addresses to which the given association on the
	 * SCTP multi-channel's socket associated with this session.
	 * 
	 * @param association the given association
	 * @return all of the remote addresses for the given association, or an empty
	 *         {@code Set} if the association has been shutdown
	 */
	Set<SocketAddress> getRemoteAddresses(Association association);

	/**
	 * Returns all of the remote addresses to which the open associations on the
	 * SCTP multi-channel associated with this session is connected.
	 * 
	 * @return All of the remote addresses to which the SCTP multi-channel
	 *         associated with this session is connected, or an empty Set if the
	 *         channel is not connected
	 */
	@Override
	Set<SocketAddress> getRemoteAddresses();
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	IFuture<Void> write(byte[] msg);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	IFuture<Void> write(byte[] msg, int offset, int length);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	void writenf(byte[] msg);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	void writenf(byte[] msg, int offset, int length);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	IFuture<Void> write(ByteBuffer msg);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	IFuture<Void> write(ByteBuffer msg, int length);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	void writenf(ByteBuffer msg);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	void writenf(ByteBuffer msg, int length);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	IFuture<Void> write(Object msg);
	
	/**
	 * {@inheritDoc}
	 * 
	 * @throws IllegalSessionStateException if this session is not open
	 * @throws IllegalStateException if the default peer address is not set in the session's configuration
	 */
	@Override
	void writenf(Object msg);
	
}
