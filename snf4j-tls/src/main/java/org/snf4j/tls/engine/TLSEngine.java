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

import static org.snf4j.core.engine.HandshakeStatus.FINISHED;
import static org.snf4j.core.engine.HandshakeStatus.NEED_TASK;
import static org.snf4j.core.engine.HandshakeStatus.NEED_UNWRAP;
import static org.snf4j.core.engine.HandshakeStatus.NEED_WRAP;
import static org.snf4j.core.engine.HandshakeStatus.NOT_HANDSHAKING;
import static org.snf4j.core.engine.Status.BUFFER_OVERFLOW;
import static org.snf4j.core.engine.Status.BUFFER_UNDERFLOW;
import static org.snf4j.core.engine.Status.CLOSED;
import static org.snf4j.core.engine.Status.OK;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.core.engine.EngineResult;
import org.snf4j.core.engine.HandshakeStatus;
import org.snf4j.core.engine.IEngine;
import org.snf4j.core.engine.IEngineResult;
import org.snf4j.core.handler.SessionIncidentException;
import org.snf4j.tls.alert.AlertDescription;
import org.snf4j.tls.alert.AlertLevel;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.alert.CloseNotifyAlert;
import org.snf4j.tls.alert.DecodeErrorAlert;
import org.snf4j.tls.alert.InternalErrorAlert;
import org.snf4j.tls.alert.RecordOverflowAlert;
import org.snf4j.tls.alert.UnexpectedMessageAlert;
import org.snf4j.tls.record.ContentType;
import org.snf4j.tls.record.Decryptor;
import org.snf4j.tls.record.Encryptor;
import org.snf4j.tls.record.Record;

public class TLSEngine implements IEngine {
	
	private final IHandshakeEngine handshaker;

	private final TLSEngineStateListener listener;
	
	private final IEngineHandler handler;

	private final HandshakeAggregator aggregator; 
	
	private final TLSHandshakeFragmenter fragmenter;
	
	private HandshakeStatus status = NOT_HANDSHAKING;
	
	private boolean outboundDone;
	
	private boolean inboundDone;
	
	private Alert alert;
		
	public TLSEngine(boolean clientMode, IEngineParameters parameters, IEngineHandler handler) {
		listener = new TLSEngineStateListener();
		this.handler = handler;
		handshaker = new HandshakeEngine(
				clientMode, 
				parameters, 
				this.handler,
				listener);
		aggregator = new HandshakeAggregator(handshaker);
		fragmenter = new TLSHandshakeFragmenter(handshaker, listener, listener);
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
			status = handshaker.isClientMode() ? NEED_WRAP : NEED_UNWRAP;
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
		if (alert == null) {
			alert = new CloseNotifyAlert("Closed");
			status = NEED_WRAP;
		}
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
		return Record.HEADER_LENGTH 
				+ handshaker.getMaxFragmentLength() 
				+ 1 
				+ 255;
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
		if (status != NOT_HANDSHAKING) {
			if (handshaker.hasTask()) {
				return NEED_TASK;
			}
		}
		return status;
	}

	@Override
	public Object getSession() {
		return handshaker.getSession();
	}

	@Override
	public Runnable getDelegatedTask() {
		return handshaker.getTask();
	}

	private int[] consumedAndPadding(int srcRemaining, int dstRemaining, Encryptor encryptor) {
		int maxFragmentLen = handshaker.getMaxFragmentLength();
		int consumed = (int) Math.min(
				srcRemaining, 
				dstRemaining - Record.HEADER_LENGTH - 1 - encryptor.getExapnsion());
		int padding;
		
		if (consumed > maxFragmentLen) {
			consumed = maxFragmentLen;
			padding = 0;
		}
		else {
			padding = handshaker.getHandler().calculatePadding(ContentType.APPLICATION_DATA, consumed);
			if (padding > 0 ) {
				if (padding > 0) {
					padding = Math.min(padding, maxFragmentLen-consumed);
				}
			}
		}
		return new int[] {consumed, padding};
	}

	private IEngineResult wrapAppData(ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
		if (srcs.length == 1) {
			return wrapAppData(srcs[0], dst);
		}
		
		Encryptor encryptor = listener.getEncryptor();
		ByteBufferArray srcArray = ByteBufferArray.wrap(srcs);
		
		int[] consPad = consumedAndPadding((int) srcArray.remaining(), dst.remaining(), encryptor);
		
		byte[] tail = new byte[1 + consPad[1]];
		ByteBufferArray dup = srcArray.duplicate();
		int consumed = consPad[0];
		int produced;
		
		tail[0] = (byte) ContentType.APPLICATION_DATA.value();

		ByteBuffer[] dupSrcs = dup.array();
		
		int remaining = consumed;
		ByteBuffer src;
		for (int i=0; i<srcs.length; ++i) {
			src = srcs[i];
			if (src.remaining() < remaining) {
				remaining -= src.remaining();
			}
			else {
				src.limit(src.position()+remaining);
				dupSrcs = Arrays.copyOf(dupSrcs, i + 2);
				break;
			}
		}
		dupSrcs[dupSrcs.length-1] = ByteBuffer.wrap(tail);
		
		produced = Record.protect(
				dupSrcs, 
				consumed + tail.length,
				encryptor,
				dst);
		
		for (int i=0; i<dupSrcs.length-1; ++i) {
			srcs[i].position(dupSrcs[i].position());
		}
		return new EngineResult(OK, status, consumed, produced);
	}

