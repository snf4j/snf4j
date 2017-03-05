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
package org.snf4j.core;

/**
 * Base class for objects that are identified by an id and their name.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class IdentifiableObject {
	
	private final long id;

	private final String name;
	
	private final String fullName;
	
	/**
	 * 
	 * Constructs an identifiable object with the specified prefix, id and name.
	 * 
	 * @param prefix
	 *            the prefix used to generate string representation of this
	 *            object
	 * @param id
	 *            the id of the object
	 * @param name
	 *            the name of the object or <code>null</code> if the name should
	 *            be auto generated
	 */
	protected IdentifiableObject(String prefix, long id, String name) {
		this.id = id;
		fullName = prefix + ( name != null ? name : Long.toString(id));
		this.name = name != null ? name : fullName;
	}

	/**
	 * Returns a string representation of this object. It can be formatted in
	 * the following ways:
	 * <p>
	 * a. if the name argument passed to the constructor was specified
	 * 
	 * <pre>
	 * return prefix + name;
	 * </pre>
	 * 
	 * b. if the name argument passed to the constructor was <code>null</code>
	 * 
	 * <pre>
	 * return prefix + Long.toString(id);
	 * </pre>
	 * 
	 * @return a string representation of this object
	 */
	@Override
	public String toString() {
		return fullName;
	}
	
	/**
	 * Returns the unique id of this object. The id is auto generated during the
	 * construction of the object.
	 * 
	 * @return the unique id of this object
	 */
	public final long getId() {
		return id;
	}
	
	/**
	 * Returns the name of this object. If the name passed to the constructor
	 * was <code>null</code> it returns auto generated name that is formatted in
	 * the following way:
	 * 
	 * <pre>
	 * return prefix + Long.toString(id);
	 * </pre>
	 * @return the name of this object
	 */
	public final String getName() {
		return name;
	}
}
