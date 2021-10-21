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

import org.snf4j.core.session.ISessionPipeline;
import org.snf4j.core.session.IStreamSession;

class StreamSessionPipeline extends SessionPipeline<StreamSession> implements ISessionPipeline<IStreamSession> {

	StreamSessionPipeline(StreamSession owner) {
		super(owner);
	}

	StreamSession check(IStreamSession session) {
		if (session instanceof StreamSession) {
			return (StreamSession) session;
		}
		throw new IllegalArgumentException("session is not an instance of StreamSession class");
	}
	
	@Override
	public void addFirst(Object key, IStreamSession session) {
		super.addFirst(key, check(session));
	}

	@Override
	public void addAfter(Object baseKey, Object key, IStreamSession session) {
		super.addAfter(baseKey, key, check(session));
	}

	@Override
	public void add(Object key, IStreamSession session) {
		super.add(key, check(session));
	}

	@Override
	public void addBefore(Object baseKey, Object key, IStreamSession session) {
		super.addBefore(baseKey, key, check(session));
	}

	@Override
	public IStreamSession replace(Object oldKey, Object key, IStreamSession session) {
		return super.replace(oldKey, key, check(session));
	}
}
