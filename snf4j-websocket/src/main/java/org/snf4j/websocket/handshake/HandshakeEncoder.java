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
package org.snf4j.websocket.handshake;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.IEventDrivenCodec;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.IWebSocketSession;
import org.snf4j.websocket.IWebSocketSessionConfig;

/**
 * Encodes a Web Socket handshake frame into bytes in the protocol version 13 format.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HandshakeEncoder implements IEncoder<HandshakeFrame,ByteBuffer>, IEventDrivenCodec {

	private final boolean clientMode;
	
	private final HandshakeFactory factory = HandshakeFactory.getDefault();
	
	private ICodecPipeline pipeline;
	
	/**
	 * Constructs a Web Socket handshake encoder.
	 * 
	 * @param clientMode determines the mode (client/server) in which the encoder
	 *                   should work
	 */
	public HandshakeEncoder(boolean clientMode) {
		this.clientMode = clientMode;
	}
	
	@Override
	public Class<HandshakeFrame> getInboundType() {
		return HandshakeFrame.class;
	}

	@Override
	public Class<ByteBuffer> getOutboundType() {
		return ByteBuffer.class;
	}

	@Override
	public void encode(ISession session, HandshakeFrame data, List<ByteBuffer> out) throws Exception {
		if (!clientMode) {
			if (data instanceof HandshakeResponse) {
				if (((HandshakeResponse)data).getStatus() == HttpStatus.SWITCHING_PROTOCOLS.getStatus()) {
					IHandshaker handshaker = ((IWebSocketSession)session).getHandshaker();
					boolean hasExtensions = handshaker.hasExtensions();
					
					
					((IWebSocketSessionConfig) session.getConfig()).switchEncoders(pipeline, hasExtensions);
					if (hasExtensions) {
						handshaker.updateExtensionEncoders(pipeline);
					}
				}
			}
		}
		
		ByteBuffer buffer = session.allocate(data.getLength());
		int tries = 0;
		
		for (;;) {
			try {
				factory.format(data, buffer, clientMode);
				break;
			}
			catch (InvalidHandshakeException e) {
				session.release(buffer);
				throw e;
			}
			catch (BufferOverflowException e) {
				session.release(buffer);
				if (tries++ > 0) {
					throw new InvalidHandshakeException(e);
				}
				buffer = session.allocate(data.getLength()*2);
			}
			catch (Throwable e) {
				session.release(buffer);
				throw new InvalidHandshakeException(e);
			}
		}
		
		buffer.flip();
		out.add(buffer);
	}

	@Override
	public void added(ISession session, ICodecPipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public void event(ISession session, SessionEvent event) {
	}

	@Override
	public void removed(ISession session, ICodecPipeline pipeline) {
		this.pipeline = null;
	}

}
