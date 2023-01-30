/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.lcd;

/**
 * PictureMode  class defined the enum for monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/11/2023
 * @since 1.0.0
 */
public enum PictureMode {

	MALL("Mall/QSR", "00"),
	GENERAL("General", "01"),
	GOV("Gov./Corp.", "02"),
	TRANSPORTATION("Transportation", "03"),
	EDUCATION("Education", "04"),
	EXPERT("Expert1", "05"),
	APS("APS", "08"),
	CALIBRATION("Calibration", "11"),
	HOSPITAL("Hospital", "12");

	private final String name;
	private final String value;

	/**
	 * PictureMode instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	PictureMode(String name, String value) {
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