/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.session.ssl.ClientAuth;
import org.snf4j.tls.alert.DecryptErrorAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.handshake.CertificateVerify;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.ICertificateVerify;
import org.snf4j.tls.handshake.IHandshake;

public class CertificateVerifyConsumerTest extends EngineTest {

	@Override
	public void before() throws Exception {
		super.before();
		params.delegatedTaskMode = DelegatedTaskMode.NONE;
	}
	
	@Test
	public void testFailingServerCertificateValidation() throws Exception {
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(false, HandshakeType.CERTIFICATE_VERIFY);
		IHandshake h = c.get(false);
		assertNotNull(h);
		assertSame(HandshakeType.CERTIFICATE_VERIFY, h.getType());
		((ICertificateVerify)h).getSignature()[0] ^= 0xaa;
		h.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals("Failed to verify certificate", e.getMessage());
		}
	}

	@Test
	public void testFailingClientCertificateValidation() throws Exception {
		params.clientAuth = ClientAuth.REQUIRED;
		HandshakeEngine cli = new HandshakeEngine(true, params, handler, handler);
		HandshakeEngine srv = new HandshakeEngine(false, params, handler, handler);
		
		HandshakeController c = new HandshakeController(cli, srv);
		
		c.run(true, HandshakeType.CERTIFICATE_VERIFY);
		IHandshake h = c.get(true);
		assertNotNull(h);
		assertSame(HandshakeType.CERTIFICATE_VERIFY, h.getType());
		((ICertificateVerify)h).getSignature()[0] ^= 0xaa;
		h.prepare();
		try {
			c.run(false, null);
			fail();
		}
		catch (DecryptErrorAlert e) {
			assertEquals("Failed to verify certificate", e.getMessage());
		}
	}

	@Test
	public void testUnexpectedMessage() throws Exception {
		CertificateVerifyConsumer cr = new CertificateVerifyConsumer();
		List<MachineState> validStates = Arrays.asList(
				MachineState.CLI_WAIT_CV,
				MachineState.SRV_WAIT_CV);
		CertificateVerify certReq = new CertificateVerify(SignatureScheme.ECDSA_SECP256R1_SHA256, new byte[10]);
		
		for (MachineState s: MachineState.values()) {
			if (validStates.contains(s)) {
				continue;
			}
			try {
				cr.consume(new EngineState(s, null, null, null), certReq, new ByteBuffer[0], false);
				fail();
			}
			catch (UnexpectedMessageAlert e) {
				assertEquals("Unexpected CertificateVerify", e.getMessage());
			}
		}
	}
	
}
