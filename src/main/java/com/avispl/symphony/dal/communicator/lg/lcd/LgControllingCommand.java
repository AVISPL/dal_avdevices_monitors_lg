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

	VOLUME("Volume(%)", false, LgLCDConstants.commandNames.VOLUME),
	PMD_MODE("PMDMode", false, LgLCDConstants.commandNames.PMD_MODE),
	MUTE("Mute", false, LgLCDConstants.commandNames.MUTE),
	INPUT_SOURCE("Input", false, LgLCDConstants.commandNames.INPUT_SOURCE),
	PMD("StandbyMode", false, LgLCDConstants.commandNames.PMD),
	BACKLIGHT("BackLight(%)", false, LgLCDConstants.commandNames.BACKLIGHT),
	INPUT_PRIORITY("InputPriority", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_UP("PriorityUp", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_DOWN("PriorityDown", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_INPUT("PriorityInput", false, LgLCDConstants.commandNames.FAILOVER),
	FAILOVER_MODE("FailOverMode", false, LgLCDConstants.commandNames.FAILOVER),
	TILE_MODE("TileMode", false, LgLCDConstants.commandNames.TILE_MODE_CONTROL),
	NATURAL_MODE("NaturalMode", false, LgLCDConstants.commandNames.NATURAL_MODE),
	INPUT_TYPE("InputType", false, LgLCDConstants.commandNames.INPUT);

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
}