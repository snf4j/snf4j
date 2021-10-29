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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.SSLEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf4j.core.HttpProxy;
import org.snf4j.core.SSLSession;
import org.snf4j.core.Server;
import org.snf4j.core.StreamSession;
import org.snf4j.core.TestHandler;
import org.snf4j.core.allocator.CachingAllocator;
import org.snf4j.core.allocator.DefaultAllocator;
import org.snf4j.core.allocator.DefaultAllocatorMetric;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.future.IFuture;
import org.snf4j.core.proxy.HttpProxyHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.session.SSLEngineCreateException;
import org.snf4j.core.timer.TestTimer;
import org.snf4j.websocket.frame.Frame;
import org.snf4j.websocket.frame.FrameAggregator;
import org.snf4j.websocket.frame.PingFrame;
import org.snf4j.websocket.frame.PongFrame;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.TestExtension;
import org.snf4j.websocket.extensions.compress.DeflateCodecTest;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension;
import org.snf4j.websocket.extensions.compress.PerMessageDeflateExtension.NoContext;
import org.snf4j.websocket.frame.BinaryFrame;
import org.snf4j.websocket.frame.CloseFrame;
import org.snf4j.websocket.frame.ContinuationFrame;
import org.snf4j.websocket.frame.TextFrame;
import org.snf4j.websocket.handshake.HandshakeTest;
import org.snf4j.websocket.handshake.IHandshaker;

public class WebSocketSessionTest extends HandshakeTest {
	
	long TIMEOUT = 2000;
	
	int PORT = 7777;
	
	boolean serverHandleClose;
	
	boolean clientHandleClose;
	
	boolean traceCloseDetails;
	
	boolean clientWaitForCloseMessage;
	
	boolean serverWaitForCloseMessage;
	
	WSServer s;
	
	WSClient c;
	
	HttpProxy p;
	
	@Before
	public void before() {
		s = c = null;
		p = null;
		serverHandleClose = clientHandleClose = false;
		traceCloseDetails = false;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
		if (p != null) p.stop(TIMEOUT);
	}
	
