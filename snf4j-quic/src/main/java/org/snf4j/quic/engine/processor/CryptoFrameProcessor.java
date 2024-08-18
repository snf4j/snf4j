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
package org.snf4j.quic.engine.processor;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.engine.EncryptionLevel;
import org.snf4j.quic.engine.PacketNumberSpace;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.FrameType;
import org.snf4j.quic.packet.IPacket;

class CryptoFrameProcessor implements IFrameProcessor<CryptoFrame> {

	@Override
	public FrameType getType() {
		return FrameType.CRYPTO;
	}

	@Override
	public void process(QuicProcessor p, CryptoFrame frame, IPacket packet) throws QuicException {
		p.adapter.consume(
				frame.getData(), 
				frame.getDataOffset(), 
				frame.getDataLength(),
				packet.getType().encryptionLevel());
	}

	@Override
	public void sending(QuicProcessor p, CryptoFrame frame, IPacket packet) {
	}
	
	@Override
	public void recover(QuicProcessor p, CryptoFrame frame, PacketNumberSpace space) {
		EncryptionLevel level;
		
		switch (space.getType()) {
		case INITIAL:
			level = EncryptionLevel.INITIAL;
			break;
			
		case HANDSHAKE:
			level = EncryptionLevel.HANDSHAKE;
			break;
			
		default:
			level = EncryptionLevel.APPLICATION_DATA;
		}
		p.fragmenter.addPending(new ProducedCrypto(frame.getData(), level, frame.getDataOffset()));
	}
	
}
