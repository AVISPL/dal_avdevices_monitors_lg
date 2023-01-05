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

	AV("AV", "20"),
	COMPONENT("", "40"),
	RGB("RGB", "60"),
	DVI_D_PC("DVI_D_PC", "70"),
	DVI_D_DTV("DVI_D_DTV", "80"),
	HDMI1_DTV("HDMI1_DTV", "90"),
	HDMI1_PC("HDMI1_PC", "a0"),
	HDMI2_OPS_DTV("HDMI2_OPS_DTV", "91"),
	HDMI2_OPS_PC("HDMI2_OPS_PC", "a1"),
	HDMI3_OPS_DVID_DTV("HDMI3_OPS_DVID_DTV", "92"),
	HDMI3_OPS_DVID_PC("HDMI3_OPS_DVID_PC", "a2"),
	OPS_DVID_DTV("OPS_DVID_DTV", "95"),
	OPS_DVID_PC("OPS_DVID_PC", "a5"),
	HDMI3_DVID_DTV("HDMI3_DVID_DTV", "96"),
	HDMI3_DVID_PC("HDMI3_DVID_PC", "a6"),
	OPS_DTV("OPS_DTV", "98"),
	OPS_PC("OPS_PC", "a8"),
	DISPLAYPORT_DTV("DISPLAYPORT_DTV", "c0"),
	DISPLAYPORT_PC("DISPLAYPORT_PC", "d0"),
	SUPERSIGN_PLAYER("SUPERSIGN_PLAYER", "e0"),
	OTHERS("OTHERS", "e1"),
	MULTI_SCREEN("MULTI_SCREEN", "e2");

	private final String name;
	private final String value;

	InputSourceDropdown(String name, String value) {
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

	public static String getNameByValue(String value) {
		for (InputSourceDropdown inputSourceDropdown : InputSourceDropdown.values()) {
			if (inputSourceDropdown.getValue().equals(value)) {
				return inputSourceDropdown.getName();
			}
		}
		return "None";
	}

	public static String getValueByName(String name) {
		for (InputSourceDropdown inputSourceDropdown : InputSourceDropdown.values()) {
			if (inputSourceDropdown.getName().equals(name)) {
				return inputSourceDropdown.getValue();
			}
		}
		return "None";
	}
}