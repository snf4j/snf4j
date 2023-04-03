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
package org.snf4j.tls.session;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class SessionCache<K> {
	
	private final Map<K, CacheEntry<K>> cache;
	
	private final ReferenceQueue<ISession> queue;

	private int limit;
	
	private long lifetime;
	
	public SessionCache(int limit, long lifetime) {
		this.limit = limit;
		this.lifetime = lifetime;
		queue = new ReferenceQueue<ISession>();
		cache = new LinkedHashMap<K, CacheEntry<K>>();
	}

	public int size() {
		return size(System.currentTimeMillis());
	}

	public int size(long currentTime) {
		refreshExpired(currentTime);
		return cache.size();
	}
	
	public void clear() {
		for (CacheEntry<K> entry: cache.values()) {
			entry.invalidate();
		}
		while(queue.poll() != null);
		cache.clear();
	}

	public void put(K key, ISession session) {
		put(key, session, System.currentTimeMillis());
	}
	
	public void put(K key, ISession session, long currentTime) {
		refresh();
		
		long expirationTime = (lifetime == 0) ? 0 : currentTime + lifetime;
		
		CacheEntry<K> newEntry = new SoftCacheEntry<K>(key, session, expirationTime, queue);
		CacheEntry<K> oldEntry = cache.put(key, newEntry);
		
		if (oldEntry != null) {
			oldEntry.invalidate();
		}
		else if (limit > 0 && cache.size() > limit) {
			refreshExpired(currentTime);
			if (cache.size() > limit) {
				Iterator<CacheEntry<K>> i = cache.values().iterator();
				CacheEntry<K> entry = i.next();
				
				i.remove();
				entry.invalidate();
			}
		}
	}

	public ISession get(K key) {
		return get(key, System.currentTimeMillis());
	}
	
	public ISession get(K key, long currentTime) {
		refresh();
		
		CacheEntry<K> entry = cache.get(key);
		
		if (entry != null) {
			if (lifetime == 0) {
				currentTime = 0;
			}
			if (entry.isValid(currentTime)) {
				return entry.getSession();
			}
			cache.remove(key);
			entry.invalidate();
		}
		return null;
	}
	
	public void remove(K key) {
		refresh();
		
		CacheEntry<K> entry = cache.remove(key);
		
		if (entry != null) {
			entry.invalidate();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void refresh() {
		CacheEntry<K> entry;
		
		while ((entry = (CacheEntry<K>) queue.poll()) != null) {
			K key = entry.getKey();
			
			if (key == null) {
				continue;
			}
			cache.remove(key, entry);
		}
	}
	
	private void refreshExpired(long currentTime) {
		refresh();
		
		if (lifetime == 0) {
			return;
		}
		
		for (Iterator<CacheEntry<K>> i = cache.values().iterator(); i.hasNext();) {
			CacheEntry<K> entry = i.next();
			
			if (!entry.isValid(currentTime)) {
				i.remove();
			}
		}
	}
	
	static interface CacheEntry<K> {

        boolean isValid(long currentTime);

        void invalidate();

        K getKey();

        ISession getSession();
    }
	
	static class SoftCacheEntry<K> extends SoftReference<ISession> implements CacheEntry<K> {

		private K key;
		
		private long expirationTime;
		
		public SoftCacheEntry(K key, ISession session, long expirationTime, ReferenceQueue<ISession> queue) {
			super(session, queue);
			this.key = key;
			this.expirationTime = expirationTime;
		}

		@Override
		public boolean isValid(long currentTime) {
            boolean valid = (currentTime <= expirationTime) && (get() != null);
            if (!valid) {
                invalidate();
            }
            return valid;
 		}

		@Override
		public void invalidate() {
			clear();
			key = null;
			expirationTime = -1;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public ISession getSession() {
			return get();
		}	
	}
}
