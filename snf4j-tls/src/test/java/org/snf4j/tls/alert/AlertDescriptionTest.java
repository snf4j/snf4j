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
package org.snf4j.tls.alert;

import org.junit.Test;
import org.snf4j.tls.IntConstantTester;

public class AlertDescriptionTest {
	
	final static String ENTRIES = 
			"|" + "close_notify(0)," +
			"|" + "unexpected_message(10)," +
			"|" +  "bad_record_mac(20)," +
			"|" + "record_overflow(22)," +
			"|" + "handshake_failure(40)," +
			"|" + "bad_certificate(42)," +
			"|" + "unsupported_certificate(43)," +
			"|" + "certificate_revoked(44)," +
			"|" + "certificate_expired(45)," +
			"|" + "certificate_unknown(46)," +
			"|" + "illegal_parameter(47)," +
			"|" + "unknown_ca(48)," +
			"|" + "access_denied(49)," +
			"|" + "decode_error(50)," +
			"|" + "decrypt_error(51)," +
			"|" + "protocol_version(70)," +
			"|" + "insufficient_security(71)," +
			"|" + "internal_error(80)," +
			"|" + "inappropriate_fallback(86)," +
			"|" + "user_canceled(90)," +
			"|" + "missing_extension(109)," +
			"|" + "unsupported_extension(110)," +
			"|" + "unrecognized_name(112)," +
			"|" + "bad_certificate_status_response(113)," +
			"|" + "unknown_psk_identity(115)," +
			"|" + "certificate_required(116)," +
			"|" + "no_application_protocol(120),";

	@Test
	public void testValues() throws Exception {
		new IntConstantTester<AlertDescription>(ENTRIES, AlertDescription.class, AlertDescription[].class).assertValues();
	}
			 
	@Test
	public void testOf() throws Exception {
		new IntConstantTester<AlertDescription>(ENTRIES, AlertDescription.class, AlertDescription[].class).assertOf();
	}
	
}
