/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.handshake;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.IExtensionDecoder;

public class Finished extends KnownHandshake implements IFinished {

	private final static HandshakeType TYPE = HandshakeType.FINISHED;
	
	private final byte[] verifyData;
	
	private final static AbstractHandshakeParser PARSER = new AbstractHandshakeParser() {

		@Override
		public HandshakeType getType() {
			return TYPE;
		}

		@Override
		public IHandshake parse(ByteBufferArray srcs, int remaining, IExtensionDecoder decoder) throws Alert {
			byte[] verifyData = new byte[remaining];
			
			srcs.get(verifyData);
			return new Finished(verifyData);
		}
		
	};
	public Finished(byte[] verifyData) {
		super(TYPE);
		Args.checkNull(verifyData, "verifyData");
		this.verifyData = verifyData;
	}

	@Override
	public int getDataLength() {
		return verifyData.length;
	}

	@Override
	public List<IExtension> getExtensioins() {
		return null;
	}

	public static IHandshakeParser getParser() {
		return PARSER;
	}
	
	@Override
	public byte[] getVerifyData() {
		return verifyData;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.put(verifyData);
	}
}
