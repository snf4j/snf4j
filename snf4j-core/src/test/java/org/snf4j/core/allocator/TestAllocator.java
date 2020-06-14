/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2018-2020 SNF4J contributors
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
package org.snf4j.core.allocator;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestAllocator extends DefaultAllocator {
	
	final List<ByteBuffer> buffers = Collections.synchronizedList(new ArrayList<ByteBuffer>());
	
	final List<ByteBuffer> released = Collections.synchronizedList(new ArrayList<ByteBuffer>());
	
	final AtomicInteger allocated = new AtomicInteger(0);
	
	final boolean releasable;
	
	public boolean ensureException;
	
	public TestAllocator(boolean direct, boolean releasable) {
		super(direct);
		this.releasable = releasable;
	}

	@Override
	public boolean isReleasable() {
		return releasable;
	}
	
	public int getSize() {
		return buffers.size();
	}
	
	public List<ByteBuffer> getReleased() {
		return released;
	}
	
	public int getReleasedCount() {
		return released.size();
	}

	public int getAllocatedCount() {
		return allocated.get();
	}
	
	@Override
	public void release(ByteBuffer buffer) {
		super.release(buffer);
		buffers.remove(buffer);
		released.add(buffer);
	}

	@Override
	public ByteBuffer allocate(int capacity) {
		ByteBuffer b = super.allocate(capacity);
		buffers.add(b);
		allocated.incrementAndGet();
		return b;
	}	
	
	@Override
	public ByteBuffer ensureSome(ByteBuffer buffer, int minCapacity, int maxCapacity) {
		ByteBuffer b = super.ensureSome(buffer, minCapacity, maxCapacity);
		if (b != buffer) {
			buffers.remove(buffer);
			buffers.add(b);
		}
		return b;
	}	
	
	@Override
	public ByteBuffer ensure(ByteBuffer buffer, int size, int minCapacity, int maxCapacity) {
		if (ensureException) {
			throw new IndexOutOfBoundsException();
		}
		ByteBuffer b = super.ensure(buffer, size, minCapacity, maxCapacity);
		if (b != buffer) {
			buffers.remove(buffer);
			buffers.add(b);
		}
		return b;
	}
	
	@Override
	public ByteBuffer reduce(ByteBuffer buffer, int minCapacity) {
		ByteBuffer b = super.reduce(buffer, minCapacity);
		if (b != buffer) {
			buffers.remove(buffer);
			buffers.add(b);
		}
		return b;
	}
	
	@Override
	public ByteBuffer extend(ByteBuffer buffer, int maxCapacity) {
		ByteBuffer b = super.extend(buffer, maxCapacity);
		if (b != buffer) {
			buffers.remove(buffer);
			buffers.add(b);
		}
		return b;
	}	
}
