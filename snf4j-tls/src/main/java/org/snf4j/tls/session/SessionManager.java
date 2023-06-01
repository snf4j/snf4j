/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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
package org.snf4j.tls.session;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.snf4j.tls.cipher.IHashSpec;
import org.snf4j.tls.engine.IEngineState;
import org.snf4j.tls.extension.IExtension;
import org.snf4j.tls.extension.OfferedPsk;
import org.snf4j.tls.handshake.NewSessionTicket;

public class SessionManager implements ISessionManager {

	private final static Random RANDOM = new Random();
	
	private final static AtomicLong ID = new AtomicLong();
	
	private final long id;
	
	private final long maskAdd;

	private final long idAdd;
	
	private final long sessionIdAdd;
	
	private final long nonceAdd;
	
	private final AtomicLong nextNonce = new AtomicLong();
	
	private final SessionCache<String> cacheByIpPort;

	private final SessionCache<Long> cacheById;
	
	private final int lifetime;
	
	public SessionManager(int lifetime, int limit) {
		id = ID.incrementAndGet();
		this.lifetime = lifetime;
		cacheByIpPort = new SessionCache<String>(limit, lifetime * 1000);
		cacheById = new SessionCache<Long>(limit, lifetime * 1000);
		idAdd = RANDOM.nextLong();
		maskAdd = RANDOM.nextLong();
		sessionIdAdd = RANDOM.nextLong();
		nonceAdd = RANDOM.nextLong();
	}

	public SessionManager() {
		this(86400, 20480);
	}
	
	static String key(String host, int port) {
		return host + ':' + port;
	}
	
	static String key(ISession session) {
		if (session.getPeerHost() != null && session.getPeerPort() != -1) {
			return key(session.getPeerHost(), session.getPeerPort());
		}
		return null;
	}
	
	void putSession(Session session, long currentTime) {
		String key = key(session);
		
		synchronized (cacheById) {
			cacheById.put(session.getId(), session, currentTime);
			if (key != null) {
				cacheByIpPort.put(key, session, currentTime);
			}
		}
	}
	
	ISession getSession(long sessionId, long currentTime) {
		synchronized (cacheById) {
			return cacheById.get(sessionId, currentTime);
		}
	}
	
	@Override
	public ISession getSession(long sessionId) {
		return getSession(sessionId, System.currentTimeMillis());
	}

	ISession getSession(String host, int port, long currentTime) {
		String key = key(host, port);
		
		synchronized (cacheById) {
			return cacheByIpPort.get(key, currentTime);
		}
	}
	
	@Override
	public ISession getSession(String host, int port) {
		return getSession(host, port, System.currentTimeMillis());
	}
	
	Certificate[] prepareCerts(Certificate[] certs) {
		return certs == null ? null : certs.clone();
	}
	
	ISession newSession(SessionInfo info, long currentTime) {
		Session session = new Session(
				this, 
				info.cipher(), 
				info.peerHost(), 
				info.peerPort(),
				currentTime,
				prepareCerts(info.peerCerts()),
				prepareCerts(info.localCerts()));
		
		putSession(session, currentTime);
		return session;
	}

	@Override
	public ISession newSession(SessionInfo info) {
		return newSession(info, System.currentTimeMillis());
	}

	void removeSession(long sessionId, long currentTime) {
		synchronized (cacheById) {
			ISession session = cacheById.get(sessionId, currentTime);
			
			if (session != null) {
				String key = key(session);

				cacheById.remove(sessionId);
				if (key != null) {
					cacheByIpPort.remove(key);
				}
			}
		}		
	}
	
	@Override
	public void removeSession(long sessionId) {
		removeSession(sessionId, System.currentTimeMillis());
	}

	void invalidateSession(Session session) {
		String key = key(session);

		synchronized (cacheById) {
			if (session.markInvalid()) {
				cacheById.remove(session.getId());
				if (key != null) {
					cacheByIpPort.remove(key);
				}
			}
		}
	}
	
	Session getSession(byte[] identity, long currentTime) {
		if (identity.length == 32) {
			ByteBuffer buffer = ByteBuffer.wrap(identity);
			long mask = buffer.getLong() - maskAdd;
			long id = (buffer.getLong() - idAdd) ^ mask;
			
			if (this.id == id) {
				long sessionId = (buffer.getLong() - sessionIdAdd) ^ mask;
		
				return (Session) getSession(sessionId, currentTime);
			}
		}
		return null;
	}
	
