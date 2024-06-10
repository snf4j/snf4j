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
package org.snf4j.quic.engine;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.snf4j.quic.QuicException;
import org.snf4j.quic.Version;
import org.snf4j.quic.cid.ConnectionIdManager;
import org.snf4j.quic.packet.PacketUtil;

/**
 * A preliminary acceptor of QUIC packets. It is responsible for the preliminary
 * acceptance of QUIC packets based on their first bytes.
 * <p>
 * It checks if fixed bit, version and the destination connection id match the
 * current QUIC connection.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PacketAcceptor {
	
	private final QuicState state;
	
	public PacketAcceptor(QuicState state) {
		this.state = state;
	}

	private boolean acceptDestinationId(byte[] destId) {
		ConnectionIdManager mgr = state.getConnectionIdManager();
		
		if (mgr.getSourcePool().get(destId) != null) {
			return true;
		}
		else if (!state.isClientMode()) {
			byte[] id = mgr.getOriginalId();
			
			if (id == null || Arrays.equals(id, destId)) {
				return true;
			}
			
			if (mgr.hasRetryId()) {
				return Arrays.equals(mgr.getRetryId(), destId);
			}
		}
		return false;
	}
	
	/**
	 * Accepts received bytes in the source buffer.
	 * 
	 * @param src the source buffer
	 * @return {@code true} if bytes have been preliminary accepted
	 * @throws QuicException if an error occurred
	 */
	public boolean accept(ByteBuffer src) throws QuicException {
		int remaining = src.remaining();
		
		if (remaining > 0) {
			int bits, len;
			
			src = src.duplicate();
			bits = src.get();
			--remaining;
			
			//Check if long header
			if ((bits & 0x80) != 0) {
				if (remaining > 4) {
					Version version = PacketUtil.identifyVersion(src.getInt());
					
					remaining -= 4;
					if (version != null) {
						
						//Check fixed bit
						if (version != Version.V0 && (bits & 0x40) == 0) {
							return false;
						}
						
						len = src.get() & 0xff;
						if (remaining > len) {
							byte[] destId = new byte[len];
							
							src.get(destId);
							return acceptDestinationId(destId);
						}
					}
				}
			}
			//Check fixed bit (short header)
			else if ((bits & 0x40) != 0) {
				len = state.getConnectionIdManager().getSourcePool().getIdLength();
				
				if (remaining > len) {
					byte[] destId = new byte[len];
					
					src.get(destId);
					return acceptDestinationId(destId);
				}
			}
		}
		return false;
	}
	
	
}
