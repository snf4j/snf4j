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

import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.handler.SessionIncidentException;

public class CloseableEngine extends BasicEngine {

	private boolean closeReceived;
	
	public CloseableEngine(int offset) {
		super(offset);
	}
	
	@Override
	public void closeInbound() throws SessionIncidentException {
		super.closeInbound();
		if (!closeReceived) {
			throw new SessionIncidentException("Closed procedure not followed by peer" , 
					SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE);
		}
	}
	
	
	protected boolean isClosingUnwrap(byte[] data) {
		boolean isClosing = Packet.isClose(data);
		
		if (isClosing) {
			closeReceived = true;
		}
		
		return isClosing;
	}
	
	@Override
	protected IEngineResult closeWrap(ByteBuffer dst) {
		int prevRemaining = dst.remaining();
		
		if (Packet.calculateMaxData(prevRemaining) < Packet.getCloseData().length) {
			return Result.BUFFER_OVERFLOW_NEED_WRAP;
		}
		
		dst.put(Packet.encode(offset, Packet.getCloseData()));
		return Result.closed(prevRemaining - dst.remaining());
	}
	
}
