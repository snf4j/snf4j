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

import org.snf4j.core.codec.ICodecExecutor;

import com.sun.nio.sctp.MessageInfo;

/**
 * A configuration for associated SCTP session.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpSessionConfig extends ISessionConfig {
	
	/**
	 * The identifier of the default codec executor.
	 */
	public final static Object DEFAULT_CODEC_EXECUTOR_IDENTIFIER = new Object();
	
	/**
	 * Returns an identifier of the codec executor that should be used for
	 * decoding/encoding SCTP messages that can be identified by the given ancillary
	 * data. If the default executor (i.e. created by the
	 * {@link #createCodecExecutor()}) should be used this method should return
	 * the {@link #DEFAULT_CODEC_EXECUTOR_IDENTIFIER} value.
	 * <p>
	 * If the codec executor identified by the ancillary data has not been created
	 * yet it will be created by {@link #createCodecExecutor(Object)} and the
	 * returned identifier will be used as the argument.
	 * <p>
	 * 
	 * @param msgInfo the ancillary data about a SCTP message to be decoded/encoded
	 * @return the identifier of the codec executor, or <code>null</code> if
	 *         decoding and encoding are not required.
	 */
	Object getCodecExecutorIdentifier(MessageInfo msgInfo);
	
	/**
	 * Creates a new codec executor that should be used for decoding/encoding SCTP
	 * messages that can be identified by the given identifier.
	 * 
	 * @param identifier the identifier of the codec executor to create
	 * @return the codec executor, or <code>null</code> if decoding and encoding are
	 *         not required.
	 */
	ICodecExecutor createCodecExecutor(Object identifier);
	
	/**
	 * Returns the stream number for the SCTP messages sent by the
	 * {@link org.snf4j.core.session.ISctpSession ISctpSession}'s write methods
	 * without specified the {@code msgInfo} argument.
	 * 
	 * @return the default stream number
	 */
	int getDefaultSctpStreamNumber();
	
	/**
	 * Returns the payload protocol identifier for the SCTP messages sent by the
	 * {@link org.snf4j.core.session.ISctpSession ISctpSession}'s write methods
	 * without specified the {@code msgInfo} argument.
	 * 
	 * @return the default payload protocol identifier
	 */
	int getDefaultSctpPayloadProtocolID();
	
	/**
	 * Returns the unordered flag for the SCTP messages sent by the
	 * {@link org.snf4j.core.session.ISctpSession ISctpSession}'s write methods
	 * without specified the {@code msgInfo} argument.
	 * 
	 * @return the default unordered flag
	 */
	boolean getDefaultSctpUnorderedFlag();
	
	/**
	 * Returns the preferred peer address for the SCTP messages sent by the
	 * {@link org.snf4j.core.session.ISctpSession ISctpSession}'s write methods
	 * without specified the {@code msgInfo} argument.
	 * 
	 * @return the preferred peer address, or {@code null} to use the peer primary
	 *         address
	 */
	SocketAddress getDefaultSctpPeerAddress();

}
