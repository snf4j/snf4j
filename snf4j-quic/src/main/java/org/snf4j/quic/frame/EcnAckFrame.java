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
package org.snf4j.quic.frame;

import java.nio.ByteBuffer;

/**
 * An ACK frame with ECN counts as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class EcnAckFrame extends AckFrame {

	private final long ect0;
		
	private final long ect1;
		
	private final long ecnCe;
	
	/**
	 * Constructs a ACK frame with the given delay, ECN counts and only one packet
	 * number to acknowledge.
	 * 
	 * @param largestAcked the packet number to acknowledge
	 * @param delay        the encoded ACK delay
	 * @param ect0         the total number of packets received with the ECT(0)
	 *                     codepoint in the packet number space of this ACK frame
	 * @param ect1         the total number of packets received with the ECT(1)
	 *                     codepoint in the packet number space of this ACK frame
	 * @param ecnCe        the total number of packets received with the ECN-CE
	 *                     codepoint in the packet number space of this ACK frame
	 */
	public EcnAckFrame(long largestAcked, long delay, long ect0, long ect1, long ecnCe) {
		this(new AckRange[] { new AckRange(largestAcked)}, delay, ect0, ect1, ecnCe);
	}

	/**
	 * Constructs a ACK frame with the given delay, ECN counts and one or more
	 * packet number ranges to acknowledge.
	 * 
	 * @param ranges the packet number ranges to acknowledge
	 * @param delay  the encoded ACK delay
	 * @param ect0   the total number of packets received with the ECT(0) codepoint
	 *               in the packet number space of this ACK frame
	 * @param ect1   the total number of packets received with the ECT(1) codepoint
	 *               in the packet number space of this ACK frame
	 * @param ecnCe  the total number of packets received with the ECN-CE codepoint
	 *               in the packet number space of this ACK frame
	 * @throws IllegalArgumentException if the number of ranges is less than 1
	 */
	public EcnAckFrame(AckRange[] ranges, long delay, long ect0, long ect1, long ecnCe) {
		super(ranges, delay);
		this.ect0 = ect0; 
		this.ect1 = ect1;
		this.ecnCe = ecnCe;
	}

	@Override
	public int getLength() {
		return super.getLength()
			+ FrameUtil.encodedIntegerLength(ect0)
			+ FrameUtil.encodedIntegerLength(ect1)
			+ FrameUtil.encodedIntegerLength(ecnCe);
	}
	
	@Override
	public void getBytes(ByteBuffer dst) {
		getBytes((byte) 3, dst);
		FrameUtil.encodeInteger(ect0, dst);
		FrameUtil.encodeInteger(ect1, dst);
		FrameUtil.encodeInteger(ecnCe, dst);
	}
	
	@Override
	public boolean hasEcnCounts() {
		return true;
	}

	@Override
	public long getEct0Count() {
		return ect0;
	}

	@Override
	public long getEct1Count() {
		return ect1;
	}

	@Override
	public long getEcnCeCount() {
		return ecnCe;
	}
	
}
