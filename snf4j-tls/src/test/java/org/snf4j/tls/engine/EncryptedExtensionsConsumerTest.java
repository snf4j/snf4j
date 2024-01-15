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

import static org.junit.Assert.assertSame;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Mac;

import org.junit.Test;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.IllegalParameterAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.Hkdf;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.crypto.TranscriptHash;
import org.snf4j.tls.extension.ALPNExtension;
import org.snf4j.tls.extension.EarlyDataExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.handshake.EncryptedExtensions;
import org.snf4j.tls.handshake.HandshakeType;

public class EncryptedExtensionsConsumerTest extends EngineTest {

	EncryptedExtensionsConsumer consumer;
	
	List<IExtension> extensions;
	
	EngineState state;
	
	@Override
	public void before() throws Exception {
		super.before();
		consumer = new EncryptedExtensionsConsumer();
		extensions = new ArrayList<IExtension>();
		state = new EngineState(MachineState.CLI_WAIT_EE,params, handler, handler);
		TranscriptHash th = new TranscriptHash(MessageDigest.getInstance("SHA-256"));
		Hkdf h = new Hkdf(Mac.getInstance("HmacSHA256"));
		KeySchedule ks = new KeySchedule(h, th, CipherSuite.TLS_AES_128_GCM_SHA256.spec());
		state.initialize(ks, CipherSuite.TLS_AES_128_GCM_SHA256);
	}
	
	EncryptedExtensions encryptedExtensions() {
		return new EncryptedExtensions(extensions);
	}
	
	@Test
	public void testType() {
		assertSame(HandshakeType.ENCRYPTED_EXTENSIONS, consumer.getType());
	}
	
	@Test
	public void testConsumeEarlyDataNoCtx() throws Alert {
		EncryptedExtensions ee = encryptedExtensions();
		extensions.add(new EarlyDataExtension());
		consumer.consume(state, ee, data(ee), false);
		assertSame(MachineState.CLI_WAIT_CERT_CR, state.getState());
	}
	
	@Test
	public void testConsumeEarlyDataWithCtx() throws Alert {
		EncryptedExtensions ee = encryptedExtensions();
		extensions.add(new EarlyDataExtension());
		state.setEarlyDataContext(new EarlyDataContext(CipherSuite.TLS_AES_128_CCM_SHA256, 1000));
		consumer.consume(state, ee, data(ee), false);
		assertSame(MachineState.CLI_WAIT_CERT_CR, state.getState());
		assertSame(EarlyDataState.PROCESSING, state.getEarlyDataContext().getState());
	}

	@Test
	public void testConsumeNoEarlyDataWithCtx() throws Alert {
		EncryptedExtensions ee = encryptedExtensions();
		state.setEarlyDataContext(new EarlyDataContext(CipherSuite.TLS_AES_128_CCM_SHA256, 1000));
		consumer.consume(state, ee, data(ee), false);
		assertSame(MachineState.CLI_WAIT_CERT_CR, state.getState());
		assertSame(EarlyDataState.REJECTED, state.getEarlyDataContext().getState());
	}

	@Test
	public void testConsumeWithPsk() throws Exception {
		EncryptedExtensions ee = encryptedExtensions();
		state.getKeySchedule().deriveEarlySecret(new byte[32], false);
		consumer.consume(state, ee, data(ee), false);
		assertSame(MachineState.CLI_WAIT_FINISHED, state.getState());
	}
	
	@Test(expected=UnexpectedMessageAlert.class)
	public void testInvalidMachineSate() throws Exception {
		EncryptedExtensions ee = encryptedExtensions();
		state.changeState(MachineState.CLI_WAIT_1_SH);
		consumer.consume(state, ee, data(ee), false);
	}

	@Test(expected=IllegalParameterAlert.class)
	public void testUnexpectedALPN() throws Exception {
		EncryptedExtensions ee = encryptedExtensions();
		extensions.add(new ALPNExtension("proto"));
		consumer.consume(state, ee, data(ee), false);
	}
	
	@Test(expected=IllegalParameterAlert.class)
	public void testUnexpectedProtocolName() throws Exception {
		EncryptedExtensions ee = encryptedExtensions();
		extensions.add(new ALPNExtension("proto"));
		params.applicationProtocols = new String[] {"xxx", "yyy"};
		consumer.consume(state, ee, data(ee), false);
	}

}
