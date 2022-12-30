/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.controlProperties;

/**
 * Unit test for Control of LgLCDDevice
 *
 * @author Harry
 * @version 1.2
 * @since 1.2
 */
public class LgLCDControlTest {

	/**
	 * Set power property with value 1 (On)
	 * Expect verify with method powerOn
	 */
	@Tag("RealDevice")
	@Test
	public void testPowerOn() throws Exception {
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty(controlProperties.power.name());
		controllableProperty.setValue("1");

		LgLCDDevice lgLCDDeviceSpy = Mockito.spy(LgLCDDevice.class);
		lgLCDDeviceSpy.controlProperty(controllableProperty);
		Mockito.verify(lgLCDDeviceSpy, times(1)).powerON();
	}

	/**
	 * Set power property with value 0 (Off)
	 * Expect verify with method powerOff
	 */
	@Tag("Mock")
	@Test
	public void testPowerOff() throws Exception {
		ControllableProperty controllableProperty = new ControllableProperty();
		controllableProperty.setProperty(LgControllingCommand.VOLUME.getCommand());
		controllableProperty.setValue("50");

		LgLCDDevice lgLCDDeviceSpy = Mockito.spy(LgLCDDevice.class);
		Mockito.doNothing().when(lgLCDDeviceSpy).powerOFF();
		lgLCDDeviceSpy.controlProperty(controllableProperty);
		Mockito.verify(lgLCDDeviceSpy, times(1)).powerOFF();
	}
}