	UsedSession useSession(OfferedPsk[] psks, IHashSpec hashSpec, long currentTime) {
		int hashOrdinal = hashSpec.getOrdinal();
		
		for (int i=0; i<psks.length; ++i) {
			OfferedPsk psk = psks[i];
			byte[] identity = psk.getIdentity().getIdentity();
			Session session = getSession(identity, currentTime);
			
			if (session != null) {
				synchronized (session.getTicketsLock()) {
					for (SessionTicket ticket: session.getTickets(currentTime)) {
						if (ticket.getCipherSuite().spec().getHashSpec().getOrdinal() == hashOrdinal) {
							if (Arrays.equals(identity, ticket.getTicket())) {
								session.removeTicket(ticket);
								return new UsedSession(session, ticket, i);
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public UsedSession useSession(OfferedPsk[] psks, IHashSpec hashSpec) {
		return useSession(psks, hashSpec, System.currentTimeMillis());
	}
	
	Session checkSession(ISession session) {
		if (session.getManager() == this) {
			return (Session)session;
		}
		throw new IllegalArgumentException("Unexpected session implementation");
	}
	
	@Override
	public void putTicket(ISession session, SessionTicket ticket) {
		Session checkedSession = checkSession(session);
		
		synchronized (checkedSession.getTicketsLock()) {
			checkedSession.addTicket(ticket);
		}
	}
	
	@Override
	public void removeTicket(ISession session, SessionTicket ticket) {
		Session checkedSession = checkSession(session);
		
		synchronized (checkedSession.getTicketsLock()) {
			checkedSession.removeTicket(ticket);
		}
	}
	
	SessionTicket[] getTickets(ISession session, long currentTime) {
		Session checkedSession = checkSession(session);
		
		synchronized (checkedSession.getTicketsLock()) {
			return checkedSession.getTickets(currentTime);
		}
	}

	@Override
	public SessionTicket[] getTickets(ISession session) {
		return getTickets(session, System.currentTimeMillis());
	}
	
	NewSessionTicket newTicket(IEngineState state, long maxEarlyDataSize, long currentTime) throws InvalidKeyException {
		Session checkedSession = checkSession(state.getSession());
		long nonce = nextNonce.incrementAndGet();
		SecureRandom random = state.getParameters().getSecureRandom();
		byte[] ticketNonce = nonce(nonce);
		byte[] psk = state.getKeySchedule().computePsk(ticketNonce);
		long ageAdd = random.nextLong();
		byte[] ticketIdentity = ticket(checkedSession, nonce, random);
		
		NewSessionTicket newTicket = new NewSessionTicket(
				ticketIdentity, 
				ticketNonce, 
				lifetime, 
				ageAdd, 
				new ArrayList<IExtension>(0));
		
		SessionTicket ticket = new SessionTicket(
				state.getCipherSuite(),
				psk, 
				ticketIdentity, 
				lifetime, 
				ageAdd,
				maxEarlyDataSize,
				currentTime);

		synchronized (checkedSession.getTicketsLock()) {
			checkedSession.addTicket(ticket);
		}
		
		return newTicket;
	}

	@Override
	public NewSessionTicket newTicket(IEngineState state, long maxEarlyDataSize) throws Exception {
		return newTicket(state, maxEarlyDataSize, System.currentTimeMillis());
	}
	
	byte[] ticket(ISession session, long nonce, SecureRandom random) {
		long mask = random.nextLong();
		long id = this.id;
		long sessionId = session.getId();
		byte[] ticket = new byte[32];
		
		id ^= mask;
		sessionId ^= mask;
		nonce ^= mask;
		mask += maskAdd;
		id += idAdd;
		sessionId += sessionIdAdd;
		nonce += nonceAdd;
		
		ByteBuffer.wrap(ticket)
			.putLong(mask)
			.putLong(id)
			.putLong(sessionId)
			.putLong(nonce);
		return ticket;
	}
	
	byte[] nonce(long nonce) {
		byte[] bytes;
		
		if (nonce < 0x10000) {
			bytes = new byte[2];		
			bytes[1] = (byte)nonce;
			bytes[0] = (byte)(nonce >> 8);
		}
		else if (nonce < 0x100000000L) {
			bytes = new byte[4];		
			bytes[3] = (byte)nonce;
			bytes[2] = (byte)(nonce >> 8);
			bytes[1] = (byte)(nonce >> 16);
			bytes[0] = (byte)(nonce >> 24);		
		}
		else {
			bytes = new byte[8];	
			for (int i=7; i>=0; --i) {
				bytes[i] = (byte)nonce;
				nonce >>= 8;
			}
		}
		return bytes;
	}
			
}
