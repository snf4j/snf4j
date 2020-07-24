/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2020 SNF4J contributors
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

import java.net.SocketAddress;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;

class InternalSSLEngine implements IEngine {

	private final SSLEngine engine;
	
	private final ISessionConfig config;
	
	static HandshakeStatus[] handshakeStatuses;

	static Status[] statuses;
	
	static {
		handshakeStatuses = new HandshakeStatus[SSLEngineResult.HandshakeStatus.values().length];
		statuses = new Status[SSLEngineResult.Status.values().length];
		
		for (HandshakeStatus status: HandshakeStatus.values()) {
			try {
				int ordinal = SSLEngineResult.HandshakeStatus.valueOf(status.name()).ordinal();
				handshakeStatuses[ordinal] = status;
			}
			catch (IllegalArgumentException e) {
			}
		}
		for (Status status: Status.values()) {
			int ordinal = SSLEngineResult.Status.valueOf(status.name()).ordinal();
			statuses[ordinal] = status;
		}
	}
	
	InternalSSLEngine(SocketAddress remoteAddress, ISessionConfig config, boolean clientMode) throws SSLEngineCreateException {
		this.config = config;
		if (remoteAddress != null) {
			this.engine = config.createSSLEngine(remoteAddress, clientMode);
		}
		else {
			this.engine = config.createSSLEngine(clientMode);
		}
	}
	
	InternalSSLEngine(SSLEngine engine, ISessionConfig config) {
		this.config = config;
		this.engine = engine;
	}
	
	@Override
	public void init() {
	}
	
	@Override
	public void cleanup() {
	}
	
	@Override
	public void beginHandshake() throws Exception {
		engine.beginHandshake();
	}
	
	@Override
	public final boolean isOutboundDone() {
		return engine.isOutboundDone();
	}

	@Override
	public final boolean isInboundDone() {
		return engine.isInboundDone();
	}

	@Override
	public final void closeOutbound() {
		engine.closeOutbound();
	}

	@Override
	public final void closeInbound() throws SessionIncidentException {
		try {
			engine.closeInbound();
		} catch (SSLException e) {
			throw new SessionIncidentException(e, SessionIncident.SSL_CLOSED_WITHOUT_CLOSE_NOTIFY);
		}
	}

	@Override
	public final int getMinApplicationBufferSize() {
		return engine.getSession().getApplicationBufferSize();
	}

	@Override
	public final int getMinNetworkBufferSize() {
		return engine.getSession().getPacketBufferSize();
	}

	@Override
	public final int getMaxApplicationBufferSize() {
		return getMinApplicationBufferSize() * config.getMaxSSLApplicationBufferSizeRatio();
	}

	@Override
	public final int getMaxNetworkBufferSize() {
		return getMinNetworkBufferSize() * config.getMaxSSLNetworkBufferSizeRatio();
	}
	
	@Override
	public final HandshakeStatus getHandshakeStatus() {
		return handshakeStatuses[engine.getHandshakeStatus().ordinal()];
	}

	@Override
	public final Runnable getDelegatedTask() {
		return engine.getDelegatedTask();
	}

	@Override
	public final IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
		return new EngineResult(engine.wrap(srcs, dst));
	}

	@Override
	public final IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		return new EngineResult(engine.wrap(src, dst));
	}

	@Override
	public final IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		return new EngineResult(engine.unwrap(src, dst));
	}

	private static class EngineResult implements IEngineResult {
		private final SSLEngineResult result;
		
		EngineResult(SSLEngineResult result) {
			this.result = result;
		}
		
		@Override
		public final int bytesConsumed() {
			return result.bytesConsumed();
		}

		@Override
		public final int bytesProduced() {
			return result.bytesProduced();
		}

		@Override
		public final Status getStatus() {
			return InternalSSLEngine.statuses[result.getStatus().ordinal()];
		}

		@Override
		public final HandshakeStatus getHandshakeStatus() {
			return InternalSSLEngine.handshakeStatuses[result.getHandshakeStatus().ordinal()];
		}
		
	}
}
