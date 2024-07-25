package org.snf4j.quic.metric;

import static org.junit.Assert.assertSame;

import org.junit.Test;

public class QuicMetricsBuilderTest {

	@Test
	public void testBuild() {
		QuicMetricsBuilder b = new QuicMetricsBuilder();
		assertSame(QuicMetrics.DEFAULT, b.build());
		assertSame(QuicMetrics.DEFAULT, b.build());
		
		CongestionControllerMetric ccm = new CongestionControllerMetric();
		b.congestion(ccm);
		IQuicMetrics m = b.build();
		assertSame(ccm, m.getCongestionMetric());
	}
}
