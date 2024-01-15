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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Test;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadDecrypt;
import org.snf4j.tls.crypto.AeadEncrypt;
import org.snf4j.tls.crypto.KeySchedule;
import org.snf4j.tls.record.Decryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.RecordType;

public class EngineStateListenerTest extends EngineTest {

	@Test
	public void testOnNewTrafficSecretsWithNextGen() throws Exception {
		new EngineStateListener().onNewTrafficSecrets(null, RecordType.NEXT_GEN);
	}

	@Test(expected = InternalErrorAlert.class)
	public void testOnNewTrafficSecretsException() throws Exception {
		new EngineStateListener().onNewTrafficSecrets(null, RecordType.HANDSHAKE);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testOnNewReceivingTraficKeyException() throws Exception {
		EngineState state = new EngineState(MachineState.SRV_CONNECTED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		new EngineStateListener().onNewReceivingTraficKey(state, RecordType.NEXT_GEN);
	}
	
	@Test(expected = InternalErrorAlert.class)
	public void testOnNewSendingTraficKeyException() throws Exception {
		EngineState state = new EngineState(MachineState.SRV_CONNECTED, params, handler, handler) {
			
			@Override
			public KeySchedule getKeySchedule() {
				throw new NullPointerException();
			}
		};
		new EngineStateListener().onNewSendingTraficKey(state, RecordType.NEXT_GEN);
	}
	
	Encryptor[] encryptors(EngineStateListener l) throws Exception {
		Field f = EngineStateListener.class.getDeclaredField("encryptors");
		f.setAccessible(true);
		return (Encryptor[]) f.get(l);
	}
	
	Decryptor[] decryptors(EngineStateListener l) throws Exception {
		Field f = EngineStateListener.class.getDeclaredField("decryptors");
		f.setAccessible(true);
		return (Decryptor[]) f.get(l);
	}
	
	StringBuilder trace = new StringBuilder();
	
	Decryptor decryptor(String id) throws Exception {
		AESAead aead = AESAead.AEAD_AES_128_GCM;
		AeadDecrypt ad = new AeadDecrypt(aead.createKey(new byte[16]), aead);
		return new Decryptor(ad, new byte[12]) {
			@Override
			public void erase() {
				trace.append(id).append('|');
			}
		};
	}

	Encryptor encryptor(String id) throws Exception {
		AESAead aead = AESAead.AEAD_AES_128_GCM;
		AeadEncrypt ae = new AeadEncrypt(aead.createKey(new byte[16]), aead);
		return new Encryptor(ae, new byte[12]) {
			@Override
			public void erase() {
				trace.append(id).append('|');
			}
		};
	}
	
	@Test
	public void testOnNewReceivingTraficKey() throws Exception {
		EngineStateListener l = new EngineStateListener();
		Decryptor[] d = decryptors(l);
		d[RecordType.HANDSHAKE.ordinal()] = decryptor("h");
		d[RecordType.APPLICATION.ordinal()] = decryptor("a");
		trace.setLength(0);
		
		l.onNewReceivingTraficKey(null, RecordType.HANDSHAKE);
		assertNotNull(d[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(d[RecordType.APPLICATION.ordinal()]);
		assertEquals("", trace.toString());
		l.onNewReceivingTraficKey(null, RecordType.HANDSHAKE);
		assertNotNull(d[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(d[RecordType.APPLICATION.ordinal()]);
		assertEquals("", trace.toString());
		l.onNewReceivingTraficKey(null, RecordType.APPLICATION);
		assertNull(d[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(d[RecordType.APPLICATION.ordinal()]);
		assertEquals("h|", trace.toString());
	}

	@Test
	public void testOnNewSendingTraficKey() throws Exception {
		EngineStateListener l = new EngineStateListener();
		Encryptor[] e = encryptors(l);
		e[RecordType.HANDSHAKE.ordinal()] = encryptor("h");
		e[RecordType.APPLICATION.ordinal()] = encryptor("a");
		trace.setLength(0);
		
		l.onNewSendingTraficKey(null, RecordType.HANDSHAKE);
		assertNotNull(e[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(e[RecordType.APPLICATION.ordinal()]);
		assertEquals("", trace.toString());
		l.onNewSendingTraficKey(null, RecordType.HANDSHAKE);
		assertNotNull(e[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(e[RecordType.APPLICATION.ordinal()]);
		assertEquals("", trace.toString());
		l.onNewSendingTraficKey(null, RecordType.APPLICATION);
		assertNull(e[RecordType.HANDSHAKE.ordinal()]);
		assertNotNull(e[RecordType.APPLICATION.ordinal()]);
		assertEquals("h|", trace.toString());
	}
	
	@Test
	public void testOnCleanup() throws Exception {
		EngineStateListener l = new EngineStateListener();
		Encryptor[] e = encryptors(l);
		Decryptor[] d = decryptors(l);

		e[1] = encryptor("e1");
		e[2] = encryptor("e2");
		d[0] = decryptor("d1");
		d[3] = decryptor("d2");
		l.onCleanup(null);
		assertEquals("d1|e1|e2|d2|", trace.toString());
		assertEquals("[null, null, null, null, null]", Arrays.deepToString(e));
		assertEquals("[null, null, null, null, null]", Arrays.deepToString(d));
	}

}
