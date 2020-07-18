package org.snf4j.core;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.snf4j.core.codec.DefaultCodecExecutor;

public class DTLSTest {
	
	long TIMEOUT = 2000;
	int PORT = 7779;
	
	DatagramHandler c;
	DatagramHandler s,s2;
	DatagramProxy p;
	
	TestCodec codec;
	
	public static void assumeJava8() {
		Assume.assumeTrue(DatagramHandler.JAVA_VER < 9.0);
	}
	
	public static void assumeJava9() {
		Assume.assumeTrue(DatagramHandler.JAVA_VER >= 9.0);
	}
	
	public static void assumeSuccessfulRehandshake() {
		Assume.assumeTrue(DatagramHandler.JAVA_VER >= 9.0 && DatagramHandler.JAVA_VER < 11.0);
	}
	
	public static void assumeFailingOrNoRehandshake() {
		Assume.assumeTrue(DatagramHandler.JAVA_VER < 9.0 || DatagramHandler.JAVA_VER >= 11.0);
	}
	
	static final boolean HANDSHAKING_AFTER_CLOSE = DatagramHandler.JAVA_VER <= 11.0;
	
	public static void assumeHandshakingAfterClose() {
		Assume.assumeTrue(DatagramHandler.JAVA_VER >= 9.0 && HANDSHAKING_AFTER_CLOSE);
	}
	
	public static void assumeNoHandshakingAfterClose() {
		Assume.assumeTrue(!HANDSHAKING_AFTER_CLOSE);
	}
	
	static final boolean TLS1_3 = SSLSessionTest.TLS1_3;
	
	@Before
	public void before() {
		s = s2 = c = null;
		p = null;
	}
	
	@After
	public void after() throws InterruptedException {
		if (c != null) c.stop(TIMEOUT);
		if (s != null) s.stop(TIMEOUT);
		if (s2 != null) s2.stop(TIMEOUT);
		if (p != null) p.stop(TIMEOUT);
	}
	
	void waitFor(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}
	
	Packet nopp(String s) {
		return new Packet(PacketType.NOP, s);
	}
	
	byte[] nop(String s) {
		return new Packet(PacketType.NOP, s).toBytes();
	}
	
	byte[] nop() {
		return nop("");
	}
	
	DefaultCodecExecutor codec() {
		codec = new TestCodec();
		DefaultCodecExecutor p = new DefaultCodecExecutor();
		p.getPipeline().add("1", codec.BasePD());
		p.getPipeline().add("2", codec.PBD());
		p.getPipeline().add("3", codec.PBE());
		p.getPipeline().add("4", codec.BPE());
		return p;
	}
	
	String getRecordedData(DatagramHandler h) {
		String s = h.getRecordedData(true);
		StringBuilder sb = new StringBuilder();
		int i, i0=0;
		int dr = 0, ds = 0;
		
		while ((i = s.indexOf('|',i0)) != -1) {
			String t = s.substring(i0, i+1);
			if (t.equals("DR|")) {
				dr++;
			}
			else if (t.equals("DS|")) {
				ds++;
			}
			else {
				if (dr > 0) {
					sb.append("DR");
					sb.append(dr > 1 ? "+|" : "|");
					dr = 0;
				}
				if (ds > 0) {
					sb.append("DS");
					sb.append(ds > 1 ? "+|" : "|");
					ds = 0;
				}
				sb.append(t);
			}
			i0 = i+1;
		}
		if (dr > 0) {
			sb.append("DR");
			sb.append(dr > 1 ? "+|" : "|");
		}
		if (ds > 0) {
			sb.append("DS");
			sb.append(ds > 1 ? "+|" : "|");
		}
		return sb.toString();
	}
	
	SocketAddress address(int port) throws UnknownHostException {
		return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port);
	}
	
	void assertReady(DatagramHandler client, DatagramHandler server) throws Exception {
		assertReady(client, server, "SCR|SOP|DR+|DS+|RDY|DR+|", "SCR|SOP|DR+|DS+|RDY|DS+|");
	}
	
	void assertReady(DatagramHandler client, DatagramHandler server, String expectedClient, String expectedServer) throws Exception {
		if (client != null) {
			client.waitForSessionReady(TIMEOUT);
		}
		if (server != null) {
			server.waitForSessionReady(TIMEOUT);
		}
		waitFor(10);
		if (client != null) {
			assertEquals(expectedClient, getRecordedData(client));
		}
		if (server != null) {
			assertEquals(expectedServer, getRecordedData(server));
		}
	}

}
