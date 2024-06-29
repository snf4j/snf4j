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
 * A disarmed anti-amplificator that does not limit the amount of data an
 * endpoint sends to an unvalidated address.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class DisarmedAnitAmplificator implements IAntiAmplificator{

	/** A sateless instance of the disarmed anti-amplificator */
	public static IAntiAmplificator INSTANCE = new DisarmedAnitAmplificator();
	
	private DisarmedAnitAmplificator() {}
	
	@Override 
	public boolean isArmed() {
		return false;
	}

	@Override
	public void disarm() {
	}
	
	@Override
	public void incReceived(int amount) {
	}

	@Override
	public boolean accept(int amount) {
		return true;
	}

	@Override
	public boolean isBlocked() {
		return false;
	}

	@Override
	public void block(byte[] datagram, List<IPacket> packets, int[] lengths) {
	}

	@Override
	public boolean needUnblock() {
		return false;
	}
	
	@Override
	public void unblock() {
	}

	@Override
	public byte[] getBlockedData() {
		return null;
	}

	@Override
	public List<IPacket> getBlockedPackets() {
		return null;
	}

	@Override
	public int[] getBlockedLengths() {
		return null;
	}

	
}
