/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022-2023 SNF4J contributors
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
package org.snf4j.tls.extension;

import java.nio.ByteBuffer;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class SupportedGroupsExtension extends KnownExtension implements ISupportedGroupsExtension {
	
	private final static ExtensionType TYPE = ExtensionType.SUPPORTED_GROUPS;
	
	private final NamedGroup[] groups;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (remaining >= 4) {
				int len = srcs.getUnsignedShort();
				
				if ((len & 1) != 0 || len == 0) {
					throw decodeError("Incorrect length");
				}
				remaining -= 2;
				if (len == remaining) {
					NamedGroup[] groups = new NamedGroup[len/2];
					
					for (int i=0; i<groups.length; ++i) {
						groups[i] = NamedGroup.of(srcs.getUnsignedShort());
					}
					return new SupportedGroupsExtension(groups);
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public SupportedGroupsExtension(NamedGroup... groups) {
		super(TYPE);
		Args.checkMin(groups, 1, "groups");
		this.groups = groups;
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}

	@Override
	public NamedGroup[] getGroups() {
		return groups;
	}
	
	@Override
	public int getDataLength() {
		return 2 + groups.length*2;
	}

	@Override
	protected void getData(ByteBuffer buffer) {
		buffer.putShort((short) (groups.length*2));
		for (NamedGroup group: groups) {
			buffer.putShort((short)group.value());
		}
	}
}
