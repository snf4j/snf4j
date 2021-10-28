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
package org.snf4j.websocket.extensions;

import java.util.List;

import org.snf4j.core.codec.ICodecPipeline;

/**
 * A Web Socket extension.
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface IExtension {
	
	/**
	 * Returns the name of the extension.
	 * 
	 * @return the name of the extension
	 */
    String getName();
    
    /**
     * Returns an object that identifies the group to which the extension belongs. This
     * identifier is used to group different extensions that cannot coexist in the list
     * of extensions being the outcome of the negotiation process.
     * 
     * @return an object identifying the group
     */
    Object getGroupId();
	
	/**
	 * Called by the Web Socket server to accept the received extension negotiation
	 * offer. The received offer is split into a list of following tokens:
	 * 
	 * <pre>extension-name, param-name1, param-value1, ... , param-nameN, param-valueN</pre>
	 * 
	 * If a parameter was offered with no value its value in the list will be
	 * {@code null}. For example following extension negotiation offer
	 * 
	 * <pre>foo; y=10; x; z="5"</pre>
	 * 
	 * will be split into following list of tokens:
	 * 
	 * <pre>"foo", "y", "10", "x", null, "z", "5"</pre>
	 * 
	 * @param offer the offer split into a list of tokens
	 * @return the extension that will be used by the Web Socket server, or
	 *         {@code null} if the negotiation offer could not be accepted by this
	 *         extension.
	 * @throws InvalidExtensionException if the negotiation offer syntax was invalid
	 */
	IExtension acceptOffer(List<String> offer) throws InvalidExtensionException;
	
	/**
	 * Called by the Web Socket client to validate the received extension negotiation
	 * response. The received response is split into a list of following tokens:
	 * 
	 * <pre>extension-name, param-name1, param-value1, ... , param-nameN, param-valueN</pre>
	 * 
	 * If a parameter was responded with no value its value in the list will be
	 * {@code null}. For example following extension negotiation response
	 * 
	 * <pre>foo; y=10; x; z="5"</pre>
	 * 
	 * will be split into following list of tokens:
	 * 
	 * <pre>"foo", "y", "10", "x", null, "z", "5"</pre>
	 * 
	 * @param response the response split into a list of tokens
	 * @return the extension that will be used by the Web Socket client, or
	 *         {@code null} if the negotiation response could not be validated by this
	 *         extension.
	 * @throws InvalidExtensionException if the negotiation response syntax was invalid
	 */
	IExtension validateResponse(List<String> response) throws InvalidExtensionException;
	
	/**
	 * Called by the Web Socket client to prepare the extension negotiation offer. 
	 * The prepared offer have to be split into a list of following tokens:
	 * 
	 * <pre>extension-name, param-name1, param-value1, ... , param-nameN, param-valueN</pre>
	 * 
	 * If a parameter in the offer has no value its value in the list have to be
	 * {@code null}. For example following extension negotiation offer
	 *  
	 * <pre>foo; y; x=10</pre>
	 * 
	 * have to be split into following list of tokens:
	 * 
	 * <pre>"foo", "y", null, "y", "10"</pre>
	 * 
	 * @return the offer split into a list of tokens
	 */
	List<String> offer();
	
	/**
	 * Called by the Web Socket server to prepare the extension negotiation response. 
	 * The prepared response have to be split into a list of following tokens:
	 * 
	 * <pre>extension-name, param-name1, param-value1, ... , param-nameN, param-valueN</pre>
	 * 
	 * If a parameter in the response has no value its value in the list have to be
	 * {@code null}. For example following extension negotiation response
	 *  
	 * <pre>foo; y; x=10</pre>
	 * 
	 * have to be split into following list of tokens:
	 * 
	 * <pre>"foo", "y", null, "y", "10"</pre>
	 * 
	 * @return the response split into a list of tokens
	 */
	List<String> response();
	
	/**
	 * Called to update the encoder pipeline.
	 * 
	 * @param pipeline the pipeline to update
	 */
	void updateEncoders(ICodecPipeline pipeline);
	
	/**
	 * Called to update the decoder pipeline.
	 * 
	 * @param pipeline the pipeline to update
	 */
	void updateDecoders(ICodecPipeline pipeline);
}
