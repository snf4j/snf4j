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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.snf4j.tls.handshake.Finished;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class ProducedHandshakeTest {

	@Test
	public void testConstructors() {
		IHandshake h = new Finished(new byte[10]);
		EarlyData ed = new EarlyData(new byte[11]);
		
		ProducedHandshake ph = new ProducedHandshake(h);
		assertSame(h, ph.getHandshake());
		assertSame(ProducedHandshake.Type.HANDSHAKE, ph.getType());
		assertNull(ph.getRecordType());
		assertNull(ph.getNextRecordType());
		
		ph = new ProducedHandshake(ed);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.APPLICATION_DATA, ph.getType());
		assertNull(ph.getRecordType());
		assertNull(ph.getNextRecordType());

		ph = new ProducedHandshake(ed, ProducedHandshake.Type.CHANGE_CIPHER_SPEC);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.CHANGE_CIPHER_SPEC, ph.getType());
		assertNull(ph.getRecordType());
		assertNull(ph.getNextRecordType());

		ph = new ProducedHandshake(h, RecordType.INITIAL);
		assertSame(h, ph.getHandshake());
		assertSame(ProducedHandshake.Type.HANDSHAKE, ph.getType());
		assertSame(RecordType.INITIAL, ph.getRecordType());
		assertNull(ph.getNextRecordType());

		ph = new ProducedHandshake(ed, RecordType.INITIAL);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.APPLICATION_DATA, ph.getType());
		assertSame(RecordType.INITIAL, ph.getRecordType());
		assertNull(ph.getNextRecordType());
		
		ph = new ProducedHandshake(ed, RecordType.NEXT_GEN, ProducedHandshake.Type.CHANGE_CIPHER_SPEC);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.CHANGE_CIPHER_SPEC, ph.getType());
		assertSame(RecordType.NEXT_GEN, ph.getRecordType());
		assertNull(ph.getNextRecordType());

		ph = new ProducedHandshake(h, RecordType.INITIAL, RecordType.HANDSHAKE);
		assertSame(h, ph.getHandshake());
		assertSame(ProducedHandshake.Type.HANDSHAKE, ph.getType());
		assertSame(RecordType.INITIAL, ph.getRecordType());
		assertSame(RecordType.HANDSHAKE, ph.getNextRecordType());

		ph = new ProducedHandshake(ed, RecordType.INITIAL, RecordType.HANDSHAKE);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.APPLICATION_DATA, ph.getType());
		assertSame(RecordType.INITIAL, ph.getRecordType());
		assertSame(RecordType.HANDSHAKE, ph.getNextRecordType());

		ph = new ProducedHandshake(ed, RecordType.NEXT_GEN, RecordType.APPLICATION, ProducedHandshake.Type.CHANGE_CIPHER_SPEC);
		assertSame(ed, ph.getHandshake());
		assertSame(ProducedHandshake.Type.CHANGE_CIPHER_SPEC, ph.getType());
		assertSame(RecordType.NEXT_GEN, ph.getRecordType());
		assertSame(RecordType.APPLICATION, ph.getNextRecordType());
		
	}
}
