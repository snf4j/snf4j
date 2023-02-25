/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2023 SNF4J contributors
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

import static org.snf4j.tls.handshake.HandshakeType.SERVER_HELLO;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.snf4j.core.ByteBufferArray;
import org.snf4j.tls.Args;
import org.snf4j.tls.alert.Alert;
import org.snf4j.tls.handshake.HandshakeType;

public class PreSharedKeyExtension extends KnownExtension implements IPreSharedKeyExtension {

	private final static ExtensionType TYPE = ExtensionType.PRE_SHARED_KEY;
	
	private final static OfferedPsk[] EMPTY = new OfferedPsk[0];
	
	private final int selectedIdentity;
	
	private final OfferedPsk[] offeredPsks;
	
	private final static AbstractExtensionParser PARSER = new AbstractExtensionParser() {

		@Override
		public ExtensionType getType() {
			return TYPE;
		}

		@Override
		public IExtension parse(HandshakeType handshakeType, ByteBufferArray srcs, int remaining) throws Alert {
			if (handshakeType.value() == SERVER_HELLO.value()) {
				if (remaining == 2) {
					return new PreSharedKeyExtension(srcs.getUnsignedShort());
				}
			}
			else if (remaining >= 2) {
				int subRemaining = srcs.getUnsignedShort();

				remaining -= 2;
				if (remaining >= subRemaining) {
					List<PskIdentity> identities = new ArrayList<PskIdentity>();

					remaining -= subRemaining;
					while (subRemaining >= 2) {
						int len = srcs.getUnsignedShort();

						if (subRemaining >= 2 + len + 4) {
							byte[] identity = new byte[len];
							long age;

							srcs.get(identity);
							age = srcs.getUnsignedInt();
							identities.add(new PskIdentity(identity, age));
							subRemaining -= 2 + len + 4;
						}
						else {
							break;
						}
					}
					if (subRemaining == 0) {
						if (remaining >= 2) {
							subRemaining = srcs.getUnsignedShort();

							remaining -= 2;
							if (remaining == subRemaining) {
								List<byte[]> binders = new ArrayList<byte[]>();

								while (subRemaining >= 1) {
									int len = srcs.getUnsigned();

									if (subRemaining >= 1 + len) {
										byte[] binder = new byte[len];

										srcs.get(binder);
										binders.add(binder);
										subRemaining -= 1 + len;
									}
									else {
										break;
									}
								}
								if (subRemaining == 0) {
									int size = identities.size();

									if (size > 0) { 
										if (size == binders.size()) {
											OfferedPsk[] psks = new OfferedPsk[size];

											for (int i=0; i<size; ++i) {
												psks[i] = new OfferedPsk(identities.get(i), binders.get(i));
											}
											return new PreSharedKeyExtension(psks);
										}
										else {
											throw decodeError("Different numbers of binders and identities");
										}
									}
									else {
										throw decodeError("No identities");
									}
								}
							}
						}
					}
				}
			}
			throw decodeError("Inconsistent length");
		}
	};
	
	public PreSharedKeyExtension(int selectedIdentity) {
		super(TYPE);
		Args.checkRange(selectedIdentity, 0, 0xffff, "selectedIdentity");
		this.selectedIdentity = selectedIdentity;
		offeredPsks = EMPTY;
	}

	public PreSharedKeyExtension(OfferedPsk... offeredPsks) {
		super(TYPE);
		Args.checkMin(offeredPsks, 1, "offeredPsks");
		this.selectedIdentity = -1;
		this.offeredPsks = offeredPsks;
	}
	
	@Override
	public OfferedPsk[] getOfferedPsks() {
		return offeredPsks;
	}

	@Override
	public int getSelectedIdentity() {
		return selectedIdentity;
	}

	@Override
	public int getDataLength() {
		if (selectedIdentity != -1) {
			return 2;
		}
		
		int sum = 4;
		
		for (OfferedPsk offeredPsk: offeredPsks) {
			sum += offeredPsk.getBinder().length + 1;
			sum += offeredPsk.getIdentity().getDataLength();
		}
		return sum;
	}
	
	@Override
	protected void getData(ByteBuffer buffer) {
		if (selectedIdentity != -1) {
			buffer.putShort((short) selectedIdentity);
		}
		else {
			int pos = buffer.position();
			
			buffer.position(pos+2);
			for (OfferedPsk offeredPsk: offeredPsks) {
				offeredPsk.getIdentity().getData(buffer);
			}			
			buffer.putShort(pos, (short) (buffer.position()-pos-2));
			
			pos = buffer.position();
			buffer.position(pos+2);
			for (OfferedPsk offeredPsk: offeredPsks) {
				byte[] binder = offeredPsk.getBinder();
				
				buffer.put((byte) binder.length);
				buffer.put(offeredPsk.getBinder());
			}			
			buffer.putShort(pos, (short) (buffer.position()-pos-2));
		}
	}

	public static IExtensionParser getParser() {
		return PARSER;
	}

	public static int bindersLength(OfferedPsk[] offeredPsks) {
		int length = 2;
		
		for (OfferedPsk offeredPsk: offeredPsks) {
			length += offeredPsk.getBinder().length + 1;
		}
		return length;
	}
	
	public static void updateBinders(byte[] clientHello, int offset, OfferedPsk[] offeredPsks) {
		offset += 2;
		for (OfferedPsk offeredPsk: offeredPsks) {
			byte[] binder = offeredPsk.getBinder();
			
			System.arraycopy(offeredPsk.getBinder(), 0, clientHello, ++offset, binder.length);
			offset += binder.length;
		}		
	}
}
