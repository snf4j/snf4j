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
package org.snf4j.websocket.extensions.compress;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.extensions.GroupIdentifier;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension.NoContext;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.FrameDecoder;
import org.snf4j.websocket.frame.FrameEncoder;
import org.snf4j.websocket.handshake.HandshakeUtilsTest;

public class PerMessageDeflateExtensionTest {

	final static PerMessageDeflateExtension.NoContext FORBIDDEN = PerMessageDeflateExtension.NoContext.FORBIDDEN;
	
	final static PerMessageDeflateExtension.NoContext OPTIONAL = PerMessageDeflateExtension.NoContext.OPTIONAL;
	
	final static PerMessageDeflateExtension.NoContext REQUIRED = PerMessageDeflateExtension.NoContext.REQUIRED;
	
	String format(String ext) {
		ext = ext.replace("EX", "permessage-deflate");
		ext = ext.replace("SN", "server_no_context_takeover");
		ext = ext.replace("CN", "client_no_context_takeover");
		ext = ext.replace("SX", "server_max_window_bits");
		ext = ext.replace("CX", "client_max_window_bits");
		return ext;
	}
	
	String reformat(String ext) {
		ext = ext.replace("permessage-deflate", "EX");
		ext = ext.replace("server_no_context_takeover", "SN");
		ext = ext.replace("client_no_context_takeover", "CN");
		ext = ext.replace("server_max_window_bits", "SX");
		ext = ext.replace("client_max_window_bits", "CX");
		return ext;
	}
	
	void assertIllegalArgument(int compressionLevel, String expected) {
		try {
			new PerMessageDeflateExtension(compressionLevel, OPTIONAL, OPTIONAL);
			fail();
		}
		catch (IllegalArgumentException e) {
			assertEquals(expected, e.getMessage());
		}
	}
	
	void assertConstructor(PerMessageDeflateExtension p, int compressionLevel, int minInflateBound, 
			NoContext compressNoContext, NoContext decompressNoContext) throws Exception {
		Field f = PerMessageDeflateExtension.class.getDeclaredField("compressionLevel");
		f.setAccessible(true);
		assertEquals(compressionLevel, f.getInt(p));
		f = PerMessageDeflateExtension.class.getDeclaredField("minInflateBound");
		f.setAccessible(true);
		assertEquals(minInflateBound, f.getInt(p));
		f = PerMessageDeflateExtension.class.getDeclaredField("compressNoContext");
		f.setAccessible(true);
		assertEquals(compressNoContext, f.get(p));
		f = PerMessageDeflateExtension.class.getDeclaredField("decompressNoContext");
		f.setAccessible(true);
		assertEquals(decompressNoContext, f.get(p));
	}

	void assertConstructor(PerMessageDeflateExtension p, int compressionLevel,
			int minInflateBound, boolean clientMode) throws Exception {
		Field f = PerMessageDeflateExtension.class.getDeclaredField("compressionLevel");
		f.setAccessible(true);
		assertEquals(compressionLevel, f.getInt(p));
		f = PerMessageDeflateExtension.class.getDeclaredField("minInflateBound");
		f.setAccessible(true);
		assertEquals(minInflateBound, f.getInt(p));
		f = PerMessageDeflateExtension.class.getDeclaredField("clientMode");
		f.setAccessible(true);
		assertEquals(clientMode, f.getBoolean(p));
	}
	
	@Test
	public void testConstructor() throws Exception {
		new PerMessageDeflateExtension(0, OPTIONAL, OPTIONAL);
		new PerMessageDeflateExtension(9, OPTIONAL, OPTIONAL);
		assertIllegalArgument(-1, "Invalid compressionLevel: -1 (expected: 0-9)");
		assertIllegalArgument(10, "Invalid compressionLevel: 10 (expected: 0-9)");
		
		PerMessageDeflateExtension p = new PerMessageDeflateExtension();
		assertConstructor(p,6,0,OPTIONAL,OPTIONAL);
		p = new PerMessageDeflateExtension(0);
		assertConstructor(p,0,0,OPTIONAL,OPTIONAL);
		p = new PerMessageDeflateExtension(0, REQUIRED, FORBIDDEN);
		assertConstructor(p,0,0,REQUIRED,FORBIDDEN);
		p = new PerMessageDeflateExtension(1, 1111, REQUIRED, FORBIDDEN);
		assertConstructor(p,1,1111,REQUIRED,FORBIDDEN);
		
		p = new PerMessageDeflateExtension(2, 333, OPTIONAL, OPTIONAL);
		IExtension p2 = p.acceptOffer(HandshakeUtilsTest.parseExtension(format("EX")));
		assertConstructor((PerMessageDeflateExtension) p2, 2, 333, false);
		p = new PerMessageDeflateExtension(1, 444, OPTIONAL, OPTIONAL);
		p2 = p.validateResponse(HandshakeUtilsTest.parseExtension(format("EX")));
		assertConstructor((PerMessageDeflateExtension) p2, 1, 444, true);
	}
	
