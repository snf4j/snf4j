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

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

class SessionPipeline<T extends InternalSession> {

	private final static Item<?>[] EMPTY = new Item<?>[0];
	
	private final T owner;

	private final LinkedList<Item<T>> items = new LinkedList<Item<T>>();
	
	private int itemsVersion;
	
	private int version;
	
	private Item<T> first;
	
	private Item<T> last;

	private volatile boolean eos;
	
	private volatile Throwable cause;
	
	private volatile boolean undone;
	
	private volatile Throwable undoneCause;
	
	SessionPipeline(T owner) {
		this.owner = owner;
	}
	
	void sync(Item<T> current) {
		synchronized (items) {
			if (itemsVersion == version || (current != null && !items.contains(current))) {
				return;
			}
			
			Iterator<Item<T>> i = items.iterator();
			Item<T> item = first;
			
			while (item != null) {
				if (!items.contains(item)) {
					item.unlink();
				}
				item = item.next;
			}
			
			if (!i.hasNext()) {
				first = last = null;
			}
			else {
				first = last = i.next();
				first.next = null;
				first.prev = null;
				while (i.hasNext()) {
					item = i.next();
					item.next = null;
					item.prev = last;
					last.next = item;
					last = item;
				}
			}
			version = itemsVersion;
		}
	}

	T first() {
		sync(null);
		return first == null ? null : first.session;
	}
	
