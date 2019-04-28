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

import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;

public class BasicEngine extends AbstractEngine {

	protected int offset;
	
	private boolean finished;
	
	public BasicEngine(int offset) {
		this.offset = offset;
	}

	protected IEngineResult closeWrap(ByteBuffer dst) {
		return Result.CLOSED;
	}
	
	protected IEngineResult preWrap(ByteBuffer[] srcs, ByteBuffer dst) {
		if (outboundDone == CloseType.PENDING) {
			IEngineResult result = closeWrap(dst);
			
			if (result.getStatus() == Status.CLOSED) {
				outboundDone = CloseType.DONE;
			}
			return result;
		}
		else if (outboundDone == CloseType.DONE) {
			return Result.CLOSED;
		}
		
		if (!finished) {
			finished = true;
			return Result.FINISHED;
		}
		return null;
	}
	
	@Override
	public IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst)
			throws Exception {
		
		IEngineResult preResult = preWrap(srcs, dst);
		if (preResult != null) {
			return updateHandshakeStatus(preResult); 
		}
		
		int maxSize = Packet.calculateMaxData(dst.remaining());
		
		if (maxSize <= 0) {
			return updateHandshakeStatus(Result.BUFFER_OVERFLOW);
		}
		
		ByteBuffer encoded = Packet.encode(offset, srcs, maxSize);
		
		int prevRemaining = dst.remaining();
		int consumed;
		
		if (encoded != null) {
			consumed = encoded.limit() - Packet.MIN_SIZE;
			dst.put(encoded);
		}
		else {
			consumed = 0;
		}
		return new EngineResult(Status.OK, getHandshakeStatus(), consumed, prevRemaining - dst.remaining());
	}

	protected IEngineResult closeUnwrap(ByteBuffer dst) {
		return Result.CLOSED;
	}
	
	protected boolean isClosingUnwrap(byte[] data) {
		return false;
	}
	
	protected IEngineResult preUnwrap(ByteBuffer src, ByteBuffer dst) {

		if (inboundDone == CloseType.PENDING) {
			IEngineResult result = closeUnwrap(dst);
			
			if (result.getStatus() == Status.CLOSED) {
				inboundDone = CloseType.DONE;
			}
			return result;
		}
		else if (inboundDone == CloseType.DONE) {
			return Result.CLOSED;
		}

		
		if (!finished) {
			finished = true;
			return Result.FINISHED;
		}
		return null;
	}
	
	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst)
			throws Exception {

		IEngineResult preResult = preUnwrap(src, dst);
		if (preResult != null) {
			return updateHandshakeStatus(preResult); 
		}
		
		int size = Packet.decodeSize(offset, src);
		
		if (size == -1) {
			return updateHandshakeStatus(Result.BUFFER_UNDERFLOW);
		}
		else if (Packet.calculateMaxData(size) > dst.remaining()) {
			return updateHandshakeStatus(Result.BUFFER_OVERFLOW);
		}
		
		int prevRemaining = src.remaining();
		byte[] decoded = Packet.decode(offset, src, size);
		
		if (isClosingUnwrap(decoded)) {
			inboundDone = CloseType.DONE;
			outboundDone = CloseType.PENDING;
			return updateHandshakeStatus(Result.closedNeedWrap(prevRemaining - src.remaining()));
		}
		dst.put(decoded);
		return new EngineResult(Status.OK, getHandshakeStatus(), prevRemaining - src.remaining(), decoded.length);
	}

}
