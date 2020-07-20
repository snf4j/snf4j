package org.snf4j.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.snf4j.core.allocator.IByteBufferAllocator;
import org.snf4j.core.factory.DefaultSessionStructureFactory;
import org.snf4j.core.factory.IDatagramHandlerFactory;
import org.snf4j.core.factory.ISessionStructureFactory;
import org.snf4j.core.handler.IDatagramHandler;
import org.snf4j.core.session.DefaultSessionConfig;
import org.snf4j.core.timer.ITimeoutModel;
import org.snf4j.core.timer.ITimer;

public class DTLSServerHandlerTest {
	
	@Test
	public void testConstructor() {
		IDatagramHandlerFactory hf = new IDatagramHandlerFactory() {

			@Override
			public IDatagramHandler create(SocketAddress remoteAddress) {
				return null;
			}
			
		};
		ISessionStructureFactory f = new ISessionStructureFactory() {

			@Override
			public IByteBufferAllocator getAllocator() {
				return null;
			}

			@Override
			public ConcurrentMap<Object, Object> getAttributes() {
				return null;
			}

			@Override
			public Executor getExecutor() {
				return null;
			}
			
			@Override
			public ITimer getTimer() {
				return null;
			}

			@Override
			public ITimeoutModel getTimeoutModel() {
				return null;
			}
			
		};
		DefaultSessionConfig c = new DefaultSessionConfig();
		
		DTLSServerHandler h = new DTLSServerHandler(hf);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig().getClass() == DefaultSessionConfig.class);
		assertFalse(h.getConfig() == c);
		
		h = new DTLSServerHandler(hf, c);
		assertTrue(h.getFactory() == DefaultSessionStructureFactory.DEFAULT);
		assertTrue(h.getConfig() == c);

		h = new DTLSServerHandler(hf, c, f);
		assertTrue(h.getFactory() == f);
		assertTrue(h.getConfig() == c);
	
	}

}
