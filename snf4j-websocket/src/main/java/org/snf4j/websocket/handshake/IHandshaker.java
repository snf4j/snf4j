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
package org.snf4j.websocket.handshake;

import java.net.URI;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.session.ISession;
import org.snf4j.websocket.extensions.IExtension;

/**
 * A handshaker responsible for processing of the Web Socket handshake phase.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IHandshaker {
	
	/**
	 * Sets the session object this handshaker is associated with.
	 * 
	 * @param session the session object
	 */
	void setSession(ISession session);
	
	/**
	 * Returns a detailed reason why the handshake phase could not finished
	 * successfully.
	 * 
	 * @return the detail reason
	 */
	String getClosingReason();
	
	/**
	 * Returns the negotiated extension identified by the specified name.
	 * 
	 * @param name the name of the extension to return
	 * @return the extension or {@code null} if no extension with specified name has
	 *         been negotiated
	 */
	IExtension getExtension(String name);
	
	/**
	 * Returns the names of all extensions that have been negotiated
	 * 
	 * @return an array of the names
	 */
	String[] getExtensionNames();
	
	/**
	 * Returns all extensions that have been negotiated
	 * 
	 * @return an array of the extensions
	 */
	IExtension[] getExtensions();
	
	/**
	 * Return the negotiated sub-protocol.
	 * 
	 * @return the negotiated sub-protocol or {@code null} if no sub-protocol has
	 *         been negotiated.
	 */
	String getSubProtocol();
	
	/**
	 * The URI identifying the Web Socket connection for both the client and
	 * server sessions.
	 * 
	 * @return the URI identifying the connection
	 */
	URI getUri();
	
	/**
	 * Initiates the handshake phase by Web Socket clients.
	 * 
	 * @return the handshake frame to be send to the Web Socket server.
	 */
	HandshakeFrame handshake();
	
	/**
	 * Processes the handshake frame received from a Web Socket client.
	 * 
	 * @param frame the received handshake frame
	 * @return the handshake frame to be send to the Web Socket client.
	 */
	HandshakeFrame handshake(HandshakeFrame frame);
	
	/**
	 * Tells is any extension has been negotiated
	 * 
	 * @return {@code true} if at least one extension has been negotiated
	 */
	boolean hasExtensions();
	
	/**
	 * Tells the mode (server/client) this handshaker is operating
	 * 
	 * @return {@code true} if this handshaker is operating in the client mode
	 */
	boolean isClientMode();
	
	/**
	 * Tells if this handshake is closing without successfully finishing the
	 * handshake phase.
	 * 
	 * @return {@code true} if this handshaker is closing
	 */
	boolean isClosing();
	
	/**
	 * Tells if this handshake has successfully finished the handshake phase.
	 * 
	 * @return {@code true} if the handshake phase has been successfully finished
	 */
	boolean isFinished();
	
	/**
	 * Called to updated the pipeline decoders based on the negotiated extensions
	 * 
	 * @param pipeline the pipeline to be updated
	 */
	void updateExtensionDecoders(ICodecPipeline pipeline);
	
	/**
	 * Called to updated the pipeline encoders based on the negotiated extensions
	 * 
	 * @param pipeline the pipeline to be updated
	 */
	void updateExtensionEncoders(ICodecPipeline pipeline);
}
