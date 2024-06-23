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

import org.junit.Before;
import org.junit.Test;

public class RttEstimatorTest {

	QuicState state;
	
	TestTime time;
	
	TestConfig config;
	
	@Before
	public void before() {
		time = new TestTime();
		config = new TestConfig();
		state = new QuicState(true, config, time);
	}
	
	long m2n(long millis) {
		return millis*1000*1000;
	}

	long m2u(long millis) {
		return millis*1000;
	}
	
	long s2n(long secs) {
		return secs*1000*1000*1000;
	}
	
	@Test
	public void testInitialValues() {
		RttEstimator e = new RttEstimator(state);
		
		assertEquals(0, e.getLatestRtt());
		assertEquals(0, e.getMinRtt());
		assertEquals(m2n(333), e.getSmoothedRtt());
		assertEquals(m2n(333) / 2, e.getRttVar());
	}
	
	@Test
	public void testSampleWithInvalidTimes() {
		RttEstimator e = new RttEstimator(state);
		
		e.addSample(s2n(1), s2n(1)+1, 0, EncryptionLevel.INITIAL);
		assertEquals(m2n(333), e.getSmoothedRtt());
		e.addSample(s2n(1), s2n(1), 0, EncryptionLevel.INITIAL);
		assertEquals(0, e.getSmoothedRtt());
	}
	
	@Test
	public void testFirstSample() {
		RttEstimator e = new RttEstimator(state);

		e.addSample(s2n(1)+2, s2n(1), 0, EncryptionLevel.INITIAL);
		assertEquals(2, e.getLatestRtt());
		assertEquals(2, e.getMinRtt());
		assertEquals(2, e.getSmoothedRtt());
		assertEquals(1, e.getRttVar());
	}
	
	@Test
	public void testSamplesForInitial() {
		RttEstimator e = new RttEstimator(state);

		e.addSample(m2n(100), 0, m2u(50), EncryptionLevel.INITIAL);
		assertEquals(m2n(100), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		assertEquals(m2n(100), e.getSmoothedRtt());
		assertEquals(m2n(50), e.getRttVar());
		
		e.addSample(m2n(200), 0, m2u(50), EncryptionLevel.INITIAL);
		assertEquals(m2n(200), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		long vvar = (3*m2n(50)+m2n(Math.abs(100-200)))/4;
		long vsmo = (7*m2n(100)+m2n(200))/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
		
		e.addSample(m2n(99), 0, m2u(50), EncryptionLevel.INITIAL);
		assertEquals(m2n(99), e.getLatestRtt());
		assertEquals(m2n(99), e.getMinRtt());
		vvar = (3*vvar + Math.abs(vsmo-m2n(99)))/4;
		vsmo = (7*vsmo + m2n(99))/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
	}
	
	@Test
	public void testSamplesForHandshake() {
		RttEstimator e = new RttEstimator(state);

		e.addSample(m2n(100), 0, m2u(50), EncryptionLevel.HANDSHAKE);
		assertEquals(m2n(100), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		assertEquals(m2n(100), e.getSmoothedRtt());
		assertEquals(m2n(50), e.getRttVar());
		
		e.addSample(m2n(200), 0, m2u(50), EncryptionLevel.HANDSHAKE);
		assertEquals(m2n(200), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		long vvar = (3*m2n(50)+m2n(Math.abs(100-200+50)))/4;
		long vsmo = (7*m2n(100)+m2n(200-50))/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
		
		e.addSample(m2n(100)-1, 0, 1, EncryptionLevel.HANDSHAKE);
		assertEquals(m2n(100)-1, e.getLatestRtt());
		assertEquals(m2n(100)-1, e.getMinRtt());
		vvar = (3*vvar + Math.abs(vsmo-m2n(100)+1))/4;
		vsmo = (7*vsmo + m2n(100)-1)/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
	}

	@Test
	public void testSamplesForAppData() {
		RttEstimator e = new RttEstimator(state);

		state.setPeerMaxAckDelay(30);
		e.addSample(m2n(100), 0, m2u(50), EncryptionLevel.APPLICATION_DATA);
		assertEquals(m2n(100), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		assertEquals(m2n(100), e.getSmoothedRtt());
		assertEquals(m2n(50), e.getRttVar());
		
		e.addSample(m2n(200), 0, m2u(50), EncryptionLevel.APPLICATION_DATA);
		assertEquals(m2n(200), e.getLatestRtt());
		assertEquals(m2n(100), e.getMinRtt());
		long vvar = (3*m2n(50)+m2n(Math.abs(100-200+30)))/4;
		long vsmo = (7*m2n(100)+m2n(200-30))/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
		
		e.addSample(m2n(100)-1, 0, 1, EncryptionLevel.APPLICATION_DATA);
		assertEquals(m2n(100)-1, e.getLatestRtt());
		assertEquals(m2n(100)-1, e.getMinRtt());
		vvar = (3*vvar + Math.abs(vsmo-m2n(100)+1))/4;
		vsmo = (7*vsmo + m2n(100)-1)/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());

		state.setPeerMaxAckDelay(70);
		e.addSample(m2n(200), 0, m2u(50), EncryptionLevel.APPLICATION_DATA);
		assertEquals(m2n(200), e.getLatestRtt());
		assertEquals(m2n(100)-1, e.getMinRtt());
		vvar = (3*vvar + Math.abs(vsmo-m2n(200-50)))/4;
		vsmo = (7*vsmo + m2n(200-50))/8;
		assertEquals(vsmo, e.getSmoothedRtt());
		assertEquals(vvar, e.getRttVar());
	
	}
}
