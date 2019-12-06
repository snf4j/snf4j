/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2019 SNF4J contributors
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
package org.snf4j.longevity;

public interface Config {
	
	static final int FIRST_PORT = 7000;
	
	static final int MAX_SESSIONS = 101;
	
	static final int MAX_PACKETS_IN_SESSION = 1000;
	
	static final int MAX_PACKET_SIZE = 8000;
	
	static final int MAX_WRITE_DELAY = 200;
	
	static final int DELAYED_WRITE_RATIO = 20;
	
	static final int SSL_SESSION_RATIO = 20;
	
	static final int MULTI_PACKET_RATIO = 20;
	
	static final int MAX_MULTI_PACKET = 20;
	
	static final int SPLIT_PACKET_RATIO = 50;
	
	static final int DIRECT_ALLOCATOR_RATIO = 0;
	
	static final int SYNC_WITH_TIMEOUT_RATIO = 50;
	
	static final int CODEC_EXECUTOR_RATIO = 50;

	static final int DEFAULT_EXECUTOR_RATIO = 50;
	
	static final int NO_CONNECTION_RATIO = 0;
	
	static final String HOST = "127.0.0.1";

}
