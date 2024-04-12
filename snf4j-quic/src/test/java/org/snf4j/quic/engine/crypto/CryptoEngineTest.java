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
package org.snf4j.quic.engine.crypto;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.junit.Test;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.QuicException;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.ProtocolVersionAlert;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EarlyData;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.HandshakeEngine;
import org.snf4j.tls.engine.ProducedHandshake;
import org.snf4j.tls.extension.CookieExtension;
import org.snf4j.tls.extension.ICookieExtension;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.handshake.EncryptedExtensions;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class CryptoEngineTest extends CommonTest {

	TestHandshakeEngine he;
	
	CryptoEngine engine;
	
	public void before() throws Exception {
		super.before();
		he = new TestHandshakeEngine();
		engine = new CryptoEngine(he);
	}
	
	IHandshake handshake(byte[] bytes) {
		ArrayList<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new CookieExtension(bytes));
		return new EncryptedExtensions(extensions);
	}

	IHandshake handshake(String hexBytes) {
		ArrayList<IExtension> extensions = new ArrayList<IExtension>();
		extensions.add(new CookieExtension(bytes(hexBytes)));
		return new EncryptedExtensions(extensions);
	}

	ProducedHandshake phandshake(byte[] bytes, RecordType type) {
		return new ProducedHandshake(handshake(bytes), type);
	}
	
	ProducedHandshake phandshake(String hexBytes, RecordType type) {
		return new ProducedHandshake(handshake(hexBytes), type);
	}

	ProducedHandshake pearlydata(String hexBytes, RecordType type) {
		return new ProducedHandshake(new EarlyData(bytes(hexBytes)), type);
	}
	
	void assertProduced(String expected, EncryptionLevel expectedType, ProducedCrypto produced) {
		ByteBuffer expectedData = ByteBuffer.allocate(16000);
		
		for (String data: expected.split(",")) {
			handshake(data).getBytes(expectedData);
		}
		expectedData.flip();
		ByteBuffer dupData = produced.getData().duplicate();
		
		byte[] expectedBytes = new byte[expectedData.remaining()];
		byte[] dupBytes = new byte[dupData.remaining()];
		
		expectedData.get(expectedBytes);
		dupData.get(dupBytes);
		assertArrayEquals(expectedBytes, dupBytes);
		assertSame(expectedType, produced.getEncryptionLevel());
	}

	void assertProducedEarlyData(String expected, EncryptionLevel expectedType, ProducedCrypto produced) {
		ByteBuffer dupData = produced.getData().duplicate();
		byte[] dupBytes = new byte[dupData.remaining()];
		
		dupData.get(dupBytes);
		assertArrayEquals(bytes(expected), dupBytes);
		assertSame(expectedType, produced.getEncryptionLevel());
	}
	
	void assertConsumed(String expected, IHandshake[] consumed) {
		String[] splitted = expected.split(",");
		
		assertEquals(splitted.length, consumed.length);
		for (int i=0; i<consumed.length; ++i) {
			assertArrayEquals(bytes(splitted[i]), ((ICookieExtension)consumed[i].getExtensions().get(0)).getCookie());
		}
	}
	
	@Test
	public void testProduce() throws Exception {
		assertEquals(0, engine.produce().length);
		
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ProducedCrypto[] produced = engine.produce();
		assertEquals(1, produced.length);
		assertProduced("00", EncryptionLevel.INITIAL, produced[0]);

		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(pearlydata("", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("02", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(3, produced.length);
		assertProduced("00", EncryptionLevel.INITIAL, produced[0]);
		assertProducedEarlyData("", EncryptionLevel.EARLY_DATA, produced[1]);
		assertProducedEarlyData("02", EncryptionLevel.EARLY_DATA, produced[2]);

		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(pearlydata("", RecordType.ZERO_RTT));
		he.addProduced(phandshake("01", RecordType.INITIAL));
		he.addProduced(pearlydata("02", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(3, produced.length);
		assertProduced("00,01", EncryptionLevel.INITIAL, produced[0]);
		assertProducedEarlyData("", EncryptionLevel.EARLY_DATA, produced[1]);
		assertProducedEarlyData("02", EncryptionLevel.EARLY_DATA, produced[2]);

		he.addProduced(pearlydata("", RecordType.ZERO_RTT));
		he.addProduced(phandshake("01", RecordType.INITIAL));
		he.addProduced(pearlydata("02", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(3, produced.length);
		assertProducedEarlyData("", EncryptionLevel.EARLY_DATA, produced[0]);
		assertProducedEarlyData("02", EncryptionLevel.EARLY_DATA, produced[1]);
		assertProduced("01", EncryptionLevel.INITIAL, produced[2]);
		
		he.addProduced(pearlydata("01", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("02", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(2, produced.length);
		assertProducedEarlyData("01", EncryptionLevel.EARLY_DATA, produced[0]);
		assertProducedEarlyData("02", EncryptionLevel.EARLY_DATA, produced[1]);
		
		he.addProduced(pearlydata("01", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("02", RecordType.ZERO_RTT));
		he.addProduced(phandshake("03", RecordType.HANDSHAKE));
		produced = engine.produce();
		assertEquals(3, produced.length);
		assertProducedEarlyData("01", EncryptionLevel.EARLY_DATA, produced[0]);
		assertProducedEarlyData("02", EncryptionLevel.EARLY_DATA, produced[1]);
		assertProduced("03", EncryptionLevel.HANDSHAKE, produced[2]);
		
		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(phandshake("01", RecordType.INITIAL));
		he.addProduced(phandshake("02", RecordType.HANDSHAKE));
		produced = engine.produce();
		assertEquals(2, produced.length);
		assertProduced("00,01", EncryptionLevel.INITIAL, produced[0]);
		assertProduced("02", EncryptionLevel.HANDSHAKE, produced[1]);

		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(phandshake("01", RecordType.HANDSHAKE));
		he.addProduced(phandshake("02", RecordType.APPLICATION));
		he.addProduced(pearlydata("", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(5, produced.length);
		assertProduced("00", EncryptionLevel.INITIAL, produced[0]);
		assertProduced("01", EncryptionLevel.HANDSHAKE, produced[1]);
		assertProduced("02", EncryptionLevel.APPLICATION_DATA, produced[2]);
		assertProducedEarlyData("", EncryptionLevel.EARLY_DATA, produced[3]);
		assertProducedEarlyData("", EncryptionLevel.EARLY_DATA, produced[4]);

		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(pearlydata("01", RecordType.ZERO_RTT));
		he.addProduced(phandshake("02", RecordType.HANDSHAKE));
		he.addProduced(phandshake("03", RecordType.APPLICATION));
		produced = engine.produce();
		assertEquals(4, produced.length);
		assertProduced("00", EncryptionLevel.INITIAL, produced[0]);
		assertProducedEarlyData("01", EncryptionLevel.EARLY_DATA, produced[1]);
		assertProduced("02", EncryptionLevel.HANDSHAKE, produced[2]);
		assertProduced("03", EncryptionLevel.APPLICATION_DATA, produced[3]);
		
		he.addProduced(phandshake("00", RecordType.INITIAL));
		he.addProduced(phandshake("01", RecordType.INITIAL));
		he.addProduced(phandshake("02", RecordType.INITIAL));
		he.addProduced(phandshake("03", RecordType.HANDSHAKE));
		he.addProduced(phandshake("04", RecordType.HANDSHAKE));
		he.addProduced(phandshake("05", RecordType.HANDSHAKE));
		he.addProduced(phandshake("06", RecordType.APPLICATION));
		he.addProduced(phandshake("07", RecordType.APPLICATION));
		he.addProduced(pearlydata("08", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("09", RecordType.ZERO_RTT));
		he.addProduced(pearlydata("10", RecordType.ZERO_RTT));
		produced = engine.produce();
		assertEquals(6, produced.length);
		assertProduced("00,01,02", EncryptionLevel.INITIAL, produced[0]);
		assertProduced("03,04,05", EncryptionLevel.HANDSHAKE, produced[1]);
		assertProduced("06,07", EncryptionLevel.APPLICATION_DATA, produced[2]);
		assertProducedEarlyData("08", EncryptionLevel.EARLY_DATA, produced[3]);
		assertProducedEarlyData("09", EncryptionLevel.EARLY_DATA, produced[4]);
		assertProducedEarlyData("10", EncryptionLevel.EARLY_DATA, produced[5]);
	}
	
	@Test
	public void testProduceWithUnexpectedEncryptionLevel() throws Exception {
		he.addProduced(phandshake("00", RecordType.NEXT_GEN));
		try {
			engine.produce();
			fail();
		}
		catch (CryptoException e) {
			assertEquals("Crypto error: Unexpected encryption level", e.getMessage());
			assertSame(AlertDescription.INTERNAL_ERROR, e.getAlert().getDescription());
		}
	}
	
	@Test
	public void testProduceWithAlert() throws Exception {
		he.produceAlert = new ProtocolVersionAlert("xxx");
		try {
			engine.produce();
			fail();
		}
		catch (CryptoException e) {
			assertEquals("Crypto error: xxx", e.getMessage());
			assertSame(AlertDescription.PROTOCOL_VERSION, e.getAlert().getDescription());
		}
	}
	
	@Test
	public void testConsumeFullInOrder() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ProducedCrypto[] produced = engine.produce();

		ByteBuffer data = produced[0].getData();
		int offset = 0;
		int nextOffset = data.remaining(); 
		engine.consume(data, offset, data.remaining());
		IHandshake[] consumed = he.getConsumed(true);
		assertConsumed("00", consumed);
		assertFalse(data.hasRemaining());
		
		he.addProduced(phandshake("01", RecordType.INITIAL));
		produced = engine.produce();
		data = produced[0].getData();
		offset = nextOffset;
		nextOffset += data.remaining();
		engine.consume(data, offset, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("01", consumed);
		assertFalse(data.hasRemaining());
	}

	@Test
	public void testConsumeFullOutOfOrder() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ProducedCrypto[] produced = engine.produce();

		//1,0
		ByteBuffer data = produced[0].getData();
		int dataSize = data.remaining();
		engine.consume(data, dataSize, data.remaining());
		IHandshake[] consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("01", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 0, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("01,00", consumed);
		assertFalse(data.hasRemaining());
		
		//2,0,1
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("01", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, dataSize*2, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("02", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 0, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("02,00,01", consumed);
		assertFalse(data.hasRemaining());
		
		//2,1,0
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 2*dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("01", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("02", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 0, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("02,01,00", consumed);
		assertFalse(data.hasRemaining());
		
		//1,0,2
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("01", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 0, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("01,00", consumed);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("02", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 2*dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("02", consumed);
		assertFalse(data.hasRemaining());

		//1,2,0
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 2*dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("01", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, 0, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("01", consumed);
		assertFalse(data.hasRemaining());
		he.addProduced(phandshake("02", RecordType.INITIAL));
		data = engine.produce()[0].getData();
		engine.consume(data, dataSize, data.remaining());
		consumed = he.getConsumed(true);
		assertConsumed("02,00", consumed);
		assertFalse(data.hasRemaining());
	}

	@Test
	public void testConsumeWithAlert() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data = engine.produce()[0].getData();

		he.consumeAlert = new ProtocolVersionAlert("xxx");
		try {
			engine.consume(data, 0, data.remaining());
			fail();
		}
		catch (CryptoException e) {
			assertEquals("Crypto error: xxx", e.getMessage());
			assertSame(AlertDescription.PROTOCOL_VERSION, e.getAlert().getDescription());
		}	
	}
	
	void assertConsumeError(CryptoEngine engine, ByteBuffer data, int offset, int length, TransportError expected) {
		try {
			engine.consume(data, offset, length);
			fail();
		}
		catch (QuicException e) {
			assertSame(expected, e.getTransportError());
		}
	}
	
	@Test
	public void testStartEx() throws Exception {
		he.startAlert = new ProtocolVersionAlert("xxx");
		try {
			engine.start();
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.CRYPTO_ERROR, e.getTransportError());
			assertEquals("Crypto error: xxx", e.getMessage());
		}
	}

	@Test
	public void testUpdateTasksEx() throws Exception {
		he.updateTasksAlert = new ProtocolVersionAlert("xxx");
		try {
			engine.updateTasks();
			fail();
		}
		catch (QuicException e) {
			assertSame(TransportError.CRYPTO_ERROR, e.getTransportError());
			assertEquals("Crypto error: xxx", e.getMessage());
		}
	}
	
	@Test
	public void testBufferExceeded() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data1 = engine.produce()[0].getData();
		
		int size = 4096 - (data1.remaining()-1);
		he.addProduced(phandshake(new byte[size], RecordType.INITIAL));
		ByteBuffer data2 = engine.produce()[0].getData();
		
		engine.consume(data2, data1.remaining(), data2.remaining());
		IHandshake[] consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		engine.consume(data1, 0, data1.remaining());
		consumed = he.getConsumed(true);
		assertEquals(2, consumed.length);
		
		engine = new CryptoEngine(he);
		he.addProduced(phandshake(new byte[size], RecordType.INITIAL));
		data2 = engine.produce()[0].getData();
		engine.consume(data2, 1, data2.remaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		assertConsumeError(engine, ByteBuffer.wrap(bytes("00")), 10000, 1, TransportError.CRYPTO_BUFFER_EXCEEDED);
	}
	
	@Test
	public void testConsumeWhileTaskNotDone() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data1 = engine.produce()[0].getData();
		he.addProduced(phandshake("01", RecordType.INITIAL));
		ByteBuffer data2 = engine.produce()[0].getData();
		he.addProduced(phandshake("02", RecordType.INITIAL));
		ByteBuffer data3 = engine.produce()[0].getData();
		
		engine.consume(data2, data2.remaining(), data2.remaining());
		he.addTask(new Runnable() {
			@Override
			public void run() {
			}
		});
		engine.consume(data1, 0, data1.remaining());
		engine.consume(data3, 0, data3.remaining());
		assertFalse(engine.needProduce());
		assertTrue(engine.updateTasks());
		engine.getTask();
		assertEquals(0, he.getConsumed(true).length);
		assertFalse(engine.updateTasks());
		assertConsumed("00,01", he.getConsumed(true));
	}
	
	@Test
	public void testInvalidOffsetAndLength() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data = engine.produce()[0].getData();
		int dataSize = data.remaining();
		ByteBuffer data1 = data.duplicate();
		ByteBuffer data2 = data.duplicate();
		
		engine.consume(data2, dataSize, data2.remaining());
		assertConsumeError(engine, data1, 1, data1.remaining(), TransportError.PROTOCOL_VIOLATION);
		
		data1 = data.duplicate();
		data2 = data.duplicate();
		engine = new CryptoEngine(he);
		engine.consume(data2, dataSize, data2.remaining());
		assertConsumeError(engine, data1, dataSize+1, data1.remaining(), TransportError.PROTOCOL_VIOLATION);
		assertConsumeError(engine, data1, dataSize, data1.remaining()-1, TransportError.PROTOCOL_VIOLATION);
		assertConsumeError(engine, data1, dataSize, data1.remaining()+1, TransportError.PROTOCOL_VIOLATION);
		
		data1 = data.duplicate();
		data2 = data.duplicate();
		engine = new CryptoEngine(he);
		engine.consume(data2, 0, data2.remaining());
		assertEquals(1, he.getConsumed(true).length);
		assertConsumeError(engine, data1, 0, data1.remaining()+1, TransportError.PROTOCOL_VIOLATION);
		assertConsumeError(engine, data1, 1, data1.remaining(), TransportError.PROTOCOL_VIOLATION);
		assertConsumeError(engine, data1, data1.remaining()-1, 2, TransportError.PROTOCOL_VIOLATION);
	}

	@Test
	public void testSameOffsetAndLength() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data1 = engine.produce()[0].getData();
		int dataSize = data1.remaining();
		he.addProduced(phandshake("01", RecordType.INITIAL));
		ByteBuffer data2 = engine.produce()[0].getData();
		he.addProduced(phandshake("02", RecordType.INITIAL));
		ByteBuffer data3 = engine.produce()[0].getData();
		
		engine.consume(data1, dataSize, data2.remaining());
		assertFalse(data1.hasRemaining());
		IHandshake[] consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		engine.consume(data2, dataSize, data2.remaining());
		assertFalse(data2.hasRemaining());
		consumed = he.getConsumed(true);
		assertEquals(0, consumed.length);
		engine.consume(data3, 0, data3.remaining());
		assertFalse(data3.hasRemaining());
		consumed = he.getConsumed(true);
		assertConsumed("02,00", consumed);
	}

	@Test
	public void testOldOffsetAndLength() throws Exception {
		he.addProduced(phandshake("00", RecordType.INITIAL));
		ByteBuffer data1 = engine.produce()[0].getData();
		he.addProduced(phandshake("01", RecordType.INITIAL));
		ByteBuffer data2 = engine.produce()[0].getData();
		he.addProduced(phandshake("02", RecordType.INITIAL));
		ByteBuffer data3 = engine.produce()[0].getData();
		
		engine.consume(data1, 0, data1.remaining());
		assertFalse(data1.hasRemaining());
		assertEquals(1, he.getConsumed(true).length);
		engine.consume(data2, 0, data2.remaining());
		assertFalse(data2.hasRemaining());
		assertEquals(0, he.getConsumed(true).length);
		engine.consume(data3, 0, data3.remaining()-1);
		assertEquals(1, data3.remaining());
		assertEquals(0, he.getConsumed(true).length);
	}
	
	@Test
	public void testConsumeInChunks() throws Exception {
		he.addProduced(phandshake("00112233445566778899", RecordType.INITIAL));
		ByteBuffer[] chunks = split(engine.produce()[0].getData(), 2, 3);
		int offset = 0;
		int nextOffset = chunks[0].remaining();
		engine.consume(chunks[0], offset, chunks[0].remaining());
		assertEquals(0, he.getConsumed(true).length);
		offset = nextOffset;
		nextOffset += chunks[1].remaining();
		engine.consume(chunks[1], offset, chunks[1].remaining());
		assertEquals(0, he.getConsumed(true).length);
		offset = nextOffset;
		nextOffset += chunks[2].remaining();
		engine.consume(chunks[2], offset, chunks[2].remaining());
		assertConsumed("00112233445566778899", he.getConsumed(true));

		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00112233445566778899", RecordType.INITIAL));
		chunks = split(engine.produce()[0].getData(), 2, 3);
		engine.consume(chunks[2], 5, chunks[2].remaining());
		assertEquals(0, he.getConsumed(true).length);
		engine.consume(chunks[1], 2, chunks[1].remaining());
		assertEquals(0, he.getConsumed(true).length);
		engine.consume(chunks[0], 0, chunks[0].remaining());
		assertConsumed("00112233445566778899", he.getConsumed(true));		

		//first full + second more than header
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00112233445566778899", RecordType.INITIAL));
		he.addProduced(phandshake("10213243546576879809", RecordType.INITIAL));
		ProducedCrypto[] produced = engine.produce();
		assertEquals(1, produced.length);
		chunks = split(produced[0].getData(), produced[0].getData().remaining()/2+6);
		int off = 0;
		int nextOff = chunks[0].remaining();
		engine.consume(chunks[0], off, chunks[0].remaining());
		assertFalse(chunks[0].hasRemaining());
		assertConsumed("00112233445566778899", he.getConsumed(true));		
		off = nextOff;
		nextOff += chunks[1].remaining();
		engine.consume(chunks[1], off, chunks[1].remaining());		
		assertFalse(chunks[1].hasRemaining());
		assertConsumed("10213243546576879809", he.getConsumed(true));		

		//first full + second less header header
		engine = new CryptoEngine(he);
		he.addProduced(phandshake("00112233445566778899", RecordType.INITIAL));
		he.addProduced(phandshake("10213243546576879809", RecordType.INITIAL));
		produced = engine.produce();
		assertEquals(1, produced.length);
		chunks = split(produced[0].getData(), produced[0].getData().remaining()/2+1);
		off = 0;
		nextOff = chunks[0].remaining();
		engine.consume(chunks[0], off, chunks[0].remaining());
		assertFalse(chunks[0].hasRemaining());
		assertConsumed("00112233445566778899", he.getConsumed(true));		
		off = nextOff;
		nextOff += chunks[1].remaining();
		engine.consume(chunks[1], off, chunks[1].remaining());		
		assertFalse(chunks[1].hasRemaining());
		assertConsumed("10213243546576879809", he.getConsumed(true));		
	}
	
	@Test
	public void testNeedProduce() throws Exception {
		assertFalse(engine.needProduce());
		he.addProduced(phandshake("00", RecordType.INITIAL));
		assertTrue(engine.needProduce());
		engine.produce();
		assertFalse(engine.needProduce());
	}
	
	@Test
	public void testCleanup() {
		assertEquals("", he.trace.get(true));
		engine.cleanup();
		assertEquals("CUP|", he.trace.get(true));
	}
	
	void assertFlags(HandshakeEngine he, CryptoEngine ce, boolean hasTask, boolean needTask, boolean needProduce) {
		assertEquals(hasTask,ce.hasTask());
		assertEquals(needTask,ce.hasTask() || ce.hasRunningTask(true));
		assertEquals(he.hasTask(),ce.hasTask());
		assertEquals(he.hasRunningTask(false),ce.hasRunningTask(false));
		assertEquals(he.hasRunningTask(true),ce.hasRunningTask(true));
		assertEquals(needProduce,ce.needProduce());
	}
	
	CryptoEngine cli, srv;
	HandshakeEngine cliHe, srvHe;
	
	void prepareEngines(DelegatedTaskMode mode) throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().delegatedTaskMode(mode);
		cliHe = new HandshakeEngine(true, epb.build(), ehb.build(), new TestEngineStateListener());
		srvHe = new HandshakeEngine(false, epb.build(), ehb.build(), new TestEngineStateListener());
		cli = new CryptoEngine(cliHe);
		srv = new CryptoEngine(srvHe);
	}

	void prepareEngines() throws Exception {
		prepareEngines(DelegatedTaskMode.ALL);
	}
	
	@Test
	public void testWithHandshakeEngine() throws Exception {
		int cliOff = 0, srvOff = 0, cliNextOff, srvNextOff;
		int cliNextProdOff;
		
		prepareEngines();
		assertFlags(srvHe, srv, false, false, false);
		srv.start();
		assertFlags(srvHe, srv, false, false, false);
		assertFlags(cliHe, cli, false, false, false);
		cli.start();
		assertFlags(cliHe, cli, true, true, true);
		Runnable task = cli.getTask();
		assertNotNull(task);
		assertFlags(cliHe, cli, false, true, true);
		task.run();
		assertFlags(cliHe, cli, false, false, true);
		cli.updateTasks();
		assertFlags(cliHe, cli, false, false, true);
		ProducedCrypto[] produced = cli.produce();
		assertFlags(cliHe, cli, false, false, false);
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		assertEquals(0, produced[0].getOffset());
		cliNextProdOff = produced[0].getData().remaining();
		
		ByteBuffer data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = srvNextOff;
		assertFlags(srvHe, srv, true, true, true);
		srv.getTask().run();
		assertFlags(srvHe, srv, true, true, true);
		srv.getTask().run();
		assertFlags(srvHe, srv, false, false, true);
		produced = srv.produce();
		assertFlags(srvHe, srv, false, false, false);
		assertEquals(2, produced.length);
		assertSame(EncryptionLevel.INITIAL, produced[0].getEncryptionLevel());
		assertSame(EncryptionLevel.HANDSHAKE, produced[1].getEncryptionLevel());
		assertEquals(0, produced[0].getOffset());
		assertEquals(produced[0].getData().remaining(), produced[1].getOffset());
		
		data = produced[0].getData();
		cliNextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = cliNextOff;
		data = produced[1].getData();
		cliNextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = cliNextOff;
		assertFlags(cliHe, cli, true, true, false);
		cli.getTask().run();
		assertFlags(cliHe, cli, false, false, false);
		assertTrue(cli.hasRunningTask(false));
		assertFalse(cli.hasRunningTask(true));
		assertFalse(cli.isHandshakeDone());
		cli.updateTasks();
		assertFlags(cliHe, cli, false, false, true);
		assertTrue(cli.isHandshakeDone());
		produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.HANDSHAKE, produced[0].getEncryptionLevel());
		assertEquals(cliNextProdOff, produced[0].getOffset());
		
		assertFalse(srv.isHandshakeDone());
		data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		assertTrue(srv.isHandshakeDone());
		assertFalse(cli.needProduce());
	}

	@Test
	public void testWithHandshakeEngineInChunks() throws Exception {
		int cliOff = 0, srvOff = 0, cliNextOff, srvNextOff;
		
		prepareEngines();
		srv.start();
		cli.start();
        cli.getTask().run();
		cli.updateTasks();
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);
		
		ByteBuffer data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = srvNextOff;
		srv.getTask().run();
		srv.getTask().run();
		produced = srv.produce();
		assertEquals(2, produced.length);
		
		data = produced[0].getData();
		cliNextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = cliNextOff;
		data = produced[1].getData();
		cliNextOff = cliOff + data.remaining()-1;
		cli.consume(data, cliOff, data.remaining()-1);
		cliOff = cliNextOff;
		cli.consume(data, cliOff, 1);
		cli.getTask().run();
		cli.updateTasks();
		produced = cli.produce();
		assertEquals(1, produced.length);
		
		data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
	}

	@Test
	public void testConsumeWithHasTask() throws Exception {
		int cliOff = 0, srvOff = 0, cliNextOff, srvNextOff;
		
		prepareEngines();
		srv.start();
		cli.start();
        cli.getTask().run();
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);

		ByteBuffer data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = srvNextOff;
		srv.getTask().run();
		srv.getTask().run();
		produced = srv.produce();
		assertEquals(2, produced.length);

		data = produced[0].getData();
		cliNextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = cliNextOff;
		data = produced[1].getData();
		cliNextOff = cliOff + data.remaining()-10;
		cli.consume(data, cliOff, data.remaining()-10);
		cliOff = cliNextOff;
		assertTrue(cli.hasTask());
		cli.consume(data, cliOff, 1);
		cliOff++;
		Runnable task = cli.getTask();
		cli.consume(data, cliOff, 1);
		cliOff++;
		task.run();
		cli.consume(data, cliOff, data.remaining());
		cli.updateTasks();
		produced = cli.produce();
		assertEquals(1, produced.length);
	}

	@Test
	public void testConsumeWithHasRunningTask() throws Exception {
		int cliOff = 0, srvOff = 0, cliNextOff, srvNextOff;
		
		prepareEngines();
		srv.start();
		cli.start();
        cli.getTask().run();
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);

		ByteBuffer data = produced[0].getData();
		srvNextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = srvNextOff;
		srv.getTask().run();
		srv.getTask().run();
		produced = srv.produce();
		assertEquals(2, produced.length);

		data = produced[0].getData();
		cliNextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = cliNextOff;
		data = produced[1].getData();
		cliNextOff = cliOff + data.remaining()-10;
		cli.consume(data, cliOff, data.remaining()-10);
		cliOff = cliNextOff;
		assertTrue(cli.hasTask());
		Runnable task = cli.getTask();
		cli.consume(data, cliOff, 1);
		cliOff++;
		cli.consume(data, cliOff, 1);
		cliOff++;
		task.run();
		cli.consume(data, cliOff, data.remaining());
		cli.updateTasks();
		produced = cli.produce();
		assertEquals(1, produced.length);
	}
	
	HandshakeStatus hs(ICryptoEngine engine) {
		if (engine.isHandshakeDone()) {
			if (engine.needProduce()) {
				return HandshakeStatus.NEED_WRAP;
			}
			if (engine.needConsume()) {
				return HandshakeStatus.NEED_UNWRAP;
			}
			return HandshakeStatus.NOT_HANDSHAKING;
		}
		if (engine.hasTask() || engine.hasRunningTask(true)) {
			return HandshakeStatus.NEED_TASK;
		}
		if (engine.needProduce()) {
			return HandshakeStatus.NEED_WRAP;
		}
		if (engine.hasRunningTask(false)) {
			return HandshakeStatus.NEED_UNWRAP_AGAIN;
		}
		return HandshakeStatus.NEED_UNWRAP;
	}
	
	@Test
	public void testHandshakeWithAllTasks() throws Exception {
		int cliOff = 0, srvOff = 0, nextOff;
		ByteBuffer data;
		
		prepareEngines();
		srv.start();
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		cli.start();
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		Runnable task = cli.getTask();
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		task.run();
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));

		data = produced[0].getData();
		nextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		task = srv.getTask();
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		task.run();
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		task = srv.getTask();
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		task.run();
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(2, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		
		data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = nextOff; 
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		data = produced[1].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		task = cli.getTask();
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		task.run();
		assertSame(HandshakeStatus.NEED_UNWRAP_AGAIN, hs(cli));
		cli.updateTasks();
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
		
		data = produced[0].getData();
		nextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.APPLICATION_DATA, produced[0].getEncryptionLevel());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(srv));
		
		data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
	}

	@Test
	public void testHandshakeWithAllTasksInChunks() throws Exception {
		int cliOff = 0, srvOff = 0, nextOff;
		ByteBuffer[] chunks;
		
		prepareEngines();
		srv.start();
		cli.start();
		Runnable task = cli.getTask();
		task.run();
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));

		chunks = split(produced[0].getData(),10);
		for (ByteBuffer chunk: chunks) {
			assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
			nextOff = srvOff + chunk.remaining();
			srv.consume(chunk, srvOff, chunk.remaining());
			srvOff = nextOff;
		}
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		srv.getTask().run();
		srv.getTask().run();
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(2, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		
		chunks = split(produced[0].getData(),10);
		for (ByteBuffer chunk: chunks) {
			assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
			nextOff = cliOff + chunk.remaining();
			cli.consume(chunk, cliOff, chunk.remaining());
			cliOff = nextOff;
		}
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		chunks = split(produced[1].getData(), 20, produced[1].getData().remaining()-20-10);
		nextOff = cliOff + chunks[0].remaining();
		cli.consume(chunks[0], cliOff, chunks[0].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		nextOff = cliOff + chunks[1].remaining();
		cli.consume(chunks[1], cliOff, chunks[1].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		cli.getTask().run();
		assertSame(HandshakeStatus.NEED_UNWRAP_AGAIN, hs(cli));
		cli.updateTasks();
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		nextOff = cliOff + chunks[2].remaining();
		cli.consume(chunks[2], cliOff, chunks[2].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
				
		chunks = split(produced[0].getData(), 10, 5);
		for (ByteBuffer chunk: chunks) {
			assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
			nextOff = srvOff + chunk.remaining();
			srv.consume(chunk, srvOff, chunk.remaining());
			srvOff = nextOff;
		}
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.APPLICATION_DATA, produced[0].getEncryptionLevel());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(srv));

		chunks = split(produced[0].getData(), 10);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
		nextOff = cliOff + chunks[0].remaining();
		cli.consume(chunks[0], cliOff, chunks[0].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		nextOff = cliOff + chunks[1].remaining();
		cli.consume(chunks[1], cliOff, chunks[1].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
	}

	@Test
	public void testHandshakeWithAllTasksInChunksOutOfOrder() throws Exception {
		int cliOff = 0, srvOff = 0, nextOff;
		int cliOff2 = 0, srvOff2 = 0;
		ByteBuffer[] chunks;
		
		prepareEngines();
		srv.start();
		cli.start();
		Runnable task = cli.getTask();
		task.run();
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));

		chunks = split(produced[0].getData(),10);
		srvOff2 = chunks[0].remaining();
		nextOff = srvOff2 + chunks[1].remaining();
		srv.consume(chunks[1], srvOff2, chunks[1].remaining());
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		srv.consume(chunks[0], srvOff, chunks[0].remaining());
		assertSame(HandshakeStatus.NEED_TASK, hs(srv));
		srvOff = nextOff;
		srv.getTask().run();
		srv.getTask().run();
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(2, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		
		chunks = split(produced[0].getData(),10);
		cliOff2 = chunks[0].remaining();
		nextOff = cliOff2 + chunks[1].remaining();
		cli.consume(chunks[1], cliOff2, chunks[1].remaining());
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		cli.consume(chunks[0], cliOff, chunks[0].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		chunks = split(produced[1].getData(),10);
		cliOff2 = cliOff + chunks[0].remaining();
		nextOff = cliOff2 + chunks[1].remaining();
		cli.consume(chunks[1], cliOff2, chunks[1].remaining());
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		cli.consume(chunks[0], cliOff, chunks[0].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NEED_TASK, hs(cli));
		cli.getTask().run();
		assertSame(HandshakeStatus.NEED_UNWRAP_AGAIN, hs(cli));
		cli.updateTasks();
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
		
		chunks = split(produced[0].getData(), 10);
		srvOff2 = srvOff + chunks[0].remaining();
		nextOff = srvOff2 + chunks[1].remaining();
		srv.consume(chunks[1], srvOff2, chunks[1].remaining());
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		srv.consume(chunks[0], srvOff, chunks[0].remaining());
		srvOff = nextOff;
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.APPLICATION_DATA, produced[0].getEncryptionLevel());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(srv));
		
		chunks = split(produced[0].getData(), 10);
		cliOff2 = cliOff + chunks[0].remaining();
		nextOff = cliOff2 + chunks[1].remaining();
		cli.consume(chunks[1], cliOff2, chunks[1].remaining());
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		cli.consume(chunks[0], cliOff, chunks[0].remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
	}
	
	@Test
	public void testHandshakeWithNoTasks() throws Exception {
		int cliOff = 0, srvOff = 0, nextOff;
		ByteBuffer data;
		
		prepareEngines(DelegatedTaskMode.NONE);
		srv.start();
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		cli.start();
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		ProducedCrypto[] produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));

		data = produced[0].getData();
		nextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(2, produced.length);
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(srv));
		
		data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = nextOff; 
		assertSame(HandshakeStatus.NEED_UNWRAP, hs(cli));
		data = produced[1].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		assertSame(HandshakeStatus.NEED_WRAP, hs(cli));
		produced = cli.produce();
		assertEquals(1, produced.length);
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
		
		data = produced[0].getData();
		nextOff = srvOff + data.remaining();
		srv.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		assertSame(HandshakeStatus.NEED_WRAP, hs(srv));
		produced = srv.produce();
		assertEquals(1, produced.length);
		assertSame(EncryptionLevel.APPLICATION_DATA, produced[0].getEncryptionLevel());
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(srv));
		
		data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		cli.consume(data, cliOff, data.remaining());
		cliOff = nextOff;
		assertSame(HandshakeStatus.NOT_HANDSHAKING, hs(cli));
	}
	
}
