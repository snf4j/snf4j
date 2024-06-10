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

import static org.snf4j.core.engine.HandshakeStatus.FINISHED;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP_AGAIN;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NOT_HANDSHAKING;
import static org.snf4j.core.engine.Status.CLOSED;
import static org.snf4j.core.engine.Status.OK;
import static org.snf4j.quic.engine.HandshakeState.CLOSING;
import static org.snf4j.quic.engine.HandshakeState.DONE;
import static org.snf4j.quic.engine.HandshakeState.DONE_SENDING;
import static org.snf4j.quic.engine.HandshakeState.DONE_WAITING;
import static org.snf4j.quic.engine.HandshakeState.INIT;
import static org.snf4j.quic.engine.HandshakeState.STARTED;
import static org.snf4j.quic.engine.HandshakeState.STARTING;

import java.nio.ByteBuffer;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.FrameInfo;
import org.snf4j.quic.frame.HandshakeDoneFrame;
import org.snf4j.quic.packet.ILongHeaderPacket;
import org.snf4j.quic.packet.IPacket;
import org.snf4j.quic.packet.PacketType;
import org.snf4j.quic.packet.PacketUtil;
import org.snf4j.tls.engine.HandshakeEngine;
import org.snf4j.tls.engine.IEngineHandler;
import org.snf4j.tls.engine.IEngineParameters;

