package org.snf4j.core.session;

import org.snf4j.core.codec.ICodecExecutor;

import com.sun.nio.sctp.MessageInfo;

public class DefaultSctpSessionConfig extends DefaultSessionConfig implements ISctpSessionConfig {

	private int minSctpStreamNumber = 0;
	
	private int maxSctpStreamNumber = 65536;
	
	private int minSctpPayloadProtocolID = Integer.MIN_VALUE;
	
	private int maxSctpPayloadProtocolID = Integer.MAX_VALUE;
	
	@Override
	public ICodecExecutor createCodecExecutor(MessageInfo msgInfo) {
		return null;
	}
	
	@Override
	public int getMinSctpStreamNumber() {
		return minSctpStreamNumber;
	}
	
	public DefaultSctpSessionConfig setMinSctpStreamNumber(int minStreamNumber) {
		minSctpStreamNumber = minStreamNumber;
		return this;
	}
	
	@Override
	public int getMaxSctpStreamNumber() {
		return maxSctpStreamNumber;
	}

	public DefaultSctpSessionConfig setMaxSctpStreamNumber(int maxStreamNumber) {
		maxSctpStreamNumber = maxStreamNumber;
		return this;
	}
	
	@Override
	public int getMinSctpPayloadProtocolID() {
		return minSctpPayloadProtocolID;
	}

	public DefaultSctpSessionConfig setMinSctpPayloadProtocolID(int minPayloadProtocolID) {
		minSctpPayloadProtocolID = minPayloadProtocolID;
		return this;
	}
	
	@Override
	public int getMaxSctpPayloadProtocolID() {
		return maxSctpPayloadProtocolID;
	}

	public DefaultSctpSessionConfig setMaxSctpPayloadProtocolID(int maxPayloadProtocolID) {
		maxSctpPayloadProtocolID = maxPayloadProtocolID;
		return this;
	}
	
}
