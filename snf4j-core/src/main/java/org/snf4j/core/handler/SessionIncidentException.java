/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.core.handler;

/**
 * Indicates some kind of incident detected while processing of I/O or protocol related 
 * operations.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class SessionIncidentException extends Exception {

	private static final long serialVersionUID = -3964386717652755341L;

	private final SessionIncident incident;
	
	/**
	 * Constructs a new exception with the specified incident.
	 * 
	 * @param incident
	 *            the incident
	 */
	public SessionIncidentException(SessionIncident incident) {
		this.incident = incident;
	}

	/**
	 * Constructs a new exception with the specified detail message and
	 * incident.
	 * 
	 * @param message
	 *            the detail message
	 * @param incident
	 *            the incident
	 */
	public SessionIncidentException(String message, SessionIncident incident) {
		super(message);
		this.incident = incident;
	}
	
	/**
	 * Constructs a new exception with the specified detail message, cause and
	 * incident.
	 * 
	 * @param message
	 *            the detail message
	 * @param cause
	 *            the detail message
	 * @param incident
	 *            the incident
	 */
	public SessionIncidentException(String message, Throwable cause, SessionIncident incident) {
		super(message, cause);
		this.incident = incident;
	}

	/**
	 * Constructs a new exception with the specified cause and incident.
	 * 
	 * @param cause
	 *            the detail message
	 * @param incident
	 *            the incident
	 */
	public SessionIncidentException(Throwable cause, SessionIncident incident) {
		super(cause);
		this.incident = incident;
	}

	/**
	 * Returns the incident of this exception.
	 * 
	 * @return the incident
	 */
	public SessionIncident getIncident() {
		return incident;
	}
}
