package org.snf4j.core;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.snf4j.core.allocator.IByteBufferAllocator;

class SctpFragments {
	
	enum State {EMPTY, SINGLE, MULTI}
	
	private State state = State.EMPTY;
	
	private ByteBuffer fragment;

	private long fragmentKey = -1;
	
	private Map<Long, ByteBuffer> fragments;
	
	private final IByteBufferAllocator allocator;
	
	private final int minCapacity;
	
	private final int maxCapacity;
	
	private final boolean optimize;
	
	private final boolean release;
	
	SctpFragments(IByteBufferAllocator allocator, int minCapacity, int maxCapacity, boolean optimize) {
		this.allocator = allocator;
		this.minCapacity = minCapacity;
		this.maxCapacity = maxCapacity;
		this.optimize = optimize;
		release = allocator.isReleasable();
	}
	
	/**
	 * Stores incomplete buffer
	 * 
	 * @param fragmentKey stream number 
	 * @param msg input buffer (not flipped yet)
	 * @return input buffer (can be null)
	 */
	ByteBuffer store(long fragmentKey, ByteBuffer msg) {
		switch (state) {
		case EMPTY:
			if (optimize) {
				fragment = msg;
				msg = null;
			}
			else {
				if (fragment == null) {
					fragment = allocator.allocate(msg.capacity());
				}
				else {
					fragment.clear();
				}
				msg.flip();
				fragment.put(msg);
				msg.clear();
			}
			this.fragmentKey = fragmentKey;
			state = State.SINGLE;
			break;
			
		case SINGLE:
			if (this.fragmentKey == fragmentKey) {
				fragment = allocator.ensure(fragment, msg.position(), minCapacity, maxCapacity);
				msg.flip();
				fragment.put(msg);
				if (optimize) {
					allocator.release(msg);
					msg = null;
				}
				else {
					msg.clear();
				}
				break;
			}
			fragments = new HashMap<Long, ByteBuffer>();
			fragments.put((long) this.fragmentKey, fragment);
			fragment = null;
			this.fragmentKey = -1;
			state = State.MULTI;
			
		case MULTI:
			ByteBuffer buf = fragments.get(fragmentKey);
			
			if (buf == null) {
				if (optimize) {
					fragments.put(fragmentKey, msg);
					return null;
				}
				else {
					buf = allocator.allocate(msg.capacity());
					fragments.put(fragmentKey, buf);
				}
			}
			else {
				int size = msg.position();
				
				if (size > buf.remaining()) {
					buf = allocator.ensure(buf, msg.position(), minCapacity, maxCapacity);
					fragments.put(fragmentKey, buf);
				}
			}
			msg.flip();
			buf.put(msg);
			if (optimize) {
				allocator.release(msg);
				msg = null;
			}
			else {
				msg.clear();
			}
			break;
		}
		return msg;
	}
	
	/**
	 * Returns complete buffer
	 * 
	 * @param fragmentKey stream number
	 * @param msg          input buffer (not flipped yet)
	 * @return complete buffer (not flipped yet)
	 */
	ByteBuffer complete(long fragmentKey, ByteBuffer msg) {
		ByteBuffer buf;
		
		switch (state) {
		case EMPTY:
			break;
			
		case SINGLE:
			if (fragmentKey != this.fragmentKey) {
				break;
			}
			buf = allocator.ensure(fragment, msg.position(), minCapacity, maxCapacity);
			msg.flip();
			buf.put(msg);
			if (release) {
				allocator.release(msg);
				fragment = null;
			}
			else {
				fragment = msg;
			}
			this.fragmentKey = -1;
			state = State.EMPTY;
			msg = buf;
			break;
			
		case MULTI:
			buf = fragments.remove(fragmentKey);
			if (buf == null) {
				break;
			}
			buf = allocator.ensure(buf, msg.position(), minCapacity, maxCapacity);
			msg.flip();
			buf.put(msg);
			if (release) {
				allocator.release(msg);
			}
			msg = buf;
			break;
			
		}
		return msg;
	}
	
	void release() {
		if (release) {
			if (fragment != null) {
				allocator.release(fragment);
			}
			if (fragments != null) {
				for (Iterator<ByteBuffer> i = fragments.values().iterator(); i.hasNext();) {
					allocator.release(i.next());
				}
			}
		}
		if (fragments != null) {
			fragments.clear();
			fragments = null;
		}
		fragment = null;
		fragmentKey = -1;
		state = State.EMPTY;
	}
	
}
