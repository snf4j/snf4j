/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2017-2020 SNF4J contributors
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

import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.session.ISession;
import org.snf4j.core.session.ISessionConfig;

/**
 * Handles events and I/O notifications sent from the associated session. It
 * also provides configuration and factories for the session.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IHandler {
	
	/**
	 * Sets the session that will be associated with this handler.
	 * 
	 * @param session
	 *            the session
	 */
	void setSession(ISession session);
	
	/**
	 * Returns the session that is associated with this handler.
	 * 
	 * @return the session
	 */
	ISession getSession();
	
	/**
	 * Returns the name of the handler. The name will be used to name the
	 * associated session created with the default constructor.
	 * 
	 * @return the name or <code>null</code> if the name is not specified.
	 */
	String getName();
	
	/**
	 * Called when new bytes were read from the input buffer.	
	 * <p>
	 * The passed array can be safely stored or modified by this method as it
	 * will not be used by the caller.
	 * 
	 * @param data
	 *            bytes that was read from the input buffer.
	 */
	void read(byte[] data);
	
	/**
	 * Called when a new message was read and decoded from the input buffer.
	 * This method is called when the associated session is configured with a
	 * codec pipeline in which the last decoder produces outbound object(s) of
	 * type different than the {@code byte[]}.
	 * 
	 * @param msg
	 *            the message that was read and decoded from the input buffer.
	 */
	void read(Object msg);
	
	/**
	 * Called to notify about a change of the associated session state.
	 * 
	 * @param event
	 *            an event related with the change of the session state
	 * @see SessionEvent
	 */
	void event(SessionEvent event);
	
	/**
	 * Called to notify about an I/O operation.
	 * 
	 * @param event
	 *            an event related with the type of I/O operation
	 * @param length
	 *            the number of bytes related with the I/O operation
	 * @see DataEvent
	 */
	void event(DataEvent event, long length);
	
	/**
	 * Called to notify about an exception caught during processing of I/O or
	 * protocol related operations. After returning form this method the
	 * associated session will be quickly closed.
	 * 
	 * @param t
	 *            the exception caught
	 */
	void exception(Throwable t);
	
	/**
	 * Called to notify about an incident that occurred during processing of I/O
	 * or protocol related operations.
	 * 
	 * @param incident
	 *            an incident that occurred
	 * 
	 * @param t
	 *            an exception that is related with the incident or
	 *            <code>null</code>
	 * @return <code>true</code> to indicate that the incident was handled and
	 *         the default action should not be executed by the SNF4J framework 
	 *         underneath.
	 */
	boolean incident(SessionIncident incident, Throwable t);
	
	/**
	 * Called to notify about an expiration of the timer identified by the
	 * specified event object.
	 * 
	 * @param event
	 *            the event object that identifies the timer
	 */
	void timer(Object event);
	
	/**
	 * Called to notify about an expiration of the timer associated with the
	 * specified task. The passed task object has not been executed yet and it
	 * is up to the handler's implementation to execute it or not.
	 * 
	 * @param task
	 *            the task to execute
	 */
	void timer(Runnable task);
	
	/**
	 * Returns the factory object that will be used to configure the internal
	 * structure of the associated session.
	 * 
	 * @return the factory object
	 * @see ISessionStructureFactory
	 */
	ISessionStructureFactory getFactory();
	
	/**
	 * Returns the configuration object that will be used to configure the
	 * behavior of the associated session.
	 * 
	 * @return the configuration object
	 * @see ISessionConfig
	 */
	ISessionConfig getConfig();
}
