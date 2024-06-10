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

import java.util.List;

import org.snf4j.quic.Version;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;

/**
 * A {@code class} providing additional information about QUIC frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class FrameInfo {

	private final static String[] DATA = new String[] {
		"00;PADDING;IH01;NP",
		"01;PING;IH01",
		"02;ACK;IH_1;NC",
		"03;ACK;IH_1;NC",
		"04;RESET_STREAM;__01",	
		"05;STOP_SENDING;__01",	
		"06;CRYPTO;IH_1",	
		"07;NEW_TOKEN;___1",	
		"08;STREAM;__01;F",
		"09;STREAM;__01;F",
		"0a;STREAM;__01;F",
		"0b;STREAM;__01;F",
		"0c;STREAM;__01;F",
		"0d;STREAM;__01;F",
		"0e;STREAM;__01;F",
		"0f;STREAM;__01;F",
		"10;MAX_DATA;__01",
		"11;MAX_STREAM_DATA;__01",	
		"12;MAX_STREAMS;__01",	
		"13;MAX_STREAMS;__01",	
		"14;DATA_BLOCKED;__01",	
		"15;STREAM_DATA_BLOCKED;__01",	
		"16;STREAMS_BLOCKED;__01",	
		"17;STREAMS_BLOCKED;__01",	
		"18;NEW_CONNECTION_ID;__01;P",
		"19;RETIRE_CONNECTION_ID;__01",	
		"1a;PATH_CHALLENGE;__01;P",
		"1b;PATH_RESPONSE;___1;P",
		"1c;CONNECTION_CLOSE;IH01;N",
		"1d;CONNECTION_CLOSE;__01;N",
		"1e;HANDSHAKE_DONE;___1"	
	};
	
	private final static Info[] INFOS = new Info[DATA.length];
	
	static {
		for (int i=0; i<DATA.length; ++i) {
			String[] s = DATA[i].split(";");
			int typeValue = Integer.parseInt(s[0], 16);
			String pkts = s[2];
			String spec = s.length > 3 ? s[3] : "";
			Info info = new Info();
			
			info.allowedPackets[PacketType.INITIAL.ordinal()] = pkts.charAt(0) == 'I';
			info.allowedPackets[PacketType.HANDSHAKE.ordinal()] = pkts.charAt(1) == 'H';
			info.allowedPackets[PacketType.ZERO_RTT.ordinal()] = pkts.charAt(2) == '0';
			info.allowedPackets[PacketType.ONE_RTT.ordinal()] = true;
			
			info.ackEliciting = !spec.contains("N");
			info.congestionControlled = !spec.contains("C");
			info.pathProbing = spec.contains("P");
			info.flowControlled = spec.contains("F");
			
			INFOS[typeValue] = info;
		}
	}
	
	private final static FrameInfo FRAME_INFO = new FrameInfo();
	
	private FrameInfo() {}
	
	/**
	 * Returns an instance of the frame information object for the given QUIC
	 * version.
	 * 
	 * @param version the QUIC version
	 * @return the instance of the frame information object
	 */
	public static FrameInfo of(Version version) {
		return FRAME_INFO;
	}
	
	/**
	 * Tells if a frame of the given type can be carried by a packet of the given
	 * type.
	 * 
	 * @param packetType the packet type
	 * @param frameType  the frame type
	 * @return {@code true} if a frame of the given type can be carried by a packet of
	 *         the given type
	 */
	public boolean isValid(PacketType packetType, int frameType) {
		return INFOS[frameType].allowedPackets[packetType.ordinal()];
	}
	
	/**
	 * Tells if a frame of the given type is an ack-eliciting frame.
	 * 
	 * @param frameType the frame type
	 * @return {@code true} a frame of the given type is an ack-eliciting frame
	 */
	public boolean isAckEliciting(int frameType) {
		return INFOS[frameType].ackEliciting;
	}

	/**
	 * Tells if a frame of the given type counts toward bytes in flight for
	 * congestion control purposes.
	 * 
	 * @param frameType the frame type
	 * @return {@code true} if a frame of the given type counts toward bytes in
	 *         flight for congestion control purposes
	 */
	public boolean isCongestionControlled(int frameType) {
		return INFOS[frameType].congestionControlled;
	}

	/**
	 * Tells if a frame of the given type can be used to probe new network paths
	 * during connection migration.
	 * 
	 * @param frameType the frame type
	 * @return {@code true} if a frame of the given type can be used to probe new
	 *         network paths during connection migration
	 */
	public boolean isPathProbing(int frameType) {
		return INFOS[frameType].pathProbing;
	}

	/**
	 * Tells if a frame of the given type is flow controlled.
	 * 
	 * @param frameType the frame type
	 * @return {@code true} if a frame of the given type is flow controlled
	 */
	public boolean isFlowControlled(int frameType) {
		return INFOS[frameType].flowControlled;
	}
	
	/**
	 * Tells if the given packet contains only frames which can be carried by this
	 * packet type.
	 * 
	 * @param packet the given packet
	 * @return {@code true} if the packet contains only frames which can be carried
	 *         by this packet type
	 */
	public boolean isValid(IPacket packet) {
		List<IFrame> frames = packet.getFrames();
		
		if (frames != null) {
			PacketType type = packet.getType();
			
			for (IFrame frame: frames) {
				if (!isValid(type, frame.getTypeValue())) {
					return false;
				}
			}			
		}
		return true;
	}
	
	/**
	 * Tells if the given packet is an ack-eliciting packet.
	 * 
	 * @param packet the given packet
	 * @return {@code true} if the packet is an ack-eliciting packet
	 */
	public boolean isAckEliciting(IPacket packet) {
		List<IFrame> frames = packet.getFrames();
		
		if (frames != null) {
			for (IFrame frame: frames) {
				if (isAckEliciting(frame.getTypeValue())) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static class Info {
		
		boolean[] allowedPackets = new boolean[PacketType.values().length];
		
		boolean ackEliciting;
		
		boolean congestionControlled;
		
		boolean pathProbing;
		
		boolean flowControlled;
	}
}
