/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import java.util.HashMap;
import java.util.Map;

public class LgLCDConstants {
	enum powerStatusNames {ON, OFF}

	final static Map<powerStatusNames, byte[]> powerStatus = new HashMap<powerStatusNames, byte[]>() {{
		put(powerStatusNames.ON, new byte[] { '0', '0' });
		put(powerStatusNames.OFF, new byte[] { '0', '1' });
	}};

	enum replyStatusNames {OK, NG}

	final static Map<replyStatusNames, byte[]> replyStatusCodes = new HashMap<replyStatusNames, byte[]>() {{
		put(replyStatusNames.OK, new byte[] { 'O', 'K' });
		put(replyStatusNames.NG, new byte[] { 'N', 'G' });
	}};

	enum fanStatusNames {FAULTY, NORMAL, NO_FAN, NOT_SUPPORTED}

	final static Map<fanStatusNames, byte[]> fanStatusCodes = new HashMap<fanStatusNames, byte[]>() {{
		put(fanStatusNames.FAULTY, new byte[] { '0', '0' });
		put(fanStatusNames.NORMAL, new byte[] { '0', '1' });
		put(fanStatusNames.NO_FAN, new byte[] { '0', '2' });
	}};

	enum commandNames {
		POWER, INPUT, TEMPERATURE, FAN_STATUS, STATUS, GET, SERIAL_NUMBER, SOFTWARE_VERSION, FAILOVER, DATE, TIME,
		TILE_MODE, PMD, DST_START_TIME, DST_END_TIME, DISPLAY_AND_SOUND, BACKLIGHT, INPUT_SOURCE, MUTE, VOLUME, PMD_MODE, PMD_MODE_PARAM, FAILOVER_INPUT_LIST
	}

	final static Map<commandNames, byte[]> commands = new HashMap<commandNames, byte[]>() {{
		put(commandNames.POWER, new byte[] { 'k', 'd' });
		put(commandNames.INPUT, new byte[] { 'x', 'b' });
		put(commandNames.TEMPERATURE, new byte[] { 'd', 'n' });
		put(commandNames.FAN_STATUS, new byte[] { 'd', 'w' });
		put(commandNames.STATUS, new byte[] { 's', 'v' });
		put(commandNames.GET, new byte[] { 'F', 'F' });
		put(commandNames.SERIAL_NUMBER, new byte[] { 'f', 'y' });
		put(commandNames.SOFTWARE_VERSION, new byte[] { 'f', 'z' });
		put(commandNames.FAILOVER, new byte[] { 'm', 'i' });
		put(commandNames.FAILOVER_INPUT_LIST, new byte[] { 'm', 'j' });
		put(commandNames.DATE, new byte[] { 'f', 'a' });
		put(commandNames.TIME, new byte[] { 'f', 'x' });
		put(commandNames.TILE_MODE, new byte[] { 'd', 'z' });
		put(commandNames.PMD, new byte[] { 'f', 'j' });
		put(commandNames.DST_START_TIME, new byte[] { 's', 'd', ' ', '0', '1', ' ', 'f', 'f', ' ', 'f', 'f', ' ', 'f', 'f', ' ', 'f', 'f' });
		put(commandNames.DST_END_TIME, new byte[] { 's', 'd', ' ', '0', '2', ' ', 'f', 'f', ' ', 'f', 'f', ' ', 'f', 'f', ' ', 'f', 'f' });
		put(commandNames.BACKLIGHT, new byte[] { 'm', 'g' });
		put(commandNames.MUTE, new byte[] { 'k', 'e' });
		put(commandNames.VOLUME, new byte[] { 'k', 'f' });
		put(commandNames.PMD_MODE, new byte[] { 's', 'n' });
		put(commandNames.PMD_MODE_PARAM, new byte[] { '0', 'c', ' ', 'f', 'f' });
		put(commandNames.INPUT_SOURCE, new byte[] { 'x', 'b' });
	}};

	final static byte[] signalStatus = { '0', '2', ' ', 'F', 'F' };

	enum syncStatusNames {NO_SYNC, SYNC}

	final static Map<syncStatusNames, byte[]> syncStatusCodes = new HashMap<syncStatusNames, byte[]>() {{
		put(syncStatusNames.NO_SYNC, new byte[] { '0', '2', '0', '0' });
		put(syncStatusNames.SYNC, new byte[] { '0', '2', '0', '1' });
	}};

	enum inputNames {HDMI3, HDMI3_PC, AV, COMPONENT, RGB, DVI_D_PC, DVI_D_DTV, HDMI1_DTV, HDMI1_PC, HDMI2_OPS_DTV, HDMI2_OPS_PC, HDMI3_OPS_DVID_DTV, HDMI3_OPS_DVID_PC, OPS_DVID_DTV, OPS_DVID_PC, HDMI3_DVID_DTV, HDMI3_DVID_PC, OPS_DTV, OPS_PC, DISPLAYPORT_DTV, DISPLAYPORT_PC, SUPERSIGN_PLAYER, OTHERS, MULTI_SCREEN, OFF}

