/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2018 SNF4J contributors
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
package org.snf4j.core;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class EventTypeTest {

	private int init(EventType[] types) {
		int bits = 0;
		
		for (int i=0; i<types.length; ++i) {
			bits |= types[i].bitMask();
		}
		return bits;
	}
	
	private EventType[] toArray(String types) {
		if (types.isEmpty()) {
			return new EventType[0];
		}
		
		String[] atypes = types.split("\\|");
		EventType[] result = new EventType[atypes.length];
		
		for (int i=0; i<result.length; ++i) {
			EventType type = EventType.valueOf(atypes[i]);
			
			assertNotNull(type);
			result[i] = type;
		}
		return result;
	}
	
	private void assertIsValid(String init, String valid) {
		int bits = init(toArray(init));
		
		EventType[] validtypes = toArray(valid);
		for (int i=0; i<validtypes.length; ++i) {
			EventType type = validtypes[i];
			
			if (type == null) {
				continue;
			}
			assertTrue(init + " is valid " + type, type.isValid(bits));
		}
		EventType[] types = EventType.values();
		
		for (int i=0; i<types.length; ++i) {
			boolean isValid = false;
			
			for(EventType type: validtypes) {
				if (type == types[i]) {
					isValid = true;
					break;
				}
			}
			
			if (!isValid) {
				assertFalse(init + " is not valid " + types[i], types[i].isValid(bits));
			}
		}
	}
	
	@Test
	public void testIsValid() {
		assertIsValid("", "SESSION_CREATED");
		
		assertIsValid("SESSION_CREATED","SESSION_OPENED|SESSION_ENDING|EXCEPTION_CAUGHT");

		assertIsValid("SESSION_CREATED|SESSION_OPENED","SESSION_READY|DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_ENDING","");
		assertIsValid("SESSION_CREATED|EXCEPTION_CAUGHT","SESSION_OPENED|SESSION_ENDING|EXCEPTION_CAUGHT");

		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY","DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|DATA_RECEIVED","SESSION_READY|DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|DATA_SENT","SESSION_READY|DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|DATA_SENT|DATA_RECEIVED","SESSION_READY|DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_CLOSED","SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_ENDING","");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|EXCEPTION_CAUGHT","SESSION_READY|DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		
		assertIsValid("SESSION_CREATED|EXCEPTION_CAUGHT|SESSION_ENDING","");
		
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|DATA_RECEIVED","DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|DATA_SENT","DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|DATA_SENT|DATA_RECEIVED","DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|SESSION_CLOSED","SESSION_ENDING|EXCEPTION_CAUGHT");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|SESSION_ENDING","");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|EXCEPTION_CAUGHT","DATA_RECEIVED|DATA_SENT|SESSION_CLOSED|SESSION_ENDING|EXCEPTION_CAUGHT");
		
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|SESSION_CLOSED|SESSION_ENDING","");
		assertIsValid("SESSION_CREATED|SESSION_OPENED|SESSION_READY|SESSION_CLOSED|EXCEPTION_CAUGHT","SESSION_ENDING|EXCEPTION_CAUGHT");
	}
}
