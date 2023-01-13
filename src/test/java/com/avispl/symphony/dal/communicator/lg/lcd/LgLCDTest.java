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
	void setUp() throws Exception {
		lgLCDDevice = new LgLCDDevice();
		lgLCDDevice.setHost("172.31.254.160");
		lgLCDDevice.init();
		lgLCDDevice.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		lgLCDDevice.disconnect();
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics get Statistic and DynamicStatistic success
	 * Expected retrieve monitoring data and non-null temperature data
	 */
	@Tag("RealDevice")
	@Test
	void testLgLCDDeviceGetStatistic() throws Exception {
		lgLCDDevice.setConfigManagement("true");
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
	void testMonitoringDeviceDashboard() throws Exception {
		long daye = System.currentTimeMillis();
		System.out.println(daye);
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		daye = System.currentTimeMillis();
		System.out.println(daye);
		List<AdvancedControllableProperty> list = extendedStatistic.getControllableProperties();

		Assertions.assertEquals("38", statistics.get(LgLCDConstants.TEMPERATURE));
		Assertions.assertEquals("NOT_SUPPORTED", statistics.get(LgLCDConstants.FAN));
		Assertions.assertEquals("HDMI1_PC", statistics.get(LgLCDConstants.INPUT));
		Assertions.assertEquals("SYNC", statistics.get(LgLCDConstants.SIGNAL));
		Assertions.assertEquals("1", statistics.get(LgLCDConstants.POWER));
		Assertions.assertEquals("908KCRNKS718", statistics.get(LgLCDConstants.SERIAL_NUMBER));
		Assertions.assertEquals("041130", statistics.get(LgLCDConstants.SOFTWARE_VERSION));
		Assertions.assertEquals("Auto", statistics.get(LgLCDConstants.FAILOVER_STATUS));
		Assertions.assertEquals("Off", statistics.get(LgLCDConstants.TILE_MODE));
	}

	/**
	 * Test lgLCDDevice.digestResponse Failed
	 * Expected exception message equal "Unexpected reply"
	 */
	@Tag("Mock")
	@Test
	void testDigestResponseFailed1() {
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
	void testDigestResponseFailed2() {
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
	void testDigestResponseFanStatusSuccess() {
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
	void testControlBackLight() throws Exception {
		lgLCDDevice.setConfigManagement("true");
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
	void testControlDPM() throws Exception {
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
	void testControlInputSource() throws Exception {
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgControllingCommand.INPUT_SOURCE.getName();
		String value = InputSourceDropdown.HDMI1_DTV.getName();
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
	void testControlMuteVolume() throws Exception {
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
	void tesControlUnmute() throws Exception {
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
	void testControlVolume() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH + LgLCDConstants.VOLUME;
		String value = "20";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("20", statistics.get(property));
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control failover is off
	 * Expected control failover is off success
	 */
	@Tag("RealDevice")
	@Test
	void testControlFailoverIsOFF() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.FAILOVER_STATUS;
		String value = "0";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("0", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control failover is on
	 * Expected control failover is on success
	 */
	@Tag("RealDevice")
	@Test
	void testControlFailoverIsOn() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.FAILOVER_STATUS;
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control InputPriority is auto
	 * Expected control InputPriority is auto success
	 */
	@Tag("RealDevice")
	@Test
	void testControlInputPriorityIsAuto() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.INPUT_PRIORITY;
		String value = "0";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("0", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control InputPriority is auto
	 * Expected control InputPriority is auto success
	 */
	@Tag("RealDevice")
	@Test
	void testControlInputPriorityIsManual() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.INPUT_PRIORITY;
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control priority down
	 * Expected control priority down success
	 */
	@Tag("RealDevice")
	@Test
	void testControlInputPriorityDown() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.PRIORITY_INPUT;
		String value = "HDMI1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("HDMI1", statistics.get(property));

		property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.PRIORITY_DOWN;
		value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control priority down
	 * Expected control priority down success
	 */
	@Tag("RealDevice")
	@Test
	void testControlInputPriorityUp() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.PRIORITY_INPUT;
		String value = "HDMI1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("HDMI1", statistics.get(property));

		property = LgLCDConstants.FAILOVER + LgLCDConstants.HASH + LgLCDConstants.PRIORITY_UP;
		value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control tile mode off
	 * Expected control tile mode off success
	 */
	@Tag("RealDevice")
	@Test
	void testControlTileModeOff() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.TILE_MODE_SETTINGS + LgLCDConstants.HASH + LgLCDConstants.TILE_MODE;
		String value = "0";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("0", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control tile mode on
	 * Expected control tile mode on success
	 */
	@Tag("RealDevice")
	@Test
	void testControlTileModeON() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.TILE_MODE_SETTINGS + LgLCDConstants.HASH + LgLCDConstants.TILE_MODE;
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control natural off
	 * Expected control natural off success
	 */
	@Tag("RealDevice")
	@Test
	void testControlNaturalModeOff() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.TILE_MODE_SETTINGS + LgLCDConstants.HASH + LgLCDConstants.NATURAL_MODE;
		String value = "0";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("0", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control natural on
	 * Expected control natural on success
	 */
	@Tag("RealDevice")
	@Test
	void testControlNaturalModeOn() throws Exception {
		lgLCDDevice.setConfigManagement("true");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		ControllableProperty controllableProperty = new ControllableProperty();
		String property = LgLCDConstants.TILE_MODE_SETTINGS + LgLCDConstants.HASH + LgLCDConstants.NATURAL_MODE;
		String value = "1";
		controllableProperty.setProperty(property);
		controllableProperty.setValue(value);
		lgLCDDevice.controlProperty(controllableProperty);

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertEquals("1", statistics.get(property));
	}

	/**
	 * Test LgLCDDevice.getMultipleStatistics control natural on
	 * Expected control natural on success
	 */
	@Tag("RealDevice")
	@Test
	void testConfigManagementIsFalse() throws Exception {
		lgLCDDevice.setConfigManagement("false");
		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();

		extendedStatistic = (ExtendedStatistics) lgLCDDevice.getMultipleStatistics().get(0);
		statistics = extendedStatistic.getStatistics();
		Assertions.assertNotNull(statistics.get("DateTime"));
		Assertions.assertEquals("172.31.254.16", statistics.get("DNSServer"));
		Assertions.assertEquals("Auto", statistics.get("FailOverMode"));
		Assertions.assertEquals("NOT_SUPPORTED", statistics.get("Fan"));
		Assertions.assertEquals("172.31.254.2", statistics.get("Gateway"));
		Assertions.assertEquals("HDMI1_PC", statistics.get("Input"));
		Assertions.assertEquals("SYNC", statistics.get("InputSignal"));
		Assertions.assertEquals("041130", statistics.get("SoftwareVersion"));
		Assertions.assertEquals("908KCRNKS718", statistics.get("SerialNumber"));
		Assertions.assertEquals("On", statistics.get("StandbyMode"));
		Assertions.assertEquals("255.255.255.0", statistics.get("SubNetmask"));
		Assertions.assertEquals("41", statistics.get("Temperature(C)"));
		Assertions.assertEquals("On", statistics.get("TileMode"));
	}

	/**
	 * Test lgLCDDevice.digestResponse volume error
	 * Expected digestResponse volume error
	 */
	@Tag("RealDevice")
	@Test
	void testControlVolumeError() throws Exception {
		byte[] commands = new byte[] { 102, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.VOLUME), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse input source error
	 * Expected digestResponse input source error
	 */
	@Tag("RealDevice")
	@Test
	void testControlInputSourceError() throws Exception {
		byte[] commands = new byte[] { 98, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse mute volume error
	 * Expected digestResponse mute volume error
	 */
	@Tag("RealDevice")
	@Test
	void testMonitoringControlMuteError() throws Exception {
		byte[] commands = new byte[] { 98, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}

	/**
	 * Test lgLCDDevice.digestResponse backlight error
	 * Expected digestResponse backlight error
	 */
	@Tag("RealDevice")
	@Test
	void testMonitoringControlBackLightError() throws Exception {
		byte[] commands = new byte[] { 103, 32, 48, 49, 32, 78, 71, 49, 52, 120 };
		Assertions.assertThrows(ResourceNotReachableException.class, () -> lgLCDDevice.digestResponse(commands, commandNames.INPUT_SOURCE), "expect throw error because response NG");
	}
}
