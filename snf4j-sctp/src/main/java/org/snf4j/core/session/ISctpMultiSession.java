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

public interface ISctpMultiSession extends ISctpSession {
	
	@Override
	ISctpMultiSession getParent();

	Set<Association> getAssociations();
	
	Set<SocketAddress> getRemoteAddresses(Association association);

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
