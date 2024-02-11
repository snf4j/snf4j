/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023-2024 SNF4J contributors
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
package org.snf4j.tls.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.handshake.Certificate;
import org.snf4j.tls.handshake.ICertificateEntry;

public class CertificateConsumerTest {
	
	@Test
	public void testNoCertificateFromServer() throws Exception {
		CertificateConsumer c = new CertificateConsumer();
		EngineState state = new EngineState(MachineState.CLI_WAIT_CERT, null, null, null);
		try {
			c.consume(state, new Certificate(new byte[10], new ICertificateEntry[0]), new ByteBuffer[0], false);
			fail();
		}
		catch (DecodeErrorAlert e) {
			assertEquals("Empty server certificate message", e.getMessage());
		}
	}
	
	@Test
	public void testUnexpectedMessage() throws Exception {
		CertificateConsumer c = new CertificateConsumer();
		List<MachineState> validStates = Arrays.asList(
				MachineState.CLI_WAIT_CERT_CR, 
				MachineState.CLI_WAIT_CERT, 
				MachineState.SRV_WAIT_CERT);
		Certificate cert = new Certificate(new byte[10], new ICertificateEntry[0]);
		
		for (MachineState s: MachineState.values()) {
			if (validStates.contains(s)) {
				continue;
			}
			try {
				c.consume(new EngineState(s, null, null, null), cert, new ByteBuffer[0], false);
				fail();
			}
			catch (UnexpectedMessageAlert e) {
				assertEquals("Unexpected Certificate", e.getMessage());
			}
		}
	}
}
