/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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

import org.junit.Test;
import org.snf4j.tls.IntConstantTester;

public class ExtensionTypeTest {
	
	final static String ENTRIES = 
			"|" + "server_name(0), /* RFC 6066 */" +
			"|" + "max_fragment_length(1), /* RFC 6066 */" +
			"|" + "status_request(5), /* RFC 6066 */" +
			"|" + "supported_groups(10), /* RFC 8422, 7919 */" +
			"|" + "signature_algorithms(13), /* RFC 8446 */" +
			"|" + "use_srtp(14), /* RFC 5764 */" +
			"|" + "heartbeat(15), /* RFC 6520 */" +
			"|" + "application_layer_protocol_negotiation(16), /* RFC 7301 */" +
			"|" + "signed_certificate_timestamp(18), /* RFC 6962 */" +
			"|" + "client_certificate_type(19), /* RFC 7250 */" +
			"|" + "server_certificate_type(20), /* RFC 7250 */" +
			"|" + "padding(21), /* RFC 7685 */" +
			"|" + "pre_shared_key(41), /* RFC 8446 */" +
			"|" + "early_data(42), /* RFC 8446 */" +
			"|" + "supported_versions(43), /* RFC 8446 */" +
			"|" + "cookie(44), /* RFC 8446 */" +
			"|" + "psk_key_exchange_modes(45), /* RFC 8446 */" +
			"|" + "certificate_authorities(47), /* RFC 8446 */" +
			"|" + "oid_filters(48), /* RFC 8446 */" +
			"|" + "post_handshake_auth(49), /* RFC 8446 */" +
			"|" + "signature_algorithms_cert(50), /* RFC 8446 */" +
			"|" + "key_share(51), /* RFC 8446 */";
	
	@Test
	public void testValues() throws Exception {
		new IntConstantTester<ExtensionType>(ENTRIES, ExtensionType.class, ExtensionType[].class).assertValues();
	}
			 
	@Test
	public void testOf() throws Exception {
		new IntConstantTester<ExtensionType>(ENTRIES, ExtensionType.class, ExtensionType[].class).assertOf();
	}
}