	void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	public void testSuccessfulHandshake(boolean ssl) throws Exception {
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		
		s.start();
		c.start();
		c.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
		c.session.close();
		c.session.getEndFuture().sync(TIMEOUT);
		s.session.getEndFuture().sync(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		s.session.getOpenFuture().sync(TIMEOUT);
		waitFor(50);
		assertFalse(s.session.getReadyFuture().isDone());
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|", s.getTrace());
		
		byte[] b = bytes("GET /uri HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 13|"
				+"|");
		
		c.session.write(b);
		s.session.getReadyFuture().sync(TIMEOUT);
		c.waitForMsgRead(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals("RE|", s.getTrace());
		assertEquals("RES(101)|", c.getTrace());
		assertEquals("Switching Protocols", c.response.getReason());
		assertFields(c.response, "Upgrade:websocket;Connection:Upgrade;Sec-WebSocket-Accept:s3pPLMBiTxaQ9kYGzzhZRbK+xOo=;");
		c.stop(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		
		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		s.session.getOpenFuture().sync(TIMEOUT);
		b = bytes("GET /uri HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 12|"
				+"Sec-WebSocket-Version: 11, 13|"
				+"Sec-WebSocket-Version: 14|"
				+"|");
		c.session.write(b);
		c.waitForMsgRead(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals(101, c.response.getStatus());
		assertFields(c.response, "Upgrade:websocket;Connection:Upgrade;Sec-WebSocket-Accept:s3pPLMBiTxaQ9kYGzzhZRbK+xOo=;");
		c.stop(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
	
		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		s.session.getOpenFuture().sync(TIMEOUT);
		b = bytes("GET /uri HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 12,|"
				+" 11, 13|"
				+"|");
		c.session.write(b);
		c.waitForMsgRead(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals(101, c.response.getStatus());
		assertFields(c.response, "Upgrade:websocket;Connection:Upgrade;Sec-WebSocket-Accept:s3pPLMBiTxaQ9kYGzzhZRbK+xOo=;");
		c.stop(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
	}

	@Test
	public void testSuccessfulHandshakeSsl() throws Exception {
		testSuccessfulHandshake(true);
	}
	
	@Test
	public void testSuccessfulHandshake() throws Exception {
		testSuccessfulHandshake(false);
	}
	
	public void testFailingHandshake(boolean ssl) throws Exception {
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		s.start();

		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		s.session.getOpenFuture().sync(TIMEOUT);
		
		byte[] b = bytes("GET /uri HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 12, 14|"
				+"|");
		c.session.write(b);
		IFuture<Void> f = s.session.getReadyFuture().await(TIMEOUT);
		assertTrue(f.isFailed());
		assertEquals("Unsupported websocket version: 12, 14", f.cause().getMessage());
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals(426, c.response.getStatus());
		assertFields(c.response, "Sec-WebSocket-Version:13;");
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		assertEquals("CR|OP|RE|RES(426)|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		b = bytes("POST /uri HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 13|"
				+"|");
		c.session.write(b);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		f = s.session.getReadyFuture();
		assertTrue(f.isFailed());
		assertEquals("Forbidden http request command", f.cause().getMessage());
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		assertEquals("CR|OP|RE|RES(403)|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		b = bytes("GET /uri HTTP/1.2|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 13|"
				+"|");
		c.session.write(b);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		f = s.session.getReadyFuture();
		assertTrue(f.isFailed());
		assertEquals("Invalid http request version", f.cause().getMessage());
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		assertEquals("CR|OP|RE|RES(400)|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);

		c = new WSClient(PORT, ssl);
		c.onlyTcp = true;
		c.start();
		c.waitForOpen(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		b = bytes("GET HTTP/1.1|"
				+"Host: host|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==|"
				+"Sec-WebSocket-Version: 13|"
				+"|");
		c.session.write(b);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		f = s.session.getReadyFuture();
		assertTrue(f.isFailed());
		assertEquals("Invalid http request", f.cause().getMessage());
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		assertEquals("CR|OP|RE|RES(400)|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		s.onlyTcp = true;
		s.start();
		c.start();
		s.waitForMsgRead(TIMEOUT);
		c.waitForOpen(TIMEOUT);
		b = bytes("HTTP/1.1 101 Switching Protocols|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"|");
		s.session.write(b);
		f = c.session.getReadyFuture().await(TIMEOUT);
		assertTrue(f.isFailed());
		assertEquals("Missing websocket key challenge", f.cause().getMessage());
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|RE|REQ(/)|CL|EN|", s.getTrace());
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);

		c = new WSClient(PORT, ssl);
		c.start();
		s.waitForMsgRead(TIMEOUT);
		c.waitForOpen(TIMEOUT);
		b = bytes("HTTP/1.2 101 Switching Protocols|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"|");
		s.session.write(b);
		f = c.session.getReadyFuture().await(TIMEOUT);
		assertTrue(f.isFailed());
		assertEquals("Invalid http response version", f.cause().getMessage());
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		if (ssl) {
			assertEquals("CR|OP|RE|REQ(/)|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", s.getTrace());
		}
		else {
			assertEquals("CR|OP|RE|REQ(/)|CL|EN|", s.getTrace());
		}
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);

		c = new WSClient(PORT, ssl);
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForMsgRead(TIMEOUT);
		b = bytes("HTTP/1.1 1010 Switching Protocols|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"|");
		s.session.write(b);
		f = c.session.getReadyFuture().await(TIMEOUT);
		assertTrue(f.isFailed());
		assertEquals("Invalid http response status", f.cause().getMessage());
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		if (ssl) {
			assertEquals("CR|OP|RE|REQ(/)|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", s.getTrace());
		}
		else {
			assertEquals("CR|OP|RE|REQ(/)|CL|EN|", s.getTrace());
		}
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);

		c = new WSClient(PORT, ssl);
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForMsgRead(TIMEOUT);
		b = bytes("HTTP/1.2 101|"
				+"Upgrade: websocket|"
				+"Connection: Upgrade|"
				+"|");
		s.session.write(b);
		f = c.session.getReadyFuture().await(TIMEOUT);
		assertTrue(f.isFailed());
		assertEquals("Invalid http response", f.cause().getMessage());
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		if (ssl) {
			assertEquals("CR|OP|RE|REQ(/)|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", s.getTrace());
		}
		else {
			assertEquals("CR|OP|RE|REQ(/)|CL|EN|", s.getTrace());
		}
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);
		
	}

	public void testInvalidHandshakeCodecs(boolean ssl) throws Exception {
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		c.changeHsEncoderMode = true;
		s.start();
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		assertEquals("CR|OP|CL|EN|", s.getTrace().replace("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", "CL|EN|"));
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.changeHsEncoderMode = true;
		c.throwInException = new RuntimeException();
		assertEquals(0, c.throwInExceptionCount.get());
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|EX|CL|EN|", c.getTrace());
		assertEquals("CR|OP|CL|EN|", s.getTrace().replace("SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", "CL|EN|"));
		assertEquals(1, c.throwInExceptionCount.get());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.changeHsDecoderMode = true;
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|EX|EX|CL|EN|", c.getTrace());
		assertEquals("CR|OP|RE|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		s.changeHsEncoderMode = true;
		s.start();
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|CL|EN|", c.getTrace());
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		s.changeHsDecoderMode = true;
		s.start();
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		if (ssl) {
			assertEquals("CR|OP|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", c.getTrace());
		}
		else {
			assertEquals("CR|OP|CL|EN|", c.getTrace());
		}
		assertEquals("CR|OP|EX|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	@Test
	public void testInvalidHandshakeCodecsSsl() throws Exception {
		testInvalidHandshakeCodecs(true);
	}
	
	@Test
	public void testInvalidHandshakeCodecs() throws Exception {
		testInvalidHandshakeCodecs(false);
	}
	
	@Test
	public void testFailingHandshakeSsl() throws Exception {
		testFailingHandshake(true);
	}

	@Test
	public void testFailingHandshake() throws Exception {
		testFailingHandshake(false);
	}
	
	void startClientServer(boolean ssl) throws Exception {
		startClientServer(ssl, null, null);
	}
	
	void startClientServer(boolean ssl, IExtension[] clientExt, IExtension[] serverExt) throws Exception {
		s = new WSServer(PORT, ssl);
		s.extensions = serverExt;
		s.handleCloseFrame = serverHandleClose;
		s.traceCloseDetails = traceCloseDetails;
		s.waitForCloseMessage = serverWaitForCloseMessage;
		c = new WSClient(PORT, ssl);
		c.extensions = clientExt;
		c.handleCloseFrame = clientHandleClose;
		c.traceCloseDetails = traceCloseDetails;
		c.waitForCloseMessage = clientWaitForCloseMessage;
		s.start();
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		s.session.getReadyFuture().sync(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
	}
	
	public void testWrite(boolean ssl) throws Exception {
		startClientServer(ssl);
		
		c.session.write(new BinaryFrame("ABC".getBytes()));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(BINARY)|", s.getTrace());
		c.session.write(new TextFrame("ABC"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		c.session.write(new TextFrame(false, 0, "ABC"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		assertFalse(((Frame)s.msgRead).isFinalFragment());
		c.session.write(new ContinuationFrame(false, 0, "ABC".getBytes()));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(CONTINUATION)|", s.getTrace());
		c.getTrace();
		c.session.write(new PingFrame(0, "".getBytes()));
		s.waitForMsgRead(TIMEOUT);
		c.waitForMsgRead(TIMEOUT);
		assertEquals("F(PING)|", s.getTrace());
		assertEquals("F(PONG)|", c.getTrace());
		c.session.write(new CloseFrame(0, "".getBytes()));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(CLOSE)|", s.getTrace());
		c.session.write(new CloseFrame(0, "".getBytes()));
		waitFor(100);
		assertEquals("", s.getTrace());
		
		s.session.write(new byte[] {(byte)0x82, (byte)0x0a, (byte)'A'});
		s.session.write("BCDEFGHI".getBytes());
		waitFor(100);
		assertEquals("", c.getTrace());
		s.session.write("J".getBytes());
		c.waitForMsgRead(TIMEOUT);
		assertEquals("F(BINARY)|", c.getTrace());
		assertEquals("ABCDEFGHIJ", new String(((Frame)c.msgRead).getPayload()));
	}

	@Test
	public void testWriteSsl() throws Exception {
		testWrite(true);
	}	

	@Test
	public void testWrite() throws Exception {
		testWrite(false);
	}	
	
	public void testReadInvalidFrame(boolean ssl) throws Exception {
		startClientServer(ssl);
		c.traceExceptionDetails = true;
		s.traceCloseDetails = true;
		c.session.write(new BinaryFrame("ABC".getBytes()));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(BINARY)|", s.getTrace());
		s.session.write(new byte[] {(byte)0x80, (byte)0x00});
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1002:)|CL|EN|", s.getTrace());
		assertEquals("EX|Continuation frame outside fragmented message|CL|EN|", c.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		startClientServer(ssl);
		c.traceExceptionDetails = true;
		s.traceCloseDetails = true;
		s.session.write(new byte[] {(byte)0x80, (byte)0x7f,-1,-1,-1,-1,-1,-1,-1,-1});
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1002:)|CL|EN|", s.getTrace());
		assertEquals("EX|Negative payload length (-1)|CL|EN|", c.getTrace());	
	}

	@Test
	public void testReadInvalidFrameSsl() throws Exception {
		testReadInvalidFrame(true);
	}

	@Test
	public void testReadInvalidFrame() throws Exception {
		testReadInvalidFrame(false);
	}
	
	public void testReadNonUtf8Frame(boolean ssl) throws Exception {
		startClientServer(ssl);
		c.traceExceptionDetails = true;
		s.traceCloseDetails = true;
		s.session.write(new TextFrame(true, 0, new byte[] {(byte) 0xdf, (byte) 0xdf}));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1007:)|CL|EN|", s.getTrace());
		assertEquals("EX|Invalid text frame payload: bytes are not UTF-8|CL|EN|", c.getTrace());	
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		startClientServer(ssl);
		c.traceExceptionDetails = true;
		s.traceCloseDetails = true;
		s.session.write(new CloseFrame(0, new byte[] {3, (byte)0xe8, (byte) 0xdf, (byte) 0xdf}));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1007:)|CL|EN|", s.getTrace());
		assertEquals("EX|Invalid close frame reason value: bytes are not UTF-8|CL|EN|", c.getTrace());	
	}

	@Test
	public void testReadNonUtf8FrameSsl() throws Exception {
		testReadNonUtf8Frame(true);
	}

	@Test
	public void testReadNonUtf8Frame() throws Exception {
		testReadNonUtf8Frame(false);
	}
	
	String[] customizedFields(int count, int nameLen, int valueLen) {
		String[] fields = new String[count*2];
		byte[] a;
		
		for (int i=0; i<count; i++) {
			a = new byte[nameLen];
			Arrays.fill(a, (byte)'N');
			String name = i + new String(a);

			a = new byte[valueLen];
			Arrays.fill(a, (byte)'V');
			String value = i + new String(a);
			fields[i*2] = name.substring(0, nameLen);
			fields[i*2+1] = value.substring(0, valueLen);
		}
		return fields;
	}
	
	public void testMaxFrameLength(boolean ssl) throws Exception {
		startClientServer(ssl);
		s.traceCloseDetails = true;
		s.session.write(new BinaryFrame(true, 0, new byte[65536]));
		c.waitForMsgRead(TIMEOUT);	
		assertEquals("F(BINARY)|", c.getTrace());
		assertEquals(65536, ((Frame)c.msgRead).getPayloadLength());
		s.session.write(new BinaryFrame(true, 0, new byte[65537]));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("EX|CL|EN|", c.getTrace());
		assertEquals("F(CLOSE=1002:)|CL|EN|", s.getTrace().replace("EX|CL|EN|", "CL|EN|"));
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.maxPayloadLength = 512;
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		s.session.getReadyFuture().sync(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
		s.session.write(new BinaryFrame(true, 0, new byte[512]));
		c.waitForMsgRead(TIMEOUT);	
		assertEquals("F(BINARY)|", c.getTrace());
		assertEquals(512, ((Frame)c.msgRead).getPayloadLength());
		s.session.write(new BinaryFrame(true, 0, new byte[513]));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("EX|CL|EN|", c.getTrace());
		assertEquals("F(CLOSE=1002:)|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.traceExceptionDetails = true;
		s.customizedResponse = customizedFields(600, 10, 100);
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|EX|Handshake frame too large|CL|EN|", c.getTrace());
		if (ssl)
			assertEquals("CR|OP|RE|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", s.getTrace());
		else
			assertEquals("CR|OP|RE|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.customizedResponse = null;
		
		String[] customizedFields = customizedFields(100, 10, 100);
		c = new WSClient(PORT, ssl);
		c.customizedRequest = customizedFields;
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		int expectedSize = 11553;
		if (!ssl)
			assertEquals(expectedSize, s.session.getReadBytes());
		c.session.close();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.customizedRequest = customizedFields;
		s.maxHandshakeLength = expectedSize;
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.stop(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		c.getTrace();
		s.getTrace();
		
		c = new WSClient(PORT, ssl);
		c.customizedRequest = customizedFields;
		s.maxHandshakeLength = expectedSize-1;
		s.traceExceptionDetails = true;
		c.traceExceptionDetails = true;
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CR|OP|EX|Invalid websocket response status: 413|CL|EN|", c.getTrace());
		assertEquals("CR|OP|EX|Handshake frame too large|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		
		c = new WSClient(PORT, ssl);
		c.customizedRequest = customizedFields(1, 100, 64000);
		s.maxHandshakeLength = -1;
		s.traceExceptionDetails = true;
		c.traceExceptionDetails = true;
		c.start();
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		if (ssl) {
			String trace = s.getTrace();
			assertTrue(trace.startsWith("CR|OP|EX|Buffer allocation failure: maximum capacity ("));
			assertTrue(trace.endsWith("|CL|EN|"));
			assertEquals("CR|OP|SSL_CLOSED_WITHOUT_CLOSE_NOTIFY|CL|EN|", c.getTrace());
		}
		else {
			assertEquals("CR|OP|CL|EN|", c.getTrace());
			assertEquals("CR|OP|EX|Buffer allocation failure: maximum capacity (8192) reached|CL|EN|", s.getTrace());
		}
		c.stop(TIMEOUT);
		
	}

	@Test
	public void testMaxFrameLengthSsl() throws Exception {
		testMaxFrameLength(true);
	}	

	@Test
	public void testMaxFrameLength() throws Exception {
		testMaxFrameLength(false);
	}	

	@Test
	public void testGetWebSocketHandler() throws Exception {
		Handler h = new Handler("Handler1");
		
		WebSocketSession s = new WebSocketSession(h, true);
		assertTrue(h == s.getWebSocketHandler());
		s = new WebSocketSession("Name1", h, true);
		assertTrue(h == s.getWebSocketHandler());
		
		SSLWebSocketSession s2 = new SSLWebSocketSession("Name1", null, h, true);
		assertTrue(h == s2.getWebSocketHandler());
		s2 = new SSLWebSocketSession((SocketAddress)null, h, true);
		assertTrue(h == s2.getWebSocketHandler());
		s2 = new SSLWebSocketSession("Name1", h, true);
		assertTrue(h == s2.getWebSocketHandler());
		s2 = new SSLWebSocketSession(h, true);
		assertTrue(h == s2.getWebSocketHandler());
	}
	
	public void testTimer(boolean ssl) throws Exception {
		s = new WSServer(PORT, ssl);
		c = new WSClient(PORT, ssl);
		c.timer = new TestTimer();
		s.start();
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		s.session.getReadyFuture().sync(TIMEOUT);
		c.session.getReadyFuture().sync(TIMEOUT);
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
		
		c.session.getTimer().scheduleEvent("T1", 300);
		waitFor(250);
		assertEquals("", c.getTrace());
		waitFor(350);
		assertEquals("TE(T1)|", c.getTrace());
		
		c.session.getTimer().scheduleTask(new Runnable() {

			@Override
			public void run() {
			}
			
			@Override
			public String toString() {
				return "T2";
			}
			
		}, 300, true);
		waitFor(250);
		assertEquals("", c.getTrace());
		waitFor(350);
		assertEquals("TT(T2)|", c.getTrace());
		
	}
	
	@Test
	public void testTimer() throws Exception {
		testTimer(false);
	}
	
	@Test
	public void testTimerSsl() throws Exception {
		testTimer(true);
	}

	IExtension[] extensions(IExtension... extensions) {
		return extensions;
	}
	
	void assertEncoders(String expected, WSServer s) {
		StringBuilder sb = new StringBuilder();
		
		for (Object key: s.session.getCodecPipeline().encoderKeys()) {
			sb.append(key);
			sb.append('|');
		}
		assertEquals(expected, sb.toString());
	}
	
	void assertDecoders(String expected, WSServer s) {
		StringBuilder sb = new StringBuilder();
		
		for (Object key: s.session.getCodecPipeline().decoderKeys()) {
			sb.append(key);
			sb.append('|');
		}
		assertEquals(expected, sb.toString());
	}
	
	void assertPipeline(String exencoder, String exdecoder, WSServer c, WSServer s) {
		assertEncoders("ws-encoder|" + exencoder, s);
		assertEncoders("ws-encoder|" + exencoder, c);
		assertDecoders("ws-decoder|" + exdecoder + "ws-utf8-validator|", s);
		assertDecoders("ws-decoder|" + exdecoder + "ws-utf8-validator|", c);
	}
	
	void assertExtensions(boolean ssl, IExtension[] cExt, IExtension[] sExt, String expEnc, String expDec) throws Exception {
		TextFrame f;

		startClientServer(ssl, cExt, sExt);
		c.session.write(new TextFrame(true, 0, "ABCDEF"));
		s.waitForMsgRead(TIMEOUT);
		f = (TextFrame)s.msgRead;
		assertEquals("", c.getTrace());
		assertEquals("F(TEXT)|", s.getTrace());
		assertEquals("ABCDEF", f.getText());
		assertEquals(0, f.getRsvBits());
		assertPipeline(expEnc, expDec, c, s);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	}
	
	void testExtensions(boolean ssl) throws Exception {
		String USER1 = "USER1";
		IExtension e1A = new TestExtension("ext1", 'A', 4);
		IExtension e1B = new TestExtension("ext1", 'B', 4);
		IExtension e2C = new TestExtension("ext2", 'C', 4, true);
		IExtension e3D = new TestExtension("ext3", USER1, 'D', 4, true);
		IExtension e1D = new TestExtension("ext1", USER1, 'E', 4, true);
		
		assertExtensions(ssl, extensions(e1A), extensions(e1A), "ex-encoderA|", "ex-decoderA|");
		assertExtensions(ssl, extensions(e1A), null, "", "");
		assertExtensions(ssl, null, extensions(e1A), "", "");
		assertExtensions(ssl, extensions(e1A,e1B), extensions(e1A,e1B), "ex-encoderA|", "ex-decoderA|");
		assertExtensions(ssl, extensions(e1B,e1A), extensions(e1A,e1B), "ex-encoderB|", "ex-decoderB|");
		assertExtensions(ssl, extensions(e1A,e1B,e2C), extensions(e1A,e1B,e2C), 
				"ex-encoderA|", "ex-decoderA|");
		assertExtensions(ssl, extensions(e1A,e1B,e3D), extensions(e1A,e1B,e3D), 
				"ex-encoderD|ex-encoderA|", "ex-decoderD|ex-decoderA|");
		assertExtensions(ssl, extensions(e1A,e1D), extensions(e1A,e1D), 
				"ex-encoderA|", "ex-decoderA|");
	}
	
	@Test
	public void testExtensions() throws Exception {
		testExtensions(false);
	}

	@Test
	public void testExtensionsSsl() throws Exception {
		testExtensions(true);
	}

	IDecoder<?,?> sdecoder,cdecoder;
	IEncoder<?,?> sencoder,cencoder;
	ZlibDecoder sd,cd;
	ZlibEncoder se,ce;
	
	void initCodecs() {
		sdecoder = (IDecoder<?, ?>) s.session.getCodecPipeline().get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER);
		sencoder = (IEncoder<?, ?>) s.session.getCodecPipeline().get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER);
		cdecoder = (IDecoder<?, ?>) c.session.getCodecPipeline().get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_DECODER);
		cencoder = (IEncoder<?, ?>) c.session.getCodecPipeline().get(PerMessageDeflateExtension.PERMESSAGE_DEFLATE_ENCODER);
	}
	
	void initZlibCodecs() throws Exception {
		sd = DeflateCodecTest.getZlibDecoder(sdecoder);
		se = DeflateCodecTest.getZlibEncoder(sencoder);
		cd = DeflateCodecTest.getZlibDecoder(cdecoder);
		ce = DeflateCodecTest.getZlibEncoder(cencoder);
	}
	
	void assertNullZlibCodecs() throws Exception {
		assertNull(DeflateCodecTest.getZlibDecoder(sdecoder));
		assertNull(DeflateCodecTest.getZlibEncoder(sencoder));
		assertNull(DeflateCodecTest.getZlibDecoder(cdecoder));
		assertNull(DeflateCodecTest.getZlibEncoder(cencoder));
	}
	
	void assertNotNullZlibCodecs() throws Exception {
		assertNotNull(DeflateCodecTest.getZlibDecoder(sdecoder));
		assertNotNull(DeflateCodecTest.getZlibEncoder(sencoder));
		assertNotNull(DeflateCodecTest.getZlibDecoder(cdecoder));
		assertNotNull(DeflateCodecTest.getZlibEncoder(cencoder));
	}
	
	void assertEqualsZlibCodecs() throws Exception {
		assertTrue(sd == DeflateCodecTest.getZlibDecoder(sdecoder));
		assertTrue(se == DeflateCodecTest.getZlibEncoder(sencoder));
		assertTrue(cd == DeflateCodecTest.getZlibDecoder(cdecoder));
		assertTrue(ce == DeflateCodecTest.getZlibEncoder(cencoder));
	}
	
	void testCompression(boolean ssl) throws Exception {
		String text = "ABCDEFGHIJKLMNOP";
		byte[] binary = new byte[1000];
		
		for (int i=0; i<binary.length; ++i) {
			if (i % 10 == 0) {
				binary[i] = (byte) i;
			}
		}
		
		startClientServer(ssl, extensions(new PerMessageDeflateExtension(6,NoContext.REQUIRED, NoContext.REQUIRED)), extensions(new PerMessageDeflateExtension()));
		initCodecs();
		c.session.write(new TextFrame(true, 0, text));
		s.waitForMsgRead(TIMEOUT);
		s.session.write(new BinaryFrame(binary));
		c.waitForMsgRead(TIMEOUT);
		assertNullZlibCodecs();
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame(true, 0, text));
			s.session.write(new BinaryFrame(binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertEquals(text, ((TextFrame)s.msgRead).getText());
			assertArrayEquals(binary, ((Frame)c.msgRead).getPayload());
			assertNullZlibCodecs();

			s.session.write(new BinaryFrame(false, 0, binary));
			c.session.write(new BinaryFrame(false, 0, binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertArrayEquals(binary, ((Frame)c.msgRead).getPayload());
			assertArrayEquals(binary, ((Frame)s.msgRead).getPayload());
			assertNotNullZlibCodecs();
			initZlibCodecs();
			s.session.write(new PongFrame(0, new byte[100]));
			c.waitForMsgRead(TIMEOUT);
			c.session.write(new PingFrame());
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			s.session.write(new ContinuationFrame(false, 0, binary));
			c.session.write(new ContinuationFrame(false, 0, binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertArrayEquals(binary, ((Frame)c.msgRead).getPayload());
			assertArrayEquals(binary, ((Frame)s.msgRead).getPayload());
			assertEqualsZlibCodecs();
			s.session.write(new ContinuationFrame(true, 0, binary));
			c.session.write(new ContinuationFrame(true, 0, binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertArrayEquals(binary, ((Frame)c.msgRead).getPayload());
			assertArrayEquals(binary, ((Frame)s.msgRead).getPayload());
			assertNullZlibCodecs();
		}
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertNullZlibCodecs();

		startClientServer(ssl, extensions(new PerMessageDeflateExtension(6,NoContext.FORBIDDEN, NoContext.FORBIDDEN)), extensions(new PerMessageDeflateExtension()));
		initCodecs();
		c.session.write(new TextFrame(true, 0, text));
		s.waitForMsgRead(TIMEOUT);
		s.session.write(new BinaryFrame(binary));
		c.waitForMsgRead(TIMEOUT);
		assertNotNullZlibCodecs();
		initZlibCodecs();
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame(true, 0, text));
			s.session.write(new BinaryFrame(binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertEquals(text, ((TextFrame)s.msgRead).getText());
			assertArrayEquals(binary, ((BinaryFrame)c.msgRead).getPayload());
			assertEqualsZlibCodecs();
		}		
		assertTrue(!sd.isFinished());
		assertTrue(!cd.isFinished());
		assertTrue(!se.isFinished());
		assertTrue(!ce.isFinished());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		assertTrue(sd.isFinished());
		assertTrue(cd.isFinished());
		assertTrue(se.isFinished());
		assertTrue(ce.isFinished());
		
		startClientServer(ssl, extensions(new PerMessageDeflateExtension(6,NoContext.REQUIRED, NoContext.FORBIDDEN)), extensions(new PerMessageDeflateExtension()));
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame(true, 0, text));
			s.session.write(new BinaryFrame(binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertEquals(text, ((TextFrame)s.msgRead).getText());
			assertArrayEquals(binary, ((BinaryFrame)c.msgRead).getPayload());
		}
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		startClientServer(ssl, extensions(new PerMessageDeflateExtension(6,NoContext.FORBIDDEN, NoContext.REQUIRED)), extensions(new PerMessageDeflateExtension()));
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame(true, 0, text));
			s.session.write(new BinaryFrame(binary));
			s.waitForMsgRead(TIMEOUT);
			c.waitForMsgRead(TIMEOUT);
			assertEquals(text, ((TextFrame)s.msgRead).getText());
			assertArrayEquals(binary, ((BinaryFrame)c.msgRead).getPayload());
		}
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}

	@Test
	public void testCompression() throws Exception {
		testCompression(false);
	}
	
	@Test
	public void testCompressionSsl() throws Exception {
		testCompression(true);
	}

	void assertNoCompression(boolean ssl, boolean invalid, int encoders, int decoders, NoContext... noContext) throws Exception {
		s = new WSServer(PORT, ssl);
		s.extensions = extensions(new PerMessageDeflateExtension(
				6, 
				noContext[2],
				noContext[3]
		));
		c = new WSClient(PORT, ssl);
		c.extensions = extensions(new PerMessageDeflateExtension(
				6,
				noContext[0], 
				noContext[1]
		));
		s.start();
		c.start();
		c.waitForOpen(TIMEOUT);
		s.waitForOpen(TIMEOUT);
		s.session.getReadyFuture().sync(TIMEOUT);
		s.waitForReady(TIMEOUT);
		s.resetDataLocks();
		assertEquals("CR|OP|RE|", s.getTrace());
		assertEquals(encoders, s.session.getCodecPipeline().encoderKeys().size());
		assertEquals(decoders, s.session.getCodecPipeline().decoderKeys().size());

		try {
			c.session.getReadyFuture().sync(TIMEOUT);
			if (invalid) {
				fail();
			}
			c.waitForReady(TIMEOUT);
			c.resetDataLocks();
			assertEquals("CR|OP|RE|", c.getTrace());
			assertEquals(encoders, c.session.getCodecPipeline().encoderKeys().size());
			assertEquals(decoders, c.session.getCodecPipeline().decoderKeys().size());
		}
		catch (ExecutionException e) {
			if (!invalid) {
				throw e;
			}
		}
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	void testNoCompression(boolean ssl) throws Exception {
		assertNoCompression(ssl, false, 2, 3, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.FORBIDDEN);
		assertNoCompression(ssl, false, 1, 2, NoContext.REQUIRED, NoContext.REQUIRED, NoContext.FORBIDDEN, NoContext.FORBIDDEN);
		assertNoCompression(ssl, false, 1, 2, NoContext.REQUIRED, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.FORBIDDEN);
		assertNoCompression(ssl, false, 1, 2, NoContext.FORBIDDEN, NoContext.REQUIRED, NoContext.FORBIDDEN, NoContext.FORBIDDEN);		
		assertNoCompression(ssl, true, 2, 3, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.REQUIRED, NoContext.REQUIRED);
		assertNoCompression(ssl, true, 2, 3, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.REQUIRED, NoContext.FORBIDDEN);
		assertNoCompression(ssl, true, 2, 3, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.REQUIRED);
	}
	
	@Test
	public void testNoCompression() throws Exception {
		testNoCompression(false);
	}
	
	@Test
	public void testNoCompressionSsl() throws Exception {
		testNoCompression(true);
	}
	
	void CompressionWithMoreExtensions(boolean ssl, int encoders, int decoders, IExtension[] cli, IExtension[] srv) throws Exception {
		startClientServer(ssl, cli, srv);
		assertEquals(encoders, s.session.getCodecPipeline().encoderKeys().size());
		assertEquals(encoders, c.session.getCodecPipeline().encoderKeys().size());
		assertEquals(decoders, s.session.getCodecPipeline().decoderKeys().size());
		assertEquals(decoders, c.session.getCodecPipeline().decoderKeys().size());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}
	
	IExtension[] extensions(NoContext... noContexts) {
		IExtension[] extensions = new IExtension[noContexts.length/2];
		
		for (int i=0; i<extensions.length; ++i) {
			extensions[i] = new PerMessageDeflateExtension(
					6,
					noContexts[i*2], 
					noContexts[i*2+1]);
		}
		return extensions;
	}
	
	void testCompressionWithMoreExtensions(boolean ssl) throws Exception {
		CompressionWithMoreExtensions(ssl, 1, 2, 
				extensions(NoContext.REQUIRED, NoContext.REQUIRED), 
				extensions(NoContext.FORBIDDEN, NoContext.FORBIDDEN));
		CompressionWithMoreExtensions(ssl, 2, 3, 
				extensions(NoContext.REQUIRED, NoContext.REQUIRED), 
				extensions(NoContext.FORBIDDEN, NoContext.FORBIDDEN, NoContext.REQUIRED, NoContext.REQUIRED));
		CompressionWithMoreExtensions(ssl, 2, 3, 
				extensions(NoContext.REQUIRED, NoContext.REQUIRED, NoContext.FORBIDDEN, NoContext.FORBIDDEN), 
				extensions(NoContext.FORBIDDEN, NoContext.FORBIDDEN));
	}
	
	@Test
	public void testCompressionWithMoreExtensions() throws Exception {
		testCompressionWithMoreExtensions(false);
	}
	
	@Test
	public void testCompressionWithMoreExtensionsSsl() throws Exception {
		testCompressionWithMoreExtensions(true);
	}
	
	void testHandleCloseFrame(boolean ssl) throws Exception {
		traceCloseDetails = true;
		startClientServer(ssl);		
		c.session.write(new TextFrame("ABC"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		c.session.write(new CloseFrame(1111));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(CLOSE=1111:)|", s.getTrace());
		s.session.write(new PongFrame());
		c.waitForMsgRead(TIMEOUT);
		assertEquals("F(PONG)|", c.getTrace());
		((IWebSocketSession)s.session).close(1112);
		s.waitForEnding(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1112:)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		serverHandleClose = true;
		clientHandleClose = true;
		startClientServer(ssl);		
		c.session.write(new TextFrame("ABC"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		c.session.write(new CloseFrame(1111));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		serverHandleClose = true;
		clientHandleClose = false;
		startClientServer(ssl);		
		c.session.write(new CloseFrame(1111, "Error"));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1111:Error)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		serverHandleClose = true;
		clientHandleClose = false;
		clientWaitForCloseMessage = true;
		startClientServer(ssl);		
		((IWebSocketSession)c.session).close(1112);		
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1112:)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		serverHandleClose = true;
		clientHandleClose = false;
		startClientServer(ssl);		
		((IWebSocketSession)c.session).close(1113, "Error2");		
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1113:Error2)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		serverHandleClose = true;
		clientHandleClose = false;
		startClientServer(ssl);		
		((IWebSocketSession)c.session).close(-1);		
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=-1:)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		serverHandleClose = true;
		clientHandleClose = false;
		clientWaitForCloseMessage = true;
		startClientServer(ssl);		
		((IWebSocketSession)c.session).close();		
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		serverHandleClose = true;
		clientHandleClose = false;
		startClientServer(ssl);		
		c.loop.execute(new Runnable() {
			@Override
			public void run() {
				((IWebSocketSession)c.session).close(1113, "Error2");
			}
		});
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(CLOSE=1113:Error2)|CL|EN|", c.getTrace());
		assertEquals("CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}

	@Test
	public void testHandleCloseFrame() throws Exception {
		testHandleCloseFrame(false);
	}

	@Test
	public void testHandleCloseFrameSsl() throws Exception {
		testHandleCloseFrame(true);
	}
	
	void testFrameAggregation(boolean ssl) throws Exception {
		traceCloseDetails = true;
		startClientServer(ssl);		
		s.session.getCodecPipeline().add("1", new FrameAggregator(100));
		
		c.session.write(new TextFrame("ABC"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|",s.getTrace());
		assertEquals("ABC", ((TextFrame)s.msgRead).getText());
		
		c.session.write(new TextFrame(false, 0, "BCD"));
		waitFor(100);
		assertEquals("",s.getTrace());
		c.session.write(new PingFrame());
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(PING)|",s.getTrace());
		c.session.write(new ContinuationFrame(false, 0, "EFG"));
		waitFor(100);
		assertEquals("",s.getTrace());
		c.session.write(new ContinuationFrame("HIJ"));
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|",s.getTrace());
		assertEquals("BCDEFGHIJ", ((TextFrame)s.msgRead).getText());
		
		c.session.write(new BinaryFrame(false, 0, new byte[99]));
		waitFor(100);
		assertEquals("",s.getTrace());
		c.session.write(new ContinuationFrame(false, 0, new byte[2]));
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("F(PONG)|F(CLOSE=1009:)|CL|EN|",c.getTrace());
		assertEquals("EX|CL|EN|",s.getTrace());
	}
	
	@Test
	public void testFrameAggregation() throws Exception {
		testFrameAggregation(false);
	}

	@Test
	public void testFrameAggregationSsl() throws Exception {
		testFrameAggregation(true);
	}
	
	void assertAllocations(DefaultAllocatorMetric m, int allocated, int allocating, int released, int releasing) {
		assertEquals("allocated", allocated, m.getAllocatedCount());
		assertEquals("allocating", allocating, m.getAllocatingCount());
		assertEquals("released",released, m.getReleasedCount());
		assertEquals("releasing", releasing, m.getReleasingCount());
	}
	
	void testBufferAllocation(boolean ssl) throws Exception {
		DefaultAllocatorMetric sm = new DefaultAllocatorMetric();
		DefaultAllocatorMetric cm = new DefaultAllocatorMetric();
		s = new WSServer(PORT, ssl);
		s.allocator = new DefaultAllocator(false, sm);
		s.optimieDataCopying = false;
		c = new WSClient(PORT, ssl);
		c.allocator = new DefaultAllocator(false, cm);
		c.optimieDataCopying = false;
		s.start();
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();
		
		int inc = ssl ? 4 : 0;
		assertAllocations(sm,3+inc,3+inc,0,0);
		assertAllocations(cm,3+inc,3+inc,0,0);
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame("ABCD")).sync(TIMEOUT);
		}
		waitFor(100);
		assertAllocations(sm,3+inc,3+inc,0,0);
		assertAllocations(cm,103+inc,103+inc,0,0);
		c.session.close();
		s.waitForEnding(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		waitFor(100);
		assertAllocations(cm,103+inc,103+inc,0,0);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);

		sm = new DefaultAllocatorMetric();
		cm = new DefaultAllocatorMetric();
		s = new WSServer(PORT, ssl);
		s.allocator = new CachingAllocator(false, sm);
		s.optimieDataCopying = false;
		c = new WSClient(PORT, ssl);
		c.allocator = new CachingAllocator(false, cm);
		c.optimieDataCopying = false;
		s.start();
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();

		assertAllocations(sm,3+inc,3+inc,0,0);
		assertAllocations(cm,3+inc,3+inc,0,0);
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame("ABCD")).sync(TIMEOUT);
		}
		waitFor(100);
		assertAllocations(sm,3+inc,3+inc,0,100);
		assertAllocations(cm,103+inc,103+inc,0,0);
		c.session.close();
		s.waitForEnding(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		waitFor(100);
		assertAllocations(sm,3+inc,3+inc,2+inc,102+inc);
		assertAllocations(cm,103+inc,103+inc,2+inc,2+inc);
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
	
		sm = new DefaultAllocatorMetric();
		cm = new DefaultAllocatorMetric();
		s = new WSServer(PORT, ssl);
		s.allocator = new CachingAllocator(false, sm);
		s.optimieDataCopying = true;
		c = new WSClient(PORT, ssl);
		c.allocator = new CachingAllocator(false, cm);
		c.optimieDataCopying = true;
		s.start();
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		c.resetDataLocks();
		s.resetDataLocks();

		long allocated = sm.getAllocatedCount();
		if (ssl) {
			assertEquals(allocated, sm.getAllocatedCount());
			assertEquals(sm.getAllocatingCount(), sm.getReleasingCount());
			assertEquals(sm.getReleasedCount(), sm.getReleasingCount());
			assertEquals(cm.getAllocatingCount(), cm.getReleasingCount());
			assertEquals(cm.getReleasedCount(), cm.getReleasingCount());
		}
		else {
			assertAllocations(sm,2,2,2,2);
			assertAllocations(cm,2,2,2,2);
		}
		for (int i=0; i<100; ++i) {
			c.session.write(new TextFrame("ABCD")).sync(TIMEOUT);
		}
		waitFor(100);
		if (ssl) {
			assertEquals(allocated, sm.getAllocatedCount());
			assertEquals(sm.getAllocatingCount(), sm.getReleasingCount());
			assertEquals(sm.getReleasedCount(), sm.getReleasingCount());
			assertEquals(cm.getAllocatingCount(), cm.getReleasingCount());
			assertEquals(cm.getReleasedCount(), cm.getReleasingCount());
		}
		else {
			assertAllocations(sm,2,102,102,102);
			assertAllocations(cm,3,102,102,102);
		}
		c.session.close();
		s.waitForEnding(TIMEOUT);
		c.waitForEnding(TIMEOUT);
		waitFor(100);
		if (ssl) {
			assertEquals(allocated, sm.getAllocatedCount());
			assertEquals(sm.getAllocatingCount(), sm.getReleasingCount());
			assertEquals(sm.getReleasedCount(), sm.getReleasingCount());
			assertEquals(cm.getAllocatingCount(), cm.getReleasingCount());
			assertEquals(cm.getReleasedCount(), cm.getReleasingCount());
		}
		else {
			assertAllocations(sm,2,103,103,103);
			assertAllocations(cm,3,103,103,103);
		}
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
	}

	@Test
	public void testBufferAllocation() throws Exception {
		testBufferAllocation(false);
	}

	@Test
	public void testBufferAllocationSsl() throws Exception {
		testBufferAllocation(true);
	}
	
	void testHandshakeInfo(boolean ssl) throws Exception {
		startClientServer(ssl);		
		IHandshaker ci = ((IWebSocketSession)c.session).getHandshaker();
		IHandshaker si = ((IWebSocketSession)s.session).getHandshaker();
		assertTrue(ci.isClientMode());
		assertFalse(si.isClientMode());
		assertNull(ci.getSubProtocol());
		assertNull(si.getSubProtocol());
		assertTrue(ci.isFinished());
		assertTrue(si.isFinished());
		assertArrayEquals(new String[0], ci.getExtensionNames());
		assertArrayEquals(new String[0], si.getExtensionNames());
		assertEquals((ssl ? "wss" : "ws") + "://127.0.0.1:7777/", si.getUri().toString());
		assertEquals((ssl ? "wss" : "ws") + "://127.0.0.1:7777/", ci.getUri().toString());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		startClientServer(ssl, new IExtension[] {new PerMessageDeflateExtension()}, new IExtension[] {new PerMessageDeflateExtension()});
		ci = ((IWebSocketSession)c.session).getHandshaker();
		si = ((IWebSocketSession)s.session).getHandshaker();
		assertArrayEquals(new String[] {"permessage-deflate"}, ci.getExtensionNames());
		assertArrayEquals(new String[] {"permessage-deflate"}, si.getExtensionNames());
		c.stop(TIMEOUT);
		s.stop(TIMEOUT);
		
		s = new WSServer(PORT, ssl);
		s.subProtocols = new String[] {"proto1", "proto2"};
		c = new WSClient(PORT, ssl);
		c.subProtocols = new String[] {"proto2"};
		c.requestUri = new URI((ssl ? "wss" : "ws") + "://127.0.0.1:7777/req?55");
		s.start();
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		ci = ((IWebSocketSession)c.session).getHandshaker();
		si = ((IWebSocketSession)s.session).getHandshaker();
		assertEquals("proto2", ci.getSubProtocol());
		assertEquals("proto2", si.getSubProtocol());
		assertEquals((ssl ? "wss" : "ws") + "://127.0.0.1:7777/req?55", si.getUri().toString());
		assertEquals((ssl ? "wss" : "ws") + "://127.0.0.1:7777/req?55", ci.getUri().toString());
		
	}	
	
	@Test
	public void testHandshakeInfo() throws Exception {
		testHandshakeInfo(false);
	}
	
	@Test
	public void testHandshakeInfoSsl() throws Exception {
		testHandshakeInfo(true);
	}
	
	void testProxyConnection(boolean ssl) throws Exception {
		//clear proxy
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT);
		s = new WSServer(PORT, ssl);
		s.start();
		c = new WSClient(PORT+1, ssl) {
			
			@Override
			StreamSession createSession(IWebSocketHandler handler) throws Exception {
				StreamSession s = super.createSession(handler);
				
				s.getPipeline().add("proxy", new StreamSession(new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT))));
				return s;
			}			
		};
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
		c.session.write(new TextFrame("Hello!")).sync(TIMEOUT);
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		((IWebSocketSession)c.session).close(CloseFrame.NORMAL);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("F(CLOSE)|CL|EN|", s.getTrace());
		c.stop(TIMEOUT);
		p.stop(TIMEOUT);
		
		//secure proxy
		p = new HttpProxy(PORT+1);
		p.start(TIMEOUT, true);
		final DefaultSessionConfig config = new DefaultSessionConfig() {
			@Override
			public SSLEngine createSSLEngine(boolean clientMode) throws SSLEngineCreateException {
				return Server.createSSLEngine(null, clientMode);
			}
		};
		config.setWaitForInboundCloseMessage(true);
		c = new WSClient(PORT+1, ssl) {
			
			@Override
			StreamSession createSession(IWebSocketHandler handler) throws Exception {
				StreamSession s = super.createSession(handler);
				
				s.getPipeline().add("proxy", new SSLSession(new HttpProxyHandler(new URI("http://127.0.0.1:" + PORT), config), true));
				return s;
			}			
		};
		s.resetDataLocks();
		c.start();
		c.waitForReady(TIMEOUT);
		s.waitForReady(TIMEOUT);
		assertEquals("CR|OP|RE|", c.getTrace());
		assertEquals("CR|OP|RE|", s.getTrace());
		c.session.write(new TextFrame("Hello!")).sync(TIMEOUT);
		s.waitForMsgRead(TIMEOUT);
		assertEquals("F(TEXT)|", s.getTrace());
		((IWebSocketSession)c.session).close(CloseFrame.NORMAL);
		c.waitForEnding(TIMEOUT);
		s.waitForEnding(TIMEOUT);
		assertEquals("CL|EN|", c.getTrace());
		assertEquals("F(CLOSE)|CL|EN|", s.getTrace());
	}

	@Test
	public void testProxyConnection() throws Exception {
		testProxyConnection(false);
	}
	
	@Test
	public void testProxyConnectionSsl() throws Exception {
		testProxyConnection(true);
	}
	
	static class Handler extends TestHandler implements IWebSocketHandler {

		Handler(String name) {
			super(name);
		}
		
		@Override
		public IWebSocketSessionConfig getConfig() {
			return new DefaultWebSocketSessionConfig();
		}
	}
}
