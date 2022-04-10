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
package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Default implementation of byte buffer holder.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ByteBufferHolder implements IByteBufferHolder, List<ByteBuffer> {

	private final List<ByteBuffer> buffers;
	
	/**
	 * Constructs an empty byte buffer holder with an initial capacity of ten.
	 */
	public ByteBufferHolder() {
		buffers = new ArrayList<ByteBuffer>();
	}

	/**
	 * Constructs an empty byte buffer holder with the specified initial capacity.
	 * 
	 * @param initialCapacity the initial capacity
	 */
	public ByteBufferHolder(int initialCapacity) {
		buffers = new ArrayList<ByteBuffer>(initialCapacity);
	}

	/**
	 * Constructs a byte buffer holder containing the byte buffers of the specified
	 * collection, in the order they are returned by the collection's iterator.
	 * 
	 * @param c the collection whose byte buffers are to be placed into this byte
	 *          buffer holder
	 */
	public ByteBufferHolder(Collection<ByteBuffer> c) {
		buffers = new ArrayList<ByteBuffer>(c);
	}
	
	@Override
	public boolean hasRemaining() {
		for (ByteBuffer buffer: buffers) {
			if (buffer.hasRemaining()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int remaining() {
		int remaining = 0;
		
		for (ByteBuffer buffer: buffers) {
			remaining += buffer.remaining();
		}
		return remaining;
	}

	@Override
	public ByteBuffer[] toArray() {
		return buffers.toArray(new ByteBuffer[buffers.size()]);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return {@code false}
	 */
	@Override
	public boolean isMessage() {
		return false;
	}
	
	@Override
	public int size() {
		return buffers.size();
	}

	@Override
	public boolean isEmpty() {
		return buffers.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return buffers.contains(o);
	}

	@Override
	public Iterator<ByteBuffer> iterator() {
		return buffers.iterator();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return buffers.toArray(a);
	}

	@Override
	public boolean add(ByteBuffer e) {
		return buffers.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return buffers.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return buffers.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends ByteBuffer> c) {
		return buffers.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends ByteBuffer> c) {
		return buffers.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return buffers.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return buffers.retainAll(c);
	}

	@Override
	public void clear() {
		buffers.clear();
	}

	@Override
	public ByteBuffer get(int index) {
		return buffers.get(index);
	}

	@Override
	public ByteBuffer set(int index, ByteBuffer element) {
		return buffers.set(index, element);
	}

	@Override
	public void add(int index, ByteBuffer element) {
		buffers.add(index, element);
	}

	@Override
	public ByteBuffer remove(int index) {
		return buffers.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return buffers.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return buffers.lastIndexOf(o);
	}

	@Override
	public ListIterator<ByteBuffer> listIterator() {
		return buffers.listIterator();
	}

	@Override
	public ListIterator<ByteBuffer> listIterator(int index) {
		return buffers.listIterator(index);
	}

	@Override
	public List<ByteBuffer> subList(int fromIndex, int toIndex) {
		return buffers.subList(fromIndex, toIndex);
	}

}
