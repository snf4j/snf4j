/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2021 SNF4J contributors
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
package org.snf4j.websocket;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.future.TaskFuture;
import org.snf4j.core.handler.AbstractStreamHandler;
import org.snf4j.core.handler.DataEvent;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.handler.SessionIncident;
import org.snf4j.core.logger.ExceptionLogger;
import org.snf4j.core.logger.IExceptionLogger;
import org.snf4j.core.logger.ILogger;
import org.snf4j.core.logger.LoggerFactory;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.core.session.IStreamSession;
import org.snf4j.websocket.frame.CloseFrame;
import org.snf4j.websocket.handshake.HandshakeFrame;
import org.snf4j.websocket.handshake.Handshaker;
import org.snf4j.websocket.handshake.IHandshaker;
import org.snf4j.websocket.handshake.InvalidHandshakeException;

class WebSocketSessionHandler extends AbstractStreamHandler {

	private final static ILogger LOGGER = LoggerFactory.getLogger(WebSocketSessionHandler.class);
	
	private final static IExceptionLogger ELOGGER = ExceptionLogger.getInstance();
	
	private final IHandshaker handshaker;
	
	private final IWebSocketHandler handler;
	
	private final boolean handleCloseFrame;
	
	WebSocketSessionHandler(IWebSocketHandler handler, boolean clientMode) {
		this.handler = handler;
		handshaker = new Handshaker(handler.getConfig(), clientMode);
		handleCloseFrame = handler.getConfig().handleCloseFrame();
	}
	
	@Override
	public void setSession(ISession session) {
		super.setSession(session);
		handler.setSession(session);
		handshaker.setSession(session);
	}
	
	public IHandshaker getHandshaker() {
		return handshaker;
	}
	
	void fireException(Throwable t) {
		try {
			getReadyFuture().abort(t);
			handler.exception(t);
		}
		catch (Throwable e) {
			ELOGGER.error(LOGGER, "Exception handling failed for {}: {}", getSession(), e);
		}
		getSession().quickClose();
	}
	
	TaskFuture<Void> getReadyFuture() {
		return (TaskFuture<Void>) getSession().getReadyFuture();
	}
	
	void handleCloseFrame(CloseFrame frame) {
		IStreamSession session = getSession();
		
		session.writenf(frame);
		session.close();
	}
	
	@Override
	public void read(Object msg) {
		if (handshaker.isFinished()) {
			if (handleCloseFrame && msg instanceof CloseFrame) {
				handleCloseFrame((CloseFrame) msg);
				return;
			}
			handler.read(msg);
		}
		else {
			HandshakeFrame frame = handshaker.handshake((HandshakeFrame) msg);
			TaskFuture<Void> readyFuture = getReadyFuture();
			IStreamSession session = getSession();
			
			if (frame != null) {
				session.writenf(frame);
			}
			if (readyFuture.isDone()) {
				return;
			}
			if (handshaker.isFinished()) {
				ICodecPipeline pipeline = session.getCodecPipeline();
				IWebSocketSessionConfig config = handler.getConfig();
				boolean hasExtensions = handshaker.hasExtensions();
				
				config.switchDecoders(pipeline, hasExtensions);
				if (hasExtensions) {
					handshaker.updateExtensionDecoders(pipeline);
				}
				if (handshaker.isClientMode()) {
					config.switchEncoders(pipeline, hasExtensions);
					if (hasExtensions) {
						handshaker.updateExtensionEncoders(pipeline);
					}
				}
				readyFuture.success();
				handler.event(SessionEvent.READY);
			}
			if (handshaker.isClosing()) {
				throw new InvalidHandshakeException(handshaker.getClosingReason(), null, true);
			}
		}
	}

	@Override
	public void event(SessionEvent event) {
		switch (event) {
		case READY:
			HandshakeFrame frame = handshaker.handshake();

			if (frame != null) {
				getSession().writenf(frame);
			}
			break;
		
		case ENDING:
			getReadyFuture().abort(null);
			
		default:
			handler.event(event);
		}
	}
	
	@Override
	public void event(DataEvent event, long length) {
		handler.event(event, length);
	}
	
	@Override
	public void exception(Throwable t) {
		getReadyFuture().abort(t);
		handler.exception(t);
	}
	
	@Override
	public boolean incident(SessionIncident incident, Throwable t) {
		if (incident == SessionIncident.ENCODING_PIPELINE_FAILURE) {
			if (t instanceof InvalidHandshakeException) {
				fireException(t);
				return true;
			}
		}
		return handler.incident(incident, t);
	}
	
	@Override
	public void timer(Object event) {	
		handler.timer(event);
	}
	
	@Override
	public void timer(Runnable task) {
		handler.timer(task);
	}

	@Override
	public ISessionConfig getConfig() {
		return handler.getConfig();
	}

	@Override
	public ISessionStructureFactory getFactory() {
		return handler.getFactory();
	}
	
	public IWebSocketHandler getHandler() {
		return handler;
	}
	
}
