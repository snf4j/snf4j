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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.Version;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.InitialPacket;

public class AntiAmplificatorTest extends CommonTest {

	List<IPacket> packets;
	
	int[] lengths;
	
	QuicState state;
	
	AntiAmplificator aa;
	
	@Override
	public void before() throws Exception {
		super.before();
		packets = new ArrayList<>();
		packets.add(new InitialPacket(bytes("00"), 0, bytes("01"), Version.V1, bytes("")));
		lengths = new int[] {100};
		state = new QuicState(false);
		aa = new AntiAmplificator(state);
	}
	
	@Test
	public void testDisarm() {
		assertTrue(aa.isArmed());
		assertFalse(aa.accept(1));
		assertTrue(aa.accept(0));
		assertTrue(aa.isBlocked());
		aa.block(new byte[1], packets, lengths);
		assertTrue(aa.needUnblock());
		assertNotNull(aa.getBlockedPackets());
		assertNotNull(aa.getBlockedData());
		assertNotNull(aa.getBlockedLengths());
		
		aa.disarm();
		assertFalse(aa.isArmed());
		assertTrue(aa.accept(100));
		assertTrue(aa.accept(0));
		assertFalse(aa.isBlocked());
		aa.block(new byte[1], packets, lengths);
		assertFalse(aa.needUnblock());
		assertNull(aa.getBlockedPackets());
		assertNull(aa.getBlockedData());
		assertNull(aa.getBlockedLengths());
	}
	
	@Test
	public void testAccept() {
		assertTrue(aa.accept(0));
		assertFalse(aa.accept(1));
		aa.incReceived(10);
		assertTrue(aa.accept(10));
		assertTrue(aa.accept(20));
		assertFalse(aa.accept(1));
		aa.incReceived(5);
		assertFalse(aa.accept(16));
		assertTrue(aa.accept(15));	
	}
	
	@Test
	public void testIsBlocked() {
		assertTrue(aa.isBlocked());
		aa.incReceived(10);
		assertFalse(aa.isBlocked());
		assertTrue(aa.accept(30));
		assertFalse(aa.isBlocked());
		assertFalse(aa.accept(1));
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertTrue(aa.accept(3));
		assertFalse(aa.accept(1));
		
		aa.incReceived(10);
		assertFalse(aa.accept(31));
		assertFalse(aa.isBlocked());
		aa.block(new byte[31], packets, lengths);
		assertTrue(aa.isBlocked());
		assertTrue(aa.needUnblock());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnblock());
		aa.unblock();
		assertFalse(aa.accept(3));
		assertTrue(aa.accept(2));
		assertFalse(aa.isBlocked());
		
		assertFalse(aa.accept(30));
		aa.block(new byte[30], packets, lengths);
		assertTrue(aa.isBlocked());
		aa.incReceived(9);
		assertTrue(aa.isBlocked());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		aa.unblock();
		assertFalse(aa.isBlocked());
		assertFalse(aa.accept(1));
		
		aa.block(new byte[1], packets, lengths);
		assertTrue(aa.isBlocked());
		state.setAddressValidated();
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnblock());
	}
	
	@Test
	public void testBlock() {
		assertFalse(aa.needUnblock());
		aa.block(bytes("010203"), packets, lengths);
		assertTrue(aa.needUnblock());
		assertArrayEquals(bytes("010203"), aa.getBlockedData());
		assertSame(packets, aa.getBlockedPackets());
		assertSame(lengths, aa.getBlockedLengths());
		assertTrue(aa.isBlocked());
		aa.incReceived(10);
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnblock());
		aa.unblock();
		assertFalse(aa.needUnblock());
		assertNull(aa.getBlockedData());
		assertNull(aa.getBlockedPackets());
		assertNull(aa.getBlockedLengths());
	}
}