	private IEngineResult wrapAppData(ByteBuffer src, ByteBuffer dst) throws Exception {
		Encryptor encryptor = listener.getEncryptor();
		int maxFragmentLen = handshaker.getMaxFragmentLength();
		int consumed = (int) Math.min(
				src.remaining(), 
				dst.remaining() - Record.HEADER_LENGTH - 1 - encryptor.getExapnsion());
		int padding;
		
		if (consumed > maxFragmentLen) {
			consumed = maxFragmentLen;
			padding = 0;
		}
		else {
			padding = handshaker.getHandler().calculatePadding(ContentType.APPLICATION_DATA, consumed);
			if (padding > 0 ) {
				if (padding > 0) {
					padding = Math.min(padding, maxFragmentLen-consumed);
				}
			}
		}

		byte[] tail = new byte[1 + padding];
		ByteBuffer dup = src.duplicate();
		int produced;
		
		tail[0] = (byte) ContentType.APPLICATION_DATA.value();
		dup.limit(dup.position() + consumed);
		produced = Record.protect(
				new ByteBuffer[] {dup, ByteBuffer.wrap(tail)}, 
				consumed + tail.length,
				encryptor,
				dst);
		src.position(dup.position());
		return new EngineResult(OK, status, consumed, produced);
	}

	private IEngineResult wrapAlert(ByteBuffer dst, HandshakeStatus status) throws Exception {
		if (fragmenter.isPending()) {
			fragmenter.clear();

			int produced = fragmenter.wrap(dst);
			if (produced < 0) {
				return new EngineResult(
						BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
			else if (produced > 0) {
				if (fragmenter.isPending()) {
					return new EngineResult(
							OK, 
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
						BUFFER_OVERFLOW, 
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
						BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
		}
		
		outboundDone = true;
		
		if (inboundDone) {
			this.status = NOT_HANDSHAKING;
		}
		else if (alert.isClosure()) {
			this.status = NEED_UNWRAP;
		}
		else {
			this.status = NOT_HANDSHAKING;
			inboundDone = true;
		}
		return new EngineResult(
				CLOSED, 
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
						CLOSED, 
						status, 
						0, 
						0);
			}
	
			switch(status) {
			case NEED_WRAP:
				if (alert != null) {
					return wrapAlert(dst, status);
				}
				if (handshaker.updateTasks()) {
					return new EngineResult(
							OK, 
							status, 
							0, 
							0);
				}
				break;

			case NOT_HANDSHAKING:
				if (srcs != null) {
					return wrapAppData(srcs, dst);
				}
				return wrapAppData(src, dst);

			default:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);
			}

			produced = fragmenter.wrap(dst);
			if (produced < 0) {
				return new EngineResult(
						BUFFER_OVERFLOW, 
						status, 
						0, 
						0);
			}
			else if (produced > 0) {
				if (fragmenter.needWrap()) {
					return new EngineResult(
							OK, 
							status, 
							0, 
							produced);
				}
			}
		}
		catch (Exception e) {
			dst.reset();
			if (alert == null) {
				if (e instanceof Alert) {
					alert = (Alert) e;
				}
				else {
					alert = new InternalErrorAlert("General failure", e);
				}
				inboundDone = true;
				if (outboundDone) {
					this.status = NOT_HANDSHAKING;
				}
				else {
					this.status = NEED_WRAP;
				}
			}
			else {
				outboundDone = true;
				this.status = NOT_HANDSHAKING;
			}
			throw e;
		}
		
		if (handshaker.isConnected()) {
			this.status = NOT_HANDSHAKING;
			return new EngineResult(
					OK, 
					FINISHED, 
					0, 
					produced);
		}
		this.status = NEED_UNWRAP;
		return new EngineResult(
				OK, 
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

	private IEngineResult unwrapAppData(ByteBuffer src, int length) throws Alert {
		Decryptor decryptor = listener.getDecryptor();
		byte[] plaintext = new byte[length - decryptor.getExapnsion()];		
		
		int remaining = Record.unprotect(
				src, 
				length, 
				decryptor, 
				ByteBuffer.wrap(plaintext)) - 1;

		if (remaining > handshaker.getMaxFragmentLength()) {
			throw new RecordOverflowAlert("Encrypted record is too big");
		}
		
		int type = 0;
		
		for (;remaining >= 0; --remaining) {
			if (plaintext[remaining] != 0) {
				type = plaintext[remaining];
				break;
			}
		}

		if (type == 0) {
			throw new UnexpectedMessageAlert("Non-zero octet in cleartext");
		}
		
		if (type == ContentType.HANDSHAKE.value()) {
			return unwrapHandshake(
					ByteBuffer.wrap(plaintext, 0, remaining), 
					0, 
					remaining, 
					length + Record.HEADER_LENGTH);
		}
		if (type == ContentType.ALERT.value()) {
			return unwrapAlert(
					ByteBuffer.wrap(plaintext, 0, remaining), 
					0, 
					remaining, 
					length + Record.HEADER_LENGTH);
		}
		throw new UnexpectedMessageAlert("Received unexpected record content type (" + type + ")");
	}

	private IEngineResult unwrapAppData(ByteBuffer src, ByteBuffer dst) throws Alert {
		int remaining = src.remaining();
		
		if (remaining >= Record.HEADER_LENGTH) {
			int type = src.get(src.position());
			
			if (type != ContentType.APPLICATION_DATA.value()) {
				throw new UnexpectedMessageAlert("Unexpected encrypted record content type (" + type + ")");
			}
			
			int length = src.getShort(src.position() + 3);
		
			if (length > handshaker.getMaxFragmentLength() + 256) {
				throw new RecordOverflowAlert("Encrypted record is too big");
			}
			
			if (length <= remaining) {
				Decryptor decryptor = listener.getDecryptor();
				
				if (dst.remaining() < length - decryptor.getExapnsion()) {
					return new EngineResult(
							BUFFER_OVERFLOW, 
							getHandshakeStatus(), 
							0, 
							0);					
				}
				
				int produced = Record.unprotect(
						src, 
						length, 
						decryptor, 
						dst) - 1;
				
				if (produced > handshaker.getMaxFragmentLength()) {
					throw new RecordOverflowAlert("Encrypted record is too big");
				}

				int padding = 0;
				
				type = 0;
				if (dst.hasArray()) {
					byte[] array = dst.array();
					int i = dst.arrayOffset() + dst.position() - 1;
					int j = i - produced;
					
					for (; i >= j; --i) {
						byte b = array[i];
						
						if (b != 0) {
							type = b;
							break;
						}
						++padding;
					}					
				}
				else {
					int i = dst.position() - 1;
					int j = i - produced;
					
					for (; i >= j; --i) {
						byte b = dst.get(i);
						
						if (b != 0) {
							type = b;
							break;
						}
						++padding;
					}
				}
				
				if (type == 0) {
					throw new UnexpectedMessageAlert("Non-zero octet in cleartext");
				}
				
				dst.position(dst.position() - padding - 1);
				produced -= padding;
				
				if (type == ContentType.APPLICATION_DATA.value()) {
					return new EngineResult(
							OK,
							NOT_HANDSHAKING,
							length + Record.HEADER_LENGTH,
							produced);
				}
				if (type == ContentType.HANDSHAKE.value()) {
					dst.position(dst.position()-produced);
					byte[] data = new byte[produced];
					ByteBuffer dup = dst.duplicate();
					
					dup.get(data);
					return unwrapHandshake(
							ByteBuffer.wrap(data),
							0,
							produced,
							length + Record.HEADER_LENGTH);
				}
				if (type == ContentType.ALERT.value()) {
					dst.position(dst.position()-produced);
					byte[] data = new byte[produced];
					ByteBuffer dup = dst.duplicate();
					
					dup.get(data);
					return unwrapAlert(
							ByteBuffer.wrap(data), 
							0, 
							produced, 
							length + Record.HEADER_LENGTH);
				}
				throw new UnexpectedMessageAlert("Received unexpected record content type (" + type + ")");
				
			}
		}
		return new EngineResult(
				BUFFER_UNDERFLOW, 
				getHandshakeStatus(), 
				0, 
				0);		
	}
	
	private IEngineResult unwrapHandshake(ByteBuffer src, int off, int length, int consumed) throws Alert {
		boolean connected = handshaker.isConnected();
		
		src.position(src.position() + off);
		if (aggregator.unwrap(src, length)) {
			if (!connected && handshaker.isConnected()) {
				if (aggregator.isEmpty()) {
					this.status = NOT_HANDSHAKING;
					return new EngineResult(
							OK, 
							FINISHED, 
							consumed, 
							0);
				}
				else {
					throw new UnexpectedMessageAlert("Received unexpected data after finished handshake");
				}
			}
		}
		else {
			this.status = NEED_WRAP;
		}
		
		return new EngineResult(
				OK,
				this.getHandshakeStatus(),
				consumed,
				0);
	}

	private IEngineResult unwrapAlert(ByteBuffer src, int off, int length, int consumed) throws Alert {
		if (length != 2) {
			throw new DecodeErrorAlert("Invalid length of alert content");
		}
		src.position(src.position() + off);
		AlertLevel level = AlertLevel.of(src.get());
		AlertDescription desc = AlertDescription.of(src.get());
		if (desc.equals(AlertDescription.CLOSE_NOTIFY)) {
			alert = new CloseNotifyAlert("Closing by peer");
			inboundDone = true;
			if (outboundDone) {
				status = NOT_HANDSHAKING;
			}
			else {
				status = NEED_WRAP;
			}
			return new EngineResult(
					CLOSED, 
					getHandshakeStatus(), 
					consumed, 
					0);
		}
		alert = Alert.of(level, desc);
		inboundDone = true;
		outboundDone = true;
		status = NOT_HANDSHAKING;
		throw alert;
	}

	private IEngineResult unwrapChangeCipherSpec(ByteBuffer src, int off, int length, int consumed) throws Alert {
		if (length == 1) {
			src.position(src.position() + off);
			if (src.get() == 1) {
				return new EngineResult(
						OK, 
						getHandshakeStatus(), 
						consumed, 
						0);
			}
		}
		throw new UnexpectedMessageAlert("Invalid change_cipher_spec message");
	}
	
	private IEngineResult unwrap(ByteBuffer src) throws Alert {
		int remaining = src.remaining();
		
		if (remaining >= Record.HEADER_LENGTH) {
			int len = src.getShort(src.position() + 3);
			
			if (len <= remaining) {
				int type = ContentType.of(src.get(src.position())).value();
				
				if (type == ContentType.APPLICATION_DATA.value()) {
					if (len > handshaker.getMaxFragmentLength() + 256) {
						throw new RecordOverflowAlert("Encrypted record is too big");
					}
					return unwrapAppData(src, len);
				}
				if (len > handshaker.getMaxFragmentLength()) {
					throw new RecordOverflowAlert("Record fragment is too big");
				}
				if (type == ContentType.HANDSHAKE.value()) {
					return unwrapHandshake(
							src, 
							Record.HEADER_LENGTH, 
							len, 
							Record.HEADER_LENGTH + len);
				}
				if (type == ContentType.ALERT.value()) {
					return unwrapAlert(
							src, 
							Record.HEADER_LENGTH, 
							len, 
							Record.HEADER_LENGTH + len);
				}
				if (type == ContentType.CHANGE_CIPHER_SPEC.value()) {
					return unwrapChangeCipherSpec(
							src,
							Record.HEADER_LENGTH,
							len,
							Record.HEADER_LENGTH + len);
				}
				throw new UnexpectedMessageAlert("Received unexpected record content type (" + type + ")");
			}
		}
		return new EngineResult(
				BUFFER_UNDERFLOW, 
				getHandshakeStatus(), 
				0, 
				0);
	}	
	
	@Override
	public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
		dst.mark();
		try {
			beginHandshake();

			HandshakeStatus status = getHandshakeStatus();

			if (inboundDone) {
				return new EngineResult(
						CLOSED, 
						status, 
						0, 
						0);
			}

			switch(status) {
			case NEED_WRAP:
			case NEED_TASK:
				return new EngineResult(
						OK, 
						status, 
						0, 
						0);

			case NOT_HANDSHAKING:
				return unwrapAppData(src, dst);

			default:
				if (handshaker.updateTasks()) {
					return new EngineResult(
							OK, 
							status, 
							0, 
							0);
				}
			}

			if (aggregator.isPending()) {
				if (aggregator.unwrapPending()) {
					if (handshaker.isConnected()) {
						if (aggregator.isEmpty()) {
							this.status = NOT_HANDSHAKING;
							return new EngineResult(
									OK, 
									FINISHED, 
									0, 
									0);
						}
						else {
							throw new UnexpectedMessageAlert("Received unexpected data after finished handshake");
						}
					}
				}
				else {
					this.status = NEED_WRAP;
				}

				status = getHandshakeStatus();

				switch(status) {
				case NEED_WRAP:
				case NEED_TASK:
					return new EngineResult(
							OK, 
							status, 
							0, 
							0);

				default:
				}
			}
			return unwrap(src);
		}
		catch (Exception e) {
			dst.reset();
			if (alert == null) {
				if (e instanceof Alert) {
					alert = (Alert) e;
				}
				else {
					alert = new InternalErrorAlert("General failure", e);
				}
				inboundDone = true;
				this.status = NEED_WRAP;
			}
			throw e;
		}
	}
	
}
