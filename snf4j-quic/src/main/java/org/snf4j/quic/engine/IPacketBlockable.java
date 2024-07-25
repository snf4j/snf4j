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
package org.snf4j.quic.engine;

import java.util.List;

import org.snf4j.quic.packet.IPacket;

/**
 * An object that provides packet blocking functionality.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IPacketBlockable {
		
	/**
	 * Tells if blocked. In the blocked state no data should be sent to peer.
	 * 
	 * @return {@code true} if blocked
	 */
	boolean isBlocked();
	
	/**
	 * Saves data (a full datagram) that could not be sent due to exceeding the limit.
	 * 
	 * @param data    the data that could not be sent
	 * @param packets the packets encrypted in the data
	 * @param lengths the lengths of packets encrypted in the data
	 */
	void block(byte[] data, List<IPacket> packets, int[] lengths);
	
	/**
	 * Tells if some data has been saved and has not been unblocked yet.
	 * 
	 * @return {@code true} if some data have been saved and has not been unblocked yet
	 */
	boolean needUnblock();
	
	/**
	 * Unblocks by clearing the saved data.
	 */
	void unblock();
	
	/**
	 * Returns the blocked data that could not be sent recently due to exceeding the
	 * limit.
	 * 
	 * @return the blocked data, or {@code null} if no data is saved
	 */
	byte[] getBlockedData();
	
	/**
	 * Returns the blocked packets that could not be sent recently due to exceeding
	 * the limit.
	 * 
	 * @return the blocked packets, or {@code null} if no data is saved
	 */
	List<IPacket> getBlockedPackets();
	
	/**
	 * Returns the lengths of blocked packets that could not be sent recently due to
	 * exceeding the limit.
	 * 
	 * @return the lengths of blocked packets, or {@code null} if no data is saved
	 */
	int[] getBlockedLengths();

}
