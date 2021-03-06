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
package org.snf4j.example.sctp.multi;

import java.net.SocketAddress;
import java.util.Set;

import org.snf4j.core.handler.ISctpHandler;
import org.snf4j.core.session.ISctpMultiSession;

import com.sun.nio.sctp.Association;

class AssociationManager {

	final AssociationContext[] contexts;
	
	final ISctpHandler handler;
	
	AssociationManager(ISctpHandler handler, int maxCount, SocketAddress... peers) {
		this.handler = handler;
		contexts = new AssociationContext[peers.length];
		
		for (int i=0; i<peers.length; ++i) {
			contexts[i] = new AssociationContext(peers[i], maxCount);
		}
	}
	
	ISctpMultiSession getSession() {
		return (ISctpMultiSession) handler.getSession();
	}
	
	AssociationContext getContext(Association association) {
		Set<SocketAddress> addresses = getSession().getRemoteAddresses(association);
		
		for (int i=0; i<contexts.length; ++i) {
			AssociationContext c = contexts[i];
			
			if (addresses.contains(c.peer)) {
				return c;
			}
		}
		throw new IllegalArgumentException(association.toString());
	}
	
	AssociationContext getContext(SocketAddress peer) {
		for (int i=0; i<contexts.length; ++i) {
			AssociationContext c = contexts[i];
			
			if (c.peer.equals(peer)) {
				return c;
			}
		}
		throw new IllegalArgumentException(peer.toString());
	}	
}
