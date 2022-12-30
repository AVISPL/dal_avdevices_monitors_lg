/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

/**
 * InputSourceEnum  class defined the enum for monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/12/2022
 * @since 1.0.0
 */
public enum InputSourceEnum {

	HDMI1("HDMI1", "a0"),
	HDMI2("HDMI2", "a1"),
	HDMI3_OPS_DTV("HDMI 3/OPS/DVI", "a2"),
	DISPLAYPORT_DTV("DISPLAYPORT", "d0"),
	NONE("None", "");

	private final String name;
	private final String value;

	InputSourceEnum(String name, String value) {
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
		for (InputSourceEnum inputSourceEnum : InputSourceEnum.values()) {
			if (inputSourceEnum.getValue().equals(value)) {
				return inputSourceEnum.getName();
			}
		}
		return InputSourceEnum.NONE.getName();
	}

	public static String getValueByName(String name) {
		for (InputSourceEnum inputSourceEnum : InputSourceEnum.values()) {
			if (inputSourceEnum.getName().equals(name)) {
				return inputSourceEnum.getValue();
			}
		}
		return InputSourceEnum.NONE.getName();
	}
}