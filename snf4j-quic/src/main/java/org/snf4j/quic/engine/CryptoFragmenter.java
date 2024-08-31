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

import static org.snf4j.quic.engine.EncryptionLevel.INITIAL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.engine.crypto.ProducedCrypto;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.AckFrame;
import org.snf4j.quic.frame.AckFrameBuilder;
import org.snf4j.quic.frame.CryptoFrame;
import org.snf4j.quic.frame.FrameInfo;
import org.snf4j.quic.frame.IFrame;
import org.snf4j.quic.frame.MultiPaddingFrame;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.tp.TransportParameters;

/**
 * A fragmenter of cryptographic data during the QUIC handshake phase. It tries
 * to consume as much as possible bytes from the pending cryptographic data. It
 * also consumes pending ACKs and other pending frames (e.g. HANDSHAKE_DONE).
 * <p>
 * NOTE: If required the resulting UDP datagrams are padded with PADDING frames.  
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class CryptoFragmenter {

	private final static ILogger LOG = LoggerFactory.getLogger(CryptoFragmenter.class);
	
	private final static int ENC_LVL_COUNT = EncryptionLevel.values().length;
	
	private final PacketProtection protection;
	
	private final IPacketProtectionListener protectionListener;
	
	private final QuicProcessor processor;
	
	private final LinkedList<ProducedCrypto> pending = new LinkedList<>();
	
	private final QuicState state;
	
	private ProducedCrypto current;
	
	/**
	 * Constructs a fragmenter associated with the given objects.
	 * 
	 * @param state              the QUIC state
	 * @param protection         the packet protection
	 * @param protectionListener the packet protection listener
	 * @param processor          the QUIC packet processor
	 */
	public CryptoFragmenter(QuicState state, PacketProtection protection, IPacketProtectionListener protectionListener, QuicProcessor processor) {
		this.state = state;
		this.protection = protection;
		this.protectionListener = protectionListener;
		this.processor = processor;
		processor.setFragmenter(this);
	}
	
	/**
	 * Tells if this framgmenter has still cryptographic data to be protected and
	 * sent.
	 * 
	 * @return {@code true} if there is still cryptographic data to be sent
	 */
	public boolean hasPending() {
		return current != null;
	}
		
	/**
	 * Adds cryptographic data to be protected and sent at the proper place in the
	 * list of pending cryptographic data.
	 * 
	 * @param produced the cryptographic data
	 */
	public void addPending(ProducedCrypto produced) {
		if (current == null) {
			current = produced;
		}
		else {
			int ord = produced.getEncryptionLevel().ordinal();
			
			if (current.getEncryptionLevel().ordinal() > ord) {
				pending.add(0, current);
				current = produced;
			}
			else {
				int size = pending.size();
				
				for (int i=0; i<size; ++i) {
					ProducedCrypto pc = pending.get(i);
					if (pc.getEncryptionLevel().ordinal() > ord) {
						pending.add(i, produced);
						return;
					}
				}
				pending.add(produced);
			}
		}
	}
	
	private IPacket packet(CryptoFragmenterContext ctx, IPacket packet) throws QuicException {
		FrameManager frames = ctx.space.frames();
		IFrame frame;
		
		if (packet == null) {
			AckFrameBuilder acks = ctx.space.acks();
			
			if (!acks.isEmpty()) {
				AckFrame ack = acks.build(
						state.getConfig().getMaxNumberOfAckRanges(), 
						ctx.ackTime(), 
						ctx.level == EncryptionLevel.APPLICATION_DATA && state.isHandshakeConfirmed()
						? state.getConfig().getAckDelayExponent()
								: TransportParameters.DEFAULT_ACK_DELAY_EXPONENT);

				packet = ctx.packet(ack);
				if (packet != null) {
					acks.keepPriorTo(ack.getSmallestPacketNumber());
					frames.fly(ack, packet.getPacketNumber());
				}
			}
			
			frame = frames.peek();
			if (frame != null) {
				boolean fly;
				
				if (packet == null) {
					int len = frame.getLength();
					
					if (len < 3) {
						packet = ctx.packet(3);
						if (packet != null) {
							packet.getFrames().add(new MultiPaddingFrame(3-len));
							packet.getFrames().add(frame);
						}
					}
					else {
						packet = ctx.packet(frame);
					}
					fly = packet != null;
				}
				else {
					fly = ctx.add(packet, frame);
				}
				if (fly) {
					do {
						frames.fly(frame, packet.getPacketNumber());
						frame = frames.peek();
					} 
					while (frame != null && ctx.add(packet, frame));
				}
			}
			ctx.commitRemaining();
		}
		else {
			ctx.rollbackRemaining();
		}
		
		if (current != null && current.getEncryptionLevel() == ctx.level) {
			if (packet == null) {
				packet = ctx.packet(Math.min(20, current.getData().remaining()));
			}
			if (packet != null) {
				frame = new CryptoFrame(current.getOffset(), current.getData());
				packet.getFrames().add(frame);
				frames.fly(frame, packet.getPacketNumber());

				int len = ctx.length(packet);

				int delta = ctx.remaining - len;

				if (delta < 0) {
					ByteBuffer data = current.getData();
					ByteBuffer dup = data.duplicate();

					data.limit(data.limit() + delta);
					dup.position(data.limit());
					current = new ProducedCrypto(dup, ctx.level, data.limit());
					ctx.remaining -= ctx.length(packet);
				}
				else {
					ctx.remaining = delta;
					current = null;
				}
			}
		}
		else if (packet != null) {
			ctx.remaining -= ctx.length(packet);
		}
		return packet;
	}
	
	/**
	 * Protects pending cryptographic data. It tries to consume as much as possible
	 * bytes from the pending cryptographic data.
	 * 
	 * @param dst the destination buffer
	 * @return the number of bytes put into the destination buffer, or -1 if there
	 *         is no enough room in the destination buffer to store protected data
	 * @throws QuicException if an error occurred
	 */
	public int protectPending(ByteBuffer dst) throws QuicException {
		IAntiAmplificator antiAmplificator = state.getAntiAmplificator();
		int available = Integer.MAX_VALUE;
		IDataBlockable toBlock;

		if (current == null) {
			current = pending.poll();
		}	
		
		if (antiAmplificator.isArmed()) {
			if (state.isAddressValidated()) {
				antiAmplificator.disarm();
			}
			else if (antiAmplificator.isBlocked()) {
				return 0;
			}
			else {
				antiAmplificator.unlock();
				available = antiAmplificator.available();
			}
		}
		
		CongestionController congestion = state.getCongestion();
		
		if (congestion.isBlocked()) {
			return 0;
		}
		else {
			int tmp = congestion.available();
		
			congestion.unlock();
			if (tmp < available) {
				available = tmp;
				toBlock = congestion;
			}
			else {
				toBlock = antiAmplificator;
			}
		}
		
		if (state.getEncryptorLevel() == null) {
			protectionListener.onInitialKeys(
					state, 
					state.getConnectionIdManager().getDestinationId(), 
					state.getVersion());
		}
		
		if (available < state.getMaxUdpPayloadSize()) {
			boolean padding = false;
			boolean debug = LOG.isDebugEnabled();
			
			if (state.getContext(INITIAL).getEncryptor() != null) {
				if (current != null && current.getEncryptionLevel() == INITIAL) {
					padding = true;
				}
				else {
					PacketNumberSpace space = state.getSpace(INITIAL);
					
					if (space.frames().hasAckEliciting(FrameInfo.of(state.getVersion()))) {
						padding = true;					
					}
					else if (!space.frames().isEmpty() || !space.acks().isEmpty()) {
						padding = state.isClientMode();
					}
				}
			}
			
			if (padding) {
				toBlock.lock(state.getMaxUdpPayloadSize());
				if (debug) {
					LOG.debug("Sending blocked by {} (needed {}, available {}) for {}",
							toBlock.name(),
							state.getMaxUdpPayloadSize(),
							available,
							state.getSession()
							);
				}
				return 0;
			}
			else {
				int min = state.getConfig().getMinNonBlockingUdpPayloadSize();
				
				if (available < min) {
					toBlock.lock(min);
					if (debug) {
						LOG.debug("Sending blocked by {} (needed {}, available {}) for {}",
								toBlock.name(),
								min,
								available,
								state.getSession()
								);
					}
					return 0;
				}
			}
		}
				
		CryptoFragmenterContext ctx = new CryptoFragmenterContext(state, available);
		int dst0 = dst.position();
		IPacket packet = null;
		List<IPacket> packets = new ArrayList<>();
		boolean padding = false;
				
		for (int i=0; i<ENC_LVL_COUNT; ++i) {
			EncryptionLevel level = EncryptionLevel.values()[i];
			
			if (ctx.level != level) {
				if (!ctx.init(level)) {
					continue;
				}
			}
			
			if (ctx.noRemaining()) {
				i = ENC_LVL_COUNT-1;
			}
			else {
				if (current != null) {
					packet = packet(ctx, packet);
					if (current == null) {
						current = pending.poll();
						if (current != null && current.getEncryptionLevel() == level) {
							--i;
							continue;
						}
					}
				}
				else {
					packet = packet(ctx, packet);
				}
			}
			
			if (packet != null) {
				if (packet.getType() == PacketType.INITIAL) {
					if (state.isClientMode() || FrameInfo.of(state.getVersion()).isAckEliciting(packet)) {
						padding = true;
					}
				}
				packets.add(packet);
				packet = null;
			}
		}
		
		int size = packets.size();
		int[] lengths = new int[size--];
		int produced;
		
		for (int i=0; i<=size; ++i) {
			int pos0;
			
			packet = packets.get(i);
			if (i == size && padding) {
				ctx.padding(packet);
			}
			pos0 = dst.position();
			protection.protect(state, packet, dst);
			lengths[i] = dst.position() - pos0;
		}
		produced = dst.position() - dst0;
		processSending(packets, lengths);
		antiAmplificator.incSent(produced);
		return produced;	
	}	
	
	private void processSending(List<IPacket> packets, int[] lengths) {
		FrameInfo info = FrameInfo.of(state.getVersion());
		int size = packets.size();
		
		processor.preSending();
		for (int i=0; i<size; ++i) {
			IPacket packet = packets.get(i);
			processor.sending(
					packet, 
					info.isAckEliciting(packet), 
					info.isCongestionControlled(packet),
					lengths[i]);
		}
	}
	
	/**
	 * Protects the given produced cryptographic data. It tries to consume as much
	 * as possible data from the pending and produced cryptographic data. All not
	 * protected data from the passed produced cryptographic data will be stored in
	 * the pending data.
	 * 
	 * @param produced the produced cryptographic data to protect
	 * @param dst      the destination buffer
	 * @return the number of bytes put into the destination buffer, or -1 if there
	 *         is no enough room in the destination buffer to store protected data
	 * @throws QuicException if an error occurred
	 */
	public int protect(ProducedCrypto[] produced, ByteBuffer dst) throws QuicException {
		for (ProducedCrypto p: produced) {
			pending.add(p);
		}
		return protectPending(dst);
		
	}

}
