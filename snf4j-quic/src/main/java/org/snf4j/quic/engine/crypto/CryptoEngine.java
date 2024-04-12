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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.quic.TransportError;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.QuicException;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.engine.HandshakeAggregator;
import org.snf4j.tls.engine.IHandshakeEngine;
import org.snf4j.tls.engine.ProducedHandshake;
import org.snf4j.tls.record.RecordType;
import org.snf4j.tls.session.ISession;

/**
 * The default QUIC cryptographic engine handling the TLS handshake.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoEngine implements ICryptoEngine {
	
	private final static ProducedCrypto[] NONE_PRODUCED = new ProducedCrypto[0];
	
	private static EncryptionLevel[] ENCRYPTION_LEVEL_MAP = new EncryptionLevel[RecordType.values().length];
	
	private final IHandshakeEngine handshaker;
	
	private final HandshakeAggregator aggregator;
	
	private final List<CryptoData> buffer = new LinkedList<>();
	
	private final int maxBufferLength;

	private int bufferLength;
	
	private long consumeOffset;
	
	private long produceOffset;
	
	static {
		ENCRYPTION_LEVEL_MAP[RecordType.INITIAL.ordinal()] = EncryptionLevel.INITIAL;
		ENCRYPTION_LEVEL_MAP[RecordType.ZERO_RTT.ordinal()] = EncryptionLevel.EARLY_DATA;
		ENCRYPTION_LEVEL_MAP[RecordType.HANDSHAKE.ordinal()] = EncryptionLevel.HANDSHAKE;
		ENCRYPTION_LEVEL_MAP[RecordType.APPLICATION.ordinal()] = EncryptionLevel.APPLICATION_DATA;
	}
	
	/**
	 * Constructs a cryptographic engine driven by the given TLS handshake engine
	 * and with maximum size of buffered out-of-order cryptographic data.
	 * 
	 * @param handshaker      the TLS handshake engine
	 * @param maxBufferLength the maximum size of buffered out-of-order
	 *                        cryptographic data
	 */
	public CryptoEngine(IHandshakeEngine handshaker, int maxBufferLength) {
		this.handshaker = handshaker;
		aggregator = new HandshakeAggregator(handshaker);
		this.maxBufferLength = maxBufferLength;
	}

	/**
	 * Constructs a cryptographic engine driven by the given TLS handshake engine
	 * and with default (4096) maximum size of buffered out-of-order cryptographic
	 * data.
	 * 
	 * @param handshaker the TLS handshake engine
	 */
	public CryptoEngine(IHandshakeEngine handshaker) {
		this(handshaker, 4096);
	}
	
	private void tryConsumeBuffer() throws Alert {
		for (Iterator<CryptoData> i = buffer.iterator(); i.hasNext();) {
			CryptoData buffered = i.next();
			if (buffered.offset == consumeOffset) {
				int length = buffered.data.remaining();
				aggregator.unwrap(buffered.data, length);
				consumeOffset = buffered.nextOffset;
				i.remove();
				bufferLength -= length;
			}
			else {
				break;
			}
		}
	}
	
	/**
	 * Tries to consume remaining data if any.
	 * 
	 * @return {@code true} if consuming should be continued
	 * @throws Alert if an alert occurred
	 */
	private boolean tryConsumeRemaining() throws Alert {
		return !aggregator.hasRemaining() || aggregator.unwrapRemaining();
	}
	
	@Override
	public boolean needConsume() {
		return bufferLength != 0 || !aggregator.isEmpty();
	}
	
	@Override
	public void consume(ByteBuffer src, long offset, int length) throws QuicException {
		try {
			if (offset == consumeOffset) {
				if (!handshaker.updateTasks()) {
					if (tryConsumeRemaining()) {
						aggregator.unwrap(src, length);
						consumeOffset += length;
						tryConsumeBuffer();
						return;
					}
				}
			}

			int bufferPos = 0;
			long nextOffset = offset + length;	
			CryptoData buffered;
			Iterator<CryptoData> i;

			if (offset >= consumeOffset) {
				for (i = buffer.iterator(); i.hasNext();) {
					buffered = i.next();
					if (offset < buffered.offset) {
						if (nextOffset > buffered.offset) {
							bufferPos = -1;
						}
						break;
					}
					else if (offset == buffered.offset && nextOffset == buffered.nextOffset) {
						src.position(src.position() + length);
						return;
					}
					else if (offset < buffered.nextOffset) {
						bufferPos = -1;
						break;
					}
					++bufferPos;
				}
			}
			else if (nextOffset <= consumeOffset) {
				src.position(src.position() + length);
				return;
			}
			else {
				bufferPos = -1;
			}

			if (bufferPos == -1) {
				throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Invalid offset and length in crypto frame");
			}

			byte[] data = new byte[length];
			CryptoData cdata;

			src.get(data);
			cdata = new CryptoData(ByteBuffer.wrap(data), offset, nextOffset);
			if (buffer.isEmpty()) {
				buffer.add(cdata);
				bufferLength = length;
			}
			else {
				if (bufferPos < buffer.size()) {
					buffer.add(bufferPos, cdata);
				}
				else {
					buffer.add(cdata);
				}
				bufferLength += length;
			}

			if (bufferLength > maxBufferLength) {
				throw new QuicException(TransportError.CRYPTO_BUFFER_EXCEEDED, "Exceeded buffer size for out-of-order crypto frames");
			}
		}
		catch (Alert e) {
			throw new CryptoException(e);
		}
	}
	
	@Override
	public void cleanup() {
		handshaker.cleanup();
	}
		
	@Override
	public Runnable getTask() {
		return handshaker.getTask();
	}
	
	@Override
	public boolean needProduce() {
		return handshaker.needProduce() || handshaker.hasProducingTask();
	}
	
	@Override
	public ProducedCrypto[] produce() throws QuicException {
		ProducedHandshake[] produced;
		
		try {
			produced = handshaker.produce();
		} catch (Alert e) {
			throw new CryptoException(e);
		}
		
		if (produced.length > 0) {
			List<ProducedCrypto> producedCrypto = new ArrayList<>(produced.length);
			List<ProducedCrypto> earlyData = null;
			RecordType earlyDataPrevType = null;
			int limit = 0;
			RecordType prevType = null;
			
			//collect early data
			for (int i=0; i<produced.length; ++i) {
				ProducedHandshake ph = produced[i];

				if (ph.getRecordType() == RecordType.ZERO_RTT) {
					if (earlyData == null) {
						earlyData = new ArrayList<>(produced.length);
						earlyDataPrevType = prevType;
					}
					ByteBuffer data = ByteBuffer.allocate(ph.getHandshake().getLength());			
					ph.getHandshake().getBytes(data);
					data.flip();
					earlyData.add(new ProducedCrypto(data, EncryptionLevel.EARLY_DATA, -1));
				}
				else {
					produced[limit++] = produced[i];
				}
				prevType = ph.getRecordType();
			}
			
			if (earlyData != null && earlyDataPrevType == null) {
				producedCrypto.addAll(earlyData);
				earlyData = null;
			}
			
			if (limit > 0) {
				int length = 0, j = 0, count = 0;

				prevType = produced[0].getRecordType();
				for (int i=0; i<limit; ++i) {
					ProducedHandshake ph = produced[i];
					boolean sameType = ph.getRecordType() == prevType;
					boolean produce;

					if (sameType) {
						length += ph.getHandshake().getLength();
						++count;
						produce = i == limit - 1;
					}
					else {
						produce = true;
					}

					if (produce) {
						EncryptionLevel level = ENCRYPTION_LEVEL_MAP[prevType.ordinal()];

						if (level == null) {
							throw new CryptoException(new InternalErrorAlert("Unexpected encryption level"));
						}

						ByteBuffer data = ByteBuffer.allocate(length);

						for (int k=0; k < count; ++k) {
							produced[j++].getHandshake().getBytes(data);
						}
						data.flip();
						producedCrypto.add(new ProducedCrypto(data, level, produceOffset));
						if (earlyData != null && prevType == earlyDataPrevType) {
							producedCrypto.addAll(earlyData);
							earlyData = null;
						}
						produceOffset += data.remaining();
						if (!sameType) {
							prevType = ph.getRecordType();
							length = 0;
							count = 0;
							--i;
						}
					}
				}
			}
			return producedCrypto.toArray(new ProducedCrypto[producedCrypto.size()]);
		}
		return NONE_PRODUCED;
	}
	
	@Override
	public void start() throws CryptoException, QuicException {
		try {
			handshaker.start();
		} catch (Alert e) {
			throw new CryptoException(e);
		}
	}

	@Override
	public boolean updateTasks() throws CryptoException, QuicException {
		try {
			if (!handshaker.updateTasks()) {
				if (tryConsumeRemaining()) {
					tryConsumeBuffer();
				}
				return false;
			}
			return true;
		} catch (Alert e) {
			throw new CryptoException(e);
		}
	}

	@Override
	public boolean hasTask() {
		return handshaker.hasTask();
	}

	@Override
	public boolean hasRunningTask(boolean onlyUndone) {
		return handshaker.hasRunningTask(onlyUndone);
	}
	
	@Override
	public boolean isHandshakeDone() {
		return handshaker.getState().isConnected();
	}
	
	private static class CryptoData {
		
		final ByteBuffer data;

		final long offset;
		
		final long nextOffset;
				
		private CryptoData(ByteBuffer data, long offset, long nextOffset) {
			this.offset = offset;
			this.nextOffset = nextOffset;
			this.data = data;
		}
	}

	@Override
	public ISession getSession() {
		return handshaker.getState().getSession();
	}

}
