/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2024 SNF4J contributors
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
package org.snf4j.quic.cid;

import java.security.SecureRandom;

/**
 * The manager of the source and destination connection ids.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class ConnectionIdManager {
	
	private byte[] originalId;
	
	private byte[] retryId;
	
	private final boolean client;
	
	private final int idLength;
	
	private final SourcePool sourcePool;
	
	private final DestinationPool destinationPool;
	
	public ConnectionIdManager(boolean client, int idLength, int limit, SecureRandom random) {
		this.client = client;
		this.idLength = idLength;
		sourcePool = new SourcePool(idLength, !client, random);
		destinationPool = new DestinationPool(limit);
	}

	/**
	 * Tells if the original destination connection id is already stored by this
	 * manager.
	 * 
	 * @return {@code true} the original destination connection id is already stored
	 */
	public boolean hasOriginalId() {
		return originalId != null;
	}
	
	/**
	 * Returns the original destination connection id that is stored by this
	 * manager.
	 * <p>
	 * NOTE: If called by clients this method always returns value different than
	 * {@code null}. If the original destination connection id was not stored before
	 * calling it a new connection id will automatically generated.
	 * 
	 * @return the original destination connection id, or {@code null} if it is not
	 *         stored yet
	 */
	public byte[] getOriginalId() {
		if (client && originalId == null) {
			originalId = sourcePool.generate(Math.max(8, idLength));
		}
		return originalId;
	}

	/**
	 * Stores the given original destination connection id in this manager
	 * 
	 * @param originalId the original destination connection id
	 */
	public void setOriginalId(byte[] originalId) {
		this.originalId = originalId;
	}

	/**
	 * Tells if the retry source connection id is already stored by this manager.
	 * 
	 * @return {@code true} the retry source connection id is already stored
	 */
	public boolean hasRetryId() {
		return retryId != null;
	}
	
	/**
	 * Returns the retry source connection id that is stored by this
	 * manager.
	 * <p>
	 * NOTE: If called by servers this method always returns value different than
	 * {@code null}. If the retry source connection id was not stored before
	 * calling it a new connection id will automatically generated.
	 * 
	 * @return the retry source connection id, or {@code null} if it is not
	 *         stored yet
	 */
	public byte[] getRetryId() {
		if (!client && retryId == null) {
			retryId = sourcePool.generate(Math.max(8, idLength));
		}
		return retryId;
	}

	/**
	 * Stores the given retry source connection id in this manager
	 * 
	 * @param retryId the retry source connection id
	 */
	public void setRetryId(byte[] retryId) {
		this.retryId = retryId;
	}
	
	/**
	 * Returns the identifier of one of the active source connection ids stored in
	 * this manager.
	 * 
	 * @return the identifier of one of the active source connection id
	 */
	public byte[] getSourceId() {
		return sourcePool.get().getId();
	}

	/**
	 * Returns the identifier of one of the active destination connection ids stored
	 * in this manager.
	 * <p>
	 * NOTE: If there is no active destination connection id stored in the pool of
	 * the destination connection ids this method can return the retry source
	 * connection id if it is already stored or the original destination connection
	 * id otherwise.
	 * 
	 * @return the identifier of one of the active destination connection id
	 */
	public byte[] getDestinationId() {
		ConnectionId cid = destinationPool.get();
		
		if (cid == null) {
			if (retryId != null) {
				return retryId;
			}
			return getOriginalId();
		}
		return cid.getId();
	}

	/**
	 * Returns the pool of source connection ids that is used by this manager.
	 * 
	 * @return the pool of source connection ids
	 */
	public ISourcePool getSourcePool() {
		return sourcePool;
	}

	/**
	 * Returns the pool of destination connection ids that is used by this manager.
	 * 
	 * @return the pool of destination connection ids
	 */
	public IDestinationPool getDestinationPool() {
		return destinationPool;
	}

}