	void assertOffer(String expected, PerMessageDeflateExtension e) {
		List<String> items = e.offer();
		StringBuilder sb = new StringBuilder();
		
		assertTrue(items.size() > 0);
		sb.append(items.get(0));

		for (int i=1; i<items.size(); i += 2) {
			sb.append(';');
			sb.append(items.get(i));
			if (items.get(i+1) != null) {
				sb.append('=');
				sb.append(items.get(i+1));
			}
		}
		assertEquals(expected, reformat(sb.toString()));
	}
	
	@Test
	public void testOffer() {
		assertOffer("EX", new PerMessageDeflateExtension());	
		assertOffer("EX", new PerMessageDeflateExtension(5, FORBIDDEN, FORBIDDEN));	
		assertOffer("EX", new PerMessageDeflateExtension(5, OPTIONAL, OPTIONAL));	
		assertOffer("EX;CN;SN", new PerMessageDeflateExtension(5, REQUIRED, REQUIRED));	
		assertOffer("EX;CN", new PerMessageDeflateExtension(5, REQUIRED, FORBIDDEN));	
		assertOffer("EX;SN", new PerMessageDeflateExtension(5, FORBIDDEN, REQUIRED));	
	}
	
	void assertValidate(PerMessageDeflateExtension e, String resp, PerMessageDeflateParams expected, String exception) throws Exception {
		List<String> extension = HandshakeUtilsTest.parseExtension(format(resp));
		IExtension valid = null;
		
		try {
			valid = e.validateResponse(extension);
		}
		catch (InvalidExtensionException ex) {
			if (exception == null) {
				throw ex;
			}
			assertEquals(exception, ex.getMessage());
		}
		
		if (expected == null) {
			assertNull(valid);
			return;
		}
		
		Field f = PerMessageDeflateExtension.class.getDeclaredField("params");
		f.setAccessible(true);
		PerMessageDeflateParams params = (PerMessageDeflateParams) f.get(valid);
		assertEquals(expected.isClientNoContext(), params.isClientNoContext());
		assertEquals(expected.isServerNoContext(), params.isServerNoContext());
		assertEquals(expected.getClientMaxWindow(), params.getClientMaxWindow());
		assertEquals(expected.getServerMaxWindow(), params.getServerMaxWindow());
	}
	
	void assertValidate(PerMessageDeflateExtension e, String resp, PerMessageDeflateParams expected) throws Exception {
		assertValidate(e, resp, expected, null);
	}
	
	
	void assertValidateException(PerMessageDeflateExtension e, String resp, String exception) throws Exception {
		assertValidate(e, resp, null, exception);
	}
	
	PerMessageDeflateParams params(boolean serverNoCtx, boolean clientNoCtx) {
		return new PerMessageDeflateParams(serverNoCtx, clientNoCtx, 15, 15);
	}
	
	@Test
	public void testValidateResponse() throws Exception {
		PerMessageDeflateExtension e = new PerMessageDeflateExtension();
		
		assertNull(e.validateResponse(new ArrayList<String>()));
		ArrayList<String> l = new ArrayList<String>();
		l.add("permessage-Deflate");
		assertNull(e.validateResponse(l));
		
		assertValidate(e, "EX", params(false, false));
		assertValidate(e, "EX;CN;SN", params(true, true));
		assertValidate(e, "EX;CN", params(false, true));
		assertValidate(e, "EX;SN", params(true, false));

		e = new PerMessageDeflateExtension(6, FORBIDDEN, FORBIDDEN);
		assertValidate(e, "EX", params(false, false));
		assertValidate(e, "EX;CN;SN", null);
		assertValidate(e, "EX;CN", null);
		assertValidate(e, "EX;SN", null);
		
		e = new PerMessageDeflateExtension(6, OPTIONAL, OPTIONAL);
		assertValidate(e, "EX", params(false, false));
		assertValidate(e, "EX;CN;SN", params(true, true));
		assertValidate(e, "EX;CN", params(false, true));
		assertValidate(e, "EX;SN", params(true, false));
		
		e = new PerMessageDeflateExtension(6, REQUIRED, REQUIRED);
		assertValidate(e, "EX", null);
		assertValidate(e, "EX;CN;SN", params(true, true));
		assertValidate(e, "EX;CN", null);
		assertValidate(e, "EX;SN", null);
		
		e = new PerMessageDeflateExtension(6, FORBIDDEN, REQUIRED);
		assertValidate(e, "EX", null);
		assertValidate(e, "EX;CN;SN", null);
		assertValidate(e, "EX;CN", null);
		assertValidate(e, "EX;SN", params(true, false));
		
		e = new PerMessageDeflateExtension(6, REQUIRED, FORBIDDEN);
		assertValidate(e, "EX", null);
		assertValidate(e, "EX;CN;SN", null);
		assertValidate(e, "EX;CN", params(false, true));
		assertValidate(e, "EX;SN", null);
		
		e = new PerMessageDeflateExtension();
		assertValidateException(e, "EX;SX=16", "Invalid parameter value");
		assertValidate(e, "EX;SX=15", params(false, false));
		assertValidate(e, "EX;SX=8", params(false, false));
		assertValidateException(e, "EX;SX=7", "Invalid parameter value");
		assertValidate(e, "EX;CX=15", params(false, false));
		assertValidate(e, "EX;CX=14", null);
	}
	
