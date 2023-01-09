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
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
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
		Assertions.assertEquals("Error Unexpected reply", exception.getMessage());
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
		LgLCDConstants.fanStatusNames fanStatusNames = (LgLCDConstants.fanStatusNames) lgLCDDevice.digestResponse(commands, commandNames.FAN_STATUS);
		Assertions.assertEquals("FAULTY", fanStatusNames.name());
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control backlight success
	 * Expected control backlight success
	 */
	@Tag("RealDevice")
	@Test
	public void testControlBackLight() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.BACKLIGHT.getName();
		String value = "20";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals(value, statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control DPM success
	 * Expected control DPM success
	 */
	@Tag("RealDevice")
	@Test
	public void testControlDPM() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.PMD.getName();
		String value = PowerManagement.SECOND_10.getName();
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals(value, statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control InputSource as HDMI1 success
	 * Expected control HDMI1 success
	 */
	@Tag("RealDevice")
	@Test
	public void testControlInputSource() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.PMD.getName();
		String value = PowerManagement.SECOND_10.getName();
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals(value, statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control Mute volume success
	 * Expected control Mute volume success
	 */
	@Tag("RealDevice")
	@Test
	public void testControlMuteVolume() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.MUTE.getName();
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control Unmute success
	 * Expected control Unmute success
	 */
	@Tag("RealDevice")
	@Test
	public void tesControlUnmute() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.MUTE.getName();
		String value = "0";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("0", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control volume success
	 * Expected control volume success
	 */
	@Tag("RealDevice")
	@Test
	public void testControlVolume() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgControllingCommand.PRIORITY_DOWN.getName();
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("20", statistics.get(property));
	}

	/**
	 * Test lgLCDDevice.digestResponse volume error
	 * Expected digestResponse volume error
	 */
	@Tag("RealDevice")
	@Test
	public void testControlVolumeError() throws Exception {
		byte[] commands = new byte[] { 102, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.VOLUME), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse input source error
	 * Expected digestResponse input source error
	 */
	@Tag("RealDevice")
	@Test
	public void testControlInputSourceError() throws Exception {
		byte[] commands = new byte[] { 98, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse mute volume error
	 * Expected digestResponse mute volume error
	 */
	@Tag("RealDevice")
	@Test
	public void testMonitoringControlMuteError() throws Exception {
		byte[] commands = new byte[] { 98, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse backlight error
	 * Expected digestResponse backlight error
	 */
	@Tag("RealDevice")
	@Test
	public void testMonitoringControlBackLightError() throws Exception {
		byte[] commands = new byte[] { 103, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}
}
