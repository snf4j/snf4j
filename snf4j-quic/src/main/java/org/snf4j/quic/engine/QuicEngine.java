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
import static org.snf4j.quic.engine.HandshakeState.CLOSE_TIMEOUT;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_WAITING;
import static org.snf4j.quic.engine.HandshakeState.DONE;
import static org.snf4j.quic.engine.HandshakeState.DONE_SENDING;
import static org.snf4j.quic.engine.HandshakeState.DONE_WAITING;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_DRAINING;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_SENDING;
import static org.snf4j.quic.engine.HandshakeState.CLOSE_SENDING_2;
import static org.snf4j.quic.engine.HandshakeState.INIT;
import static org.snf4j.quic.engine.HandshakeState.STARTED;
import static org.snf4j.quic.engine.HandshakeState.STARTING;

import java.nio.ByteBuffer;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionTimer;
import org.snf4j.core.timer.ITimerTask;
import org.snf4j.quic.QuicException;
import org.snf4j.quic.TransportError;
import org.snf4j.quic.Version;
import org.snf4j.quic.engine.crypto.CryptoEngine;
import org.snf4j.quic.engine.processor.QuicProcessor;
import org.snf4j.quic.frame.ConnectionCloseFrame;
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

	private final static ILogger LOG = LoggerFactory.getLogger(QuicEngine.class);
	
	private final QuicState state;
	
	private final CryptoEngineStateListener cryptoListener;
	
	private final CryptoEngine cryptoEngine;
	
	private final CryptoEngineAdapter cryptoAdapter;
	
	private final CryptoFragmenter cryptoFragmenter;
	
	private final PacketProtection protection;
	
	private final PacketAcceptor acceptor;
	
	private final QuicProcessor processor;
	
	private ITimerTask closingTimer;
	
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
		
	private Runnable closingTimeout = new Runnable() {

		@Override
		public void run() {
			if (!inboundDone) {
				LOG.debug("Closing timeout for {}", state.getSession());
				inboundDone = true;
				state.setHandshakeState(HandshakeState.CLOSE_TIMEOUT);
				closingTimer = null;
			}
		}
	};
	
	private boolean outboundDone;
	
	private boolean inboundDone;
	
	private QuicException error;

	/**
	 * Constructs a QUIC engine.
	 * 
	 * @param clientMode determines if the engine is created for server or client
	 * @param parameters the TLS engine parameters
	 * @param handler    the TLS engine handler
	 */
	public QuicEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler) {
		this(new QuicState(clientMode), parameters, handler);
	}
	
	/**
	 * Constructs a QUIC engine with given QUIC state, TLS engine parameters and
	 * handler.
	 * 
	 * @param state      the QUIC state
	 * @param parameters the TLS engine parameters
	 * @param handler    the TLS engine handler
	 */
	public QuicEngine(QuicState state, IEngineParameters parameters, IEngineHandler handler) {
		this.state = state;
		cryptoListener = new CryptoEngineStateListener(state);
		cryptoEngine = new CryptoEngine(new HandshakeEngine(state.isClientMode(), parameters, handler, cryptoListener));
		cryptoAdapter = new CryptoEngineAdapter(cryptoEngine);
		protection = new PacketProtection(protectionListener);
		processor = new QuicProcessor(state, cryptoAdapter);
		cryptoFragmenter = new CryptoFragmenter(state, protection, protectionListener, processor);
		acceptor = new PacketAcceptor(state);
		state.getLossDetector().setProcessor(processor);
	}

	@Override
	public void link(ISession session) {
		state.setSession(session);
	}
	
	void initialKeys(byte[] salt, byte[] connectionId) throws Exception {
		cryptoListener.onInit(salt, connectionId);
	}
	
	@Override
	public void init() {
	}

	@Override
	public void timer(ISessionTimer timer, Runnable awakeningTask) throws Exception {
		state.getTimer().init(timer, awakeningTask);
	}
	
	private void cancelClosingTimer() {
		if (closingTimer != null) {
			LOG.debug("Canceled closing timer for {}", state.getSession());
			closingTimer.cancelTask();
			closingTimer = null;
		}
	}
	
	@Override
	public void cleanup() {
		cryptoEngine.cleanup();
		state.getLossDetector().disable();
		cancelClosingTimer();
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
	
	private void closing(TransportError error, String message, HandshakeState nextState) {
		this.error = new QuicException(error, message);
		if (state.initImmediateClose(new ConnectionCloseFrame(this.error.getErrorCode(), 0, null)) == 0) {
			inboundDone = true;
			outboundDone = true;
			state.setHandshakeState(CLOSE_DRAINING);
		}
		else {
			state.setHandshakeState(nextState);
		}
	}
	
	@Override
	public void closeOutbound() {
		if (error == null) {
			closing(TransportError.NO_ERROR, "Closed", CLOSE_SENDING);
		}
	}

	@Override
	public void closeInbound() throws SessionIncidentException {
		if (error == null) {
			closing(TransportError.INTERNAL_ERROR, "Closed without close message", CLOSE_SENDING_2);
			inboundDone = true;
			throw new SessionIncidentException(SessionIncident.CLOSED_WITHOUT_CLOSE_MESSAGE);
		}
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

	private HandshakeStatus needWrapIfAllowed() {
		return state.isBlocked() ? NEED_UNWRAP : NEED_WRAP;
	}

	private HandshakeStatus needUnwrapIfIdle() {
		return needWrap() ? NEED_WRAP : NEED_UNWRAP;
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		switch (state.getHandshakeState()) {
		case INIT:                 return NOT_HANDSHAKING;
		case STARTING:             return state.isClientMode() ? NEED_WRAP : NEED_UNWRAP;
		case DONE_SENDING:         return needWrapIfAllowed();
		case DONE_SENT:            return NEED_WRAP;
		case DONE_WAITING:         return needUnwrapIfIdle();
		case DONE_RECEIVED:        return NEED_UNWRAP_AGAIN;
		case CLOSE_PRE_SENDING_2:  return NEED_UNWRAP_AGAIN;
		case CLOSE_SENDING_2:      return needWrapIfAllowed();
		case CLOSE_SENDING:        return needWrapIfAllowed();
		case CLOSE_PRE_WAITING:    return NEED_WRAP;
		case CLOSE_WAITING:        return needUnwrapIfIdle();
		case CLOSE_TIMEOUT:        return NEED_UNWRAP_AGAIN;
		case CLOSE_PRE_DRAINING:   return NEED_UNWRAP_AGAIN;
		case CLOSE_PRE_DRAINING_2: return NEED_WRAP;
		case CLOSE_DRAINING:       return NOT_HANDSHAKING;
		default:
		}
		
		if (cryptoEngine.isHandshakeDone()) {
			if (cryptoEngine.needProduce()) {
				return needWrapIfAllowed();
			}
			if (cryptoEngine.needConsume()) {
				return needUnwrapIfIdle();
			}
			return NOT_HANDSHAKING;
		}
		if (!state.getTimer().isInitialized()) {
			return HandshakeStatus.NEED_TIMER;
		}
		if (cryptoEngine.hasTask() || cryptoEngine.hasRunningTask(true)) {
			return NEED_TASK;
		}
		boolean unblocked = !state.isBlocked();
		if (unblocked) {
			if (cryptoEngine.needProduce() || cryptoFragmenter.hasPending()) {
				return NEED_WRAP;
			}
		}
		if (cryptoEngine.hasRunningTask(false)) {
			return NEED_UNWRAP_AGAIN;
		}
		if (unblocked && state.needSend()) {
			return NEED_WRAP;
		}
		return NEED_UNWRAP;
	}

	@Override
	public boolean needWrap() { 
		if (!outboundDone && !state.isBlocked()) {
			return state.needSend() 
				|| cryptoEngine.needProduce() 
				|| cryptoFragmenter.hasPending();
		}
		return false; 
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
		boolean debug = LOG.isDebugEnabled();
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
				
			case NOT_HANDSHAKING:
				break;
				
			default:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);
			}
			
			if (!state.getHandshakeState().isTransitory()) {
				produced = cryptoFragmenter.protect(cryptoAdapter.produce(), dst);
			}
			else {
				produced = 0;
			}
			
			switch (state.getHandshakeState()) {
			case STARTED:
				if (state.isClientMode()) {
					if (cryptoEngine.isHandshakeDone()) {
						state.setHandshakeState(DONE_WAITING);
					}
				}
				break;
				
			case DONE_SENT:
				if (debug) {
					LOG.debug("Handshake finished for {}", state.getSession());
					LOG.debug("Erasing HANDSHAKE keys for {}", state.getSession());
				}
				state.setHandshakeState(DONE);
				state.eraseKeys(EncryptionLevel.HANDSHAKE, state.getTime().nanoTime());
				return new EngineResult(
						OK, 
						FINISHED, 
						0, 
						produced);
				
			case CLOSE_PRE_WAITING:
				long delay = state.getLossDetector().getPtoPeriod() * 3;

				state.getLossDetector().disable();
				state.setHandshakeState(CLOSE_WAITING);
				if (debug) {
					LOG.debug("Scheduled closing timer with delay {} ns for {}", 
							delay, 
							state.getSession());						
				}
				closingTimer = state.getTimer().scheduleTask(closingTimeout, delay);
				outboundDone = true;
				return new EngineResult(
						CLOSED, 
						getHandshakeStatus(), 
						0, 
						produced);
				
			case CLOSE_PRE_DRAINING_2:	
				state.getLossDetector().disable();
				state.setHandshakeState(CLOSE_DRAINING);
				outboundDone = true;
				return new EngineResult(
						CLOSED, 
						getHandshakeStatus(), 
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
	
	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws QuicException {
		boolean debug = LOG.isDebugEnabled();
		int consumed;

		dst.mark();
		try {
			beginHandshake0();
			
			HandshakeStatus status = getHandshakeStatus();
			
			if (inboundDone) {
				if (state.getHandshakeState() == CLOSE_TIMEOUT) {
					state.setHandshakeState(CLOSE_DRAINING);
					status = getHandshakeStatus();
				}
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
				break;
				
			default:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);
			}
			
			if (!state.getHandshakeState().isTransitory()) {
				consumed = src.remaining();
				boolean udpChecked = false;
				boolean accepted = false;

				processor.preProcess();
				while (acceptor.accept(src)) {
					IPacket packet;

					if (!accepted) {
						boolean wasBlocked = state.getAntiAmplificator().isBlocked();

						accepted = true;
						state.getAntiAmplificator().incReceived(consumed);
						if (wasBlocked && !state.getAntiAmplificator().isBlocked()) {
							long currentTime = state.getTime().nanoTime();

							state.getLossDetector().setLossDetectionTimer(currentTime, true);
						}
					}

					packet = protection.unprotect(state, src);				
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
			}
			else {
				consumed = 0;
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
				if (debug) {
					LOG.debug("Handshake finished for {}", state.getSession());
					LOG.debug("Erasing HANDSHAKE keys for {}", state.getSession());
				}
				state.setHandshakeState(DONE);
				state.eraseKeys(EncryptionLevel.HANDSHAKE, state.getTime().nanoTime());
				return new EngineResult(
						OK, 
						FINISHED, 
						consumed, 
						0);
				
			case CLOSE_PRE_SENDING_2:
				if (error == null) {
					closing(TransportError.NO_ERROR, "Closed", CLOSE_SENDING_2);
				}
				else {
					state.setHandshakeState(CLOSE_SENDING_2);
				}
				inboundDone = true;
				return new EngineResult(
						CLOSED, 
						getHandshakeStatus(), 
						consumed, 
						0);
				
			case CLOSE_PRE_DRAINING:
				state.setHandshakeState(CLOSE_DRAINING);
				inboundDone = true;
				return new EngineResult(
						CLOSED, 
						getHandshakeStatus(), 
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
