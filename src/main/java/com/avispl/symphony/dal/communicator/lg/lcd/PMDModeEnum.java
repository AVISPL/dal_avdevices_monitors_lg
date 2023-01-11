/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

/**
 * PMDModeEnum class defined the enum provides list PDM mode value
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/28/2022
 * @since 1.0.0
 */
public enum PMDModeEnum {

	POWER_OFF("Power Off", "00"),
	SUSTAIN_ASPECT_RATIO("Sustain Aspect Ratio", "01"),
	SCREEN_OFF("Screen Off", "02"),
	SCREEN_OFF_ALWAYS("Screen Off Always", "03"),
	SCREEN_OFF_BACKLIGHT_ON("Screen Off & Backlight On", "04"),
	NETWORK_READY("Network Ready", "05");

	private final String name;
	private final String value;

	/**
	 * PMDModeEnum instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	PMDModeEnum(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}
}