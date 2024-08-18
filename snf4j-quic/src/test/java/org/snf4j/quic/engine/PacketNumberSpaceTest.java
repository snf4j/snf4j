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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.PingFrame;

public class PacketNumberSpaceTest {

	@Test
	public void testTypeOrdinal() {
		assertEquals(0, PacketNumberSpace.Type.INITIAL.ordinal());
		assertEquals(1, PacketNumberSpace.Type.HANDSHAKE.ordinal());
		assertEquals(2, PacketNumberSpace.Type.APPLICATION_DATA.ordinal());
	}
	
	@Test
	public void testNext() {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA, 10);
		
		assertEquals(0, s.next());
		assertEquals(1, s.next());
		assertSame(PacketNumberSpace.Type.APPLICATION_DATA, s.getType());
	}
	
	@Test
	public void testUpdate() throws Exception {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA, 10);
		
		assertEquals(-1, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.frames().fly(new PingFrame(), 100);
		s.updateAcked(100);
		assertEquals(100, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.updateAcked(99);
		assertEquals(100, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		s.frames().fly(new PingFrame(), 101);
		s.updateAcked(101);
		assertEquals(101, s.getLargestAcked());
		assertEquals(-1, s.getLargestProcessed());
		
		s.updateProcessed(50);
		assertEquals(101, s.getLargestAcked());
		assertEquals(50, s.getLargestProcessed());
		s.updateProcessed(49);
		assertEquals(101, s.getLargestAcked());
		assertEquals(50, s.getLargestProcessed());
		s.updateProcessed(51);
		assertEquals(101, s.getLargestAcked());
		assertEquals(51, s.getLargestProcessed());
		
		try {
			s.updateAcked(102);
			fail();
		}
		catch (QuicException e) {
		}
		s.frames().fly(new PingFrame(), 102);
		s.updateAcked(102);
	}
	
	@Test
	public void testAckRangeLimit() {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA, 2);
		
		s.acks().add(0, 1000);
		s.acks().add(2, 1000);
		s.acks().add(4, 1000);
		AckFrame ack = s.acks().build(4, 1000, 0);
		assertEquals(2, ack.getRanges().length);
		assertEquals(4, ack.getLargestPacketNumber());
		assertEquals(2, ack.getSmallestPacketNumber());
	}
	
	@Test
	public void testGetLastAckElictingTime() {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA, 2);
		
		assertEquals(0, s.getLastAckElicitingTime());
		s.setLastAckElicitingTime(11100022);
		assertEquals(11100022, s.getLastAckElicitingTime());
	}
	
	@Test
	public void testGetLossTime() {
		PacketNumberSpace s = new PacketNumberSpace(PacketNumberSpace.Type.APPLICATION_DATA, 2);
		
		assertEquals(0, s.getLossTime());
		s.setLossTime(1112);
		assertEquals(1112, s.getLossTime());
	}
	
	@Test
	public void testUpdateAckElicitingInFlight() {
		QuicState state = new QuicState(true);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.HANDSHAKE);
		assertFalse(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(1);
		assertTrue(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(-1);
		assertFalse(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(2);
		assertTrue(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(-1);
		assertTrue(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(-1);
		assertFalse(state.isAckElicitingInFlight());
		space.updateAckElicitingInFlight(1000);
		assertTrue(state.isAckElicitingInFlight());
		space.clearAckElicitingInFlight();
		assertFalse(state.isAckElicitingInFlight());
	}
	
	@Test
	public void testGetEcnCeCount() {
		QuicState state = new QuicState(true);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.HANDSHAKE);
		assertEquals(0, space.getEcnCeCount());
		space.setEcnCeCount(111);
		assertEquals(111, space.getEcnCeCount());
	}
	
	@Test
	public void testNeedSend() {
		QuicState state = new QuicState(true);
		PacketNumberSpace space = state.getSpace(EncryptionLevel.HANDSHAKE);
		assertFalse(space.needSend());
		space.frames().add(PingFrame.INSTANCE);
		assertTrue(space.needSend());
		space.frames().fly(PingFrame.INSTANCE, 0);
		assertFalse(space.needSend());
		space.acks().add(0, 1000);
		assertTrue(space.needSend());
		space.acks().keepPriorTo(0);
		assertFalse(space.needSend());
	}
}
