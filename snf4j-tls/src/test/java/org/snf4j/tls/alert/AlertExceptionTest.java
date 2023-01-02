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
package org.snf4j.tls.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.Test;

public class AlertExceptionTest {

	@Test
	public void testAll() {
		AlertException e = new AlertException("Message1", AlertLevel.WARNING, AlertDescription.ACCESS_DENIED);
		assertEquals("Message1", e.getMessage());
		assertSame(AlertLevel.WARNING, e.getLevel());
		assertSame(AlertDescription.ACCESS_DENIED, e.getDescription());

		e = new AlertException("Message1", AlertDescription.ACCESS_DENIED);
		assertEquals("Message1", e.getMessage());
		assertSame(AlertLevel.FATAL, e.getLevel());
		assertSame(AlertDescription.ACCESS_DENIED, e.getDescription());

		Exception cause = new Exception();
		e = new AlertException("Message1", AlertLevel.WARNING, AlertDescription.ACCESS_DENIED, cause);
		assertEquals("Message1", e.getMessage());
		assertSame(AlertLevel.WARNING, e.getLevel());
		assertSame(AlertDescription.ACCESS_DENIED, e.getDescription());
		assertSame(cause, e.getCause());
		
		e = new AlertException("Message1", AlertDescription.ACCESS_DENIED, cause);
		assertEquals("Message1", e.getMessage());
		assertSame(AlertLevel.FATAL, e.getLevel());
		assertSame(AlertDescription.ACCESS_DENIED, e.getDescription());
		assertSame(cause, e.getCause());
	}
	
	static void assertAlert(AlertException e, String msg, Throwable cause) throws Exception {
		assertSame(AlertLevel.FATAL, e.getLevel());
		assertEquals(msg, e.getMessage());
		assertEquals(cause, e.getCause());
		
		String name = e.getClass().getSimpleName().toUpperCase();
		
		for (Field field: AlertDescription.class.getDeclaredFields()) {
			String fname = field.getName().replace("_", "");
			
			if (name.startsWith(fname + "ALERT")) {
				assertSame(field.get(null), e.getDescription());
				return;
			}
		}
		fail();
	}
	
	@Test
	public void testImplementations() throws Exception {
		Exception cause = new Exception();
		AlertException[] alerts = new AlertException[] {
				new InternalErrorAlertException("Text1",cause)
		};
		for (AlertException alert: alerts) {
			assertAlert(alert, "Text1", cause);
		}
		
		alerts = new AlertException[] {
				new InternalErrorAlertException("Text1"),
				new DecodeErrorAlertException("Text1"),
				new UnsupportedExtensionAlertException("Text1"),
				new HandshakeFailureAlertException("Text1"),
				new IllegalParameterAlertException("Text1"),
				new MissingExtensionAlertException("Text1"),
				new ProtocolVersionAlertException("Text1"),
				new UnexpectedMessageAlertException("Text1")
		};
		for (AlertException alert: alerts) {
			assertAlert(alert, "Text1", null);
		}
	}
}
