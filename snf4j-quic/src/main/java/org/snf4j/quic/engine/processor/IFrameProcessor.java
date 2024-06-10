/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.engine.processor;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.packet.IPacket;

/**
 * A stateless processor for QUIC frames of the given type.
 * 
 * @param <F> the QUIC frame being processed by this processor
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
interface IFrameProcessor<F extends IFrame> {

	/**
	 * Returns the type of frames being processed by this processor.
	 * 
	 * @return the frame type
	 */
	FrameType getType();

	/**
	 * Processes received QUIC frame.
	 * 
	 * @param processor the QUIC processor associated with this frame processor
	 * @param frame     the received QUIC frame
	 * @param packet    the QUIC packet carrying the frame being processed
	 * @throws QuicException if an error occurred during processing of the frame
	 */
	void process(QuicProcessor processor, F frame, IPacket packet) throws QuicException;

	/**
	 * Called when the given frame is about to be sent that is the packet carrying
	 * it has been successfully protected and put into a destination buffer.
	 * 
	 * @param processor the QUIC processor associated with this frame processor
	 * @param frame     the QUIC frame to be sent
	 * @param packet    the QUIC packet carrying the frame
	 */
	void sending(QuicProcessor processor, F frame, IPacket packet);
}
