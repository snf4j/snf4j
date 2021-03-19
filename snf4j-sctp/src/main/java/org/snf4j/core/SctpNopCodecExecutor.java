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
package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.codec.IBaseDecoder;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

class SctpNopCodecExecutor implements ICodecExecutor {

	static final ICodecExecutor INSTANCE = new SctpNopCodecExecutor();
	
	private SctpNopCodecExecutor() {		
	}
	
	@Override
	public ICodecPipeline getPipeline() {
		return null;
	}

	@Override
	public void syncDecoders(ISession session) {
	}

	@Override
	public void syncEncoders(ISession session) {
	}

	@Override
	public void syncEventDrivenCodecs(ISession session) {
	}

	@Override
	public IBaseDecoder<?> getBaseDecoder() {
		return null;
	}

	@Override
	public boolean hasDecoders() {
		return false;
	}

	@Override
	public List<Object> encode(ISession session, ByteBuffer data) throws Exception {
		return null;
	}

	@Override
	public List<Object> encode(ISession session, byte[] data) throws Exception {
		return null;
	}

	@Override
	public List<Object> encode(ISession session, Object msg) throws Exception {
		return null;
	}

	@Override
	public List<Object> decode(ISession session, byte[] data) throws Exception {
		return null;
	}

	@Override
	public List<Object> decode(ISession session, ByteBuffer data) throws Exception {
		return null;
	}

	@Override
	public void event(ISession session, SessionEvent event) {
	}
	
	@Override
	public void addChild(ISession session, ICodecExecutor executor) {	
	}

}
