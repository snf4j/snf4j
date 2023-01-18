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

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.engine.Status;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.tls.alert.AlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;
import org.snf4j.tls.crypto.IAeadDecrypt;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.Decryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.Record;

public class TLSEngine implements IEngine {
	
	private final IHandshakeEngine handshaker;

	private final TLSEngineStateListener listener;
	
	private final IEngineHandler handler;

	private final HandshakeAggregator aggregator; 
	
	private final TLSHandshakeWrapper handshakeWrapper;
	
	private HandshakeStatus status;
	
	private boolean outboundDone;
	
	private boolean inboundDone;
	
	private AlertException alert;
		
	public TLSEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler) {
		listener = new TLSEngineStateListener();
		this.handler = handler;
		handshaker = new HandshakeEngine(
				clientMode, 
				parameters, 
				this.handler,
				listener);
		aggregator = new HandshakeAggregator(handshaker);
		handshakeWrapper = new TLSHandshakeWrapper(handshaker, listener, listener);
		status = clientMode ? HandshakeStatus.NEED_WRAP : HandshakeStatus.NEED_UNWRAP;
	}

	@Override
	public void init() {
	}

	@Override
	public void cleanup() {
	}

	@Override
	public void beginHandshake() throws Exception {
		if (!handshaker.isStarted()) {
			handshaker.start();
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
	}

	@Override
	public void closeInbound() throws SessionIncidentException {
	}

	@Override
	public int getMinApplicationBufferSize() {
		return 0;
	}

	@Override
	public int getMinNetworkBufferSize() {
		return 5 + handshaker.getMaxFragmentLength() + 1 + 255;
	}

	@Override
	public int getMaxApplicationBufferSize() {
		return 0;
	}

	@Override
	public int getMaxNetworkBufferSize() {
		return 0;
	}

	@Override
	public HandshakeStatus getHandshakeStatus() {
		if (status != HandshakeStatus.NOT_HANDSHAKING && handshaker.hasTask()) {
			return HandshakeStatus.NEED_TASK;
		}
		return status;
	}

	@Override
	public Object getSession() {
		return null;
	}

	@Override
	public Runnable getDelegatedTask() {
		return handshaker.getTask();
	}

	IEngineResult wrap(List<ProducedHandshake> handshakes, int length, Encryptor encryptor, ByteBuffer dst) {
		return new EngineResult(Status.OK, HandshakeStatus.NEED_UNWRAP, 0, 0);
	}
	
	IEngineResult wrapAppData(ByteBufferArray src, ByteBuffer dst) throws Exception {
		return new EngineResult(Status.OK, status, 0, 0);
	}

	IEngineResult wrapAlert(ByteBuffer dst, HandshakeStatus status) throws Exception {
		if (handshakeWrapper.needPendingWrap()) {
			handshakeWrapper.clear();

			int produced = handshakeWrapper.wrap(dst);
			if (produced < 0) {
				return new EngineResult(
						Status.BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
			else if (produced > 0) {
				if (handshakeWrapper.needPendingWrap()) {
					return new EngineResult(
							Status.OK, 
							status, 
							0, 
							produced);
				}
			}
		}
		
		Encryptor encryptor = listener.getEncryptor();
		int produced;
		
		if (encryptor == null) {
			if (Record.checkForAlert(dst)) {
				produced = Record.alert(alert, dst);
			}
			else {
				return new EngineResult(
						Status.BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
		}
		else {
			int padding = handshaker.getHandler().calculatePadding(
					ContentType.ALERT, 
					Record.ALERT_CONTENT_LENGTH);

			if (padding > 0) {
				padding = Math.min(padding, handshaker.getMaxFragmentLength()-Record.ALERT_CONTENT_LENGTH);
			}
			if (Record.checkForAlert(dst, padding, encryptor)) {
				produced = Record.alert(alert, padding, encryptor, dst);
			}
			else {
				return new EngineResult(
						Status.BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
		}
		
		outboundDone = true;
		this.status = HandshakeStatus.NOT_HANDSHAKING;
		return new EngineResult(
				Status.CLOSED, 
				getHandshakeStatus(), 
				0, 
				produced);
	}
	
	private IEngineResult wrap(ByteBuffer src, ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
		int produced;
		
		dst.mark();
		try {
			beginHandshake();

			HandshakeStatus status = getHandshakeStatus();
			
			if (outboundDone) {
				return new EngineResult(
						Status.CLOSED, 
						status, 
						0, 
						0);
			}
	
			switch(status) {
			case NEED_WRAP:
				if (alert != null) {
					return wrapAlert(dst, status);
				}
				if (handshaker.hasPendingTasks()) {
					return new EngineResult(
							Status.OK, 
							status, 
							0, 
							0);
				}
				break;

			case NOT_HANDSHAKING:
				if (srcs != null) {
					return wrapAppData(ByteBufferArray.wrap(srcs), dst);
				}
				return wrapAppData(ByteBufferArray.wrap(new ByteBuffer[] {src}), dst);

			default:
				return new EngineResult(
						Status.OK, 
						status, 
						0, 
						0);
			}

			produced = handshakeWrapper.wrap(dst);
			if (produced < 0) {
				return new EngineResult(
						Status.BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
			else if (produced > 0) {
				if (handshakeWrapper.needWrap()) {
					return new EngineResult(
							Status.OK, 
							status, 
							0, 
							produced);
				}
			}
		}
		catch (Exception e) {
			dst.reset();
			if (alert == null) {
				if (e instanceof AlertException) {
					alert = (AlertException) e;
				}
				else {
					alert = new InternalErrorAlertException("General failure", e);
				}
				inboundDone = true;
				if (!outboundDone) {
					this.status = HandshakeStatus.NEED_WRAP;
				}
				else {
					this.status = HandshakeStatus.NOT_HANDSHAKING;
				}
			}
			else {
				outboundDone = true;
				this.status = HandshakeStatus.NOT_HANDSHAKING;
			}
			throw e;
		}
		
		if (handshaker.isConnected()) {
			this.status = HandshakeStatus.NOT_HANDSHAKING;
			return new EngineResult(
					Status.OK, 
					HandshakeStatus.FINISHED, 
					0, 
					produced);
		}
		this.status = HandshakeStatus.NEED_UNWRAP;
		return new EngineResult(
				Status.OK, 
				getHandshakeStatus(), 
				0, 
				produced);
	}
	
	@Override
	public IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
		return wrap(null, srcs, dst);
	}

	@Override
	public IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		return wrap(src, null, dst);
	}

	HandshakeStatus unwrapAppData(ByteBuffer src, byte[] additionalData, int remaining, ByteBuffer dst) throws Exception {
		Decryptor decryptor = listener.getDecryptor();
		byte[] nonce = decryptor.nextNonce();
		IAeadDecrypt aead = decryptor.getAead();
		byte[] plaintext;
		
		plaintext = new byte[remaining - aead.getAead().getTagLength()];
		ByteBuffer out = ByteBuffer.wrap(plaintext);
		ByteBuffer in = src.duplicate();
		
		in.limit(in.position() + remaining);
		aead.decrypt(nonce, additionalData, in, out);
		src.position(in.position());
		ContentType type = null;
		
		for (remaining = plaintext.length-1;remaining>=0; --remaining) {
			if (plaintext[remaining] != 0) {
				type = ContentType.of(plaintext[remaining]);
				break;
			}
		}
		out.flip();
		if (dst == null) {
			if (type.equals(ContentType.HANDSHAKE)) {
				return aggregator.unwrap(out, remaining);
			}
			throw new Exception();
		}
		
		if (type.equals(ContentType.APPLICATION_DATA)) {
			if (dst.remaining() >= out.remaining()) {
				dst.put(out);
				return HandshakeStatus.NOT_HANDSHAKING;
			}
		}
		throw new Exception();
	}
	
	IEngineResult unwrap(ByteBuffer src) throws Exception {
		int remaining = src.remaining();
		
		if (remaining >= 5) {
			int len = src.getShort(3);
			
			remaining -= 5;
			if (len <= remaining) {
				ContentType type = ContentType.of(src.get(0));
				int pos0 = src.position();
				
				if (type.equals(ContentType.HANDSHAKE)) {
					src.position(src.position() + 5);
					status = aggregator.unwrap(src, len);
				}
				else if (type.equals(ContentType.APPLICATION_DATA)) {
					byte[] additionalData = new byte[5];
					
					src.get(additionalData);
					status = unwrapAppData(src, additionalData, len, null);
				}
				else {
					throw new Exception();
				}
				return new EngineResult(Status.OK, getHandshakeStatus(), src.position()-pos0, 0);
			}
		}
		return new EngineResult(Status.BUFFER_UNDERFLOW, getHandshakeStatus(), 0, 0);
	}
	
	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		beginHandshake();
		
		HandshakeStatus status = getHandshakeStatus();
		
		switch(status) {
		case NEED_WRAP:
		case NEED_TASK:
			return new EngineResult(Status.OK, status, 0, 0);
			
		default:
		}
		
		if (!handshaker.isConnected()) {
			
			if (handshaker.hasPendingTasks()) {
				return new EngineResult(Status.OK, status, 0, 0);
			}
			
			this.status = aggregator.unwrapPending();
			
			if (handshaker.needProduce()) {
				this.status = HandshakeStatus.NEED_WRAP;
			}
			else if (handshaker.isConnected()) {
				this.status = HandshakeStatus.NOT_HANDSHAKING;
				return new EngineResult(Status.OK, HandshakeStatus.FINISHED, 0, 0);
			}
			
			status = getHandshakeStatus();
			
			switch(status) {
			case NEED_WRAP:
			case NEED_TASK:
				return new EngineResult(Status.OK, status, 0, 0);
				
			default:
				return unwrap(src);
			}
		}
		return new EngineResult(Status.OK, status, 0, 0);
	}
}
