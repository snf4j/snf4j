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

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Set;

import org.snf4j.core.ImmutableSctpMessageInfo;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.handler.ISctpHandler;

import com.sun.nio.sctp.Association;

/**
 * Extends the {@link ISession} interface to cover SCTP functionalities.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpSession extends ISession {
	
	/**
	 * Gets the SCTP handler associated with this session
	 * 
	 * @return the SCTP handler
	 */
	@Override
	ISctpHandler getHandler();

	@Override
	ISctpSession getParent();
	
	/**
	 * Returns the association on the SCTP channel's socket associated with this
	 * session
	 * 
	 * @return the association, or {@code null} if the SCTP channel's socket is not
	 *         connected
	 */
	Association getAssociation();
	
	/**
	 * Adds the given address to the bound addresses for the SCTP channel's socket
	 * associated with this session
	 * 
	 * @param address the address to add to the bound addresses
	 * @return the future associated with this bind operation
	 */
	IFuture<Void> bindAddress(InetAddress address);
	
	/**
	 * Removes the given address from the bound addresses for the SCTP channel's
	 * socket associated with this session
	 * 
	 * @param address the address to remove from the bound addresses
	 * @return the future associated with this unbind operation
	 */
	IFuture<Void> unbindAddress(InetAddress address);
	
	/**
	 * Returns one of the local addresses this session is bound to.
	 * <p>
	 * NOTE: The return address cannot be treated as the local primary address. It
	 * is the application's responsibility to keep track of it's local primary
	 * address.
	 */
	@Override
	SocketAddress getLocalAddress();
	
	/**
	 * Returns one of the remote addresses to which this session is connected.
	 * <p>
	 * NOTE: The return address cannot be treated as the peer's primary address. It
	 * is the application's responsibility to keep track of it's peer's primary
	 * address.
	 */
	@Override
	SocketAddress getRemoteAddress();

	/**
	 * Returns all of the socket addresses to which the SCTP channel associated with
	 * this session is bound.
	 * 
	 * @return All the socket addresses that the SCTP channel associated with this
	 *         session is bound to, or an empty Set if the channel is not bound
	 */
	Set<SocketAddress> getLocalAddresses();
	
	/**
	 * Returns all of the remote addresses to which the SCTP channel associated with
	 * this session is connected.
	 * 
	 * @return All of the remote addresses to which the SCTP channel associated with
	 *         this session is connected, or an empty Set if the channel is not
	 *         connected
	 */
	Set<SocketAddress> getRemoteAddresses();

	/**
	 * Writes a message from the specified byte array to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the default values for
	 * {@code streamNumber}, {@code payloadProtocolID} and {@code unordered} that
	 * were read from the session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] msg);
	
	/**
	 * Writes a message of given length from the specified byte array to the SCTP
	 * channel that is associated with this session. The message will be written
	 * with the {@code com.sun.nio.sctp.MessageInfo} created based on the default
	 * values for {@code streamNumber}, {@code payloadProtocolID} and
	 * {@code unordered} that were read from the session's configuration during
	 * creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param offset offset within the array of the first byte to be written
	 * @param length number of bytes to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] msg, int offset, int length);
	
	/**
	 * Writes a message from the specified byte array to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] msg, ImmutableSctpMessageInfo msgInfo);

	/**
	 * Writes a message of given length from the specified byte array to the SCTP
	 * channel that is associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param offset offset within the array of the first byte to be written
	 * @param length number of bytes to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message from the specified byte array to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the default values for
	 * {@code streamNumber}, {@code payloadProtocolID} and {@code unordered} that
	 * were read from the session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(byte[] msg);
	
	/**
	 * Writes a message of given length from the specified byte array to the SCTP
	 * channel that is associated with this session. The message will be written
	 * with the {@code com.sun.nio.sctp.MessageInfo} created based on the default
	 * values for {@code streamNumber}, {@code payloadProtocolID} and
	 * {@code unordered} that were read from the session's configuration during
	 * creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param offset offset within the array of the first byte to be written
	 * @param length number of bytes to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(byte[] msg, int offset, int length);
	
	/**
	 * Writes a message from the specified byte array to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(byte[] msg, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message of given length from the specified byte array to the SCTP
	 * channel that is associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param offset offset within the array of the first byte to be written
	 * @param length number of bytes to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(byte[] msg, int offset, int length, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message from the specified byte buffer to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the default values for
	 * {@code streamNumber}, {@code payloadProtocolID} and {@code unordered} that
	 * were read from the session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer msg);
	
	/**
	 * Writes a message of given length from the specified byte buffer to the SCTP
	 * channel that is associated with this session. The message will be written
	 * with the {@code com.sun.nio.sctp.MessageInfo} created based on the default
	 * values for {@code streamNumber}, {@code payloadProtocolID} and
	 * {@code unordered} that were read from the session's configuration during
	 * creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param length number of bytes to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer msg, int length);
	
	/**
	 * Writes a message from the specified byte buffer to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message of given length from the specified byte buffer to the SCTP
	 * channel that is associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param length number of bytes to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message from the specified byte buffer to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the default values for
	 * {@code streamNumber}, {@code payloadProtocolID} and {@code unordered} that
	 * were read from the session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(ByteBuffer msg);
	
	/**
	 * Writes a message of given length from the specified byte buffer to the SCTP
	 * channel that is associated with this session. The message will be written
	 * with the {@code com.sun.nio.sctp.MessageInfo} created based on the default
	 * values for {@code streamNumber}, {@code payloadProtocolID} and
	 * {@code unordered} that were read from the session's configuration during
	 * creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param length number of bytes to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(ByteBuffer msg, int length);
	
	/**
	 * Writes a message from the specified byte buffer to the SCTP channel that is
	 * associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(ByteBuffer msg, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message of given length from the specified byte buffer to the SCTP
	 * channel that is associated with this session. The message will be written with the
	 * {@code com.sun.nio.sctp.MessageInfo} created based on the specified immutable
	 * ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg    the byte array containing the message to be written
	 * @param length number of bytes to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(ByteBuffer msg, int length, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message to the SCTP channel that is associated with this session.
	 * The message will be written with the {@code com.sun.nio.sctp.MessageInfo}
	 * created based on the default values for {@code streamNumber},
	 * {@code payloadProtocolID} and {@code unordered} that were read from the
	 * session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method usually requires that the session has configured a codec pipeline
	 * with at least one encoder that accepts the msg as the inbound data. If a
	 * codec pipeline is not configured or no encoder accept the msg object it still
	 * accepts messages that are of the <code>byte[]</code> or {@link ByteBuffer}
	 * type.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(Object msg);
	
	/**
	 * Writes a message to the SCTP channel that is associated with this session.
	 * The message will be written with the {@code com.sun.nio.sctp.MessageInfo}
	 * created based on the specified immutable ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method usually requires that the session has configured a codec pipeline
	 * with at least one encoder that accepts the msg as the inbound data. If a
	 * codec pipeline is not configured or no encoder accept the msg object it still
	 * accepts messages that are of the <code>byte[]</code> or {@link ByteBuffer}
	 * type.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 * @return the future associated with this write operation
	 */
	IFuture<Void> write(Object msg, ImmutableSctpMessageInfo msgInfo);
	
	/**
	 * Writes a message to the SCTP channel that is associated with this session.
	 * The message will be written with the {@code com.sun.nio.sctp.MessageInfo}
	 * created based on the default values for {@code streamNumber},
	 * {@code payloadProtocolID} and {@code unordered} that were read from the
	 * session's configuration during creation of this session.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method usually requires that the session has configured a codec
	 * pipeline with at least one encoder that accepts the msg as the inbound
	 * data. If a codec pipeline is not configured or no encoder accept the msg
	 * object it still accepts messages that are of the <code>byte[]</code> or
	 * {@link ByteBuffer} type.
	 * <p>
	 * This method should be used whenever there will be no need to 
	 * synchronize on a future object. This will save some resources and 
	 * may improve performance.
	 * 
	 * @param msg the byte array containing the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(Object msg);
	
	/**
	 * Writes a message to the SCTP channel that is associated with this session.
	 * The message will be written with the {@code com.sun.nio.sctp.MessageInfo}
	 * created based on the specified immutable ancillary data.
	 * <p>
	 * The operation is asynchronous.
	 * <p>
	 * This method usually requires that the session has configured a codec pipeline
	 * with at least one encoder that accepts the msg as the inbound data. If a
	 * codec pipeline is not configured or no encoder accept the msg object it still
	 * accepts messages that are of the <code>byte[]</code> or {@link ByteBuffer}
	 * type.
	 * <p>
	 * This method should be used whenever there will be no need to synchronize on a
	 * future object. This will save some resources and may improve performance.
	 * 
	 * @param msg     the byte array containing the message to be written
	 * @param msgInfo immutable ancillary data about the message to be written
	 * @throws IllegalSessionStateException if this session is not open
	 */
	void writenf(Object msg, ImmutableSctpMessageInfo msgInfo);
	
}
