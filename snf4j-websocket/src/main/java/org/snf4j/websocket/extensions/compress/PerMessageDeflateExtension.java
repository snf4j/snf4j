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
package org.snf4j.websocket.extensions.compress;

import java.util.List;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.extensions.ExtensionGroup;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;
import org.snf4j.websocket.frame.Frame;

public class PerMessageDeflateExtension implements IExtension {
	
	final static String NAME = "permessage-deflate";
	
	public final static String PERMESSAGE_DEFLATE_DECODER = "permessage-deflate-decoder";
	
	public final static String PERMESSAGE_DEFLATE_ENCODER = "permessage-deflate-encoder";
	
	public enum NoContext {
		FORBIDDEN,
		OPTIONAL,
		REQUIRED
	}
	
	private final int compressionLevel;

	private NoContext compressNoContext;
	
	private NoContext decompressNoContext;
	
	private boolean clientMode;
	
	private int minInflateBound;
	
	private PerMessageDeflateParams params;
	
	public PerMessageDeflateExtension(int compressionLevel, int minInflateBound, NoContext compressNoContext, NoContext decompressNoContext) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException(
                    "Invalid compressionLevel: " + compressionLevel + " (expected: 0-9)");
        } 		
        this.compressionLevel = compressionLevel;
		this.compressNoContext = compressNoContext;
		this.decompressNoContext = decompressNoContext;
		this.minInflateBound = minInflateBound;
	}
	
	public PerMessageDeflateExtension(int compressionLevel, NoContext compressNoContext, NoContext decompressNoContext) {
		this(compressionLevel, 0, compressNoContext, decompressNoContext);
	}
	
	public PerMessageDeflateExtension(int compressionLevel) {
		this(compressionLevel, NoContext.OPTIONAL, NoContext.OPTIONAL);
	}
	
	public PerMessageDeflateExtension() {
		this(6, NoContext.OPTIONAL, NoContext.OPTIONAL);
	}

	PerMessageDeflateExtension(int compressionLevel, int minInflateBound, PerMessageDeflateParams params, boolean clientMode) {
		this.compressionLevel = compressionLevel;
		this.minInflateBound = minInflateBound;
		this.params = params;
		this.clientMode = clientMode;
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public ExtensionGroup getGroup() {
		return ExtensionGroup.COMPRESS;
	}

	boolean wrongName(List<String> extension) {
		return extension.size() < 1 || !NAME.equals(extension.get(0));
	}
	
	@Override
	public IExtension acceptOffer(List<String> extension) throws InvalidExtensionException {
		if (wrongName(extension)) {
			return null;
		}

		PerMessageDeflateParams params = PerMessageDeflateParams.parse(extension);
		boolean clientNoContext = false;
		boolean serverNoContext = false;
		
		if (params.isClientNoContext()) {
			if (decompressNoContext == NoContext.FORBIDDEN) {
				return null;
			} else {
				clientNoContext = true;
			}
		} else if (decompressNoContext == NoContext.REQUIRED) {
			return null;
		}
		
		if (params.isServerNoContext()) {
			if (compressNoContext == NoContext.FORBIDDEN) {
				return null;
			} else {
				serverNoContext = true;
			}
		} else if (compressNoContext == NoContext.REQUIRED) {
			return null;
		}
		
		Integer size = params.getServerMaxWindow();
		if (size != null && size.intValue() < PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE) {
			return null;
		}

		return new PerMessageDeflateExtension(compressionLevel, minInflateBound,
				new PerMessageDeflateParams(serverNoContext, 
						clientNoContext,
						PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE,
						PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE
				),
				false);
	}

	@Override
	public IExtension validateResponse(List<String> extension) throws InvalidExtensionException {
		if (wrongName(extension)) {
			return null;
		}
		
		PerMessageDeflateParams params = PerMessageDeflateParams.parse(extension);
		boolean clientNoContext = false;
		boolean serverNoContext = false;
		
		if (params.isClientNoContext()) {
			if (compressNoContext == NoContext.FORBIDDEN) {
				return null;
			} else {
				clientNoContext = true;
			}
		} else if (compressNoContext == NoContext.REQUIRED) {
			return null;
		}
		
		if (params.isServerNoContext()) {
			if (decompressNoContext == NoContext.FORBIDDEN) {
				return null;
			} else {
				serverNoContext = true;
			}
		} else if (decompressNoContext == NoContext.REQUIRED) {
			return null;
		}
		
		Integer size = params.getClientMaxWindow();
		if (size != null && size.intValue() < PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE) {
			return null;
		}
		
		return new PerMessageDeflateExtension(compressionLevel, minInflateBound,
				new PerMessageDeflateParams(serverNoContext, 
						clientNoContext,
						PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE,
						PerMessageDeflateParams.MAX_MAX_WINDOW_VALUE
				),
				true);
	}

	@Override
	public String[] offer() {
		int len = 1;
		
		if (compressNoContext == NoContext.REQUIRED) {
			len += 2;
		}
		if (decompressNoContext == NoContext.REQUIRED) {
			len += 2;
		}
		
		String[] offer = new String[len];
		int i = 0;
		
		offer[i++] = NAME;
		
		if (compressNoContext == NoContext.REQUIRED) {
			offer[i] = PerMessageDeflateParams.CLIENT_NO_CONTEXT;
			i += 2;
		}
		if (decompressNoContext == NoContext.REQUIRED) {
			offer[i] = PerMessageDeflateParams.SERVER_NO_CONTEXT;
			i += 2;
		}
		return offer;
	}

	@Override
	public String[] response() {
		int len = 1;
		
		if (params.isClientNoContext()) {
			len += 2;
		}
		if (params.isServerNoContext()) {
			len += 2;
		}
		
		String[] response = new String[len];
		int i = 0;
		
		response[i++] = NAME;
		
		if (params.isClientNoContext()) {
			response[i] = PerMessageDeflateParams.CLIENT_NO_CONTEXT;
			i += 2;
		}
		if (params.isServerNoContext()) {
			response[i] = PerMessageDeflateParams.SERVER_NO_CONTEXT;
			i += 2;
		}
		return response;
	}

	protected void updatePipeline(ICodecPipeline pipeline, IEncoder<Frame,Frame> encoder) {
		pipeline.addAfter(IWebSocketSessionConfig.WEBSOCKET_ENCODER, PERMESSAGE_DEFLATE_ENCODER, encoder);		
	}
	
	@Override
	public void updateEncoders(ICodecPipeline pipeline) {
		if (params != null) {
			boolean noContext = clientMode ? params.isClientNoContext() : params.isServerNoContext();

			updatePipeline(pipeline, new PerMessageDeflateEncoder(compressionLevel, noContext));
		}
	}
	
	protected void updatePipeline(ICodecPipeline pipeline, IDecoder<Frame,Frame> decoder) {
		pipeline.addAfter(IWebSocketSessionConfig.WEBSOCKET_DECODER, PERMESSAGE_DEFLATE_DECODER, decoder);
	}

	@Override
	public void updateDecoders(ICodecPipeline pipeline) {
		if (params != null) {
			boolean noContext = clientMode ? params.isServerNoContext() : params.isClientNoContext();

			updatePipeline(pipeline, new PerMessageDeflateDecoder(noContext, minInflateBound));
		}
	}
		
}
