package org.snf4j.core.session;

import java.util.concurrent.Executor;

public interface IEngineSession {
	
	/**
	 * Sets the executor that will be used to execute delegated tasks required
	 * by this session to complete operations that block, or may take an
	 * extended period of time to complete.
	 * 
	 * @param executor
	 *            the new executor, or <code>null</code> to use the executor
	 *            configured in the selector loop that handles this session.
	 */
	void setExecutor(Executor executor);
	
	/**
	 * Returns the executor that will be used to execute delegated tasks
	 * required by this session to complete operations that block, or may take
	 * an extended period of time to complete.
	 * <p>
	 * By default, this method returns the executor configured in the selector 
	 * loop that handles this session, or <code>null</code>.
	 * 
	 * @return the current executor, or <code>null</code> if the executor is
	 *         undefined (i.e. the session is not associated with the selector 
	 *         loop and the executor is not set)
	 */
	Executor getExecutor();
	
	/**
	 * Initiates handshaking (initial or renegotiation) on the protocol engine
	 * driving this session. After calling this method the handshake will start
	 * immediately.
	 * <p>
	 * This method is not needed for the initial handshake, as the <code>wrap</code> and 
	 * <code>unwrap</code> methods of the protocol engine should initiate it if the 
	 * handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	void beginHandshake();
	
	/**
	 * Initiates lazy handshaking (initial or renegotiation) on the protocol
	 * engine driving this session. After calling this method the handshake will
	 * not start immediately. It will start when new data is received from a
	 * remote peer or following methods are called: <code>write</code>,
	 * <code>writenf</code>, <code>beginHandshake</code>.
	 * <p>
	 * This method is not needed for the initial handshake, as the
	 * <code>wrap</code> and <code>unwrap</code> methods of the protocol engine
	 * should initiate it if the handshaking has not already begun.
	 * <p>
	 * The operation is asynchronous.
	 */
	void beginLazyHandshake();
	
}
