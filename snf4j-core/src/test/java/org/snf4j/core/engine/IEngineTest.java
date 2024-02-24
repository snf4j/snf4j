/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.core.engine;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.snf4j.core.handler.SessionIncidentException;

public class IEngineTest {

	@Test
	public void testTimer() throws Exception {
		new Engine().timer(null, null);
	}
	
	static class Engine implements IEngine {

		@Override
		public void init() {
		}

		@Override
		public void cleanup() {
		}

		@Override
		public void beginHandshake() throws Exception {
		}

		@Override
		public boolean isOutboundDone() {
			return false;
		}

		@Override
		public boolean isInboundDone() {
			return false;
		}

		@Override
		public void closeOutbound() {
		}

		@Override
		public void closeInbound() throws SessionIncidentException {
		}

		@Override
		public int getMinApplicationBufferSize() {
			return 0;
		}

		@Override
		public int getMinNetworkBufferSize() {
			return 0;
		}

		@Override
		public int getMaxApplicationBufferSize() {
			return 0;
		}

		@Override
		public int getMaxNetworkBufferSize() {
			return 0;
		}

		@Override
		public HandshakeStatus getHandshakeStatus() {
			return null;
		}

		@Override
		public Object getSession() {
			return null;
		}

		@Override
		public Runnable getDelegatedTask() {
			return null;
		}

		@Override
		public IEngineResult wrap(ByteBuffer[] srcs, ByteBuffer dst) throws Exception {
			return null;
		}

		@Override
		public IEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
			return null;
		}

		@Override
		public IEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
			return null;
		}
		
	}
}
