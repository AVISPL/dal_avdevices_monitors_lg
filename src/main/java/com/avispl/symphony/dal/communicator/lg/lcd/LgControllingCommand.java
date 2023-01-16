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
	PMD_MODE("PowerManagementMode", false, LgLCDConstants.commandNames.PMD_MODE),
	MUTE("Mute", false, LgLCDConstants.commandNames.MUTE),
	INPUT_SOURCE("Input", false, LgLCDConstants.commandNames.INPUT_SOURCE),
	PMD("DisplayStandbyMode", false, LgLCDConstants.commandNames.PMD),
	BACKLIGHT("BackLight(%)", false, LgLCDConstants.commandNames.BACKLIGHT),
	INPUT_PRIORITY("InputPriority", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_UP("PriorityUp", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_DOWN("PriorityDown", false, LgLCDConstants.commandNames.FAILOVER),
	PRIORITY_INPUT("PriorityInput", false, LgLCDConstants.commandNames.FAILOVER),
	FAILOVER_MODE("FailOverMode", false, LgLCDConstants.commandNames.FAILOVER),
	TILE_MODE("TileMode", false, LgLCDConstants.commandNames.TILE_MODE_CONTROL),
	NATURAL_MODE("NaturalMode", false, LgLCDConstants.commandNames.NATURAL_MODE),
	INPUT_TYPE("InputType", false, LgLCDConstants.commandNames.INPUT),
	ASPECT_RATIO("AspectRatio", true, LgLCDConstants.commandNames.ASPECT_RATIO),
	BRIGHTNESS_SIZE("BrightnessSize", true, LgLCDConstants.commandNames.BRIGHTNESS_SIZE),
	CONTRAST("Contrast", true, LgLCDConstants.commandNames.CONTRAST),
	PICTURE_MODE("PictureMode", true, LgLCDConstants.commandNames.PICTURE_MODE),
	BRIGHTNESS("Brightness", true, LgLCDConstants.commandNames.BRIGHTNESS),
	SHARPNESS("Sharpness", true, LgLCDConstants.commandNames.SHARPNESS),
	SCREEN_COLOR("ScreenColor", true, LgLCDConstants.commandNames.SCREEN_COLOR),
	TINT("Tint", true, LgLCDConstants.commandNames.TINT),
	COLOR_TEMPERATURE("ColorTemperature", true, LgLCDConstants.commandNames.COLOR_TEMPERATURE),
	BALANCE("Balance", true, LgLCDConstants.commandNames.BALANCE),
	SOUND_MODE("SoundMode", true, LgLCDConstants.commandNames.SOUND_MODE),
	NO_SIGNAL_POWER_OFF("NoSignalPowerOff", true, LgLCDConstants.commandNames.NO_SIGNAL_POWER_OFF),
	NO_IR_POWER_OFF("NoIRPowerOff", true, LgLCDConstants.commandNames.NO_IR_POWER_OFF),
	LANGUAGE("Language", true, LgLCDConstants.commandNames.LANGUAGE),
	POWER_ON_STATUS("PowerOnStatus", true, LgLCDConstants.commandNames.POWER_ON_STATUS);

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