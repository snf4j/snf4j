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

import java.nio.ByteBuffer;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.engine.crypto.CryptoException;
import org.snf4j.quic.engine.crypto.ICryptoEngine;
import org.snf4j.quic.engine.crypto.ProducedCrypto;

/**
 * An adapter adapting some of functionalities from {@link ICryptoEngine} to the
 * QUIC specification.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoEngineAdapter {
	
	private final static int OFFSETS_NUMBER = EncryptionLevel.APPLICATION_DATA.cryptoOrdinal()+1;
	
	private final ICryptoEngine engine;
	
	private final long[] produceOffsets = new long[OFFSETS_NUMBER];

	private final long[] consumeOffsets = new long[OFFSETS_NUMBER];
	
	/**
	 * Constructs the adapter for the given cryptographic engine.
	 * 
	 * @param engine the cryptographic engine to adapt
	 */
	public CryptoEngineAdapter(ICryptoEngine engine) {
		this.engine = engine;
	}
	
	/**
	 * Consumes cryptographic message data from a CRYPTO frame.
	 * <p>
	 * NOTE: The offset points to the position in the stream of the cryptographic
	 * message data in each encryption level separately.
	 * 
	 * @param src    the cryptographic message data
	 * @param offset the byte offset in the stream of the cryptographic message
	 *               data in the given encryption level
	 * @param length the length of the cryptographic message data
	 * @param level  the encryption level
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	public void consume(ByteBuffer src, long offset, int length, EncryptionLevel level) throws QuicException {
		int j = level.cryptoOrdinal();
		long amount = offset + length;
		
		if (j > 0) {
			amount += consumeOffsets[j - 1]; 
			offset += consumeOffsets[j - 1];
		}
		if (consumeOffsets[j] < amount) {
			consumeOffsets[j] = amount;
		}
		engine.consume(src, offset, length);
	}
	
	/**
	 * Tries to produce some cryptographic message data.
	 * <p>
	 * NOTE: The offsets in the returned holders point to the position in the stream
	 * of the cryptographic message data in each encryption level separately.
	 * 
	 * @return the produced cryptographic message data or an empty array if no
	 *         cryptographic data was ready to be produced
	 * @throws CryptoException if a handshake related error occurred
	 * @throws QuicException   if an error occurred
	 */
	public ProducedCrypto[] produce() throws QuicException {
		ProducedCrypto[] produced = engine.produce();
		int j;
		long offset;
		
		for (int i=0; i<produced.length; ++i) {
			ProducedCrypto p = produced[i];
			
			if (p.getEncryptionLevel() != EncryptionLevel.EARLY_DATA) {
				offset = p.getOffset();
				j = p.getEncryptionLevel().cryptoOrdinal();
				produceOffsets[j] = offset+p.getData().remaining();
				if (j > 0) {
					produced[i] = new ProducedCrypto(
							p.getData(), 
							p.getEncryptionLevel(), 
							offset - produceOffsets[j-1]);
				}
			}
		}
		return produced;
	}

	/**
	 * Returns the cryptographic engine associated with this adapter.
	 * 
	 * @return the cryptographic engine associated with this adapter
	 */
	public ICryptoEngine getEngine() {
		return engine;
	}
}