	IExtension assertAccept(PerMessageDeflateExtension e, String offer, PerMessageDeflateParams expected, String exception) throws Exception {
		List<String> extension = HandshakeUtilsTest.parseExtension(format(offer));
		IExtension accepted = null;
		
		try {
			accepted = e.acceptOffer(extension);
		}
		catch (InvalidExtensionException ex) {
			if (exception == null) {
				throw ex;
			}
			assertEquals(exception, ex.getMessage());
		}
		
		if (expected == null) {
			assertNull(accepted);
			return accepted;
		}
		
		Field f = PerMessageDeflateExtension.class.getDeclaredField("params");
		f.setAccessible(true);
		PerMessageDeflateParams params = (PerMessageDeflateParams) f.get(accepted);
		assertEquals(expected.isClientNoContext(), params.isClientNoContext());
		assertEquals(expected.isServerNoContext(), params.isServerNoContext());
		assertEquals(expected.getClientMaxWindow(), params.getClientMaxWindow());
		assertEquals(expected.getServerMaxWindow(), params.getServerMaxWindow());
		return accepted;
	}

	IExtension assertAccept(PerMessageDeflateExtension e, String resp, PerMessageDeflateParams expected) throws Exception {
		return assertAccept(e, resp, expected, null);
	}

	IExtension assertAcceptException(PerMessageDeflateExtension e, String resp, String exception) throws Exception {
		return assertAccept(e, resp, null, exception);
	}
	
	void assertResponse(IExtension e, String expected) {
		String s = HandshakeUtilsTest.parseExtension(e.response());
		assertEquals(expected, reformat(s));
	}
	
	@Test
	public void testAcceptOffer() throws Exception {
		PerMessageDeflateExtension e = new PerMessageDeflateExtension();
		IExtension e2;
		
		assertNull(e.acceptOffer(new ArrayList<String>()));
		ArrayList<String> l = new ArrayList<String>();
		l.add("permessage-Deflate");
		assertNull(e.acceptOffer(l));
		
		assertAccept(e, "EX", params(false, false));
		assertAccept(e, "EX;CN;SN", params(true, true));
		assertAccept(e, "EX;CN", params(false, true));
		assertAccept(e, "EX;SN", params(true, false));

		e = new PerMessageDeflateExtension(6, FORBIDDEN, FORBIDDEN);
		assertAccept(e, "EX", params(false, false));
		assertAccept(e, "EX;CN;SN", null);
		assertAccept(e, "EX;CN", null);
		assertAccept(e, "EX;SN", null);
		
		e = new PerMessageDeflateExtension(6, OPTIONAL, OPTIONAL);
		e2 = assertAccept(e, "EX", params(false, false));
		assertResponse(e2, "EX");
		e2 = assertAccept(e, "EX;CN;SN", params(true, true));
		assertResponse(e2, "EX; CN; SN");
		e2 = assertAccept(e, "EX;CN", params(false, true));
		assertResponse(e2, "EX; CN");
		e2 = assertAccept(e, "EX;SN", params(true, false));
		assertResponse(e2, "EX; SN");
		
		e = new PerMessageDeflateExtension(6, REQUIRED, REQUIRED);
		assertAccept(e, "EX", params(true, true));
		assertAccept(e, "EX;CN;SN", params(true, true));
		assertAccept(e, "EX;CN", params(true, true));
		assertAccept(e, "EX;SN", params(true, true));
		
		e = new PerMessageDeflateExtension(6, FORBIDDEN, REQUIRED);
		assertAccept(e, "EX", params(false, true));
		assertAccept(e, "EX;CN;SN", null);
		assertAccept(e, "EX;CN", params(false, true));
		assertAccept(e, "EX;SN", null);
		
		e = new PerMessageDeflateExtension(6, REQUIRED, FORBIDDEN);
		assertAccept(e, "EX", params(true, false));
		assertAccept(e, "EX;CN;SN", null);
		assertAccept(e, "EX;CN", null);
		assertAccept(e, "EX;SN", params(true, false));

		e = new PerMessageDeflateExtension();
		assertAcceptException(e, "EX;SX=16", "Invalid parameter value");
		assertAccept(e, "EX;SX=15", params(false, false));
		assertAccept(e, "EX;SX=8", null);
		assertAcceptException(e, "EX;SX=7", "Invalid parameter value");
		assertAccept(e, "EX;CX=15", params(false, false));
		assertAccept(e, "EX;CX=14", params(false, false));
	}
	
