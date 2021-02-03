package org.snf4j.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefaultSctpSessionConfigTest {
	
	@Test
	public void testAll() throws Exception {
		DefaultSctpSessionConfig config = new DefaultSctpSessionConfig();
		
		assertEquals(0, config.getMinSctpStreamNumber());
		assertEquals(65536, config.getMaxSctpStreamNumber());
		assertEquals(Integer.MIN_VALUE, config.getMinSctpPayloadProtocolID());
		assertEquals(Integer.MAX_VALUE, config.getMaxSctpPayloadProtocolID());
		assertEquals(0, config.getDefaultSctpStreamNumber());
		assertEquals(0, config.getDefaultSctpPayloadProtocolID());
		assertFalse(config.getDefaultSctpUnorderedFlag());
		assertNull(config.createCodecExecutor(null));
		
		config.setMinSctpStreamNumber(40)
			.setMaxSctpStreamNumber(100)
			.setMinSctpPayloadProtocolID(77)
			.setMaxSctpPayloadProtocolID(777)
			.setDefaultSctpStreamNumber(103)
			.setDefaultSctpPayloadProtocolID(44)
			.setDefaultSctpUnorderedFlag(true);
		
		assertEquals(40, config.getMinSctpStreamNumber());
		assertEquals(100, config.getMaxSctpStreamNumber());
		assertEquals(77, config.getMinSctpPayloadProtocolID());
		assertEquals(777, config.getMaxSctpPayloadProtocolID());
		assertEquals(103, config.getDefaultSctpStreamNumber());
		assertEquals(44, config.getDefaultSctpPayloadProtocolID());
		assertTrue(config.getDefaultSctpUnorderedFlag());
		
	}
}
