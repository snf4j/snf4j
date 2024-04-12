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

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.quic.CommonTest;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.engine.crypto.TestEngineStateListener;
import org.snf4j.tls.engine.DelegatedTaskMode;
import org.snf4j.tls.engine.EngineHandlerBuilder;
import org.snf4j.tls.engine.EngineParametersBuilder;
import org.snf4j.tls.engine.HandshakeEngine;

public class CryptoEngineAdapterTest extends CommonTest {

	@Test
	public void testProduce() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		CryptoEngine cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngine srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngineAdapter cliA = new CryptoEngineAdapter(cliE);
		CryptoEngineAdapter srvA = new CryptoEngineAdapter(srvE);
		int cliOff = 0, srvOff = 0, nextOff;
		
		srvA.getEngine().start();
		cliA.getEngine().start();
		ProducedCrypto[] produced = cliA.produce();
		assertEquals(1, produced.length);
		assertEquals(0, produced[0].getOffset());
		ByteBuffer data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		srvE.consume(data, cliOff, data.remaining());
		cliOff = nextOff;
		produced = srvA.produce();
		assertEquals(2, produced.length);
		assertEquals(0, produced[0].getOffset());
		data = produced[0].getData();
		nextOff = srvOff + data.remaining();
		cliE.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		data = produced[1].getData();
		assertEquals(0, produced[1].getOffset());
		nextOff = srvOff + data.remaining();
		cliE.consume(data, srvOff, data.remaining());
		srvOff = nextOff;
		produced = cliA.produce();
		assertEquals(1, produced.length);
		assertEquals(0, produced[0].getOffset());
		data = produced[0].getData();
		nextOff = cliOff + data.remaining();
		srvE.consume(data, cliOff, data.remaining());
		cliOff = nextOff;
		produced = srvA.produce();
		assertEquals(1, produced.length);
		assertEquals(0, produced[0].getOffset());
	}

	@Test
	public void testConsume() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		CryptoEngine cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngine srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngineAdapter cliA = new CryptoEngineAdapter(cliE);
		CryptoEngineAdapter srvA = new CryptoEngineAdapter(srvE);
		
		srvA.getEngine().start();
		cliA.getEngine().start();
		ProducedCrypto[] produced = cliA.produce();
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		srvA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		produced = srvA.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		cliA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		data = produced[1].getData();
		assertFalse(cliA.getEngine().isHandshakeDone());
		cliA.consume(data, produced[1].getOffset(), data.remaining(), produced[1].getEncryptionLevel());
		assertTrue(cliA.getEngine().isHandshakeDone());
		produced = cliA.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		assertFalse(srvA.getEngine().isHandshakeDone());
		srvA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
		assertTrue(srvA.getEngine().isHandshakeDone());
		produced = srvA.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		cliA.consume(data, produced[0].getOffset(), data.remaining(), produced[0].getEncryptionLevel());
	}

	@Test
	public void testConsumeOutOfOrder() throws Exception {
		EngineHandlerBuilder ehb = new EngineHandlerBuilder(km(), tm());
		EngineParametersBuilder epb = new EngineParametersBuilder().delegatedTaskMode(DelegatedTaskMode.NONE);
		CryptoEngine cliE = new CryptoEngine(new HandshakeEngine(true, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngine srvE = new CryptoEngine(new HandshakeEngine(false, epb.build(), ehb.build(), new TestEngineStateListener()));
		CryptoEngineAdapter cliA = new CryptoEngineAdapter(cliE);
		CryptoEngineAdapter srvA = new CryptoEngineAdapter(srvE);
		
		srvA.getEngine().start();
		cliA.getEngine().start();
		ProducedCrypto[] produced = cliA.produce();
		assertEquals(1, produced.length);
		ByteBuffer data = produced[0].getData();
		ByteBuffer[] splitted = split(produced[0].getData(),10); 
		srvA.consume(splitted[1], produced[0].getOffset()+10, data.remaining()-10, produced[0].getEncryptionLevel());
		srvA.consume(splitted[0], produced[0].getOffset(), 10, produced[0].getEncryptionLevel());
		produced = srvA.produce();
		assertEquals(2, produced.length);
		data = produced[0].getData();
		splitted = split(produced[0].getData(),10); 
		cliA.consume(splitted[1], produced[0].getOffset()+10, data.remaining()-10, produced[0].getEncryptionLevel());
		cliA.consume(splitted[0], produced[0].getOffset(), 10, produced[0].getEncryptionLevel());
		data = produced[1].getData();
		splitted = split(produced[1].getData(),10); 
		assertFalse(cliA.getEngine().isHandshakeDone());
		cliA.consume(splitted[1], produced[1].getOffset()+10, data.remaining()-10, produced[1].getEncryptionLevel());
		cliA.consume(splitted[0], produced[1].getOffset(), 10, produced[1].getEncryptionLevel());
		assertTrue(cliA.getEngine().isHandshakeDone());
		produced = cliA.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		splitted = split(produced[0].getData(),10); 
		assertFalse(srvA.getEngine().isHandshakeDone());
		srvA.consume(splitted[1], produced[0].getOffset()+10, data.remaining()-10, produced[0].getEncryptionLevel());
		srvA.consume(splitted[0], produced[0].getOffset(), 10, produced[0].getEncryptionLevel());
		assertTrue(srvA.getEngine().isHandshakeDone());
		produced = srvA.produce();
		assertEquals(1, produced.length);
		data = produced[0].getData();
		splitted = split(produced[0].getData(),10); 
		cliA.consume(splitted[1], produced[0].getOffset()+10, data.remaining()-10, produced[0].getEncryptionLevel());
		cliA.consume(splitted[0], produced[0].getOffset(), 10, produced[0].getEncryptionLevel());
	}

}
