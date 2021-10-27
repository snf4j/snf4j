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
package org.snf4j.websocket.frame;

import java.util.List;

import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.IStreamSession;

/**
 * Aggregates fragmented Web Socket frames into complete (final) frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameAggregator implements IDecoder<Frame,Frame> {

	private final int maxAggregatedLength;
	
	private IAggregatedFrame frame;
	
	/**
	 * Construct a Web Socket frame aggregator.
	 * 
	 * @param maxAggregatedLength maximum length of a aggregated frame's payload
	 *                            data. Setting it to an appropriate value can
	 *                            prevent from denial of service attacks
	 */
	public FrameAggregator(int maxAggregatedLength) {
		this.maxAggregatedLength = maxAggregatedLength;
	}
	
	@Override
	public Class<Frame> getInboundType() {
		return Frame.class;
	}

	@Override
	public Class<Frame> getOutboundType() {
		return Frame.class;
	}
	
	private RuntimeException tooBig(ISession session, String message) throws InvalidFrameException {
		((IStreamSession)session).writenf(new CloseFrame(CloseFrame.TOO_BIG));
		return new InvalidFrameException(message);
	}

	@Override
	public void decode(ISession session, Frame data, List<Frame> out) throws Exception {
		switch (data.getOpcode()) {
		case BINARY:
			if (data.isFinalFragment()) {
				break;
			}
			frame = new AggregatedBinaryFrame(true, data.getRsvBits(), data.getPayload());
			return;
			
		case TEXT:
			if (data.isFinalFragment()) {
				break;
			}
			frame = new AggregatedTextFrame(true, data.getRsvBits(), data.getPayload());
			return;
			
		case CONTINUATION:
			if (frame != null) {
				if (frame.getPayloadLength() + data.getPayloadLength() > maxAggregatedLength) {
					throw tooBig(session, "Too big payload for aggregated frame");
				}
				frame.addFragment(data.getPayload());
				if (!data.isFinalFragment()) {
					return;
				}
				data = (Frame)frame;
				frame = null;
			}
			
		default:
		}
		out.add(data);
	} 

}
