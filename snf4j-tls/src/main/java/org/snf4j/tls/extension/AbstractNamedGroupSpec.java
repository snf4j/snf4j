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

import java.nio.ByteBuffer;

import org.snf4j.tls.alert.DecodeErrorAlertException;
import org.snf4j.tls.alert.InternalErrorAlertException;

abstract public class AbstractNamedGroupSpec implements INamedGroupSpec {

	protected DecodeErrorAlertException decodeError(String message) {
		return new DecodeErrorAlertException("Extension 'key_share' parsing failure: " + message);
	}

	protected InternalErrorAlertException internalError(String message, Throwable cause) {
		return new InternalErrorAlertException("Extension 'key_share' internal failure: " + message, cause);
	}
	
	static protected void getDataWithLeftPadding(ByteBuffer buffer, byte[] data, int length) {
		int dataLen = data.length;
		
		if (dataLen == length) {
			buffer.put(data);
		}
		else {
			int padding = dataLen - length;
			
			if (padding > 0) {
				for (int i=0; i<padding; ++i) {
					if (data[i] != 0) {
						throw new IllegalArgumentException("Data too big for padding");
					}
				}
				buffer.put(data, padding, length);
			}
			else {
				buffer.put(new byte[-padding]);
				buffer.put(data);
			}
		}
	}

	static protected void getDataWithRightPadding(ByteBuffer buffer, byte[] data, int length) {
		int dataLen = data.length;
		
		if (dataLen == length) {
			buffer.put(data);
		}
		else {
			int padding = dataLen - length;
			
			if (padding > 0) {
				for (int i=0; i<padding; ++i) {
					if (data[length+i] != 0) {
						throw new IllegalArgumentException("Data too big for padding");
					}
				}
				buffer.put(data, 0, length);
			}
			else {
				buffer.put(data);
				buffer.put(new byte[-padding]);
			}
		}
	}
	
}
