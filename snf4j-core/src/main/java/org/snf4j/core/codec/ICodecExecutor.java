/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019-2021 SNF4J contributors
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
package org.snf4j.core.codec;

import java.nio.ByteBuffer;
import java.util.List;

import org.snf4j.core.handler.SessionEvent;
import org.snf4j.core.session.ISession;

/**
 * An executor responsible for encoding and decoding outbound and inbound data
 * for the associated session.
 * <p>
 * <b>Thread-safe considerations:</b> All methods in this interface, except
 * {@link #getPipeline()}, are always called by the SNF4J framework in the same
 * thread. <br>
 * All new changes done in the associated pipeline should not immediately affect
 * the encoding and decoding done by the executor. The synchronization between
 * the pipeline and the executor should be perfomed in the sync methods.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ICodecExecutor {
	
	/**
	 * Returns the codec pipeline that is associated with this codec executor.
	 * 
	 * @return the codec pipeline.
	 */
	ICodecPipeline getPipeline();
	
	/**
	 * Informs the codec executor that right now is the best moment to safely
	 * synchronize any pending changes for decoders in the associated
	 * pipeline.
	 * <p>
	 * <b>Performance considerations</b>: This method is called every time new
	 * data need to be decoded and so it should not perform any heavy tasks.
	 * 
	 * @param session the session the codec executor is associated with
	 */
	void syncDecoders(ISession session);

	/**
	 * Informs the codec executor that right now is the best moment to safely
	 * synchronize any pending changes for encoders in the associated
	 * pipeline.
	 * <p>
	 * <b>Performance considerations</b>: This method is called every time new
	 * data need to be encoded and so it should not perform any heavy tasks.
	 * 
	 * @param session the session the codec executor is associated with
	 */
	void syncEncoders(ISession session);
	
	/**
	 * Informs the codec executor that right now is the best moment to safely
	 * synchronize any pending changes for event-driven codecs in the associated
	 * pipeline. An Implementation of this method is responsible for signaling
	 * following events to event-driven codecs: adding and removing of a codec from
	 * the associated pipeline.
	 * <p>
	 * <b>Performance considerations</b>: This method is called every time the state
	 * of the associated session is changing so it should not perform any heavy
	 * tasks.
	 * 
	 * @param session the session the codec executor is associated with
	 * @see IEventDrivenCodec
	 */
	void syncEventDrivenCodecs(ISession session);
	
	/**
	 * Gets the base decoder.
	 * <p>
	 * The returned value should not be affected by changes in the associated
	 * pipeline that have not been synchronized yet by calling
	 * {@link #syncDecoders}.
	 * <p>
	 * <b>Performance considerations</b>: This method is called every time new
	 * data need to be decoded and so it should not perform any heavy tasks.
	 * 
	 * @return the first decoder in the pipeline but only if it implements
	 *         {@link IBaseDecoder}, if the first decoder does not implement it
	 *         then <code>null</code> should be returned.
	 */
	IBaseDecoder<?,?> getBaseDecoder();

	/**
	 * Informs if the pipeline has decoders that produce outbound object(s).
	 * <p>
	 * The returned value should not be affected by changes in the associated
	 * pipeline that have not been synchronized yet by calling
	 * {@link #syncDecoders}.
	 * <p>
	 * <b>Performance considerations</b>: This method is called every time new
	 * data need to be decoded and so it should not perform any heavy tasks.
	 * 
	 * @return <code>true</code> if the pipeline has at least one encoder that
	 *         produces outbound object(s).
	 */
	boolean hasDecoders();
	
	/**
	 * Encodes bytes from a byte buffer.
	 * <p>
	 * This method should ignore all changes in the associated pipeline that
	 * have not been synchronized yet by calling {@link #syncEncoders}.
	 * 
	 * @param session
	 *            the session for which the encoding is performed
	 * @param data
	 *            the bytes to encode
	 * @return a list of produced outbound objects, or {@code null} if the
	 *         pipeline is empty or has no encoders that produce outbound
	 *         object(s)
	 * @throws Exception
	 *             if one of the encoders failed during the encoding
	 */
	List<Object> encode(ISession session, ByteBuffer data) throws Exception;

	/**
	 * Encodes bytes from a byte array.
	 * <p>
	 * This method should ignore all changes in the associated pipeline that
	 * have not been synchronized yet by calling {@link #syncEncoders}.
	 * 
	 * @param session
	 *            the session for which the encoding is performed
	 * @param data
	 *            the bytes to encode
	 * @return a list of produced outbound objects, or {@code null} if the
	 *         pipeline is empty or has no encoders that produce outbound
	 *         object(s)
	 * @throws Exception
	 *             if one of the encoders failed during the encoding
	 */
	List<Object> encode(ISession session, byte[] data) throws Exception;

	/**
	 * Encodes a message.
	 * <p>
	 * This method should ignore all changes in the associated pipeline that
	 * have not been synchronized yet by calling {@link #syncEncoders}.
	 * 
	 * @param session
	 *            the session for which the encoding is performed
	 * @param msg
	 *            the message to encode
	 * @return a list of produced outbound objects, or {@code null} if the
	 *         pipeline is empty or has no encoders that produce outbound
	 *         object(s)
	 * @throws Exception
	 *             if one of the encoders failed during the encoding
	 */
	List<Object> encode(ISession session, Object msg) throws Exception;

	/**
	 * Decodes bytes from a byte array.
	 * <p>
	 * This method should ignore all changes in the associated pipeline that
	 * have not been synchronized yet by calling {@link #syncDecoders}.
	 * 
	 * @param session
	 *            the session for which the decoding is performed
	 * @param data
	 *            the bytes to decode
	 * @return a list of produced outbound objects, or {@code null} if the
	 *         pipeline is empty or has no decoders that produce outbound
	 *         object(s)
	 * @throws Exception
	 *             if one of the decoders failed during the decoding
	 */
	List<Object> decode(ISession session, byte[] data) throws Exception;
	
	/**
	 * Decodes bytes from a byte buffer.
	 * <p>
	 * This method should ignore all changes in the associated pipeline that
	 * have not been synchronized yet by calling {@link #syncDecoders}.
	 * 
	 * @param session
	 *            the session for which the decoding is performed
	 * @param data
	 *            the bytes to decode
	 * @return a list of produced outbound objects, or {@code null} if the
	 *         pipeline is empty or has no decoders that produce outbound
	 *         object(s)
	 * @throws Exception
	 *             if one of the decoders failed during the decoding
	 */
	List<Object> decode(ISession session, ByteBuffer data) throws Exception;
	
	/**
	 * Signals a session event to all event-driven codecs in the associated
	 * pipeline.
	 * 
	 * @param session the session the codec executor is associated with
	 * @param event the session event
	 */
	void event(ISession session, SessionEvent event);
	
	/**
	 * Adds a child codec executor that will be receiving session events 
	 * received by this codec executor.
	 * 
	 * @param session the session the codec executor is associated with
	 * @param executor a child codec executor
	 */
	void addChild(ISession session, ICodecExecutor executor);
}
