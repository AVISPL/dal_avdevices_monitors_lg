/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.commandNames;

/**
 * LgControllingCommand class defined the enum provides list controlling command
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/28/2022
 * @since 1.0.0
 */
public enum LgControllingCommand {

	INPUT_PRIORITY(LgLCDConstants.INPUT_PRIORITY, false, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_UP(LgLCDConstants.PRIORITY_UP, false, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_DOWN(LgLCDConstants.PRIORITY_DOWN, false, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_INPUT(LgLCDConstants.PRIORITY_INPUT, false, false, LgLCDConstants.commandNames.FAILOVER),

	FAILOVER_INPUT_LIST(LgLCDConstants.FAILOVER_INPUT_LIST, true, false, LgLCDConstants.commandNames.FAILOVER_INPUT_LIST),
	TILE_ID(LgLCDConstants.TILE_MODE_ID, true, false, LgLCDConstants.commandNames.TILE_ID),
	NATURAL_MODE(LgLCDConstants.NATURAL_MODE, true, false, LgLCDConstants.commandNames.NATURAL_MODE),
	NATURAL_SIZE(LgLCDConstants.NATURAL_SIZE, true, false, LgLCDConstants.commandNames.NATURAL_SIZE),
	SOFTWARE_VERSION(LgLCDConstants.SOFTWARE_VERSION, true, false, LgLCDConstants.commandNames.SOFTWARE_VERSION),
	TILE_MODE_SETTINGS(LgLCDConstants.TILE_MODE_SETTINGS, true, false, LgLCDConstants.commandNames.TILE_MODE_SETTINGS),
	SERIAL_NUMBER(LgLCDConstants.SERIAL_NUMBER, true, false, LgLCDConstants.commandNames.SERIAL_NUMBER),
	DISPLAY_STAND_BY_MODE(LgLCDConstants.DISPLAY_STAND_BY_MODE, true, false, LgLCDConstants.commandNames.DISPLAY_STAND_BY_MODE),
	DATE(LgLCDConstants.DATE, true, false, LgLCDConstants.commandNames.DATE),
	TIME(LgLCDConstants.TIME, true, false, LgLCDConstants.commandNames.TIME),
	NETWORK_SETTING(LgLCDConstants.NETWORK_SETTING, true, false, LgLCDConstants.commandNames.NETWORK_SETTING),
	VOLUME(LgLCDConstants.VOLUME, false, true, LgLCDConstants.commandNames.VOLUME),
	MUTE(LgLCDConstants.MUTE, false, true, LgLCDConstants.commandNames.MUTE),
	POWER_MANAGEMENT_MODE(LgLCDConstants.POWER_MANAGEMENT_MODE, false, true, LgLCDConstants.commandNames.POWER_MANAGEMENT_MODE),
	INPUT_SELECT(LgLCDConstants.INPUT_SELECT, true, false, LgLCDConstants.commandNames.INPUT_SELECT),
	BACKLIGHT(LgLCDConstants.BACKLIGHT, false, true, LgLCDConstants.commandNames.BACKLIGHT),
	TILE_MODE(LgLCDConstants.TILE_MODE, true, false, LgLCDConstants.commandNames.TILE_MODE_CONTROL),
	POWER(LgLCDConstants.POWER, false, true, LgLCDConstants.commandNames.POWER),
	TEMPERATURE(LgLCDConstants.TEMPERATURE, true, false, LgLCDConstants.commandNames.TEMPERATURE),
	FAN_STATUS(LgLCDConstants.FAN, true, false, LgLCDConstants.commandNames.FAN_STATUS),
	SYNC_STATUS(LgLCDConstants.SIGNAL, true, false, LgLCDConstants.commandNames.SYNC_STATUS),
	FAILOVER(LgLCDConstants.FAILOVER_MODE, true, false, LgLCDConstants.commandNames.FAILOVER),

	ASPECT_RATIO(LgLCDConstants.ASPECT_RATIO, false, true, LgLCDConstants.commandNames.ASPECT_RATIO),
	BRIGHTNESS_CONTROL(LgLCDConstants.BRIGHTNESS_CONTROL, false, true, LgLCDConstants.commandNames.BRIGHTNESS_CONTROL),
	CONTRAST(LgLCDConstants.CONTRAST, false, true, LgLCDConstants.commandNames.CONTRAST),
	PICTURE_MODE(LgLCDConstants.PICTURE_MODE, false, true, LgLCDConstants.commandNames.PICTURE_MODE),
	BRIGHTNESS(LgLCDConstants.BRIGHTNESS, false, true, LgLCDConstants.commandNames.BRIGHTNESS),
	SHARPNESS(LgLCDConstants.SHARPNESS, false, true, LgLCDConstants.commandNames.SHARPNESS),
	SCREEN_COLOR(LgLCDConstants.SCREEN_COLOR, false, true, LgLCDConstants.commandNames.SCREEN_COLOR),
	TINT(LgLCDConstants.TINT, false, true, LgLCDConstants.commandNames.TINT),
	COLOR_TEMPERATURE(LgLCDConstants.COLOR_TEMPERATURE, false, true, LgLCDConstants.commandNames.COLOR_TEMPERATURE),
	BALANCE(LgLCDConstants.BALANCE, false, true, LgLCDConstants.commandNames.BALANCE),
	SOUND_MODE(LgLCDConstants.SOUND_MODE, false, true, LgLCDConstants.commandNames.SOUND_MODE),
	NO_SIGNAL_POWER_OFF(LgLCDConstants.NO_SIGNAL_POWER_OFF, false, true, LgLCDConstants.commandNames.NO_SIGNAL_POWER_OFF),
	NO_IR_POWER_OFF(LgLCDConstants.NO_IR_POWER_OFF, false, true, LgLCDConstants.commandNames.NO_IR_POWER_OFF),
	LANGUAGE(LgLCDConstants.LANGUAGE, false, true, LgLCDConstants.commandNames.LANGUAGE),
	POWER_ON_STATUS(LgLCDConstants.POWER_ON_STATUS, false, true, LgLCDConstants.commandNames.POWER_ON_STATUS);

	private final String name;
	private final boolean isMonitorType;
	private final boolean isControlType;
	private commandNames commandNames;

	/**
	 * InputSourceDropdown instantiation
	 *
	 * @param name {@link #name}
	 * @param isMonitor {@link #isMonitorType}
	 * @param isControl {@link #isControlType}
	 * @param commandNames {@link #commandNames}
	 */
	LgControllingCommand(String name, boolean isMonitor, boolean isControl, commandNames commandNames) {
		this.name = name;
		this.isMonitorType = isMonitor;
		this.isControlType = isControl;
		this.commandNames = commandNames;
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
	 * Retrieves {@link #isControlType}
	 *
	 * @return value of {@link #isControlType}
	 */
	public boolean isControlType() {
		return isControlType;
	}

	/**
	 * Retrieves {@link #commandNames}
	 *
	 * @return value of {@link #commandNames}
	 */
	public LgLCDConstants.commandNames getCommandNames() {
		return commandNames;
	}

	/**
	 * Retrieves {@link #isMonitorType}
	 *
	 * @return value of {@link #isMonitorType}
	 */
	public boolean isMonitorType() {
		return isMonitorType;
	}

	/**
	 * Get command name by name of property
	 *
	 * @param value the value is name of command
	 * @return LgControllingCommand is LgControllingCommand instance
	 */
	public static LgControllingCommand getCommandByName(String value) {
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.getName().equalsIgnoreCase(value)) {
				return lgControllingCommand;
			}
		}
		throw new IllegalArgumentException(String.format("The command %s doesn't support", value));
	}

	/**
	 * Get command name by name of property
	 *
	 * @param value the value is name of command
	 * @return LgControllingCommand is LgControllingCommand instance
	 */
	public static String getNameByCommand(commandNames value) {
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.getCommandNames().name().equals(value.name())) {
				return lgControllingCommand.getName();
			}
		}
		throw new IllegalArgumentException(String.format("The command %s doesn't support", value));
	}
}