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
package org.snf4j.core.proxy;

/**
 * Reply information sent from the SOCKS server. 
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public interface ISocksReply {
	
	/**
	 * Tells if the reply is successful.
	 * 
	 * @return {@code true} if the reply is successful
	 */
	boolean isSuccessful();
		
	/**
	 * Returns the status in the received reply.
	 * 
	 * @return the status of the reply
	 */
	int getStatus();
	
	/**
	 * Returns the description of the status in the received reply.
	 * @return
	 */
	String getStatusDescription();
	
	/**
	 * Returns the port in the received reply.
	 * 
	 * @return the port
	 */
	int getPort();
	
	/**
	 * Returns the address in the received reply.
	 * 
	 * @return the address
	 */
	String getAddress();
	
	/**
	 * Returns the type of the address in the received reply.
	 * 
	 * @return the type of the address
	 */
	SocksAddressType getAddressType();
	
}
