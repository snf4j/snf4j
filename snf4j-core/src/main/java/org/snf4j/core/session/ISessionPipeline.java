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
package org.snf4j.core.session;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * A manager that manages sessions in the pipeline that can be associated with a
 * session.
 * <p>
 * <b>Thread-safe considerations:</b> Any class implementing this interface
 * should be thread safe as it is expected that the internal state of the
 * pipeline can be changed at any time.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISessionPipeline<T extends ISession> {
	
	/**
	 * Inserts a session at the first position of this pipeline.
	 *
	 * @param key     the key under which the session should be inserted first
	 * @param session the session to insert first
	 *
	 * @throws IllegalArgumentException if the session or the key already exists in
	 *                                  this pipeline
	 * @throws NullPointerException     if any of the arguments is {@code null}
	 */
	void addFirst(Object key, T session);
	
	/**
	 * Inserts a session after an existing session of this pipeline.
	 *
	 * @param baseKey the key identifying the existing session
	 * @param key     the key under which the session should be inserted after
	 * @param session the session to insert after
	 *
	 * @throws NoSuchElementException   if the session identified by the specified
	 *                                  base key does not exist in this pipeline
	 * @throws IllegalArgumentException if the session or the key already exists in
	 *                                  this pipeline
	 * @throws NullPointerException     if any of the arguments is {@code null}
	 */
	void addAfter(Object baseKey, Object key, T session);
	
	/**
	 * Appends a session at the last position of this pipeline.
	 *
	 * @param key     the key under which the session should be appended
	 * @param session the session to append
	 *
	 * @throws IllegalArgumentException if the session or the key already exists in
	 *                                  this pipeline
	 * @throws NullPointerException     if any of the arguments is {@code null}
	 */
	void add(Object key, T session);
	
	/**
	 * Inserts a session before an existing session of this pipeline.
	 *
	 * @param baseKey the key identifying the existing session
	 * @param key     the key under which the session should be inserted before
	 * @param session the session to insert before
	 *
	 * @throws NoSuchElementException   if the session identified by the specified
	 *                                  base key does not exist in this pipeline
	 * @throws IllegalArgumentException if the session or the key already exists in
	 *                                  this pipeline
	 * @throws NullPointerException     if any of the arguments is {@code null}
	 */
	void addBefore(Object baseKey, Object key, T session);
	
	/**
	 * Replaces the session identified by the specified old key with a new session
	 * in this pipeline.
	 *
	 * @param oldKey  the key of the session to be replaced
	 * @param key     the key under which the replacement should be added
	 * @param session the session which is used as replacement
	 *
	 * @return the removed session
	 *
	 * @throws NoSuchElementException   if the session identified by the specified
	 *                                  old key does not exist in this pipeline
	 * @throws IllegalArgumentException if the session or the key already exists in
	 *                                  this pipeline
	 * @throws NullPointerException     if any of the arguments is {@code null}
	 */
	T replace(Object oldKey, Object key, T session);
	
	/**
	 * Removes the session identified by the specified key from this pipeline.
	 * 
	 * @param key the key under which the session was stored
	 * @return the removed session, or {@code null} if there's no such session in
	 *         this pipeline
	 * @throws NullPointerException if the specified key is {@code null}
	 */
	T remove(Object key);
	
	/**
	 * Returns the session identified by the specified key in this pipeline.
	 * 
	 * @param key
	 *            the key under which the session was stored
	 *
	 * @return the session that is identified by the specified key, or
	 *         {@code null} if there's no such session in this pipeline.
	 */
	T get(Object key);

	/**
	 * Returns the session being the owner of this pipeline.
	 * @return the owner session
	 */
	T getOwner();
	
	/**
	 * Returns a list of keys that identify all session in this pipeline. The
	 * keys in the list are in the order they were added to this pipeline.
	 * 
	 * @return a list of the keys
	 */
	List<Object> getKeys();
	
	/**
	 * Marks this pipeline for closing.
	 * <p>
	 * Marking a pipeline for closing changes the way the session's close methods
	 * work. Calling the close methods in such case will close the connection
	 * instead of passing the processing to the next session in the pipeline.
	 */
	void markClosed();
	
	/**
	 * Marks this pipeline for closing with specified cause.
	 * <p>
	 * Marking a pipeline for closing changes the way the session's close methods
	 * work. Calling the close methods in such case will close the connection
	 * instead of passing the processing to the next session in the pipeline.
	 * 
	 * @param cause the closing cause
	 */
	void markClosed(Throwable cause);

	/**
	 * Marks this pipeline as undone.
	 * <p>
	 * Marking a pipeline as undone changes the way the session's close methods
	 * work. Calling the close methods in such state will close the connection
	 * instead of passing the processing to the next session in the pipeline.
	 */
	void markUndone();
	
	/**
	 * Marks this pipeline as undone with specified cause.
	 * <p>
	 * Marking a pipeline as undone changes the way the session's close methods
	 * work. Calling the close methods in such state will close the connection
	 * instead of passing the processing to the next session in the pipeline.
	 */
	void markUndone(Throwable cause);
	
	/**
	 * Marks this pipeline as done. It simply reverts the undone state. 
	 */
	void markDone();
	
	/**
	 * Marks this pipeline for closing and gently closes the currently processed
	 * session in the pipeline.
	 * <p>
	 * If the session owning this pipeline has not been registered with a selector
	 * loop all session in the pipeline will be closed by calling the
	 * {@link ISession#close() close()} method.
	 */
	void close();
	
	/**
	 * Marks this pipeline for closing and quickly closes the currently processed
	 * session in the pipeline.
	 * <p>
	 * If the session owning this pipeline has not been registered with a selector
	 * loop all session in the pipeline will be closed by calling the
	 * {@link ISession#quickClose() quickClose()} method.
	 */
	void quickClose();
	
	/**
	 * Marks this pipeline for closing and dirty closes the currently processed
	 * session in the pipeline.
	 * <p>
	 * If the session owning this pipeline has not been registered with a selector
	 * loop all session in the pipeline will be closed by calling the
	 * {@link ISession#dirtyClose() dirtyClose()} method.
	 */
	void dirtyClose();
	
}
