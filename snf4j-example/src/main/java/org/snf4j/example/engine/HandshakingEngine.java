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
import java.util.Random;

import org.snf4j.core.engine.IEngineResult;

public class HandshakingEngine extends CloseableEngine {

	protected enum HandshakeState { NONE, PENDING, FINISHING, FINISHED };
	
	private final boolean clientMode;
	
	private HandshakeState state = HandshakeState.NONE;
	
	public HandshakingEngine(int offset, boolean clientMode) {
		super(offset);
		this.clientMode = clientMode;
	}
	
	@SuppressWarnings("incomplete-switch")
	private IEngineResult clientHandshaking(ByteBuffer src, ByteBuffer dst) {
		switch (state) {
		case NONE:
			int offsetInc = new Random().nextInt(254) + 1;
			int prevPosition = dst.position();
			
			dst.put(Packet.encode(offset, new byte[] {(byte)offsetInc}));
			offset += offsetInc;
			state = HandshakeState.PENDING;
			return Result.needUnwrap(dst.position() - prevPosition);
			
		case PENDING:
			int prevRemaining = src.remaining();
			int size = Packet.decodeSize(offset, src);
			
			if (size == -1) {
				return Result.BUFFER_UNDERFLOW_NEED_UNWRAP;
			}
			
			byte[] decoded = Packet.decode(offset, src, size);
			if (decoded.length != 1 || decoded[0] != 0) {
				throw new IllegalArgumentException();
			}

			state = HandshakeState.FINISHED;
			return Result.finished(prevRemaining - src.remaining(), 0);
			
		}
		return null;
	}

	@SuppressWarnings("incomplete-switch")
	private IEngineResult serverHandshaking(ByteBuffer src, ByteBuffer dst) {
		switch (state) {
		case NONE:
			state = HandshakeState.PENDING;
			return Result.needUnwrap(0);
			
		case PENDING:
			int prevRemaining = src.remaining();
			int size = Packet.decodeSize(offset, src);
			
			if (size == -1) {
				return Result.BUFFER_UNDERFLOW_NEED_UNWRAP;
			}
			
			byte[] decoded = Packet.decode(offset, src, size);
			if (decoded.length != 1) {
				throw new IllegalArgumentException();
			}

			offset += decoded[0];
			state = HandshakeState.FINISHING;
			return Result.needWrap(prevRemaining - src.remaining());

		case FINISHING:
			int prevPosition = dst.position();

			dst.put(Packet.encode(offset, new byte[] {0}));
			state = HandshakeState.FINISHED;
			return Result.finished(0, dst.position() - prevPosition);
			
		}
		return null;
	}
	
	@Override
	public IEngineResult preWrap(ByteBuffer[] srcs, ByteBuffer dst) {
		IEngineResult result;
		
		result = clientMode ? clientHandshaking(null, dst) : serverHandshaking(null, dst);
		if (result != null) {
			return result;
		}
		return super.preWrap(srcs, dst);
	}

	@Override
	public IEngineResult preUnwrap(ByteBuffer src, ByteBuffer dst) {
		IEngineResult result;

		result = clientMode ? clientHandshaking(src, dst) : serverHandshaking(src, dst);
		if (result != null) {
			return result;
		}
		return super.preUnwrap(src, dst);
	}
}
