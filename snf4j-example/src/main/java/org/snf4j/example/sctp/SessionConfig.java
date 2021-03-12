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
			pipeline.add("D2", new AggregatingDecoder(SctpClient.SIZE));
			pipeline.add("E1", new ZlibEncoder());
			pipeline.add("E2", new BufferToArrayEncoder(true));
			return executor;
		}
		return null;
	}
}
