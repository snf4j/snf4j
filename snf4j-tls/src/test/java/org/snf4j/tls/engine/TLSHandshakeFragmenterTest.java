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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.crypto.AESAead;
import org.snf4j.tls.crypto.AeadEncrypt;
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.handshake.ClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.IEncryptorHolder;
import org.snf4j.tls.record.RecordType;

public class TLSHandshakeFragmenterTest extends EngineTest {

	private TLSHandshakeFragmenter wrapper;
	
	private int maxFragmentLength;
	
	private List<ProducedHandshake> produced;
	
	private ByteBuffer dst;
	
	private StringBuilder trace = new StringBuilder();
	
	private Encryptor[] encryptors = new Encryptor[RecordType.values().length];
	                                               
	@Override
	public void before() throws Exception {
		super.before();
		wrapper = new TLSHandshakeFragmenter(new TestEngine(), new TestEncryptors(), new TestListener());
		maxFragmentLength = 1000;
		produced = new ArrayList<ProducedHandshake>();
		dst = ByteBuffer.allocate(1000+5);
	}
	
	private void trace(String s) {
		trace.append(s).append('|');
	}
	
	private String trace() {
		String s = trace.toString();
		trace.setLength(0);
		return s;
	}
	
	private void add(IHandshake handshake, RecordType type, RecordType nextType) {
		produced.add(new ProducedHandshake(handshake, type, nextType));
	}

	private void add(IHandshake handshake, RecordType type) {
		add(handshake, type, null);
	}
	
	private IHandshake handshake(int id, int size) {
		return new ClientHello(
				id, 
				new byte[32], 
				new byte[32], 
				new CipherSuite[] {CipherSuite.TLS_AES_128_GCM_SHA256},
				new byte[1],
				Arrays.asList(new IExtension[] {new CookieExtension(new byte[size-85])}));
	}
	
