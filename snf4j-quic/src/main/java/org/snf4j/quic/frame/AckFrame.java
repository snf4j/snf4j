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
import java.util.LinkedList;
import java.util.List;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.tls.Args;

/**
 * An ACK frame without ECN counts as defined in RFC 9000. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class AckFrame implements IFrame {

	private final static FrameType TYPE = FrameType.ACK;
	
	private final long delay;
	
	private final AckRange[] ranges;
	
	private final static IFrameParser PARSER = new IFrameParser() {

		@Override
		public FrameType getType() {
			return TYPE;
		}

		private long checkNegative(long i) throws QuicException {
			if (i < 0) {
				throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Negative packet number in Ack range");
			}
			return i;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public AckFrame parse(ByteBuffer src, int remaining, int type) throws QuicException {
			if (remaining > 1) {
				int[] remainings = new int[] {remaining};
				long from = FrameUtil.decodeInteger(src, remainings);
				
				if (remainings[0] > 1) {
					long delay = FrameUtil.decodeInteger(src, remainings);
					
					if (remainings[0] > 1) {
						long count = FrameUtil.decodeInteger(src, remainings);
						
						if (remainings[0] >= 1) {
							long to = checkNegative(from - FrameUtil.decodeInteger(src, remainings));
							List<AckRange> ranges = new LinkedList<>();
							boolean ok = true;

							ranges.add(new AckRange(from, to));
							while (count-- > 0) {
								if (remainings[0] > 1) {
									from = checkNegative(to - FrameUtil.decodeInteger(src, remainings) -2);
									if (remainings[0] >= 1) {
										to = checkNegative(from - FrameUtil.decodeInteger(src, remainings));
										ranges.add(new AckRange(from, to));
									}
									else {
										ok = false;
										break;
									}
								}
								else {
									ok = false;
									break;
								}
							}
							
							if (ok) {
								if (type == FrameType.ACK.lastValue()) {
									if (remainings[0] > 1) {
										long ect0 = FrameUtil.decodeInteger(src, remainings);
										
										if (remainings[0] > 1) {
											long ect1 = FrameUtil.decodeInteger(src, remainings);
											
											if (remainings[0] >= 1) {
												long ecnCe = FrameUtil.decodeInteger(src, remainings);
												
												return new EcnAckFrame(
													ranges.toArray(new AckRange[ranges.size()]),
													delay,
													ect0,
													ect1,
													ecnCe);		
											}
										}
									}
								}
								else {
									return new AckFrame(
										ranges.toArray(new AckRange[ranges.size()]),
										delay);
								}
							}
						}
					}
				}
			}
			throw new QuicException(TransportError.FRAME_ENCODING_ERROR, "Inconsistent length of Ack frame");
		}
		
	};
	
	/**
	 * Constructs a ACK frame with the given delay and only one packet number to
	 * acknowledge.
	 * 
	 * @param largestAcked the packet number to acknowledge
	 * @param delay        the encoded ACK delay
	 */
	public AckFrame(long largestAcked, long delay) {
		ranges = new AckRange[] { new AckRange(largestAcked)};
		this.delay = delay;
	}

	/**
	 * Constructs a ACK frame with the given delay and one or more packet number
	 * ranges to acknowledge.
	 * 
	 * @param ranges the packet number ranges to acknowledge
	 * @param delay  the encoded ACK delay
	 * @throws IllegalArgumentException if the number of ranges is less than 1
	 */
	public AckFrame(AckRange[] ranges, long delay) {
		Args.checkMin(ranges, 1, "ranges");
		this.ranges = ranges;
		this.delay = delay;
	}
	
	/**
	 * Return the default ACK frame parser for both types of ACK frames.
	 * 
	 * @return the ACK frame parser
	 */
	public static IFrameParser getParser() {
		return PARSER;
	}
	
	@Override
	public FrameType getType() {
		return TYPE;
	}

	@Override
	public int getTypeValue() {
		return 0x02;
	}
	
	@Override
	public int getLength() {
		AckRange range = ranges[0];
		int len = 1;

		len += FrameUtil.encodedIntegerLength(range.getFrom());
		len += FrameUtil.encodedIntegerLength(delay);
		len += FrameUtil.encodedIntegerLength(ranges.length-1);
		len += FrameUtil.encodedIntegerLength(range.getFrom()-range.getTo());
		for (int i=1; i<ranges.length; ++i) {
			AckRange next = ranges[i];
			len += FrameUtil.encodedIntegerLength(range.getTo()-next.getFrom()-2);
			len += FrameUtil.encodedIntegerLength(next.getFrom()-next.getTo());
			range = next;
		}
		return len;
	}
	
	@Override
	public void getBytes(ByteBuffer dst) {
		AckRange range = ranges[0];
		
		dst.put((byte) getTypeValue());
		FrameUtil.encodeInteger(range.getFrom(), dst);
		FrameUtil.encodeInteger(delay, dst);
		FrameUtil.encodeInteger(ranges.length-1, dst);
		FrameUtil.encodeInteger(range.getFrom()-range.getTo(), dst);
		for (int i=1; i<ranges.length; ++i) {
			AckRange next = ranges[i];
			FrameUtil.encodeInteger(range.getTo()-next.getFrom()-2, dst);
			FrameUtil.encodeInteger(next.getFrom()-next.getTo(), dst);
			range = next;
		}
	}

	/**
	 * Returns the encoded ACK delay in microseconds.
	 * 
	 * @return the encoded ACK delay
	 */
	public long getDelay() {
		return delay;
	}

	/**
	 * Returns all packet number ranges being acknowledged by this frame.
	 * 
	 * @return all packet number ranges
	 */
	public AckRange[] getRanges() {
		return ranges;
	}

	/**
	 * Returns the smallest packet number being acknowledged by this frame.
	 * 
	 * @return the smallest packet number
	 */
	public long getSmallestPacketNumber() {
		return ranges[ranges.length-1].getTo();
	}

	/**
	 * Returns the largest packet number being acknowledged by this frame.
	 * 
	 * @return the largest packet number
	 */
	public long getLargestPacketNumber() {
		return ranges[0].getFrom();
	}
	
	/**
	 * Tells if this ACK frame carries the ECN counts
	 * 
	 * @return {@code true} if the ECN counts are carried by the ACK frame
	 */
	public boolean hasEcnCounts() {
		return false;
	}
	
	/**
	 * Returns the total number of packets received with the ECT(0) codepoint in the
	 * packet number space of this ACK frame
	 * 
	 * @return the total number of packets received with the ECT(0) codepoint, or -1
	 *         if it is not carried
	 */
	public long getEct0Count() {
		return -1;
	}

	/**
	 * Returns the total number of packets received with the ECT(1) codepoint in the
	 * packet number space of this ACK frame
	 * 
	 * @return the total number of packets received with the ECT(1) codepoint, or -1
	 *         if it is not carried
	 */
	public long getEct1Count() {
		return -1;
	}

	/**
	 * Returns the total number of packets received with the ECN-CE codepoint in the
	 * packet number space of this ACK frame
	 * 
	 * @return the total number of packets received with the ECN-CE codepoint, or -1
	 *         if it is not carried
	 */
	public long getEcnCeCount() {
		return -1;
	}

}
