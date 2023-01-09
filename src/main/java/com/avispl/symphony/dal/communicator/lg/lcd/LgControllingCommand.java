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

	VOLUME("Volume", ""),
	PMD_MODE("PMDMode", ""),
	MUTE("Mute", ""),
	INPUT_SOURCE("InputSource", ""),
	PMD("DPM", ""),
	BACKLIGHT("BackLight(%)", ""),
	INPUT_PRIORITY("InputPriority", ""),
	PRIORITY_UP("PriorityUp", ""),
	PRIORITY_DOWN("PriorityDown", ""),
	PRIORITY_INPUT("PriorityInput", ""),
	FAILOVER_STATUS("FailOverStatus", "");

	private final String name;
	private final String command;

	LgControllingCommand(String name, String command) {
		this.name = name;
		this.command = command;
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
	 * Retrieves {@link #command}
	 *
	 * @return value of {@link #command}
	 */
	public String getCommand() {
		return command;
	}


	public static LgControllingCommand getCommandByName(String value) {
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.getName().equalsIgnoreCase(value)) {
				return lgControllingCommand;
			}
		}
		throw new IllegalArgumentException(String.format("The command %s doesn't support", value));
	}
}