	@Test
	public void testWrap() throws Exception {
		assertEquals(0, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		
		add(handshake(1, 100), RecordType.INITIAL);
		assertEquals(105, wrapper.wrap(dst));
		assertEquals(105, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());

		dst.clear();
		add(handshake(1, 100), RecordType.INITIAL);
		add(handshake(1, 200), RecordType.INITIAL);
		assertEquals(305, wrapper.wrap(dst));
		assertEquals(305, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		
		dst.clear();
		add(handshake(1, 1000), RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());

		dst.clear();
		add(handshake(1, 500), RecordType.INITIAL);
		add(handshake(1, 300), RecordType.INITIAL);
		add(handshake(1, 200), RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		assertEquals("", trace());
	}
	
	@Test
	public void testWrapWithFragment() throws Exception {
		add(handshake(1, 1001), RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		
		dst.clear();
		assertEquals(6, wrapper.wrap(dst));
		assertEquals(6, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		
		dst.clear();
		add(handshake(1, 2001), RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		
		dst.clear();
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());

		dst.clear();
		assertEquals(6, wrapper.wrap(dst));
		assertEquals(6, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		
		dst.clear().position(1);
		add(handshake(1, 100), RecordType.INITIAL);
		assertEquals(-1, wrapper.wrap(dst));
		assertEquals(1, dst.position());
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		
		wrapper.clear();
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());

		dst.clear();
		add(handshake(1, 1100), RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		
		dst.clear().position(1);
		assertEquals(-1, wrapper.wrap(dst));
		assertEquals(1, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("", trace());
	}

	@Test
	public void testWrapDifferentRecordTypes() throws Exception {
		add(handshake(1, 100), RecordType.INITIAL);
		add(handshake(1, 200), RecordType.INITIAL, RecordType.HANDSHAKE);
		add(handshake(1, 400), RecordType.HANDSHAKE);
		assertEquals(305, wrapper.wrap(dst));
		assertEquals(305, dst.position());
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("OSTK(HANDSHAKE)|", trace());
		
		dst.clear();
		assertEquals(405, wrapper.wrap(dst));
		assertEquals(405, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		assertEquals("", trace());
		
		dst.clear();
		add(handshake(1, 300), RecordType.INITIAL);
		add(handshake(1, 701), RecordType.INITIAL, RecordType.HANDSHAKE);
		add(handshake(1, 400), RecordType.HANDSHAKE);
		assertEquals(1005, wrapper.wrap(dst));
		assertEquals(1005, dst.position());
		assertTrue(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("OSTK(HANDSHAKE)|", trace());
		
		dst.clear();
		assertEquals(6, wrapper.wrap(dst));
		assertEquals(6, dst.position());
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("", trace());

		dst.clear();
		assertEquals(405, wrapper.wrap(dst));
		assertEquals(405, dst.position());
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		assertEquals("", trace());
	}
	
	@Test
	public void testWrapContent() throws Exception {
		IHandshake handshake = handshake(1, 2500);
		ByteBuffer expected = ByteBuffer.allocate(3000);
		ByteBuffer bytes = ByteBuffer.allocate(3000);
		handshake.getBytes(expected);
		expected.flip();
		
		add(handshake, RecordType.INITIAL);
		assertEquals(1005, wrapper.wrap(dst));
		dst.flip().position(5);
		bytes.put(dst);
		dst.clear();
		assertEquals(1005, wrapper.wrap(dst));
		dst.flip().position(5);
		bytes.put(dst);
		dst.clear();
		assertEquals(505, wrapper.wrap(dst));
		dst.flip().position(5);
		bytes.put(dst);
		bytes.flip();
		assertEquals(expected, bytes);
	}
	
	@Test
	public void testWithoutRecordType() throws Exception {
		add(handshake(1, 300), RecordType.INITIAL);
		add(handshake(1, 700), null);
		assertEquals(305, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("", trace());
		assertEquals(700, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		assertEquals("", trace());
		
		add(handshake(1, 200), RecordType.INITIAL, RecordType.HANDSHAKE);
		add(handshake(1, 100), null);
		add(handshake(1, 300), RecordType.HANDSHAKE);
		dst.clear();
		assertEquals(205, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("OSTK(HANDSHAKE)|", trace());
		assertEquals(100, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertTrue(wrapper.needWrap());
		assertEquals("", trace());
		dst.clear();
		assertEquals(305, wrapper.wrap(dst));
		assertFalse(wrapper.isPending());
		assertFalse(wrapper.needWrap());
		assertEquals("", trace());
		
		dst.clear();
		add(handshake(1, 1005), null);
		assertEquals(1005, wrapper.wrap(dst));

		dst.clear();
		add(handshake(1, 1006), null);
		assertEquals(-1, wrapper.wrap(dst));
	}
	
	@Test
	public void testWrapWithEncryptor() throws Exception {
		AESAead aead = AESAead.AEAD_AES_128_GCM;
		SecretKey key = aead.createKey(new byte[16]);
		byte[] iv = bytes(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16);
		dst = ByteBuffer.allocate(1000+5+1+16);
		
		encryptors[RecordType.HANDSHAKE.ordinal()] = new Encryptor(new AeadEncrypt(key, aead), iv);
		add(handshake(1, 300), RecordType.HANDSHAKE);
		assertEquals(322, wrapper.wrap(dst));
		
		dst.clear();
		handler.padding = 100;
		add(handshake(1, 300), RecordType.HANDSHAKE);
		assertEquals(422, wrapper.wrap(dst));
		
		dst.clear();
		handler.padding = 0;
		add(handshake(1, 1000), RecordType.HANDSHAKE);
		assertEquals(1022, wrapper.wrap(dst));
		
		dst.clear();
		handler.padding = 1;
		add(handshake(1, 1000), RecordType.HANDSHAKE);
		assertEquals(1022, wrapper.wrap(dst));

		dst.clear();
		handler.padding = 1;
		add(handshake(1, 999), RecordType.HANDSHAKE);
		assertEquals(1022, wrapper.wrap(dst));
		
	}
	
	class TestEngine implements IHandshakeEngine {

		@Override
		public IEngineHandler getHandler() {
			return handler;
		}

		@Override
		public void consume(ByteBuffer[] srcs, int remaining) throws Alert {
		}

		@Override
		public void consume(ByteBufferArray srcs, int remaining) throws Alert {
		}

		@Override
		public boolean needProduce() {
			return !produced.isEmpty();
		}

		@Override
		public ProducedHandshake[] produce() throws Alert {
			ProducedHandshake[] result = produced.isEmpty() 
					? new ProducedHandshake[0] 
					: produced.toArray(new ProducedHandshake[0]);
			produced.clear();
			return result;
		}

		@Override
		public boolean updateTasks() throws Alert {
			return false;
		}

		@Override
		public boolean hasProducingTask() {
			return false;
		}

		@Override
		public boolean hasRunningTask() {
			return false;
		}

		@Override
		public boolean hasTask() {
			return false;
		}

		@Override
		public Runnable getTask() {
			return null;
		}

		@Override
		public boolean isStarted() {
			return false;
		}

		@Override
		public boolean isConnected() {
			return false;
		}

		@Override
		public boolean isClientMode() {
			return false;
		}

		@Override
		public void start() throws Alert {
		}

		@Override
		public int getMaxFragmentLength() {
			return maxFragmentLength;
		}
	}
	
	class TestEncryptors implements IEncryptorHolder {

		@Override
		public Encryptor getEncryptor(RecordType type) {
			return encryptors[type.ordinal()];
		}

		@Override
		public Encryptor getEncryptor() {
			return null;
		}
	}
	
	class TestListener implements IEngineStateListener {

		@Override
		public void onEarlyTrafficSecret(EngineState state) throws Exception {
			trace("X");
		}

		@Override
		public void onHandshakeTrafficSecrets(EngineState state) throws Exception {
			trace("X");
		}

		@Override
		public void onApplicationTrafficSecrets(EngineState state) throws Exception {
			trace("X");
		}

		@Override
		public void onReceivingTraficKey(RecordType recordType) {
			trace("X");
		}

		@Override
		public void onSendingTraficKey(RecordType recordType) {
			trace("OSTK(" + recordType + ")");
		}

		@Override
		public void produceChangeCipherSpec(EngineState state) {
			trace("X");
		}

		@Override
		public void prepareChangeCipherSpec(EngineState state) {
			trace("X");
		}
	}
}
