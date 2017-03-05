/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017 SNF4J contributors
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
package org.snf4j.core.session;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.snf4j.core.IdentifiableObject;

/**
 * Base implementation of the {@link ISession} interface.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
abstract public class AbstractSession extends IdentifiableObject implements ISession {

	private volatile ConcurrentMap<Object,Object> attributes;

	private final Object attributesLock = new Object();

	/**
	 * Constructs the base implementation of the {@link ISession} interface.
	 *
	 * @param prefix
	 *            the prefix used to generate string representation of this
	 *            object
	 * @param id
	 *            the id of the object
	 * @param name
	 *            the name of the object or <code>null</code> if the name should
	 *            be auto generated
	 * @param attributes
	 *            the attributes for this session, or <code>null</code> if this
	 *            session should have its own copy of attributes
	 */
	protected AbstractSession(String prefix, long id, String name, ConcurrentMap<Object,Object> attributes) {
		super(prefix, id, name);
		this.attributes = attributes;
	}

	@Override
	public ConcurrentMap<Object, Object> getAttributes() {
		ConcurrentMap<Object, Object> attributes = this.attributes;
		
		if (attributes == null) {
			synchronized (attributesLock) {
				if (this.attributes == null) {
					attributes = new ConcurrentHashMap<Object, Object>();
					this.attributes = attributes;
				}
				else {
					attributes = this.attributes;
				}
			}
		}
		return attributes;
	}
	
}
