/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.example.engine;

import java.nio.ByteBuffer;

import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.handler.SessionIncidentException;

public abstract class AbstractEngine implements IEngine {

	protected enum CloseType { NONE, PENDING, DONE };
	
	protected CloseType outboundDone = CloseType.NONE;

	protected CloseType inboundDone = CloseType.NONE;

	protected HandshakeStatus handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
	
	@Override
	public void init() {
	}
	
	@Override
	public void cleanup() {
	}
	
	@Override
	public void beginHandshake() throws Exception {
	}
	
	@Override
	public boolean isOutboundDone() {
		return outboundDone != CloseType.NONE;
	}

	@Override
	public boolean isInboundDone() {
		return inboundDone != CloseType.NONE;
	}

	@Override
	public void closeOutbound() {
		outboundDone = CloseType.PENDING;
	}

	@Override
	public void closeInbound() throws SessionIncidentException {
		inboundDone = CloseType.PENDING;
	}
	
	@Override
	public int getMinApplicationBufferSize() {
		return Packet.MAX_DATA;
	}

	@Override
	public int getMinNetworkBufferSize() {
		return Packet.MAX_SIZE;
	}

	@Override
	public int getMaxApplicationBufferSize() {
		return getMinApplicationBufferSize();
	}

	@Override
	public int getMaxNetworkBufferSize() {
		return getMinNetworkBufferSize();
	}
	
	@Override
	public HandshakeStatus getHandshakeStatus() {
		return handshakeStatus;
	}

	@Override
	public Runnable getDelegatedTask() {
		return null;
	}
	
	@Override
	public IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		return wrap(new ByteBuffer[] {src}, dst);
	}
	
	protected IEngineResult updateHandshakeStatus(IEngineResult result) {
		if (result.getHandshakeStatus() != HandshakeStatus.FINISHED) {
			handshakeStatus = result.getHandshakeStatus();
		}
		else {
			handshakeStatus = HandshakeStatus.NOT_HANDSHAKING;
		}
		return result;
	}
	
}
