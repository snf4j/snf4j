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
package org.snf4j.core.codec;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A manager that manages encoders and decoders in the pipeline that is
 * associated with the {@link ICodecExecutor}.
 * <p>
 * All the methods that change the pipeline should always validate if the change
 * that is about to be made will not break the matching of the inbound and
 * outbound types between codecs in the pipeline.
 * <p>
 * Special care must be taken when adding or inserting the first codec into the
 * pipeline:
 * <ul>
 * <li>for the first decoder it is required that the inbound type can be either
 * {@code byte[]} or {@link ByteBuffer}
 * <li>for the first encoder it is required that the outbound type can be either
 * {@code byte[]} or {@link ByteBuffer}
 * </ul>
 * <p>
 * The diagram describing how data are processed thru a pipeline
 * 
 * <pre>
 *     	      I/O read                       I/O write
 *                |                              |
 *  +-------------+------------------------------+-------------+
 *  |             |         CodecPipeline       /|\            |
 *  |            \|/                             |             |
 *  |    +--------+--------+            +--------+--------+    |
 *  |    |  First Decoder  |            |  First Encoder  |    |
 *  |    +--------+--------+            +--------+--------+    |
 *  |             |                             /|\            |
 *  |            \|/                             |             |
 *  |    +--------+--------+            +--------+--------+    |
 *  |    | Second Decoder  |            | Second Encoder  |    |
 *  |    +--------+--------+            +--------+--------+    |
 *  |             .                             /|\            |
 *  |             .                              .             |
 *  |             .                              .             |
 *  |             .                              .             |
 *  |            \|/                             .             |
 *  |    +--------+--------+            +--------+--------+    |
 *  |    |   Last Decoder  |            |   Last Encoder  |    |
 *  |    +--------+--------+            +--------+--------+    |
 *  |             |                             /|\            |
 *  |            \|/                             |             |
 *  +-------------+------------------------------+-------------+
 *                |                              |
 *         Handler.read()                 Session.write()
 * </pre>
 * 
 * <b>Thread-safe considerations:</b> Any class implementing this interface
 * should be thread safe as it is expected that the internal state of the
 * pipeline can be changed at any time.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ICodecPipeline {

	/**
	 * Appends a decoder at the last position of this pipeline.
	 *
	 * @param key
	 *            the key under which the decoder should be appended
	 * @param decoder
	 *            the decoder to append
	 *
	 * @throws IllegalArgumentException
	 *             if a decoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the decoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void add(Object key, IDecoder<?,?> decoder);

	/**
	 * Appends a encoder at the last position of this pipeline.
	 *
	 * @param key
	 *            the key under which the encoder should be appended
	 * @param encoder
	 *            the encoder to append
	 *
	 * @throws IllegalArgumentException
	 *             if a encoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the encoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void add(Object key, IEncoder<?,?> encoder);

	/**
	 * Inserts a decoder after an existing decoder of this pipeline.
	 *
	 * @param baseKey
	 *            the key identifying the existing decoder
	 * @param key
	 *            the key under which the decoder should be inserted after
	 * @param decoder
	 *            the decoder to insert after
	 *
	 * @throws NoSuchElementException
	 *             if the decoder identified by the specified base key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a decoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the decoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void addAfter(Object baseKey, Object key, IDecoder<?,?> decoder);

	/**
	 * Inserts a encoder after an existing decoder of this pipeline.
	 *
	 * @param baseKey
	 *            the key identifying the existing encoder
	 * @param key
	 *            the key under which the encoder should be inserted after
	 * @param encoder
	 *            the encoder to insert after
	 *
	 * @throws NoSuchElementException
	 *             if the encoder identified by the specified base key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a encoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the encoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void addAfter(Object baseKey, Object key, IEncoder<?,?> encoder);
	
	/**
	 * Inserts a decoder at the first position of this pipeline.
	 *
	 * @param key
	 *            the key under which the decoder should be inserted first
	 * @param decoder
	 *            the decoder to insert first
	 *
	 * @throws IllegalArgumentException
	 *             if a decoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the decoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
 	void addFirst(Object key, IDecoder<?,?> decoder);

	/**
	 * Inserts a encoder at the first position of this pipeline.
	 *
	 * @param key
	 *            the key under which the encoder should be inserted first
	 * @param encoder
	 *            the encoder to insert first
	 *
	 * @throws IllegalArgumentException
	 *             if a encoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the encoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void addFirst(Object key, IEncoder<?,?> encoder);
	
	/**
	 * Inserts a decoder before an existing decoder of this pipeline.
	 *
	 * @param baseKey
	 *            the key identifying the existing decoder
	 * @param key
	 *            the key under which the decoder should be inserted before
	 * @param decoder
	 *            the decoder to insert before
	 *
	 * @throws NoSuchElementException
	 *             if the decoder identified by the specified base key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a decoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the decoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void addBefore(Object baseKey, Object key, IDecoder<?,?> decoder);

	/**
	 * Inserts a encoder before an existing encoder of this pipeline.
	 *
	 * @param baseKey
	 *            the key identifying the existing encoder
	 * @param key
	 *            the key under which the encoder should be inserted before
	 * @param encoder
	 *            the encoder to insert before
	 *
	 * @throws NoSuchElementException
	 *             if the encoder identified by the specified base key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a encoder identified by the specified key already exists
	 *             in this pipeline
	 * @throws IllegalArgumentException
	 *             if the encoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	void addBefore(Object baseKey, Object key, IEncoder<?,?> encoder);

	/**
	 * Replaces the decoder identified by the specified old key with a new
	 * decoder in this pipeline.
	 *
	 * @param oldKey
	 *            the key of the decoder to be replaced
	 * @param key
	 *            the key under which the replacement should be added
	 * @param decoder
	 *            the decoder which is used as replacement
	 *
	 * @return the removed decoder
	 *
	 * @throws NoSuchElementException
	 *             if the decoder identified by the specified old key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a decoder identified by the specified key already exists
	 *             in this pipeline, except for the decoder to be replaced
	 * @throws IllegalArgumentException
	 *             if the decoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	IDecoder<?,?> replace(Object oldKey, Object key, IDecoder<?,?> decoder);
	
	/**
	 * Replaces the encoder identified by the specified old key with a new
	 * encoder in this pipeline.
	 *
	 * @param oldKey
	 *            the key of the encoder to be replaced
	 * @param key
	 *            the key under which the replacement should be added
	 * @param encoder
	 *            the encoder which is used as replacement
	 *
	 * @return the removed encoder
	 *
	 * @throws NoSuchElementException
	 *             if the encoder identified by the specified old key does not
	 *             exist in this pipeline
	 * @throws IllegalArgumentException
	 *             if a encoder identified by the specified key already exists
	 *             in this pipeline, except for the encoder to be replaced
	 * @throws IllegalArgumentException
	 *             if the encoder has incompatible the inbound or the outbound
	 *             type
	 * @throws NullPointerException
	 *             if any of the arguments is {@code null}
	 */
	IEncoder<?, ?> replace(Object oldKey, Object key, IEncoder<?, ?> encoder);

	/**
	 * Removes the codec identified by the specified key from this pipeline.
	 * 
	 * @param key
	 *            the key under which the codec was stored
	 * @return the removed codec
	 * @throws NoSuchElementException
	 *             if there's no such codec that is identified by the specified
	 *             key in this pipeline
	 * @throws IllegalArgumentException
	 *             if removing of the codec that is identified by the specified
	 *             key would break the matching of the inbound and outbound
	 *             types between other codecs in the pipeline.
	 * @throws NullPointerException
	 *             if the specified key is {@code null}
	 */
	ICodec<?,?> remove(Object key); 
	
	/**
	 * Returns the codec identified by the specified key in this pipeline.
	 * 
	 * @param key
	 *            the key under which the codec was stored
	 *
	 * @return the codec that is identified by the specified key, or
	 *         {@code null} if there's no such codec in this pipeline.
	 */
	ICodec<?,?> get(Object key);

	/**
	 * Returns a list of keys that identify all encoders in this pipeline. The
	 * keys in the list are in the order they were added to this pipeline.
	 * 
	 * @return a list of the keys
	 */
	List<Object> encoderKeys();

	/**
	 * Returns a list of keys that identify all decoders in this pipeline. The
	 * keys in the list are in the order they were added to this pipeline.
	 * 
	 * @return a list of the keys
	 */
	List<Object> decoderKeys();
}
