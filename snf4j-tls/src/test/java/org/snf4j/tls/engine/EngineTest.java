/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.CommonTest;
import org.snf4j.tls.cipher.CipherSuite;
import org.snf4j.tls.extension.ExtensionType;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IKeyShareExtension;
import org.snf4j.tls.extension.ISupportedVersionsExtension;
import org.snf4j.tls.extension.KeyShareEntry;
import org.snf4j.tls.extension.KeyShareExtension;
import org.snf4j.tls.extension.NamedGroup;
import org.snf4j.tls.extension.ParsedKey;
import org.snf4j.tls.extension.ServerNameExtension;
import org.snf4j.tls.extension.SignatureAlgorithmsCertExtension;
import org.snf4j.tls.extension.SignatureAlgorithmsExtension;
import org.snf4j.tls.extension.SignatureScheme;
import org.snf4j.tls.extension.SupportedGroupsExtension;
import org.snf4j.tls.extension.SupportedVersionsExtension;
import org.snf4j.tls.handshake.HandshakeType;
import org.snf4j.tls.handshake.IClientHello;
import org.snf4j.tls.handshake.IHandshake;
import org.snf4j.tls.record.RecordType;

public class EngineTest extends CommonTest {

	TestHandshakeHandler handler;
	
	TestParameters params;

	byte[] random;
	
	byte[] legacySessionId;

	@Override
	public void before() throws Exception {
		super.before();
		handler = new TestHandshakeHandler();
		params = new TestParameters();
		random = new byte[32];
		legacySessionId = new byte[0];
	}
	
	EngineState serverState() {
		return new EngineState(MachineState.SRV_WAIT_1_CH, params, handler, handler);
	}
	
	static CipherSuite[] suites(CipherSuite... suites) {
		return suites;
	}

	static ServerNameExtension serverName(String serverName) {
		return new ServerNameExtension(serverName);
	}
	
	static SupportedVersionsExtension versions(int... versions) {
		return new SupportedVersionsExtension(ISupportedVersionsExtension.Mode.CLIENT_HELLO, versions);
	}
	
	static KeyShareExtension keyShare(NamedGroup... groups) throws Exception {
		ArrayList<KeyShareEntry> entries = new ArrayList<KeyShareEntry>();
		
		for (NamedGroup group: groups) {
			KeyPair pair = group.spec().getKeyExchange().generateKeyPair();
			
			ByteBuffer buffer = ByteBuffer.allocate(1000);
			group.spec().getData(buffer, pair.getPublic());
			buffer.flip();
			ParsedKey parsedKey = group.spec().parse(ByteBufferArray.wrap(new ByteBuffer[] {buffer}), buffer.remaining());
			
			entries.add(new KeyShareEntry(group, parsedKey));
		}
		return new KeyShareExtension(IKeyShareExtension.Mode.CLIENT_HELLO, entries.toArray(new KeyShareEntry[entries.size()]));
	}
	
	static SupportedGroupsExtension groups(NamedGroup... groups) {
		return new SupportedGroupsExtension(groups);
	}
	
	static SignatureAlgorithmsExtension schemes(SignatureScheme... schemes) {
		return new SignatureAlgorithmsExtension(schemes);
	}

	static SignatureAlgorithmsExtension certSchemes(SignatureScheme... schemes) {
		return new SignatureAlgorithmsCertExtension(schemes);
	}
	
	static void replace(List<IExtension> extensions, IExtension extension) {
		for (int i=0; i<extensions.size(); ++i) {
			IExtension e = extensions.get(i);
			
			if (e.getType().equals(extension.getType())) {
				extensions.set(i, extension);
			}
		}
	}

	static void remove(List<IExtension> extensions, ExtensionType type) {
		for (int i=0; i<extensions.size(); ++i) {
			IExtension e = extensions.get(i);
			
			if (e.getType().equals(type)) {
				extensions.remove(i);
			}
		}
	}
	
	protected static void assertProduced(ProducedHandshake h, HandshakeType type, RecordType recordType) {
		assertEquals(type.value(), h.getHandshake().getType().value());
		assertEquals(recordType, h.getRecordType());
	}
	
	@SuppressWarnings("unchecked")
	static <T extends IExtension> T findExtension(IHandshake handshake, ExtensionType type) {
		for (IExtension e: handshake.getExtensioins()) {
			if (e.getType().value() == type.value()) {
				return (T) e;
			}
		}
		return null;
	}
	
	protected static void assertCipherSuites(IClientHello ch, CipherSuite... suites) {
		assertArrayEquals(suites, ch.getCipherSuites());
	}
	
	protected static void assertServerName(IHandshake handshake, String name) {
		ServerNameExtension e = findExtension(handshake, ExtensionType.SERVER_NAME);

		if (name != null) {
			assertEquals(name, e.getHostName());
		}
		else {
			assertNull(e);
		}
	}

	protected static void assertSupportedVersions(IHandshake handshake, int... versions) {
		SupportedVersionsExtension e = findExtension(handshake, ExtensionType.SUPPORTED_VERSIONS);

		if (versions != null) {
			assertArrayEquals(versions, e.getVersions());
		}
		else {
			assertNull(e);
		}
	}

	protected static void assertSupportedGroups(IHandshake handshake, NamedGroup... groups) {
		SupportedGroupsExtension e = findExtension(handshake, ExtensionType.SUPPORTED_GROUPS);

		if (groups != null) {
			assertArrayEquals(groups, e.getGroups());
		}
		else {
			assertNull(e);
		}
	}

	protected static void assertSignatureAlgorithms(IHandshake handshake, SignatureScheme... schemes) {
		SignatureAlgorithmsExtension e = findExtension(handshake, ExtensionType.SIGNATURE_ALGORITHMS);

		if (schemes != null) {
			assertArrayEquals(schemes, e.getSchemes());
		}
		else {
			assertNull(e);
		}
	}
	
	protected static void assertKeyShare(IHandshake handshake, Boolean server, NamedGroup... groups) {
		KeyShareExtension e = findExtension(handshake, ExtensionType.KEY_SHARE);
		
		if (groups != null) {
			if (server == null) {
				assertEquals(IKeyShareExtension.Mode.HELLO_RETRY_REQUEST, e.getMode());
				assertEquals(groups[0], e.getNamedGroup());
			}
			else if (server) {
				assertEquals(IKeyShareExtension.Mode.SERVER_HELLO, e.getMode());
				assertEquals(1, e.getEntries().length);
				assertEquals(groups[0], e.getEntries()[0].getNamedGroup());
			}
			else {
				assertEquals(IKeyShareExtension.Mode.CLIENT_HELLO, e.getMode());
				assertEquals(groups.length, e.getEntries().length);
				for (int i=0; i<groups.length; ++i) {
					assertEquals(groups[i], e.getEntries()[i].getNamedGroup());
				}
			}
		}
		else {
			assertNull(e);
		}
	}
}
