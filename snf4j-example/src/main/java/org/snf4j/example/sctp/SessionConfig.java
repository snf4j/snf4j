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
package org.snf4j.example.sctp;

import org.snf4j.core.codec.DefaultCodecExecutor;
import org.snf4j.core.codec.ICodecExecutor;
import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.bytes.BufferToArrayEncoder;
import org.snf4j.core.codec.zip.ZlibDecoder;
import org.snf4j.core.codec.zip.ZlibEncoder;
import org.snf4j.core.session.DefaultSctpSessionConfig;
import org.snf4j.core.session.ISctpSessionConfig;

import com.sun.nio.sctp.MessageInfo;

public class SessionConfig extends DefaultSctpSessionConfig {

	final static int CODEC_STREAM_NUMBER = 1;
	
	final static Object COMPRESSING_CODEC_EXECUTOR_IDENTIFIER = new Object();
	
	@Override
	public Object getCodecExecutorIdentifier(MessageInfo msgInfo) {
		if (msgInfo.streamNumber() == CODEC_STREAM_NUMBER) {
			return msgInfo.isUnordered() 
					? ISctpSessionConfig.DEFAULT_CODEC_EXECUTOR_IDENTIFIER 
					: COMPRESSING_CODEC_EXECUTOR_IDENTIFIER; 
		}
		return null;
	}
	
	@Override
	public ICodecExecutor createCodecExecutor() {
		DefaultCodecExecutor executor = new DefaultCodecExecutor();
		ICodecPipeline pipeline = executor.getPipeline();
		
		pipeline.add("D1", new Decoder());
		pipeline.add("E1", new Encoder());
		return executor;
	}
	
	@Override
	public ICodecExecutor createCodecExecutor(Object identifier) {
		if (identifier == COMPRESSING_CODEC_EXECUTOR_IDENTIFIER) {
			DefaultCodecExecutor executor = new DefaultCodecExecutor();
			ICodecPipeline pipeline = executor.getPipeline();
			
			pipeline.add("D1", new ZlibDecoder() {
				
				@Override
				protected int inflateBound(int len) {
					return SctpClient.SIZE;
				}
			});
			pipeline.add("D2", new Aggregator(SctpClient.SIZE));
			pipeline.add("E1", new ZlibEncoder());
			pipeline.add("E2", new BufferToArrayEncoder(true));
			return executor;
		}
		return null;
	}
}