/**
 * The QUIC engine.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class QuicEngine implements IEngine {

	private final QuicState state;
	
	private final CryptoEngineStateListener cryptoListener;
	
	private final CryptoEngine cryptoEngine;
	
	private final CryptoEngineAdapter cryptoAdapter;
	
	private final CryptoFragmenter cryptoFragmenter;
	
	private final PacketProtection protection;
	
	private final PacketAcceptor acceptor;
	
	private final QuicProcessor processor;
	
	private final IPacketProtectionListener protectionListener = new IPacketProtectionListener() {

		@Override
		public void onInitialKeys(QuicState state, byte[] destinationId, Version version) throws QuicException {
			try {
				initialKeys(InitialSalt.of(version), destinationId);
			} catch (Exception e) {
				throw new QuicException(TransportError.INTERNAL_ERROR, "", e);
			}
		}

		@Override
		public void onKeysRotation(QuicState state) throws QuicException {
		}
	};
		
	private boolean outboundDone;
	
	private boolean inboundDone;
	
	QuicException error;

	/**
	 * Constructs a QUIC engine.
	 * 
	 * @param clientMode determines if the engine is created for server or client
	 * @param parameters the TLS engine parameters
	 * @param handler    the TLS engine handler
	 */
	public QuicEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler) {
		state = new QuicState(clientMode);
		cryptoListener = new CryptoEngineStateListener(state);
		cryptoEngine = new CryptoEngine(new HandshakeEngine(clientMode, parameters, handler, cryptoListener));
		cryptoAdapter = new CryptoEngineAdapter(cryptoEngine);
		processor = new QuicProcessor(state, cryptoAdapter);
		protection = new PacketProtection(protectionListener);
		cryptoFragmenter = new CryptoFragmenter(state, protection, protectionListener, processor);
		acceptor = new PacketAcceptor(state);
	}

	void initialKeys(byte[] salt, byte[] connectionId) throws Exception {
		cryptoListener.onInit(salt, connectionId);
	}
	
	@Override
	public void init() {
	}

	@Override
	public void cleanup() {
		cryptoEngine.cleanup();
	}
	
	@Override
	public void beginHandshake() throws QuicException {
		if (state.getHandshakeState() == INIT) {
			state.setHandshakeState(STARTING);
		}
	}

	private void beginHandshake0() throws QuicException {
		if (state.getHandshakeState().notStarted()) {
			state.setHandshakeState(STARTED);
			cryptoEngine.start();
		}
	}
	
	@Override
	public boolean isOutboundDone() {
		return outboundDone;
	}

	@Override
	public boolean isInboundDone() {
		return inboundDone;
	}

	@Override
	public void closeOutbound() {
		if (error == null) {
			error = new QuicException(TransportError.NO_ERROR, "Closed");
			state.setHandshakeState(HandshakeState.CLOSING);
		}
	}

	@Override
	public void closeInbound() throws SessionIncidentException {
	}

	@Override
	public int getMinApplicationBufferSize() {
		return 4096;
	}

	@Override
	public int getMinNetworkBufferSize() {
		return state.getMaxUdpPayloadSize();
	}

	@Override
	public int getMaxApplicationBufferSize() {
		return getMinApplicationBufferSize();
	}

	@Override
	public int getMaxNetworkBufferSize() {
		return getMinNetworkBufferSize();
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		switch (state.getHandshakeState()) {
		case INIT:         return NOT_HANDSHAKING;
		case STARTING:     return state.isClientMode() ? NEED_WRAP : NEED_UNWRAP;
		case DONE_SENDING: return NEED_WRAP;
		case DONE_WAITING: return NEED_UNWRAP;
		case CLOSING:      return NEED_WRAP;
		case CLOSED:       return NOT_HANDSHAKING;
		default:
		}
		
		if (cryptoEngine.isHandshakeDone()) {
			if (cryptoEngine.needProduce()) {
				return NEED_WRAP;
			}
			if (cryptoEngine.needConsume()) {
				return NEED_UNWRAP;
			}
			return NOT_HANDSHAKING;
		}
		if (cryptoEngine.hasTask() || cryptoEngine.hasRunningTask(true)) {
			return NEED_TASK;
		}
		if (cryptoEngine.needProduce()) {
			return NEED_WRAP;
		}
		if (cryptoEngine.hasRunningTask(false)) {
			return NEED_UNWRAP_AGAIN;
		}
		return NEED_UNWRAP;
	}

	@Override
	public Object getSession() {
		return cryptoEngine.getSession();
	}

	@Override
	public Runnable getDelegatedTask() {
		return cryptoEngine.getTask();
	}

	@Override
	public IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws QuicException {
		return null;
	}

	IEngineResult exception(Exception e, ByteBuffer src, ByteBuffer dst, boolean wrapping) throws QuicException {
		QuicException qe;
		
		if (e instanceof QuicException) {
			qe = (QuicException) e;
		}
		else {
			qe = new QuicException(TransportError.INTERNAL_ERROR, "", e);
		}
		dst.reset();
		throw qe;
	}
	
	boolean updateTasks() throws QuicException {
		return cryptoEngine.updateTasks();
	}
	
	@Override
	public IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws QuicException {
		int produced;
		
		dst.mark();
		try {
			beginHandshake0();
			
			HandshakeStatus status = getHandshakeStatus();
			
			if (outboundDone) {
				return new EngineResult(
						CLOSED, 
						status, 
						0, 
						0);
			}

			if (state.getHandshakeState() == CLOSING) {
				state.setHandshakeState(HandshakeState.CLOSED);
				outboundDone = true;
				inboundDone = true;
				return new EngineResult(
						CLOSED, 
						getHandshakeStatus(), 
						0, 
						0);
			}
			
			switch (status) {
			case NEED_WRAP:
				if (updateTasks()) {
					return new EngineResult(
							OK, 
							status, 
							0, 
							0);
					
				}
				break;
				
			default:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);
			}
			
			produced = cryptoFragmenter.protect(cryptoAdapter.produce(), dst);			
			
			switch (state.getHandshakeState()) {
			case STARTED:
				if (state.isClientMode()) {
					if (cryptoEngine.isHandshakeDone()) {
						state.setHandshakeState(DONE_WAITING);
					}
				}
				break;
				
			case DONE_SENT:
				state.setHandshakeState(DONE);
				return new EngineResult(
						OK, 
						FINISHED, 
						0, 
						produced);
				
			default:
			}
		}
		catch (Exception e) {
			return exception(e, src, dst, true);
		}
		
		return new EngineResult(
				OK, 
				getHandshakeStatus(), 
				0, 
				produced);
	}
	
	private IEngineResult unwrapAppData(ByteBuffer src, ByteBuffer dst) throws QuicException {
		int consumed = src.remaining();
		
		while (acceptor.accept(src)) {
			protection.unprotect(state, src);
		}
		
		return new EngineResult(
				OK, 
				HandshakeStatus.NOT_HANDSHAKING, 
				consumed, 
				0);		
	}
	
	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws QuicException {
		int consumed;

		dst.mark();
		try {
			beginHandshake0();
			
			HandshakeStatus status = getHandshakeStatus();
			
			if (inboundDone) {
				return new EngineResult(
						CLOSED, 
						status, 
						0, 
						0);
			}

			switch (status) {
			case NEED_UNWRAP:
			case NEED_UNWRAP_AGAIN:
				if (updateTasks()) {
					return new EngineResult(
							OK, 
							status, 
							0, 
							0);
					
				}
				break;
				
			case NOT_HANDSHAKING:
				return unwrapAppData(src, dst);
				
			default:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);
			}
			
			consumed = src.remaining();
			boolean udpChecked = false;
			
			while (acceptor.accept(src)) {
				IPacket packet = protection.unprotect(state, src);
				
				if (packet != null) {
					if (!FrameInfo.of(state.getVersion()).isValid(packet)) {
						throw new QuicException(TransportError.PROTOCOL_VIOLATION, "Frame not permitted in packet");
					}
					
					boolean ackElicting;
					FrameInfo info;
					
					if (packet.getType().hasLongHeader()) {
						info = FrameInfo.of(((ILongHeaderPacket)packet).getVersion());
					}
					else {
						info = FrameInfo.of(state.getVersion());
					}
					ackElicting = info.isAckEliciting(packet);
					
					if (!udpChecked && packet.getType() == PacketType.INITIAL) {
						if (!state.isClientMode() || ackElicting) {
							if (consumed < PacketUtil.MIN_MAX_UDP_PAYLOAD_SIZE) {
								return new EngineResult(
										OK, 
										getHandshakeStatus(), 
										consumed, 
										0);
							}
							udpChecked = true;
						}
					}
					processor.process(packet, ackElicting);
				}
			}
			
			switch (state.getHandshakeState()) {
			case STARTED:
				if (!state.isClientMode()) {
					if (cryptoEngine.isHandshakeDone()) {
						PacketNumberSpace space = state.getSpace(EncryptionLevel.APPLICATION_DATA);
						
						space.frames().add(HandshakeDoneFrame.INSTANCE);
						state.setHandshakeState(DONE_SENDING);
					}
				}
				break;
				
			case DONE_RECEIVED:
				state.setHandshakeState(DONE);
				return new EngineResult(
						OK, 
						FINISHED, 
						consumed, 
						0);
			default:
			}
			
		}
		catch (Exception e) {
			return exception(e, src, dst, false);
		}
		
		src.position(src.limit());
		return new EngineResult(
				OK, 
				getHandshakeStatus(), 
				consumed, 
				0);
	}
}
