/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

/**
 * LgControllingCommand class defined the enum provides list controlling command
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/28/2022
 * @since 1.0.0
 */
public enum LgControllingCommand {

	VOLUME("Volume(%)"),
	PMD_MODE("PMDMode"),
	MUTE("Mute"),
	INPUT_SOURCE("Input"),
	PMD("StandbyMode"),
	BACKLIGHT("BackLight(%)"),
	INPUT_PRIORITY("InputPriority"),
	PRIORITY_UP("PriorityUp"),
	PRIORITY_DOWN("PriorityDown"),
	PRIORITY_INPUT("PriorityInput"),
	FAILOVER_MODE("FailOverMode"),
	TILE_MODE("TileMode"),
	NATURAL_MODE("NaturalMode"),
	INPUT_TYPE("InputType");

	private final String name;

	/**
	 * InputSourceDropdown instantiation
	 *
	 * @param name {@link #name}
	 */
	LgControllingCommand(String name) {
		this.name = name;
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
}