	@Test
	public void testNameAndGroup() {
		assertEquals("permessage-deflate", new PerMessageDeflateExtension().getName());
		assertEquals(GroupIdentifier.COMPRESSION, new PerMessageDeflateExtension().getGroupId());
	}
	
	@Test
	public void testUpdateCodecs() throws Exception {
		Extension e = new Extension(6, 0, null, true);
		DefaultCodecExecutor codec = new DefaultCodecExecutor();
		ICodecPipeline pipeline = codec.getPipeline();
		
		pipeline.add(IWebSocketSessionConfig.WEBSOCKET_DECODER, new FrameDecoder(true, true, 1024));
		pipeline.add(IWebSocketSessionConfig.WEBSOCKET_ENCODER, new FrameEncoder(true));
		
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_DECODER}, pipeline.decoderKeys().toArray());
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_ENCODER}, pipeline.encoderKeys().toArray());
		e.updateDecoders(pipeline);
		e.updateEncoders(pipeline);
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_DECODER}, pipeline.decoderKeys().toArray());
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_ENCODER}, pipeline.encoderKeys().toArray());
		
		Field eNC = DeflateEncoder.class.getDeclaredField("noContext");
		eNC.setAccessible(true);
		Field eCL = DeflateEncoder.class.getDeclaredField("compressionLevel");
		eCL.setAccessible(true);
		Field dNC = DeflateDecoder.class.getDeclaredField("noContext");
		dNC.setAccessible(true);
		Field dIB = DeflateDecoder.class.getDeclaredField("minInflateBound");
		dIB.setAccessible(true);
		boolean b;
		int i;
		
		e = new Extension(5, 0, params(true,false), true);
		e.updateDecoders(pipeline);
		e.updateEncoders(pipeline);
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_DECODER,
				PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER}, pipeline.decoderKeys().toArray());
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_ENCODER,
				PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER}, pipeline.encoderKeys().toArray());
		b = eNC.getBoolean(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER));
		i = eCL.getInt(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER));
		assertFalse(b);
		assertEquals(5,i);
		b = dNC.getBoolean(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER));
		i = dIB.getInt(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER));
		assertTrue(b);
		assertEquals(0,i);
		
		e = new Extension(6, 4000, params(true,false), false);
		pipeline.remove(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER);
		pipeline.remove(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER);
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_DECODER}, pipeline.decoderKeys().toArray());
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_ENCODER}, pipeline.encoderKeys().toArray());
		e.updateDecoders(pipeline);
		e.updateEncoders(pipeline);
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_DECODER,
				PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER}, pipeline.decoderKeys().toArray());
		assertArrayEquals(new String[] {IWebSocketSessionConfig.WEBSOCKET_ENCODER,
				PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER}, pipeline.encoderKeys().toArray());
		b = eNC.getBoolean(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER));
		i = eCL.getInt(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER));
		assertTrue(b);
		assertEquals(6,i);
		b = dNC.getBoolean(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER));
		i = dIB.getInt(pipeline.get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER));
		assertFalse(b);
		assertEquals(4000,i);
	}
	
	class Extension extends PerMessageDeflateExtension {
		
		StringBuilder trace = new StringBuilder();
		
		String getTrace() {
			String s = trace.toString();
			trace.setLength(0);
			return s;
		}
		
		Extension(int compressionLevel, int minInflateBound, PerMessageDeflateParams params, boolean clientMode) {
			super(compressionLevel, minInflateBound, params, clientMode);
		}
		
		@Override
		protected void updatePipeline(ICodecPipeline pipeline, IEncoder<Frame,Frame> encoder) {
			super.updatePipeline(pipeline, encoder);
			trace.append("ENC|");
		}	
		
		@Override
		protected void updatePipeline(ICodecPipeline pipeline, IDecoder<Frame,Frame> decoder) {
			super.updatePipeline(pipeline, decoder);
			trace.append("DEC|");
		}		
	}
}
