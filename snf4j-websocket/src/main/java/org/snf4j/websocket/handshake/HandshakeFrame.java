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
package org.snf4j.websocket.handshake;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Web Socket handshake frames.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class HandshakeFrame {
	
	private final Map<String,String> values = new HashMap<String,String>();
	
	private final List<String> names = new ArrayList<String>();
	
	private StringBuilder pendingName;
	
	private String lastKey;
	
	private int length;
	
	/**
	 * Construct a base Web Socket handshake frame.
	 */
	protected HandshakeFrame() {
	}
	
	/**
	 * Returns the value of the header field identified by the specified name.
	 * 
	 * @param name a case insensitive name of the header field which value should be
	 *             returned.
	 * @return the value of the header field or {@code null} if the header field
	 *         could not be found
	 */
	public String getValue(String name) {
		return values.get(key(name));
	}

	/**
	 * Adds a value of the header field identified by the specified name.
	 * <p>
	 * If the header field already exists the value will be appended to the existing
	 * value and the string {@code ", "} will be used to separate the appended value
	 * from the existing one. 
	 * 
	 * @param name  a case insensitive name of the header field which value should
	 *              be added.
	 * @param value the value to be added
	 */
	public void addValue(String name, String value) {
		if (pendingName != null) {
			pendingName.append(name);
			name = pendingName.toString();
			pendingName = null;
		}
		lastKey = key(name);
		String oldValue = values.get(lastKey);
		
		if (oldValue != null) {
			length -= oldValue.length();
			value = oldValue + ", " + value;
		}
		length += value.length();
		if (values.put(lastKey, value) == null) {
			length += name.length()+2+2;
			names.add(name);
		}
	}
	
	void appendValue(String value) throws InvalidHandshakeException {
		if (lastKey == null) {
			throw new InvalidHandshakeException("No header field to extend");
		}
		length += value.length();
		value = values.get(lastKey) + value;
		values.put(lastKey, value);
	}
	
	/**
	 * Checks if the header field identified by the specified name exists.
	 * 
	 * @param name a case insensitive name of the header field to check.
	 * @return {@code true} if the header field exists
	 */
	public boolean hasValue(String name) {
		return values.containsKey(key(name));
	}
	
	void addValue(String name, int value) {
		addValue(name, Integer.toString(value));
	}
	
	int getLength() {
		return length;
	}
	
	/**
	 * Returns names of header fields already added to this frame.
	 * 
	 * @return a list of the names 
	 */
	public List<String> getNames() {
		return names;
	}
	
	String key(String name) {
		return name.toUpperCase();
	}
	
	boolean pendingName() {
		return pendingName != null;
	}
	
	void pendingName(String name) {
		if (pendingName == null) {
			pendingName = new StringBuilder();
		}
		pendingName.append(name);
	}
	
}
