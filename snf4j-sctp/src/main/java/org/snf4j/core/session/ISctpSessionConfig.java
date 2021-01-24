package org.snf4j.core.session;

import org.snf4j.core.codec.ICodecExecutor;

import com.sun.nio.sctp.MessageInfo;

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
	 * For other stream numbers the SCTP messages should encoded/decoded by the
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
	 * For other stream numbers the SCTP messages should encoded/decoded by the
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
	 * For other payload protocol identifiers the SCTP messages should
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
	 * For other payload protocol identifiers the SCTP messages should
	 * encoded/decoded by the default codec executor (created by
	 * {@link #createCodecExecutor()}).
	 * 
	 * @return the maximum payload protocol identifier
	 */
	int getMaxSctpPayloadProtocolID();
}
