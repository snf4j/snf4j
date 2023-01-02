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
package org.snf4j.tls.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.snf4j.tls.handshake.HandshakeType;

public class ExtensionValidatorTest {
	
	final static String ENTRIES = 
			 "| server_name [RFC6066] | CH, EE |"+
			 "| max_fragment_length [RFC6066] | CH, EE |"+
			 "| status_request [RFC6066] | CH, CR, CT |"+
			 "| supported_groups [RFC7919] | CH, EE |"+
			 "| signature_algorithms (RFC 8446) | CH, CR |"+
			 "| use_srtp [RFC5764] | CH, EE |"+
			 "| heartbeat [RFC6520] | CH, EE |"+
			 "| application_layer_protocol_negotiation [RFC7301] | CH, EE |"+
			 "| signed_certificate_timestamp [RFC6962] | CH, CR, CT |"+
			 "| client_certificate_type [RFC7250] | CH, EE |"+
			 "| server_certificate_type [RFC7250] | CH, EE |"+
			 "| padding [RFC7685] | CH |"+
			 "| key_share (RFC 8446) | CH, SH, HRR |"+
			 "| pre_shared_key (RFC 8446) | CH, SH |"+
			 "| psk_key_exchange_modes (RFC 8446) | CH |"+
			 "| early_data (RFC 8446) | CH, EE, NST |"+
			 "| cookie (RFC 8446) | CH, HRR |"+
			 "| supported_versions (RFC 8446) | CH, SH, HRR |"+
			 "| certificate_authorities (RFC 8446) | CH, CR |"+
			 "| oid_filters (RFC 8446) | CR |"+
			 "| post_handshake_auth (RFC 8446) | CH |"+
			 "| signature_algorithms_cert (RFC 8446) | CH, CR |";

	@Test
	public void testIsAllowed() throws Exception {
		ExtensionValidator v = new ExtensionValidator();
		String[] entries = ENTRIES.split("\\|");
		
		for (int i=0; i<entries.length; i += 3) {
			String name = entries[i+1].trim();
			String[] valid = entries[i+2].split(",");
			boolean[] bits = new boolean[32];
			
			name = name.substring(0, name.indexOf(' '));
			ExtensionType type = (ExtensionType) ExtensionType.class.getDeclaredField(name.toUpperCase()).get(null);
			
			for (int j=0; j<valid.length; ++j) {
				String hvalid = valid[j].trim();
				HandshakeType htype = null;
				
				if ("CH".equals(hvalid)) {
					htype = HandshakeType.CLIENT_HELLO;
				} else if ("SH".equals(hvalid)) {
					htype = HandshakeType.SERVER_HELLO;
				} else if ("EE".equals(hvalid)) {
					htype = HandshakeType.ENCRYPTED_EXTENSIONS;
				} else if ("CT".equals(hvalid)) {
					htype = HandshakeType.CERTIFICATE;
				} else if ("CR".equals(hvalid)) {
					htype = HandshakeType.CERTIFICATE_REQUEST;
				} else if ("NST".equals(hvalid)) {
					htype = HandshakeType.NEW_SESSION_TICKET;
				} else if ("HRR".equals(hvalid)) {
					htype = HandshakeType.of(6);
				} else {
					fail();
				}
				bits[htype.value()] = true;
			}
			for (int b=0; b<bits.length; ++b) {
				HandshakeType htype = HandshakeType.of(b);
				String msg = type.name() + " in " + htype.name();
				
				if (b == 6) {
					assertFalse(htype.isKnown());
					assertEquals(msg, bits[b], v.isAllowedInHelloRetryRequest(type));
					assertEquals(msg, bits[b], v.isAllowed(type, htype));
					assertTrue(v.isAllowedInHelloRetryRequest(ExtensionType.of(-1)));
					assertTrue(v.isAllowedInHelloRetryRequest(ExtensionType.of(52)));
				}
				else {
					assertEquals(msg, bits[b] || !htype.isKnown(), v.isAllowed(type, htype));
					assertTrue(v.isAllowed(ExtensionType.of(-1), htype));
					assertTrue(v.isAllowed(ExtensionType.of(52), htype));
				}
			}
			assertTrue(v.isAllowed(type, HandshakeType.of(-1)));
			assertTrue(v.isAllowed(type, HandshakeType.of(32)));
		}
	}
}
