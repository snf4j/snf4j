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
package org.snf4j.websocket;

import java.net.URI;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.session.ISessionConfig;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.handshake.HandshakeRequest;
import org.snf4j.websocket.handshake.HandshakeResponse;

/**
 * A configuration for associated Web Socket session.
 *  
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IWebSocketSessionConfig extends ISessionConfig {
	
	/**
	 * The default key identifying the handshake decoder
	 * {@link org.snf4j.websocket.handshake.HandshakeDecoder HandshakeDecoder} in
	 * the default codec pipeline created by the SNF4J framework.
	 */
	public final static String HANDSHAKE_DECODER = "hs-decoder";
	
	/**
	 * The default key identifying the handshake encoder
	 * {@link org.snf4j.websocket.handshake.HandshakeEncoder HandshakeEncoder} in
	 * the default codec pipeline created by the SNF4J framework.
	 */
	public final static String HANDSHAKE_ENCODER = "hs-encoder";
	
	/**
	 * The default key identifying the Web Socket frame decoder
	 * {@link org.snf4j.websocket.frame.FrameDecoder FrameDecoder} in
	 * the default codec pipeline created by the SNF4J framework.
	 */
	public final static String WEBSOCKET_DECODER = "ws-decoder";

	/**
	 * The default key identifying the Web Socket frame encoder
	 * {@link org.snf4j.websocket.frame.FrameEncoder FrameEncoder} in
	 * the default codec pipeline created by the SNF4J framework.
	 */
	public final static String WEBSOCKET_ENCODER = "ws-encoder";

	/**
	 * The default key identifying the UTF8 validator
	 * {@link org.snf4j.websocket.frame.FrameUtf8Validator FrameUtf8Validator} in
	 * the default codec pipeline created by the SNF4J framework.
	 */
	public final static String WEBSOCKET_UTF8_VALIDATOR = "ws-utf8-validator";
	
	/**
	 * Determines the request URI (the server endpoint) in the client Web Socket
	 * handshake request. It is used to configure the Request-URI of the HTTP GET
	 * method and the value of the Host header field.
	 * <p>
	 * It is not used for configuration of server sessions.
	 * 
	 * @return the request URI
	 */
	URI getRequestUri();

	/**
	 * Determines the value of the Origin header field in the client Web Socket
	 * handshake request.
	 * <p>
	 * It is not used for configuration of server sessions.
	 * 
	 * @return the value of the Origin header field, or {@code null} if the Origin
	 *         field is not required in the request.
	 */
	String getRequestOrigin();
	
	/**
	 * Determines the names of supported subprotocols for both the server and client
	 * Web Socket sessions.
	 * 
	 * @return the names of supported subprotocols
	 */
	String[] getSupportedSubProtocols();
	
	/**
	 * Determines the supported extensions for both the server and client
	 * Web Socket sessions.
	 * 
	 * @return the supported extensions
	 */
	IExtension[] getSupportedExtensions();
	
	/**
	 * Called when the encoders in the codec pipeline should be switched after
	 * finishing of the Web Socket handshake.
	 * 
	 * @param pipeline        the codec pipeline to be changed
	 * @param allowExtensions informs that some extensions have been negotiated
	 *                        during the Web Socket handshake
	 */
	void switchEncoders(ICodecPipeline pipeline, boolean allowExtensions);
	
	/**
	 * Called when the decoders in the codec pipeline should be switched after
	 * finishing of the Web Socket handshake.
	 * 
	 * @param pipeline        the codec pipeline to be changed
	 * @param allowExtensions informs that some extensions have been negotiated
	 *                        during the Web Socket handshake
	 */
	void switchDecoders(ICodecPipeline pipeline, boolean allowExtensions);
	
	/**
	 * Called when the Web Socket handshake request is ready to be sent and can be
	 * now customized, if needed.
	 * 
	 * @param request the handshake request to be sent
	 */
	void customizeHeaders(HandshakeRequest request);
	
	/**
	 * Called when the Web Socket handshake response is ready to be sent and can be
	 * now customized, if needed.
	 * 
	 * @param response the handshake response to be sent
	 */
	void customizeHeaders(HandshakeResponse response);
	
	/**
	 * Determines if the server session associated with this configuration object
	 * should ignore missing the Host header field in the received Web Socket
	 * handshake request.
	 * 
	 * @return {@code true} to ignore missing the Host header field
	 */
	boolean ignoreHostHeaderField();
	
	/**
	 * Determines if a received Web Socket handshake request with given request URI
	 * should be accepted by the server session associated with this configuration
	 * object. 
	 * <p>
	 * The {@code requestUri} argument is built based on the Host header field
	 * and the "Request-URI" of the GET method. If the Host header field is missed and the
	 * {@link #ignoreHostHeaderField} returns {@code true} the host-port part of the
	 * URI will be set to the "null" string.
	 * 
	 * @param requestUri the request URI in the received Web Socket handshake
	 *                   request
	 * @return {@code true} if the request URI should be accepted
	 */
	boolean acceptRequestUri(URI requestUri);
	
	/**
	 * Determines the maximum length of the decoded Web Socket handshake frames. All
	 * bigger frames will be rejected and the affected handshakes will not be finished
	 * and the sessions will be closed.
	 * 
	 * @return the maximum length of the decoded handshake frames
	 */
	int getMaxHandshakeFrameLength();
	
	/**
	 * Determines the maximum length of the payload in the decoded Web Socket
	 * frames. All bigger frames will be rejected and the affected sessions will be
	 * closed.
	 * 
	 * @return the maximum length of the payload in the decoded frames
	 */
	int getMaxFramePayloadLength();
	
	/**
	 * Determines if the received Web Socket close frames should be handled
	 * automatically. The automatically handled close frames will not be passed
	 * to the user's web socket handlers.
	 * 
	 * @return {@code true} if the close frames should be handled automatically
	 */
	boolean handleCloseFrame();
}
