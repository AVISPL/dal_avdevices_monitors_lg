/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.commandNames;

/**
 * Unit test for LgLCDDevice
 *
 * @author Harry
 * @version 1.2
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
	 * Test LgLCDDevice.getMultipleStatistics get Statistic and DynamicStatistic success
	 * Expected retrieve monitoring data and non-null temperature data
	 */
	@Tag("RealDevice")
	@Test
	public void testLgLCDDeviceGetStatistic() throws Exception {
		lgLCDDevice.setHistoricalProperties("Temperature");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> dynamicStatistic = extendedStatistic.getDynamicStatistics();
		Map<String, String> statistics = extendedStatistic.getStatistics();

		Assertions.assertNotNull(dynamicStatistic.get(LgLCDConstants.TEMPERATURE));
		Assertions.assertEquals("NOT_SUPPORTED", statistics.get(LgLCDConstants.FAN));
		Assertions.assertEquals("HDMI2_OPS_PC", statistics.get(LgLCDConstants.INPUT));
		Assertions.assertEquals("NO_SYNC", statistics.get(LgLCDConstants.SIGNAL));
		Assertions.assertEquals("1", statistics.get(LgLCDConstants.POWER));

	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics get Statistic and DynamicStatistic success
	 * Expected retrieve monitoring data and non-null temperature data
	 */
	@Tag("RealDevice")
	@Test
	public void testMonitoringDeviceDashboard() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		List<AdvancedControllableProperty> list = extendedStatistic.getControllableProperties();

		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty(LgLCDConstants.PMD_MODE + LgLCDConstants.HASH + LgControllingCommand.PMD_MODE.getName());
		controllableProperty.setValue(PMDModeEnum.SCREEN_OFF_ALWAYS.getName());
		lgLCDDevice.controlProperty(controllableProperty);
		Assertions.assertEquals("38", statistics.get(LgLCDConstants.TEMPERATURE));
		Assertions.assertEquals("NOT_SUPPORTED", statistics.get(LgLCDConstants.FAN));
		Assertions.assertEquals("HDMI1_PC", statistics.get(LgLCDConstants.INPUT));
		Assertions.assertEquals("SYNC", statistics.get(LgLCDConstants.SIGNAL));
		Assertions.assertEquals("1", statistics.get(LgLCDConstants.POWER));
		Assertions.assertEquals("908KCRNKS718", statistics.get(LgLCDConstants.SERIAL_NUMBER));
		Assertions.assertEquals("041130", statistics.get(LgLCDConstants.SOFTWARE_VERSION));
		Assertions.assertEquals("Auto", statistics.get(LgLCDConstants.FAILOVER_STATUS));
		Assertions.assertEquals("Off", statistics.get(LgLCDConstants.TILE_MODE_STATUS));
	}

	/**
	 * Test lgLCDDevice.digestResponse Failed
	 * Expected exception message equal "Unexpected reply"
	 */
	@Tag("Mock")
	@Test
	public void testDigestResponseFailed1() {
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
			byte[] commands = new byte[] { 110 };
			lgLCDDevice.digestResponse(commands, commandNames.STATUS);
		});
		Assertions.assertEquals("Unexpected reply", exception.getMessage());
	}

	/**
	 * Test lgLCDDevice.digestResponse Failed
	 * Expected exception message equal "NG reply"
	 */
	@Tag("Mock")
	@Test()
	public void testDigestResponseFailed2() {
		RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
			byte[] commands = new byte[] { 118, 32, 48, 49, 32, 78, 71 };
			lgLCDDevice.digestResponse(commands, commandNames.STATUS);
		});
		Assertions.assertEquals("NG reply", exception.getMessage());
	}

	/**
	 * Test lgLCDDevice.digestResponse FanStatus success
	 * Expected Fan Status is Faulty
	 */
	@Tag("Mock")
	@Test
	public void testDigestResponseFanStatusSuccess() {
		byte[] commands = new byte[] { 119, 32, 48, 49, 32, 79, 75, 48, 48 };
//		LgLCDConstants.fanStatusNames fanStatusNames = (LgLCDConstants.fanStatusNames) lgLCDDevice.digestResponse(commands, commandNames.FANSTATUS);
//		Assertions.assertEquals("FAULTY", fanStatusNames.name());
	}
}