	private int indexOf(Object key) {
		int index = 0;
		
		for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
			if (i.next().key.equals(key)) {
				return index;
			}
			++index;
		}		
		throw new NoSuchElementException("key '" + key +"' does not exist");
	}
	
	private void notExists(Object key) {
		for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
			if (i.next().key.equals(key)) {
				throw new IllegalArgumentException("key '" + key +"' already exists");
			}
		}		
	}

	private void notExists(T session, Item<T> ignore) {
		if (session == owner) {
			throw new IllegalArgumentException("session is owner");
		}
		for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
			Item<T> item = i.next();
			
			if (item != ignore && item.session == session) {
				throw new IllegalArgumentException("session already exists");
			}
		}		
	}
	
	private void notNull(Object argument, String argName) {
		if (argument == null) {
			throw new NullPointerException(argName + " is null");
		}
	}
	
	private Item<T> item(Object key, T session) {
		notExists(key);
		return new Item<T>(key, session, this);
	}
	
	void addFirst(Object key, T session) {
		notNull(key, "key");
		notNull(session, "session");
		synchronized (items) {
			notExists(session, null);
			items.addFirst(item(key, session));
			session.setPipeline(this);
			++itemsVersion;
		}
	}

	void addAfter(Object baseKey, Object key, T session) {
		notNull(baseKey, "baseKey");
		notNull(key, "key");
		notNull(session, "session");
		synchronized (items) {
			notExists(session, null);
			items.add(indexOf(baseKey)+1, item(key, session));
			session.setPipeline(this);
			++itemsVersion;
		}
	}
	
	void add(Object key, T session) {
		notNull(key, "key");
		notNull(session, "session");
		synchronized (items) {
			notExists(session, null);
			items.addLast(item(key, session));
			session.setPipeline(this);
			++itemsVersion;
		}
	}

	void addBefore(Object baseKey, Object key, T session) {
		notNull(baseKey, "baseKey");
		notNull(key, "key");
		notNull(session, "session");
		synchronized (items) {
			notExists(session, null);
			items.add(indexOf(baseKey), item(key, session));
			session.setPipeline(this);
			++itemsVersion;
		}
	}
	
	T replace(Object oldKey, Object key, T session) {
		notNull(oldKey, "oldKey");
		notNull(key, "key");
		notNull(session, "session");
		synchronized (items) {
			int i = indexOf(oldKey);
			Item<T> item, oldItem;
			
			notExists(session, items.get(i));
			if (oldKey.equals(key)) {
				oldItem = items.remove(i);
				item = item(key, session);
			}
			else {
				item = item(key, session);
				oldItem = items.remove(i);
			}
			items.add(i, item);
			session.setPipeline(this);
			++itemsVersion;
			return oldItem.session;
		}
	}
	
	public T remove(Object key) {
		notNull(key, "key");
		synchronized (items) {
			for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
				Item<T> item = i.next();
				
				if (item.key.equals(key)) {
					i.remove();
					++itemsVersion;
					return item.session;
				}
			}
		}
		return null;
	}
	
	public T get(Object key) {
		notNull(key, "key");
		synchronized (items) {
			for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
				Item<T> item = i.next();

				if (item.key.equals(key)) {
					return item.session;
				}
			}			
		}
		return null;
	}

	public T getOwner() {
		return owner;
	}
	
	public List<Object> getKeys() {
		List<Object> keys = new ArrayList<Object>();
		
		synchronized (items) {
			for (Iterator<Item<T>> i = items.iterator(); i.hasNext();) {
				keys.add(i.next().key);
			}			
		}
		return keys;
	}
	
	public void markClosed() {
		eos = true;
	}
	
	public void markClosed(Throwable cause) {
		this.cause = cause;
		eos = true;
	}

	public void markUndone() {
		markUndone(null);
	}
	
	public void markUndone(Throwable cause) {
		undone = true;
		undoneCause = cause;
	}
	
	public void markDone() {
		undone = false;
		undoneCause = null;
	}

	private void close0(InternalSession current, StoppingType type) {
		switch (type) {
		case GENTLE:
			current.close();
			break;
			
		case QUICK:
			current.quickClose();
			break;
			
		case DIRTY:
			current.dirtyClose();
			break;
		}
	}
	
	private void close0(StoppingType type) {
		Item<T> item = first;
		T current = owner;
		
		while (item != null) {
			T session = item.session();
			SelectionKey key = session.key;
			
			if (key != null && ((ChannelContext<?>)key.attachment()).getSession() == session) {
				current = session;
				break;
			}
			item = item.next;
		}
		close0(current, type);
	}
	
	private void close(final StoppingType type) {
		T session = null;
		Item<?>[] itemArray = EMPTY;
		
		markClosed();
		if (owner.loop != null) {
			session = owner;
		}
		else {
			synchronized (items) {
				Item<T> item = first;

				while (item != null) {
					if (item.session().loop != null) {
						session = item.session();
						break;
					}
					item = item.next;
				}
				if (session == null && !items.isEmpty()) {
					itemArray = items.toArray(new Item<?>[items.size()]);
				}
			}
		}
		
		if (session != null) {
			session.execute(new Runnable() {

				@Override
				public void run() {
					close0(type);
				}
			});
		}
		else {
			for (Item<?> item: itemArray) {
				close0(item.session(), type);
			}
			close0(owner, type);
		}
	}
	
	public void close() {
		close(StoppingType.GENTLE);
	}	

	public void quickClose() {
		close(StoppingType.QUICK);
	}	
	
	public void dirtyClose() {
		close(StoppingType.DIRTY);
	}
	
	static class Item<T extends InternalSession> {

		private final T session;
		
		private final Object key;
		
		private final SessionPipeline<T> pipeline;
		
		Item<T> next;
		
		Item<T> prev;
		
		Item(Object key, T session, SessionPipeline<T> pipeline) {
			this.session = session;
			this.key = key;
			this.pipeline = pipeline;
			session.pipelineItem = this;
		}
		
		T owner() {
			return pipeline.owner;
		}
		
		Throwable cause() {
			Throwable cause = pipeline.cause;
			
			return cause != null ? cause : pipeline.undoneCause;
		}
		
		void cause(Throwable cause) {
			pipeline.cause = cause;
		}
		
		T session() {
			return session;
		}
		
		T next() {
			pipeline.sync(this);
			Item<T> next = this.next;
			
			return next == null ? owner() : next.session;
		}
		
		void markEos() {
			pipeline.eos = true;
		}
		
		boolean canClose() {
			return pipeline.eos || pipeline.cause != null || pipeline.undone;
		}
		
		void unlink() {
			if (session.pipelineItem == this) {
				session.setPipeline(null);
				session.pipelineItem = null;
			}
		}
	}
}
