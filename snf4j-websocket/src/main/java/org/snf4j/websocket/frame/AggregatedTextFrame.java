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

/**
 * Aggregated Web Socket text frame.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AggregatedTextFrame extends TextFrame implements IAggregatedFrame {
	
	private final PayloadAggregator aggregator;

	/**
	 * Constructs an aggregated Web Socket text frame.
	 * <p>
	 * NOTE: The binary data in the {@code payload} argument should be a valid UTF-8
	 * encoding.
	 * 
	 * @param finalFragment determines if the created frame is the final fragment in
	 *                      a message
	 * @param rsvBits       reserved bits for extensions or future versions
	 * @param payload       payload data
	 */
	public AggregatedTextFrame(boolean finalFragment, int rsvBits, byte[] payload) {
		super(finalFragment, rsvBits, EMPTY_PAYLOAD);
		aggregator = new PayloadAggregator(payload);
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * NOTE: The binary data in the {@code payload} argument should be a valid UTF-8
	 * encoding.
	 */
	@Override
	public void addFragment(byte[] payload) {
		aggregator.add(payload);
	}
	
	public List<byte[]> getFragments() {
		return aggregator.getFragments();
	}
	
	@Override
	public int getPayloadLength() {
		return aggregator.getLength();
	}
	
	@Override
	public byte[] getPayload() {
		return aggregator.get();
	}
	
	/**
	 * Returns a text created by decoding the specified array of bytes in the UTF-8
	 * encoding.
	 * 
	 * @param fragment the array of bytes to decode the text from
	 * @return the text being decoded
	 */
	public static String toText(byte[] fragment) {
		return fromBytes(fragment);
	}

}
