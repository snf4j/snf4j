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

import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.codec.ICodecPipeline;
import org.snf4j.websocket.IWebSocketSessionConfig;

public class TestExtension implements IExtension {

	private final static String MASK_PARAM = "mask";

	private final char mask;
	
	private final String name;
	
	private final Object group;
	
	private final int rsv;
	
	private final boolean skip;
	
	public TestExtension(String name, Object group, char mask, int rsv, boolean skip) {
		this.mask = mask;
		this.name = name;
		this.group = group;
		this.rsv = rsv;
		this.skip = skip;
	}

	public TestExtension(String name, char mask, int rsv, boolean skip) {
		this(name, GroupIdentifier.COMPRESSION, mask, rsv, skip);
	}

	public TestExtension(String name, char mask, int rsv) {
		this(name, GroupIdentifier.COMPRESSION, mask, rsv, false);
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getGroupId() {
		return group;
	}
	
	@Override
	public IExtension acceptOffer(List<String> extension) throws InvalidExtensionException {
		int size = extension.size();
		
		if (size < 0) {
			return null;
		}
		if (name.equals(extension.get(0))) {
			if (size != 3 || !MASK_PARAM.equals(extension.get(1))) {
				throw new InvalidExtensionException("");
			}
			if ((""+mask).equals(extension.get(2))) {
				return this;
			}
		}
		return null;
	}

	@Override
	public IExtension validateResponse(List<String> extension) throws InvalidExtensionException {
		if (extension.size() == 3 && name.equals(extension.get(0)) 
				&& MASK_PARAM.equals(extension.get(1))
				&& (""+mask).equals(extension.get(2))) {
			return this;
		}
		return null;
	}

	@Override
	public List<String> offer() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(name);
		list.add(MASK_PARAM);
		list.add(""+mask);
		return list;
	}

	@Override
	public List<String> response() {
		return offer();
	}

	@Override
	public void updateEncoders(ICodecPipeline pipeline) {
		pipeline.addAfter(IWebSocketSessionConfig.WEBSOCKET_ENCODER, "ex-encoder"+mask, new TestEncoder('A', rsv, skip));
	}

	@Override
	public void updateDecoders(ICodecPipeline pipeline) {
		pipeline.addAfter(IWebSocketSessionConfig.WEBSOCKET_DECODER, "ex-decoder"+mask, new TestDecoder('A', rsv, skip));
	}

}
