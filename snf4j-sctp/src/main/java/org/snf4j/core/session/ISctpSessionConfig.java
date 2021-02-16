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

import org.snf4j.core.codec.ICodecExecutor;

import com.sun.nio.sctp.MessageInfo;

/**
 * A configuration for associated SCTP session.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISctpSessionConfig extends ISessionConfig {
	
	/**
	 * Creates a new codec executor that should be used for decoding/encoding SCTP
	 * messages that can by identified by given ancillary data.
	 * 
	 * @param msgInfo ancillary data about the message
	 * @return the codec executor, or <code>null</code> if decoding and encoding are
	 *         not required.
	 */
	ICodecExecutor createCodecExecutor(MessageInfo msgInfo);
	
	/**
	 * Returns the minimum stream number of the SCTP messages that should be
	 * encoded/decoded by the codec executors created by the
	 * {@link #createCodecExecutor(MessageInfo)}.
	 * <p>
	 * For other stream numbers the SCTP messages should be encoded/decoded by the
	 * default codec executor (created by {@link #createCodecExecutor()}).
	 * 
	 * @return the minimum stream number
	 */
	int getMinSctpStreamNumber();
	
	/**
	 * Returns the maximum stream number of the SCTP messages that should be
	 * encoded/decoded by the codec executors created by the
	 * {@link #createCodecExecutor(MessageInfo)}.
	 * <p>
	 * For other stream numbers the SCTP messages should be encoded/decoded by the
	 * default codec executor (created by {@link #createCodecExecutor()}).
	 * 
	 * @return the maximum stream number
	 */
	int getMaxSctpStreamNumber();
	
	/**
	 * Returns the minimum payload protocol identifier of the SCTP messages that
	 * should be encoded/decoded by the codec executors created by the
	 * {@link #createCodecExecutor(MessageInfo)}.
	 * <p>
	 * For other payload protocol identifiers the SCTP messages should be
	 * encoded/decoded by the default codec executor (created by
	 * {@link #createCodecExecutor()}).
	 * 
	 * @return the minimum payload protocol identifier
	 */
	int getMinSctpPayloadProtocolID();
	
	/**
	 * Returns the maximum payload protocol identifier of the SCTP messages that
	 * should be encoded/decoded by the codec executors created by the
	 * {@link #createCodecExecutor(MessageInfo)}.
	 * <p>
	 * For other payload protocol identifiers the SCTP messages should be
	 * encoded/decoded by the default codec executor (created by
	 * {@link #createCodecExecutor()}).
	 * 
	 * @return the maximum payload protocol identifier
	 */
	int getMaxSctpPayloadProtocolID();
	
	/**
	 * Returns the stream number for the SCTP messages sent by the
	 * {@link org.snf4j.core.SctpSession SctpSession}'s write methods without
	 * specified the {@code msgInfo} argument.
	 * 
	 * @return the default stream number
	 */
	int getDefaultSctpStreamNumber();
	
	/**
	 * Returns the payload protocol identifier for the SCTP messages sent by the
	 * {@link org.snf4j.core.SctpSession SctpSession}'s write methods without
	 * specified the {@code msgInfo} argument.
	 * 
	 * @return the default payload protocol identifier
	 */
	int getDefaultSctpPayloadProtocolID();
	
	/**
	 * Returns the unordered flag for the SCTP messages sent by the
	 * {@link org.snf4j.core.SctpSession SctpSession}'s write methods without
	 * specified the {@code msgInfo} argument.
	 * 
	 * @return the default unordered flag
	 */
	boolean getDefaultSctpUnorderedFlag();

}
