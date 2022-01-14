/*
 * Copyright (c) 2021 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;


import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.statisticsProperties;

/**
 * Unit test for LG LCD Device
 *
 * Send and retrieve data success
 *
 * @author Harry
 * @since 1.2
 */
public class LgLCDTest {

	private ExtendedStatistics extendedStatistic;
	private LgLCDDevice lgLCDDevice;

	@BeforeEach
	public void setUp() throws Exception {
		lgLCDDevice = new LgLCDDevice();
		lgLCDDevice.setHost("172.31.254.160");
		lgLCDDevice.init();
		lgLCDDevice.connect();
	}

	@AfterEach
	public void destroy() throws Exception {
		lgLCDDevice.disconnect();
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics get DynamicStatistic success
	 * Expected retrieve monitoring data and non null temperature data
	 */
	@Tag("RealDevice")
	@Test
	public void testLgLCDDeviceGetDynamicStatistic() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> dynamicStatistic = extendedStatistic.getDynamicStatistics();
		Map<String, String> statistics = extendedStatistic.getStatistics();

		Assertions.assertNotNull(dynamicStatistic.get(statisticsProperties.temperature.name()));
		Assertions.assertEquals("NOT_SUPPORTED", statistics.get(statisticsProperties.fan.name()));
		Assertions.assertEquals("HDMI1_PC", statistics.get(statisticsProperties.input.name()));
		Assertions.assertEquals("SYNC", statistics.get(statisticsProperties.signal.name()));
	}
}
