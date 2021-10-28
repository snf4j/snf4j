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

import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.core.codec.IDecoder;
import org.snf4j.core.codec.IEncoder;
import org.snf4j.websocket.IWebSocketSessionConfig;
import org.snf4j.websocket.extensions.GroupIdentifier;
import org.snf4j.websocket.extensions.IExtension;
import org.snf4j.websocket.extensions.InvalidExtensionException;
import org.snf4j.websocket.frame.Frame;

/**
 * The WebSocket Per-Message Compression Extension as described in RFC 7692
 * 
 * @author <a href="http://snf4j.org">SNF4J.ORG</a>
 */
public class PerMessageDeflateExtension implements IExtension {
	
	final static String NAME = "permessage-deflate";
	
	/**
	 * The default key identifying the pre-message deflate decoder
	 * {@link PerMessageDeflateDecoder} in the default codec pipeline created by the
	 * SNF4J framework.
	 */
	public final static String PERMESSAGE_DEFLATE_DECODER = "permessage-deflate-decoder";
	
	/**
	 * The default key identifying the pre-message deflate encoder
	 * {@link PerMessageDeflateEncoder} in the default codec pipeline created by the
	 * SNF4J framework.
	 */
	public final static String PERMESSAGE_DEFLATE_ENCODER = "permessage-deflate-encoder";
	
	/** 
	 * The context takeover control options
	 */
	public enum NoContext {
		
		/** The no context takeover is forbidden */
		FORBIDDEN,
		
		/** The context takeover is the preferred option but can be changed during the negotiation */
		OPTIONAL,
		
		/** The no context takeover is required */
		REQUIRED
	}
	
	private final int compressionLevel;

	private NoContext compressNoContext;
	
	private NoContext decompressNoContext;
	
	private boolean clientMode;
	
	private int minInflateBound;
	
	private PerMessageDeflateParams params;
	
	/**
	 * Constructs a pre-message deflate extension with specified compression level,
	 * minimum upper bound on the decompressed size and the context takeover control
	 * options for compression/decompression.
	 * 
	 * @param compressionLevel    the compression level (0-9)
	 * @param minInflateBound     determines the minimum upper bound on the
	 *                            decompressed size. Setting this parameter to
	 *                            proper value may speed up the decompression of
	 *                            highly compressed data.
	 * @param compressNoContext   the context takeover control for compression
	 * @param decompressNoContext the context takeover control for decompression
	 */
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
	
	/**
	 * Constructs a pre-message deflate extension with specified compression level
	 * and the context takeover control options for compression/decompression.
	 * 
	 * @param compressionLevel    the compression level (0-9)
	 * @param compressNoContext   the context takeover control for compression
	 * @param decompressNoContext the context takeover control for decompression
	 */
	public PerMessageDeflateExtension(int compressionLevel, NoContext compressNoContext, NoContext decompressNoContext) {
		this(compressionLevel, 0, compressNoContext, decompressNoContext);
	}
	
	/**
	 * Constructs a pre-message deflate extension with specified compression level
	 * and the {@link NoContext#OPTIONAL OPTIONAL} context takeover control option for
	 * compression/decompression.
	 * 
	 * @param compressionLevel the compression level (0-9)
	 */
	public PerMessageDeflateExtension(int compressionLevel) {
		this(compressionLevel, NoContext.OPTIONAL, NoContext.OPTIONAL);
	}
	
	/**
	 * Constructs a pre-message deflate extension with default compression level (6)
	 * and the {@link NoContext#OPTIONAL OPTIONAL} context takeover control option for
	 * compression/decompression.
	 */
	public PerMessageDeflateExtension() {
		this(6, NoContext.OPTIONAL, NoContext.OPTIONAL);
	}

	PerMessageDeflateExtension(int compressionLevel, int minInflateBound, PerMessageDeflateParams params, boolean clientMode) {
		this.compressionLevel = compressionLevel;
		this.minInflateBound = minInflateBound;
		this.params = params;
		this.clientMode = clientMode;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @return "permessage-deflate"
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @return {@link GroupIdentifier#COMPRESSION}
	 */
	@Override
	public Object getGroupId() {
		return GroupIdentifier.COMPRESSION;
	}

	boolean wrongName(List<String> extension) {
		return extension.size() < 1 || !NAME.equals(extension.get(0));
	}
	
	@Override
	public IExtension acceptOffer(List<String> offer) throws InvalidExtensionException {
		if (wrongName(offer)) {
			return null;
		}

		PerMessageDeflateParams params = PerMessageDeflateParams.parse(offer);
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
	public IExtension validateResponse(List<String> response) throws InvalidExtensionException {
		if (wrongName(response)) {
			return null;
		}
		
		PerMessageDeflateParams params = PerMessageDeflateParams.parse(response);
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
	public List<String> offer() {
		List<String> offer = new ArrayList<String>();
		
		offer.add(NAME);
		
		if (compressNoContext == NoContext.REQUIRED) {
			offer.add(PerMessageDeflateParams.CLIENT_NO_CONTEXT);
			offer.add(null);
		}
		if (decompressNoContext == NoContext.REQUIRED) {
			offer.add(PerMessageDeflateParams.SERVER_NO_CONTEXT);
			offer.add(null);
		}
		return offer;
	}

	@Override
	public List<String> response() {
		List<String> response = new ArrayList<String>();
		
		response.add(NAME);
		
		if (params.isClientNoContext()) {
			response.add(PerMessageDeflateParams.CLIENT_NO_CONTEXT);
			response.add(null);
		}
		if (params.isServerNoContext()) {
			response.add(PerMessageDeflateParams.SERVER_NO_CONTEXT);
			response.add(null);
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
