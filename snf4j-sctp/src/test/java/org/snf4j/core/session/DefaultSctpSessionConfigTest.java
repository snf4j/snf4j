package org.snf4j.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class DefaultSctpSessionConfigTest {
	
	@Test
	public void testAll() throws Exception {
		DefaultSctpSessionConfig config = new DefaultSctpSessionConfig();
		
		assertEquals(0, config.getMinSctpStreamNumber());
		assertEquals(65536, config.getMaxSctpStreamNumber());
		assertEquals(Integer.MIN_VALUE, config.getMinSctpPayloadProtocolID());
		assertEquals(Integer.MAX_VALUE, config.getMaxSctpPayloadProtocolID());
		assertNull(config.createCodecExecutor(null));
		
		config.setMinSctpStreamNumber(40)
			.setMaxSctpStreamNumber(100)
			.setMinSctpPayloadProtocolID(77)
			.setMaxSctpPayloadProtocolID(777);
		
		assertEquals(40, config.getMinSctpStreamNumber());
		assertEquals(100, config.getMaxSctpStreamNumber());
		assertEquals(77, config.getMinSctpPayloadProtocolID());
		assertEquals(777, config.getMaxSctpPayloadProtocolID());
		
	}
}
