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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
	public void testName() {
		assertEquals("anti-amplificator", aa.name());
	}
	
	@Test
	public void testDisarm() {
		assertTrue(aa.isArmed());
		assertEquals(0, aa.available());
		assertTrue(aa.isBlocked());
		aa.lock(1);
		assertTrue(aa.needUnlock());
		
		aa.disarm();
		assertFalse(aa.isArmed());
		assertEquals(Integer.MAX_VALUE, aa.available());
		assertFalse(aa.isBlocked());
		aa.lock(1);
		assertFalse(aa.needUnlock());
	}
	
	@Test
	public void testAvailable() {
		assertEquals(0, aa.available());
		aa.incReceived(10);
		assertEquals(30, aa.available());
		aa.incReceived(5);
		assertEquals(45, aa.available());
		aa.incSent(4);
		assertEquals(41, aa.available());
	}
	
	@Test
	public void testIsBlocked() {
		assertTrue(aa.isBlocked());
		aa.incReceived(10);
		assertEquals(30, aa.available());
		assertFalse(aa.isBlocked());
		aa.incSent(30);
		assertTrue(aa.isBlocked());
		assertEquals(0, aa.available());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertEquals(3, aa.available());
		aa.incSent(3);
		assertEquals(0, aa.available());
		
		aa.incReceived(10);
		assertEquals(30, aa.available());
		assertFalse(aa.isBlocked());
		aa.lock(31);
		assertTrue(aa.isBlocked());
		assertTrue(aa.needUnlock());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnlock());
		aa.unlock();
		assertEquals(33, aa.available());
		aa.incSent(33);
		assertEquals(0, aa.available());
		assertTrue(aa.isBlocked());
		
		aa.lock(30);
		assertTrue(aa.isBlocked());
		aa.incReceived(9);
		assertTrue(aa.isBlocked());
		aa.incReceived(1);
		assertFalse(aa.isBlocked());
		aa.unlock();
		assertEquals(30, aa.available());
		aa.incSent(30);
		
		assertTrue(aa.isBlocked());
		assertEquals(0, aa.available());
		
		aa.lock(1);
		assertTrue(aa.isBlocked());
		state.setAddressValidated();
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnlock());
	}
	
	@Test
	public void testBlock() {
		assertFalse(aa.needUnlock());
		aa.lock(3);
		assertTrue(aa.needUnlock());
		assertTrue(aa.isBlocked());
		aa.incReceived(10);
		assertFalse(aa.isBlocked());
		assertTrue(aa.needUnlock());
		aa.unlock();
		assertFalse(aa.needUnlock());
	}
}
