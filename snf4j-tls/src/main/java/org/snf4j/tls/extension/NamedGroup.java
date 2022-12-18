/*
 * -------------------------------- MIT License --------------------------------
 * 
 * Copyright (c) 2022 SNF4J contributors
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

import org.snf4j.tls.IntConstant;

public class NamedGroup extends IntConstant {

	public static final NamedGroup SECP256R1 = new NamedGroup("secp256r1",0x0017, ECNamedGroupSpec.SECP256R1);

	public static final NamedGroup SECP384R1 = new NamedGroup("secp384r1",0x0018, ECNamedGroupSpec.SECP384R1);

	public static final NamedGroup SECP521R1 = new NamedGroup("secp521r1",0x0019, ECNamedGroupSpec.SECP521R1);
	
	public static final NamedGroup X25519 = new NamedGroup("x25519",0x001D, XECNamedGroupSpec.X25519);

	public static final NamedGroup X448 = new NamedGroup("x448",0x001E, XECNamedGroupSpec.X448);
	
	public static final NamedGroup FFDHE2048 = new NamedGroup("ffdhe2048",0x0100, DHNamedGroupSpec.FFDHE2048);

	public static final NamedGroup FFDHE3072 = new NamedGroup("ffdhe3072",0x0101, DHNamedGroupSpec.FFDHE3072);

	public static final NamedGroup FFDHE4096 = new NamedGroup("ffdhe4096",0x0102, DHNamedGroupSpec.FFDHE4096);
	
	public static final NamedGroup FFDHE6144 = new NamedGroup("ffdhe6144",0x0103, DHNamedGroupSpec.FFDHE6144);
   
	public static final NamedGroup FFDHE8192 = new NamedGroup("ffdhe8192",0x0104, DHNamedGroupSpec.FFDHE8192);
    
	private final static NamedGroup[] KNOWN = new NamedGroup[FFDHE8192.value()+1];
	
	private static void known(NamedGroup... knowns) {
		for (NamedGroup known: knowns) {
			KNOWN[known.value()] = known;
		}
	}

	static {
		known(
				SECP256R1,
				SECP384R1,
				SECP521R1,
				X25519,
				X448,
				FFDHE2048,
				FFDHE3072,
				FFDHE4096,
				FFDHE6144,
				FFDHE8192
				);
	}	
	
	private final INamedGroupSpec spec;
	
	protected NamedGroup(String name, int value, INamedGroupSpec spec) {
		super(name, value);
		this.spec = spec;
	}

	protected NamedGroup(int value) {
		super(value);
		this.spec = null;
	}
	
	public INamedGroupSpec spec() {
		return spec;
	}
	
	public static NamedGroup of(int value) {
		if (value >= 0 && value < KNOWN.length) {
			NamedGroup known = KNOWN[value];
			
			if (known != null) {
				return known;
			}
		}
		return new NamedGroup(value);
	}
}
