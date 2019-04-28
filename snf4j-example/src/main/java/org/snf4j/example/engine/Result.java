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

import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.Status;

public class Result {

	static final EngineResult BUFFER_OVERFLOW = new EngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);   
	
	static final EngineResult BUFFER_OVERFLOW_NEED_WRAP = new EngineResult(Status.BUFFER_OVERFLOW, HandshakeStatus.NEED_WRAP, 0, 0);   

	static final EngineResult BUFFER_UNDERFLOW = new EngineResult(Status.BUFFER_UNDERFLOW, HandshakeStatus.NOT_HANDSHAKING, 0, 0);   

	static final EngineResult BUFFER_UNDERFLOW_NEED_UNWRAP = new EngineResult(Status.BUFFER_UNDERFLOW, HandshakeStatus.NEED_UNWRAP, 0, 0);   

	static final EngineResult FINISHED = new EngineResult(Status.OK, HandshakeStatus.FINISHED, 0, 0);   

	static final EngineResult CLOSED = new EngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, 0);   

	static final EngineResult CLOSED_NEED_WRAP = new EngineResult(Status.CLOSED, HandshakeStatus.NEED_WRAP, 0, 0);   

	
	static EngineResult closedNeedWrap(int bytesConsumed) {
		return new EngineResult(Status.CLOSED, HandshakeStatus.NEED_WRAP, bytesConsumed, 0);
	}

	static EngineResult closed(int bytesProduced) {
		return new EngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, bytesProduced);
	}

	static EngineResult needUnwrap(int bytesProduced) {
		return new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 0, bytesProduced);
	}

	static EngineResult needWrap(int bytesConsumed) {
		return new EngineResult(Status.OK, HandshakeStatus.NEED_WRAP, bytesConsumed, 0);
	}

	static EngineResult finished(int bytesConsumed, int bytesProduced) {
		return new EngineResult(Status.OK, HandshakeStatus.FINISHED, bytesConsumed, bytesProduced);
	}
}
