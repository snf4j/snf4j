/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2018 SNF4J contributors
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

import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.IDatagramHandler;

/**
 * Extends the {@link ISession} interface to cover datagram-oriented functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IDatagramSession extends ISession {
	
	/**
	 * Gets the datagram-oriented handler associated with this session
	 * 
	 * @return the datagram-oriented handler
	 */
	@Override
	IDatagramHandler getHandler();


	/**
	 * Writes a <code>datagram.length</code> byte datagram from the specified
	 * byte array to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is 
	 * connected, in which case it writes the datagram directly to the socket's 
	 * peer.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] datagram);

	/**
	 * Writes a <code>datagram.length</code> byte datagram from the specified
	 * byte array to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is 
	 * connected, in which case it writes the datagram directly to the socket's 
	 * peer.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(byte[] datagram);
	
	/**
	 * Writes a <code>length</code> byte datagram from the specified byte array
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @param offset
	 *            offset within the array of the first byte to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] datagram, int offset, int length);

	/**
	 * Writes a <code>length</code> byte datagram from the specified byte array
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @param offset
	 *            offset within the array of the first byte to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(byte[] datagram, int offset, int length);

	/**
	 * Writes a <code>datagram.remaining()</code> byte datagram from the specified byte buffer
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer datagram);

	/**
	 * Writes a <code>datagram.remaining()</code> byte datagram from the specified byte buffer
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(ByteBuffer datagram);

	/**
	 * Writes a <code>length</code> byte datagram from the specified byte buffer
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer datagram, int length);

	/**
	 * Writes a <code>length</code> byte datagram from the specified byte buffer
	 * to the datagram-oriented channel associated with this session. 
	 * <p>
	 * This method may only be invoked if the associated channel's socket is connected, 
	 * in which case it writes the datagram directly to the socket's peer.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param datagram
	 *            the datagram to be written
	 * @param length
	 *            number of bytes to be written
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void writenf(ByteBuffer datagram, int length);
	
	/**
	 * Sends a <code>datagram.length</code> byte datagram from the specified
	 * byte array to a remote end via the datagram-oriented channel associated 
	 * with this session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this send operation
	 */	
	IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram);

	/**
	 * Sends a <code>datagram.length</code> byte datagram from the specified
	 * byte array to a remote end via the datagram-oriented channel associated 
	 * with this session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */	
	void sendnf(SocketAddress remoteAddress, byte[] datagram);
	
	/**
	 * Sends a <code>length</code> byte datagram from the specified byte array 
	 * to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @param offset
	 *            offset within the array of the first byte to be sent
	 * @param length
	 *            number of bytes to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this send operation
	 */
	IFuture<Void> send(SocketAddress remoteAddress, byte[] datagram, int offset, int length);

	/**
	 * Sends a <code>length</code> byte datagram from the specified byte array 
	 * to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @param offset
	 *            offset within the array of the first byte to be sent
	 * @param length
	 *            number of bytes to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */
	void sendnf(SocketAddress remoteAddress, byte[] datagram, int offset, int length);

	/**
	 * Sends a <code>datagram.remaining()</code> byte datagram from the specified byte 
	 * buffer to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this send operation
	 */	
	IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram);

	/**
	 * Sends a <code>datagram.remaining()</code> byte datagram from the specified byte 
	 * buffer to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */	
	void sendnf(SocketAddress remoteAddress, ByteBuffer datagram);

	/**
	 * Sends a <code>length</code> byte datagram from the specified byte buffer 
	 * to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @param length
	 *            number of bytes to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 * @return the future associated with this send operation
	 */	
	IFuture<Void> send(SocketAddress remoteAddress, ByteBuffer datagram, int length);

	/**
	 * Sends a <code>length</code> byte datagram from the specified byte buffer 
	 * to a remote end via the datagram-oriented channel associated with this 
	 * session. 
	 * <p>
	 * If the <code>remoteAddress</code> argument is not <code>null</code> then 
	 * the method may only be invoked if the associated channel's socket is not 
	 * connected. In case the argument is <code>null</code> the method will work 
	 * as the adequate write method. 
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on
	 * a future object. This will save some resources and may improve performance.
	 * 
	 * @param remoteAddress
	 *            the address of the remote end where the datagram should be sent
	 * @param datagram
	 *            the datagram to be sent
	 * @param length
	 *            number of bytes to be sent
	 * @throws IllegalSessionStateException
	 *             if this session is not open
	 */	
	void sendnf(SocketAddress remoteAddress, ByteBuffer datagram, int length);
}
