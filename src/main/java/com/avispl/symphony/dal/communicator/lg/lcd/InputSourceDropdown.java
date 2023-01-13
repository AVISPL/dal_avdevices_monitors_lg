/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

/**
 * InputSourceDropdown class defined the enum provides list input source
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/4/2023
 * @since 1.0.0
 */
public enum InputSourceDropdown {

	AV("AV", "20", false),
	COMPONENT("", "40", false),
	RGB("RGB", "60", false),
	DVI_D_PC("DVI", "70", true),
	DVI_D_DTV("DVI", "80", false),
	HDMI1_DTV("HDMI1", "90", false),
	HDMI1_PC("HDMI1", "a0", true),
	HDMI2_OPS_DTV("HDMI2", "91", false),
	HDMI2_OPS_PC("HDMI2", "a1", true),
	HDMI3_OPS_DVID_DTV("HDMI3", "92", false),
	HDMI3_OPS_DVID_PC("HDMI3", "a2", true),
	OPS_DVID_DTV("OPS", "95", false),
	OPS_DVID_PC("OPS", "a5", true),
	HDMI3_DVID_DTV("HDMI3", "96", false),
	HDMI3_DVID_PC("HDMI3", "a6", true),
	OPS_DTV("OPS", "98", false),
	OPS_PC("OPS", "a8", true),
	DISPLAYPORT_DTV("DISPLAYPORT", "c0", false),
	DISPLAYPORT_PC("DISPLAYPORT", "d0", true),
	SUPERSIGN_PLAYER("SUPERSIGN", "e0", false),
	OTHERS("OTHERS", "e1", false),
	MULTI_SCREEN("MULTI_SCREEN", "e2", false);

	private final String name;
	private final String value;
	private final boolean isPCType;

	/**
	 * InputSourceDropdown instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 * @param isPCType {@link #isPCType}
	 */
	InputSourceDropdown(String name, String value, boolean isPCType) {
		this.name = name;
		this.value = value;
		this.isPCType = isPCType;
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

	/**
	 * Retrieves {@link #isPCType}
	 *
	 * @return value of {@link #isPCType}
	 */
	public boolean isPCType() {
		return isPCType;
	}

	/**
	 * Get value of input by name and type
	 *
	 * @param name the name is name of input
	 * @param type the type is boolean type value
	 * @return String is value of input
	 */
	public static String getValueOfEnumByNameAndType(String name, boolean type) {
		for (InputSourceDropdown inputSourceDropdown : InputSourceDropdown.values()) {
			if (inputSourceDropdown.getName().equals(name) && type == inputSourceDropdown.isPCType()) {
				return inputSourceDropdown.getValue();
			}
		}
		return LgLCDConstants.NONE;
	}
}