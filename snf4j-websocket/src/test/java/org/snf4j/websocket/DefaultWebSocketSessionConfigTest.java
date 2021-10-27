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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.websocket.extensions.ExtensionGroup;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;
import org.snf4j.websocket.handshake.HandshakeFrame;
import org.snf4j.websocket.handshake.HandshakeRequest;
import org.snf4j.websocket.handshake.HandshakeResponse;

public class DefaultWebSocketSessionConfigTest {
	
	@Test
	public void testAll() throws Exception {
		DefaultWebSocketSessionConfig config = new DefaultWebSocketSessionConfig();
		URI uri = new URI("http://snf4j.org");
		
		assertFalse(config.isClientMode());
		assertNull(config.getRequestUri());
		for (int i=0; i<2; ++i) {
			assertNull(config.getRequestOrigin());
			assertNull(config.getSupportedSubProtocols());
			assertNull(config.getSupportedExtensions());
			assertFalse(config.ignoreHostHeaderField());
			assertEquals(65536, config.getMaxHandshakeFrameLength());
			assertEquals(65536, config.getMaxFramePayloadLength());
			assertTrue(config.acceptRequestUri(null));
			assertTrue(config.handleCloseFrame());
			config = new DefaultWebSocketSessionConfig(uri);
		}
		assertTrue(config.isClientMode());
		assertTrue(config.getRequestUri() == uri);
		
		Extension e1 = new Extension();
		Extension e2 = new Extension();
		
		config.setRequestOrigin("Origin")
			.setSupportedSubProtocols("proto1", "proto2")
			.setSupportedExtensions(e1,e2)
			.setIgnoreHostHeaderField(true)
			.setMaxHandshakeFrameLength(101)
			.setMaxFramePayloadLength(102)
			.setHandleCloseFrame(false);
		
		assertEquals("Origin", config.getRequestOrigin());
		assertArrayEquals(new String[] {"proto1","proto2"}, config.getSupportedSubProtocols());
		assertArrayEquals(new Extension[] {e1,e2}, config.getSupportedExtensions());
		assertTrue(config.ignoreHostHeaderField());
		assertEquals(101, config.getMaxHandshakeFrameLength());
		assertEquals(102, config.getMaxFramePayloadLength());
		assertFalse(config.handleCloseFrame());
		
		ICodecPipeline pipeline = config.createCodecExecutor().getPipeline();
		List<Object> decoders = pipeline.decoderKeys();
		List<Object> encoders = pipeline.encoderKeys();
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.HANDSHAKE_DECODER}, decoders.toArray());
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.HANDSHAKE_ENCODER}, encoders.toArray());
		
		config.switchDecoders(pipeline, true);
		decoders = pipeline.decoderKeys();
		encoders = pipeline.encoderKeys();
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.WEBSOCKET_DECODER,
				DefaultWebSocketSessionConfig.WEBSOCKET_UTF8_VALIDATOR}, decoders.toArray());
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.HANDSHAKE_ENCODER}, encoders.toArray());
		
		config.switchEncoders(pipeline, true);
		decoders = pipeline.decoderKeys();
		encoders = pipeline.encoderKeys();
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.WEBSOCKET_DECODER,
				DefaultWebSocketSessionConfig.WEBSOCKET_UTF8_VALIDATOR}, decoders.toArray());
		assertArrayEquals(new Object[] {DefaultWebSocketSessionConfig.WEBSOCKET_ENCODER}, encoders.toArray());
		
		HandshakeRequest request = new HandshakeRequest("/");
		Method m = HandshakeFrame.class.getDeclaredMethod("getLength");
		m.setAccessible(true);
		Integer len = (Integer) m.invoke(request);
		config.customizeHeaders(request);
		assertEquals(len, (Integer) m.invoke(request));
		
		HandshakeResponse response = new HandshakeResponse(1000, "");
		len = (Integer) m.invoke(response);
		config.customizeHeaders(response);
		assertEquals(len, (Integer) m.invoke(response));
	}
	
	static class Extension implements IExtension {

		@Override
		public String getName() {
			return null;
		}

		@Override
		public ExtensionGroup getGroup() {
			return ExtensionGroup.COMPRESS;
		}

		@Override
		public IExtension acceptOffer(List<String> extension) throws InvalidExtensionException {
			return null;
		}

		@Override
		public String[] offer() {
			return null;
		}

		@Override
		public String[] response() {
			return null;
		}

		@Override
		public IExtension validateResponse(List<String> extension) throws InvalidExtensionException {
			return null;
		}

		@Override
		public void updateEncoders(ICodecPipeline pipeline) {
		}

		@Override
		public void updateDecoders(ICodecPipeline pipeline) {
		}
		
	}
}