	final static Map<inputNames, byte[]> inputs = new HashMap<inputNames, byte[]>() {{
		put(inputNames.AV, new byte[] { '2', '0' });
		put(inputNames.COMPONENT, new byte[] { '4', '0' });
		put(inputNames.RGB, new byte[] { '6', '0' });
		put(inputNames.DVI_D_PC, new byte[] { '7', '0' });
		put(inputNames.DVI_D_DTV, new byte[] { '8', '0' });
		put(inputNames.HDMI1_DTV, new byte[] { '9', '0' });
		put(inputNames.HDMI1_PC, new byte[] { 'a', '0' });
		put(inputNames.HDMI2_OPS_DTV, new byte[] { '9', '1' });
		put(inputNames.HDMI2_OPS_PC, new byte[] { 'a', '1' });
		put(inputNames.HDMI3_OPS_DVID_DTV, new byte[] { '9', '2' });
		put(inputNames.HDMI3_OPS_DVID_PC, new byte[] { 'a', '2' });
		put(inputNames.OPS_DVID_DTV, new byte[] { '9', '5' });
		put(inputNames.OPS_DVID_PC, new byte[] { 'a', '5' });
		put(inputNames.HDMI3_DVID_DTV, new byte[] { '9', '6' });
		put(inputNames.HDMI3_DVID_PC, new byte[] { 'a', '6' });
		put(inputNames.OPS_DTV, new byte[] { '9', '8' });
		put(inputNames.OPS_PC, new byte[] { 'a', '8' });
		put(inputNames.DISPLAYPORT_DTV, new byte[] { 'c', '0' });
		put(inputNames.DISPLAYPORT_PC, new byte[] { 'd', '0' });
		put(inputNames.SUPERSIGN_PLAYER, new byte[] { 'e', '0' });
		put(inputNames.OTHERS, new byte[] { 'e', '1' });
		put(inputNames.MULTI_SCREEN, new byte[] { 'e', '2' });
		put(inputNames.HDMI3_PC, new byte[] { 'd', '2' });
		put(inputNames.HDMI3, new byte[] { 'c', '2' });
	}};

	enum controlProperties {power, input}

	public static String POWER = "Power";
	public static String FAN = "Fan";
	public static String INPUT = "Input";
	public static String TEMPERATURE = "Temperature(C)";
	public static String SIGNAL = "InputSignal";
	public static String SERIAL_NUMBER = "SerialNumber";
	public static String SOFTWARE_VERSION = "SoftwareVersion";
	public static String FAILOVER_STATUS = "FailOverMode";
	public static String DATE_TIME = "DateTime";
	public static String TILE_MODE_STATUS = "TileMode";
	public static String DPM_STATUS = "StandbyMode";
	public static String START_TIME = "DSTStartTime";
	public static String END_TIME = "DSTEndTime";
	public static String DISPLAY_AND_SOUND = "DisplayAndSound";
	public static String FAILOVER = "FailOver";
	public static String HASH = "#";
	public static String BACKLIGHT = "BackLight(%)";
	public static String BACKLIGHT_VALUE = "BacklightValue(%)";
	public static String DPM = "StandbyMode";
	public static String INPUT_SOURCE = "Input";
	public static String MUTE = "Mute";
	public static String VOLUME = "Volume(%)";
	public static String VOLUME_VALUE = "VolumeValue(%)";
	public static String ON = "On";
	public static String OFF = "Off";
	public static String PMD_MODE = "PMDMode";
	public static int NUMBER_ONE = 1;
	public static int ZERO = 0;
	public static int MAX_RANGE_BACKLIGHT = 100;
	public static int MAX_RANGE_VOLUME = 100;
	public static String MUTE_VALUE = "00";
	public static String UNMUTE_VALUE = "01";
	public static String NONE = "None";
	public static String EMPTY_STRING = "";
	public static String COLON = ":";
	public static String SPACE = " ";
	public static String AUTO = "Auto";
	public static String INPUT_PRIORITY = "InputPriority";
	public static String PRIORITY = "Priority";
	public static String PRIORITY_INPUT = "PriorityInput";
	public static String PRIORITY_DOWN = "PriorityDown";
	public static String UPPING = "Upping";
	public static String UP = "Up";
	public static String PRIORITY_UP = "PriorityUp";
	public static String DOWNING = "Downing";
	public static String DOWN = "Down";
	public static String MANUAL = "Manual";
	public static String HDMI_1 = "HDMI1";
	public static String HDMI_2 = "HDMI2";
	public static String HDMI_3 = "HDMI3";
	public static String DISPLAYPORT = "DISPLAYPORT";
	public static String BYTE_COMMAND = "0c ";
	public static String IS_VALID_CONFIG_MANAGEMENT = "true";
	public static String AM = "AM";
	public static String PM = "PM";
}
