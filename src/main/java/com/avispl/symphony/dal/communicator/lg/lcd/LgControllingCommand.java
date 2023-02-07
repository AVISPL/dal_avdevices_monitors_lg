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

	VOLUME(LgLCDConstants.VOLUME, false, LgLCDConstants.commandNames.VOLUME),
	DISPLAY_STAND_BY_MODE(LgLCDConstants.DISPLAY_STAND_BY_MODE, false, LgLCDConstants.commandNames.DISPLAY_STAND_BY_MODE),
	MUTE(LgLCDConstants.MUTE, false, LgLCDConstants.commandNames.MUTE),
	INPUT_SELECT(LgLCDConstants.INPUT_SELECT, false, LgLCDConstants.commandNames.INPUT_SELECT),
	FAILOVER_INPUT_LIST(LgLCDConstants.FAILOVER_INPUT_LIST, false, LgLCDConstants.commandNames.FAILOVER_INPUT_LIST),
	POWER_MANAGEMENT_MODE(LgLCDConstants.POWER_MANAGEMENT_MODE, false, LgLCDConstants.commandNames.POWER_MANAGEMENT_MODE),
	BACKLIGHT(LgLCDConstants.BACKLIGHT, false, LgLCDConstants.commandNames.BACKLIGHT),
	INPUT_PRIORITY(LgLCDConstants.INPUT_PRIORITY, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_UP(LgLCDConstants.PRIORITY_UP, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_DOWN(LgLCDConstants.PRIORITY_DOWN, false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_INPUT(LgLCDConstants.PRIORITY_INPUT, false, LgLCDConstants.commandNames.FAILOVER),
	FAILOVER_MODE(LgLCDConstants.FAILOVER_MODE, false, LgLCDConstants.commandNames.FAILOVER),
	TILE_MODE(LgLCDConstants.TILE_MODE, false, LgLCDConstants.commandNames.TILE_MODE_CONTROL),
	NATURAL_MODE(LgLCDConstants.NATURAL_MODE, false, LgLCDConstants.commandNames.NATURAL_MODE),
	TILE_ID(LgLCDConstants.TILE_MODE_ID, false, LgLCDConstants.commandNames.TILE_ID),
	TILE_MODE_CONTROL(LgLCDConstants.TILE_MODE_SETTINGS, false, LgLCDConstants.commandNames.TILE_MODE_CONTROL),
	NATURAL_SIZE(LgLCDConstants.NATURAL_SIZE, false, LgLCDConstants.commandNames.NATURAL_SIZE),
	POWER(LgLCDConstants.POWER, false, LgLCDConstants.commandNames.POWER),
	NETWORK_SETTING(LgLCDConstants.NETWORK_SETTING, false, LgLCDConstants.commandNames.NETWORK_SETTING),
	INPUT(LgLCDConstants.INPUT_SELECT, false, LgLCDConstants.commandNames.INPUT),
	TEMPERATURE(LgLCDConstants.TEMPERATURE, false, LgLCDConstants.commandNames.TEMPERATURE),
	FAN_STATUS(LgLCDConstants.FAN, false, LgLCDConstants.commandNames.FAN_STATUS),
	SYNC_STATUS(LgLCDConstants.SIGNAL, false, LgLCDConstants.commandNames.SYNC_STATUS),
	SERIAL_NUMBER(LgLCDConstants.SERIAL_NUMBER, false, LgLCDConstants.commandNames.SERIAL_NUMBER),
	FAILOVER(LgLCDConstants.FAILOVER_MODE, false, LgLCDConstants.commandNames.FAILOVER),
	SOFTWARE_VERSION(LgLCDConstants.SOFTWARE_VERSION, false, LgLCDConstants.commandNames.SOFTWARE_VERSION),
	DATE(LgLCDConstants.DATE, false, LgLCDConstants.commandNames.DATE),
	TIME(LgLCDConstants.TIME, false, LgLCDConstants.commandNames.TIME),
	TILE_MODE_SETTINGS(LgLCDConstants.TILE_MODE_SETTINGS, false, LgLCDConstants.commandNames.TILE_MODE_SETTINGS),
	ASPECT_RATIO(LgLCDConstants.ASPECT_RATIO, true, LgLCDConstants.commandNames.ASPECT_RATIO),
	BRIGHTNESS_CONTROL(LgLCDConstants.BRIGHTNESS_CONTROL, true, LgLCDConstants.commandNames.BRIGHTNESS_CONTROL),
	CONTRAST(LgLCDConstants.CONTRAST, true, LgLCDConstants.commandNames.CONTRAST),
	PICTURE_MODE(LgLCDConstants.PICTURE_MODE, true, LgLCDConstants.commandNames.PICTURE_MODE),
	BRIGHTNESS(LgLCDConstants.BRIGHTNESS, true, LgLCDConstants.commandNames.BRIGHTNESS),
	SHARPNESS(LgLCDConstants.SHARPNESS, true, LgLCDConstants.commandNames.SHARPNESS),
	SCREEN_COLOR(LgLCDConstants.SCREEN_COLOR, true, LgLCDConstants.commandNames.SCREEN_COLOR),
	TINT(LgLCDConstants.TINT, true, LgLCDConstants.commandNames.TINT),
	COLOR_TEMPERATURE(LgLCDConstants.COLOR_TEMPERATURE, true, LgLCDConstants.commandNames.COLOR_TEMPERATURE),
	BALANCE(LgLCDConstants.BALANCE, true, LgLCDConstants.commandNames.BALANCE),
	SOUND_MODE(LgLCDConstants.SOUND_MODE, true, LgLCDConstants.commandNames.SOUND_MODE),
	NO_SIGNAL_POWER_OFF(LgLCDConstants.NO_SIGNAL_POWER_OFF, true, LgLCDConstants.commandNames.NO_SIGNAL_POWER_OFF),
	NO_IR_POWER_OFF(LgLCDConstants.NO_IR_POWER_OFF, true, LgLCDConstants.commandNames.NO_IR_POWER_OFF),
	LANGUAGE(LgLCDConstants.LANGUAGE, true, LgLCDConstants.commandNames.LANGUAGE),
	POWER_ON_STATUS(LgLCDConstants.POWER_ON_STATUS, true, LgLCDConstants.commandNames.POWER_ON_STATUS);

	private final String name;
	private final boolean isControlType;
	private commandNames commandNames;

	/**
	 * InputSourceDropdown instantiation
	 *
	 * @param name {@link #name}
	 * @param isControl {@link #isControlType}
	 * @param commandNames {@link #commandNames}
	 */
	LgControllingCommand(String name, boolean isControl, commandNames commandNames) {
		this.name = name;
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