/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SocketCommunicator;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.commandNames;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.controlProperties;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.powerStatusNames;
import com.avispl.symphony.dal.communicator.lg.lcd.LgLCDConstants.replyStatusNames;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * LG LCD Device Adapter
 * An implementation of SocketCommunicator to provide communication and interaction with LG device
 *
 * Static Monitored Statistics
 * <li>
 * Temperature(C), SerialNumber, SoftwareVersion, InputSignal, InputSelect, Fan, StandbyMode, TileMode,
 * FailOverMode, SoftwareVersion, SerialNumber, DateTim, SubNetmask, IPAddress, DNSServer, Gateway
 * </li>
 *
 * Management Control
 *
 * Display
 * <li>
 * Power, Language, BackLight(%), Mute, Volume(%), AspectRatio, BrightnessSize, PictureMode, Contrast, Brightness, Sharpness, Color, Tint, ColorTemperature(K), Balance, SoundMode
 * </li>
 *
 * FailOver
 * <li>
 * FailOverMode, InputPriority, PriorityInput, PriorityUp, PriorityDown
 * </li>
 *
 * Input
 * <li>
 * InputType, InputSelect
 * </li>
 * Historical Monitored Statistics
 * <li> Temperature </li>
 *
 * @author Harry, Kevin
 * @version 1.4
 * @since 1.4
 */
public class LgLCDDevice extends SocketCommunicator implements Controller, Monitorable {

	int monitorID;
	private final Set<String> historicalProperties = new HashSet<>();
	private boolean isEmergencyDelivery;
	private Map<String, String> cacheMapOfPropertyNameAndValue = new HashMap<>();
	private Map<String, String> cacheMapOfPriorityInputAndValue = new HashMap<>();
	private ExtendedStatistics localExtendedStatistics;
	private boolean isInputTypePC;
	private Set<String> failedMonitor = new HashSet<>();

	/**
	 * store configManagement adapter properties
	 */
	private String configManagement;
	/**
	 * configManagement in boolean value
	 */
	private boolean isConfigManagement;

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		if (cacheMapOfPriorityInputAndValue != null) {
			cacheMapOfPriorityInputAndValue.clear();
		}

		if (cacheMapOfPropertyNameAndValue != null) {
			cacheMapOfPropertyNameAndValue.clear();
		}

		failedMonitor.clear();
		super.internalDestroy();
	}

	/**
	 * Constructor set the TCP/IP port to be used as well the default monitor ID
	 */
	public LgLCDDevice() {
		super();
		this.setPort(9761);
		this.monitorID = 1;

		// set list of command success strings (included at the end of response when command succeeds, typically ending with command prompt)
		this.setCommandSuccessList(Collections.singletonList("OK"));
		// set list of error response strings (included at the end of response when command fails, typically ending with command prompt)
		this.setCommandErrorList(Collections.singletonList("NG"));
	}

	/**
	 * Retrieves {@link #configManagement}
	 *
	 * @return value of {@link #configManagement}
	 */
	public String getConfigManagement() {
		return configManagement;
	}

	/**
	 * Sets {@link #configManagement} value
	 *
	 * @param configManagement new value of {@link #configManagement}
	 */
	public void setConfigManagement(String configManagement) {
		this.configManagement = configManagement;
	}

	/**
	 * Retrieves {@link #historicalProperties}
	 *
	 * @return value of {@link #historicalProperties}
	 */
	public String getHistoricalProperties() {
		return String.join(LgLCDConstants.COMMA, this.historicalProperties);
	}

	/**
	 * Sets {@link #historicalProperties} value
	 *
	 * @param historicalProperties new value of {@link #historicalProperties}
	 */
	public void setHistoricalProperties(String historicalProperties) {
		this.historicalProperties.clear();
		Arrays.asList(historicalProperties.split(LgLCDConstants.COMMA)).forEach(propertyName -> this.historicalProperties.add(propertyName.trim()));
	}

	/**
	 * This method is recalled by Symphony to get the current monitor ID (Future purpose)
	 *
	 * @return int This returns the current monitor ID.
	 */
	public int getMonitorID() {
		return monitorID;
	}

	/**
	 * This method is used by Symphony to set the monitor ID (Future purpose)
	 *
	 * @param monitorID This is the monitor ID to be set
	 */
	public void setMonitorID(int monitorID) {
		this.monitorID = monitorID;
	}

	/**
	 * This method is recalled by Symphony to control a list of properties
	 *
	 * @param controllableProperties This is the list of properties to be controlled
	 * @return byte This returns the calculated xor checksum.
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : controllableProperties) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method is recalled by Symphony to control specific property
	 *
	 * @param controllableProperty This is the property to be controlled
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {

		reentrantLock.lock();
		try {
			if (localExtendedStatistics == null) {
				return;
			}
			Map<String, String> stats = this.localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = this.localExtendedStatistics.getControllableProperties();
			isEmergencyDelivery = true;
			String value = String.valueOf(controllableProperty.getValue());
			String property = controllableProperty.getProperty();
			if (controllableProperty.getProperty().equalsIgnoreCase(controlProperties.power.name())) {
				if (controllableProperty.getValue().toString().equals(String.valueOf(LgLCDConstants.NUMBER_ONE))) {
					powerON();
				} else if (controllableProperty.getValue().toString().equals(String.valueOf(LgLCDConstants.ZERO))) {
					powerOFF();
				}
			} else {
				String propertyKey;
				String[] propertyList = property.split(LgLCDConstants.HASH);
				String group = property + LgLCDConstants.HASH;
				if (property.contains(LgLCDConstants.HASH)) {
					propertyKey = propertyList[1];
					group = propertyList[0] + LgLCDConstants.HASH;
				} else {
					propertyKey = property;
				}
				LgControllingCommand lgControllingCommand = LgControllingCommand.getCommandByName(propertyKey);
				switch (lgControllingCommand) {
					case VOLUME:
						String dataConvert = Integer.toHexString((int) Float.parseFloat(value));
						sendRequestToControlValue(commandNames.VOLUME, dataConvert.getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.VOLUME_VALUE, String.valueOf((int) Float.parseFloat(value)));
						break;
					case MUTE:
						String mute = LgLCDConstants.UNMUTE_VALUE;
						if (String.valueOf(LgLCDConstants.NUMBER_ONE).equals(value)) {
							mute = LgLCDConstants.MUTE_VALUE;
						}
						sendRequestToControlValue(commandNames.MUTE, mute.getBytes(StandardCharsets.UTF_8));
						break;
					case BACKLIGHT:
						dataConvert = Integer.toHexString((int) Float.parseFloat(value));
						sendRequestToControlValue(commandNames.BACKLIGHT, dataConvert.getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.BACKLIGHT_VALUE, String.valueOf((int) Float.parseFloat(value)));
						break;
					case INPUT_SOURCE:
						dataConvert = InputSourceDropdown.getValueOfEnumByNameAndType(value, isInputTypePC);
						sendRequestToControlValue(commandNames.INPUT_SOURCE, dataConvert.getBytes(StandardCharsets.UTF_8));
						String inputSourceValue = InputSourceDropdown.getValueOfEnumByNameAndType(value, isInputTypePC);
						stats.put(LgLCDConstants.INPUT_SELECT, EnumTypeHandler.getNameEnumByValue(InputSourceDropdown.class, inputSourceValue));
						String signal = getSyncStatus().name();
						stats.put(LgLCDConstants.SIGNAL, signal);
						break;
					case INPUT_TYPE:
						isInputTypePC = LgLCDConstants.PC.equalsIgnoreCase(value);
						break;
					case PMD_MODE:
						dataConvert = LgLCDConstants.BYTE_COMMAND + EnumTypeHandler.getValueOfEnumByName(PowerManagementModeEnum.class, value);
						sendRequestToControlValue(commandNames.PMD_MODE, dataConvert.getBytes(StandardCharsets.UTF_8));
						break;
					case PMD:
						dataConvert = EnumTypeHandler.getValueOfEnumByName(PowerManagement.class, value);
						sendRequestToControlValue(commandNames.PMD, dataConvert.getBytes(StandardCharsets.UTF_8));
						if (LgLCDConstants.OFF.equalsIgnoreCase(value)) {
							stats.put(LgLCDConstants.STAND_BY_MODE, LgLCDConstants.OFF);
						} else {
							stats.put(LgLCDConstants.STAND_BY_MODE, LgLCDConstants.ON);
						}
						break;
					case FAILOVER_MODE:
						String inputPriority = group + LgLCDConstants.INPUT_PRIORITY;
						String priorityInput = group + LgLCDConstants.PRIORITY_INPUT;
						String priorityInputDown = group + LgLCDConstants.PRIORITY_DOWN;
						String priorityInputUp = group + LgLCDConstants.PRIORITY_UP;
						int failOverStatus = Integer.parseInt(value);
						String failOverName = LgLCDConstants.OFF;
						if (failOverStatus == LgLCDConstants.ZERO) {
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.OFF.getValue().getBytes(StandardCharsets.UTF_8));
							//Remove all priority 0,1,2,3.etc, priorityInput, and inputPriority.
							stats.remove(inputPriority);
							advancedControllableProperties.removeIf(item -> item.getName().equals(inputPriority));

							stats.remove(priorityInput);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInput));

							stats.remove(priorityInputDown);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

							stats.remove(priorityInputUp);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

							if (cacheMapOfPriorityInputAndValue != null) {
								for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
									stats.remove(group + input.getKey());
								}
							}
						} else if (failOverStatus == LgLCDConstants.NUMBER_ONE) {
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8));
							updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);

							AdvancedControllableProperty controlInputPriority = controlSwitch(stats, group + LgLCDConstants.INPUT_PRIORITY, String.valueOf(LgLCDConstants.ZERO),
									LgLCDConstants.AUTO,
									LgLCDConstants.MANUAL);
							advancedControllableProperties.add(controlInputPriority);
							failOverName = LgLCDConstants.AUTO;
						}
						stats.put(LgLCDConstants.FAILOVER_STATUS, failOverName);
						break;
					case INPUT_PRIORITY:
						String failoverStatus = LgLCDConstants.AUTO;
						if (String.valueOf(LgLCDConstants.ZERO).equals(value)) {
							if (cacheMapOfPriorityInputAndValue != null) {
								for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
									stats.remove(group + input.getKey());
								}
							}
							priorityInputDown = group + LgLCDConstants.PRIORITY_DOWN;
							priorityInputUp = group + LgLCDConstants.PRIORITY_UP;
							priorityInput = group + LgLCDConstants.PRIORITY_INPUT;

							stats.remove(priorityInputDown);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

							stats.remove(priorityInputUp);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

							stats.remove(priorityInput);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInput));

							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8));
						} else {
							failoverStatus = LgLCDConstants.MANUAL;
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.MANUAL.getValue().getBytes(StandardCharsets.UTF_8));
							retrieveDataByCommandName(commandNames.FAILOVER_INPUT_LIST, commandNames.GET);
							// failover is Manual
							AdvancedControllableProperty controlInputPriority = controlSwitch(stats, group + LgLCDConstants.INPUT_PRIORITY, String.valueOf(LgLCDConstants.NUMBER_ONE), LgLCDConstants.AUTO,
									LgLCDConstants.MANUAL);
							advancedControllableProperties.add(controlInputPriority);
							for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
								stats.put(group + entry.getKey(), entry.getValue());
							}
							stats.put(group + LgLCDConstants.PRIORITY_UP, LgLCDConstants.EMPTY_STRING);
							advancedControllableProperties.add(createButton(group + LgLCDConstants.PRIORITY_UP, LgLCDConstants.UP, LgLCDConstants.UPPING, 0));

							stats.put(group + LgLCDConstants.PRIORITY_DOWN, LgLCDConstants.EMPTY_STRING);
							advancedControllableProperties.add(createButton(group + LgLCDConstants.PRIORITY_DOWN, LgLCDConstants.DOWN, LgLCDConstants.DOWNING, 0));

							String[] inputSelected = cacheMapOfPriorityInputAndValue.values().toArray(new String[0]);

							String inputSourceDefaulValue = LgLCDConstants.NONE;
							Optional<Entry<String, String>> inputSourceOption = cacheMapOfPriorityInputAndValue.entrySet().stream().findFirst();
							if (inputSourceOption.isPresent()) {
								inputSourceDefaulValue = inputSourceOption.get().getValue();
							}
							AdvancedControllableProperty controlInputSource = controlDropdown(stats, inputSelected, group + LgLCDConstants.PRIORITY_INPUT, inputSourceDefaulValue);
							advancedControllableProperties.add(controlInputSource);
						}
						stats.put(LgLCDConstants.FAILOVER_STATUS, failoverStatus);
						break;
					case PRIORITY_INPUT:
						cacheMapOfPropertyNameAndValue.remove(LgLCDConstants.PRIORITY_INPUT);
						cacheMapOfPropertyNameAndValue.put(propertyKey, value);
						break;
					case PRIORITY_DOWN:
						String currentPriority = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.PRIORITY_INPUT);
						Map<String, String> newPriorityMap = new HashMap<>();
						Entry<String, String> priorityKey = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> item.getValue().equals(currentPriority)).findFirst().orElse(null);
						int len = cacheMapOfPriorityInputAndValue.size();
						for (int i = 1; i <= len; i++) {
							String currentKeyOfPriority = LgLCDConstants.PRIORITY + i;
							String previousKeyOfPriority = LgLCDConstants.PRIORITY + (i - 1);
							String nextKeyOfPriority = LgLCDConstants.PRIORITY + (i + 1);
							if (currentPriority.equals(cacheMapOfPriorityInputAndValue.get(LgLCDConstants.PRIORITY + len))) {
								break;
							} else {
								if (priorityKey.getKey().equals(currentKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(nextKeyOfPriority));
								} else if (priorityKey.getKey().equals(previousKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(previousKeyOfPriority));
								} else {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(currentKeyOfPriority));
								}
							}
						}
						if (!newPriorityMap.isEmpty()) {
							cacheMapOfPriorityInputAndValue = newPriorityMap;
						}
						StringBuilder stringBuilder = new StringBuilder();
						for (String values : cacheMapOfPriorityInputAndValue.values()) {
							stringBuilder.append(EnumTypeHandler.getValueOfEnumByName(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgLCDConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8));
						for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
							stats.put(group + input.getKey(), input.getValue());
						}
						break;
					case PRIORITY_UP:
						currentPriority = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.PRIORITY_INPUT);
						newPriorityMap = new HashMap<>();
						priorityKey = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> item.getValue().equals(currentPriority)).findFirst().orElse(null);
						len = cacheMapOfPriorityInputAndValue.size();
						for (int i = 1; i <= len; i++) {
							String currentKeyOfPriority = LgLCDConstants.PRIORITY + i;
							String previousKeyOfPriority = LgLCDConstants.PRIORITY + (i - 1);
							String nextKeyOfPriority = LgLCDConstants.PRIORITY + (i + 1);
							if (currentPriority.equals(cacheMapOfPriorityInputAndValue.get(LgLCDConstants.PRIORITY + 1))) {
								break;
							} else {
								if (priorityKey.getKey().equals(nextKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(nextKeyOfPriority));
								} else if (priorityKey.getKey().equals(currentKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(previousKeyOfPriority));
								} else {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(currentKeyOfPriority));
								}
							}
						}
						if (!newPriorityMap.isEmpty()) {
							cacheMapOfPriorityInputAndValue = newPriorityMap;
						}
						stringBuilder = new StringBuilder();
						for (String values : cacheMapOfPriorityInputAndValue.values()) {
							stringBuilder.append(EnumTypeHandler.getValueOfEnumByName(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgLCDConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8));
						for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
							stats.remove(group + entry.getKey());
							stats.put(group + entry.getKey(), entry.getValue());
						}
						break;
					case TILE_MODE:
						String tileModeValue = LgLCDConstants.OFF;
						String naturalModeKey = group + LgLCDConstants.NATURAL_MODE;
						String naturalSize = group + LgLCDConstants.NATURAL_SIZE;
						String tileID = group + LgLCDConstants.TILE_MODE_ID;
						String paramTileMode;
						if (String.valueOf(LgLCDConstants.ZERO).equals(value)) {
							stats.remove(naturalModeKey);
							stats.remove(naturalSize);
							stats.remove(tileID);
							advancedControllableProperties.removeIf(item -> item.getName().equals(naturalModeKey));
							paramTileMode = String.valueOf(LgLCDConstants.ZERO) + LgLCDConstants.ZERO;
							sendRequestToControlValue(commandNames.TILE_MODE_CONTROL, paramTileMode.getBytes(StandardCharsets.UTF_8));
						} else {
							tileModeValue = LgLCDConstants.ON;
							retrieveDataByCommandName(commandNames.TILE_MODE_SETTINGS, commandNames.GET);
							paramTileMode =
									Integer.toHexString(Integer.parseInt(stats.get(group + LgLCDConstants.TILE_MODE_COLUMN))) + Integer.toHexString(Integer.parseInt(stats.get(group + LgLCDConstants.TILE_MODE_ROW)));
							sendRequestToControlValue(commandNames.TILE_MODE_CONTROL, paramTileMode.getBytes(StandardCharsets.UTF_8));
							String naturalMode = retrieveDataByCommandName(commandNames.NATURAL_MODE, commandNames.GET);
							int naturalModeValue = !LgLCDConstants.NONE.equals(naturalMode) && LgLCDConstants.ZERO == Integer.parseInt(naturalMode) ? 0 : 1;
							AdvancedControllableProperty controlNaturalMode = controlSwitch(stats, group + LgLCDConstants.NATURAL_MODE, String.valueOf(naturalModeValue), LgLCDConstants.OFF,
									LgLCDConstants.ON);
							advancedControllableProperties.add(controlNaturalMode);

							if (naturalModeValue == 1) {
								stats.put(group + LgLCDConstants.NATURAL_SIZE, retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM));
							}

							String tileModeID = retrieveDataByCommandName(commandNames.TILE_ID, commandNames.GET);
							if (!LgLCDConstants.NONE.equals(tileModeID)) {
								tileModeID = String.valueOf(Integer.parseInt(tileModeID));
							}
							stats.put(group + LgLCDConstants.TILE_MODE_ID, tileModeID);
						}
						stats.put(LgLCDConstants.TILE_MODE, tileModeValue);
						break;
					case NATURAL_MODE:
						naturalSize = group + LgLCDConstants.NATURAL_SIZE;
						String paramNatural = String.valueOf(LgLCDConstants.ZERO);
						if (String.valueOf(LgLCDConstants.ZERO).equals(value)) {
							stats.remove(naturalSize);
							paramNatural = paramNatural + LgLCDConstants.ZERO;
							sendRequestToControlValue(commandNames.NATURAL_MODE, paramNatural.getBytes(StandardCharsets.UTF_8));
						} else {
							paramNatural = paramNatural + LgLCDConstants.NUMBER_ONE;
							sendRequestToControlValue(commandNames.NATURAL_MODE, paramNatural.getBytes(StandardCharsets.UTF_8));
							stats.put(group + LgLCDConstants.NATURAL_SIZE, retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM));
						}
						break;
					case BALANCE:
						int balance = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(balance).getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.BALANCE_VALUE, String.valueOf(balance));
						break;
					case BRIGHTNESS:
						int brightness = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(brightness).getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.BRIGHTNESS_VALUE, String.valueOf(brightness));
						break;
					case COLOR_TEMPERATURE:
						int colorTemperature = (int) convertFromUIValueToApiValue(String.valueOf((int) Float.parseFloat(value)), LgLCDConstants.COLOR_TEMPERATURE_UI_MAX_VALUE,
								LgLCDConstants.COLOR_TEMPERATURE_UI_MIN_VALUE);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(colorTemperature).getBytes(StandardCharsets.UTF_8));
						int newValue = (int) convertFromApiValueToUIValue(String.valueOf(colorTemperature), LgLCDConstants.COLOR_TEMPERATURE_MAX_VALUE, LgLCDConstants.COLOR_TEMPERATURE_MIN_VALUE);
						stats.put(group + LgLCDConstants.COLOR_TEMPERATURE_VALUE, String.valueOf(newValue));
						break;
					case CONTRAST:
						int contrast = (int) Float.parseFloat(value);
						dataConvert = Integer.toHexString(contrast);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), dataConvert.getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.CONTRAST_VALUE, String.valueOf(contrast));
						break;
					case SCREEN_COLOR:
						int screenColor = (int) Float.parseFloat(value);
						dataConvert = Integer.toHexString(screenColor);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), dataConvert.getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.SCREEN_COLOR_VALUE, String.valueOf(screenColor));
						break;
					case SHARPNESS:
						int sharpness = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(sharpness).getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.SHARPNESS_VALUE, String.valueOf(sharpness));
						break;
					case TINT:
						int tint = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(tint).getBytes(StandardCharsets.UTF_8));
						stats.put(group + LgLCDConstants.TINT_VALUE, String.valueOf(tint));
						break;
					case ASPECT_RATIO:
						String aspectRatio = EnumTypeHandler.getValueOfEnumByName(AspectRatio.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), aspectRatio.getBytes(StandardCharsets.UTF_8));
						break;
					case BRIGHTNESS_SIZE:
						String brightnessSize = EnumTypeHandler.getValueOfEnumByName(BrightnessSize.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), brightnessSize.getBytes(StandardCharsets.UTF_8));
						break;
					case LANGUAGE:
						String language = EnumTypeHandler.getValueOfEnumByName(Language.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), language.getBytes(StandardCharsets.UTF_8));
						break;
					case SOUND_MODE:
						String soundMode = EnumTypeHandler.getValueOfEnumByName(SoundMode.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), soundMode.getBytes(StandardCharsets.UTF_8));
						break;
					case PICTURE_MODE:
						String pictureMode = EnumTypeHandler.getValueOfEnumByName(PictureMode.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), pictureMode.getBytes(StandardCharsets.UTF_8));
						break;
					case POWER_ON_STATUS:
						String powerStatus = EnumTypeHandler.getValueOfEnumByName(PowerStatus.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), powerStatus.getBytes(StandardCharsets.UTF_8));
						break;
					case NO_IR_POWER_OFF:
					case NO_SIGNAL_POWER_OFF:
						String powerValue = String.valueOf(LgLCDConstants.ZERO) + LgLCDConstants.ZERO;
						if (String.valueOf(LgLCDConstants.NUMBER_ONE).equals(value)) {
							powerValue = String.valueOf(LgLCDConstants.ZERO) + LgLCDConstants.NUMBER_ONE;
						}
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), powerValue.getBytes(StandardCharsets.UTF_8));
						break;
					default:
						logger.debug(String.format("Property name %s doesn't support", propertyKey));
				}
			}
			updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);
			destroyChannel();
		} finally {
			reentrantLock.unlock();
		}
	}

	/**
	 * This method is recalled by Symphony to get the list of statistics to be displayed
	 *
	 * @return List<Statistics> This return the list of statistics.
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {

		reentrantLock.lock();
		try {
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
			Map<String, String> statistics = new HashMap<>();
			Map<String, String> controlStatistics = new HashMap<>();
			Map<String, String> dynamicStatistics = new HashMap<>();

			if (!isEmergencyDelivery) {
				failedMonitor.clear();
				//Use power command to check if connection is connected, if connection timeout will get timeout exception
				retrievePowerStatus(advancedControllableProperties, controlStatistics);

				//populate the monitoring and controlling data if connected with the device successfully
				if (!controlStatistics.containsKey(LgLCDConstants.CONTROL_PROTOCOL_STATUS)) {
					populateMonitoringData(statistics, dynamicStatistics);
					populateControllingData(controlStatistics, advancedControllableProperties);

					//If failed for all monitoring data
					if (failedMonitor.size() == LgLCDConstants.NO_OF_MONITORING_PROPERTY) {
						throw new ResourceNotReachableException("Get monitoring data failed for all monitoring data");
					}
					//destroy channel after collecting all device's information
					destroyChannel();
					isValidConfigManagement();
					if (isConfigManagement) {
						extendedStatistics.setControllableProperties(advancedControllableProperties);
						statistics.putAll(controlStatistics);
					}
				} else {
					statistics = controlStatistics;
				}
				extendedStatistics.setStatistics(statistics);
				extendedStatistics.setDynamicStatistics(dynamicStatistics);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}

		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * Retrieve temperature
	 *
	 * @param statistics the statistics list of statistics.
	 * @param dynamicStatistics the dynamicStatistics list of dynamicStatistics.
	 */
	private void retrieveTemperature(Map<String, String> statistics, Map<String, String> dynamicStatistics) {
		//getting temperature status from device and put into dynamicStatistic
		try {
			String temperatureParameter = LgLCDConstants.TEMPERATURE;
			String temperatureValue = String.valueOf(getTemperature());
			if (!historicalProperties.isEmpty() && historicalProperties.contains(temperatureParameter)) {
				dynamicStatistics.put(temperatureParameter, temperatureValue);
			} else {
				statistics.put(temperatureParameter, temperatureValue);
			}
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during temperature", e);
			}
		}
	}

	/**
	 * Retrieve power status
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrievePowerStatus(List<AdvancedControllableProperty> advancedControllableProperties, Map<String, String> controlStatistics) {
		try {
			powerStatusNames powerStatus = getPower();
			//Display a field ControlProtocolStatus : UNAVAILABLE, handle case multiple connections are connected to the device
			if (powerStatusNames.UNAVAILABLE.equals(powerStatus)) {
				controlStatistics.put(LgLCDConstants.CONTROL_PROTOCOL_STATUS, LgLCDConstants.UNAVAILABLE);
				return;
			}
			String power = powerStatus == null ? LgLCDConstants.NONE : powerStatus.name();
			if (LgLCDConstants.ON.toUpperCase(Locale.ROOT).compareTo(power) == 0) {
				power = String.valueOf(LgLCDConstants.NUMBER_ONE);
			} else if (LgLCDConstants.OFF.toUpperCase(Locale.ROOT).compareTo(power) == 0) {
				power = String.valueOf(LgLCDConstants.ZERO);
			}
			AdvancedControllableProperty controlPower = controlSwitch(controlStatistics, LgLCDConstants.POWER, power, LgLCDConstants.OFF, LgLCDConstants.ON);
			advancedControllableProperties.add(controlPower);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during getPower", e);
			}
			throw e;
		}
	}

	/**
	 * This method is used to convert from api value string to ui value in integer
	 *
	 * @param apiCurrentValue current api value of property
	 * @param apiMaxValue max api value of property
	 * @param apiMinValue min api value of property
	 * @return float ui value
	 */
	private float convertFromApiValueToUIValue(String apiCurrentValue, int apiMaxValue, int apiMinValue) {
		if (StringUtils.isNotNullOrEmpty(apiCurrentValue) && !LgLCDConstants.NONE.equals(apiCurrentValue)) {
			int a = Integer.parseInt(apiCurrentValue) - apiMinValue;
			int b = apiMaxValue - apiMinValue;
			return a * (LgLCDConstants.COLOR_TEMPERATURE_UI_MAX_VALUE - LgLCDConstants.COLOR_TEMPERATURE_UI_MIN_VALUE) / b + LgLCDConstants.COLOR_TEMPERATURE_UI_MIN_VALUE;
		}
		return 0f;
	}

	/**
	 * This method is used to convert from ui value in integer to api hex string
	 *
	 * @param currentValue current ui value of property
	 * @param maxValue max api value of property
	 * @param minValue min api value of property
	 * @return Float api current value
	 */
	private float convertFromUIValueToApiValue(String currentValue, int maxValue, int minValue) {
		if (StringUtils.isNotNullOrEmpty(currentValue) && !LgLCDConstants.NONE.equals(currentValue)) {
			int a = Integer.parseInt(currentValue) - minValue;
			int b = maxValue - minValue;
			return a * (LgLCDConstants.COLOR_TEMPERATURE_MAX_VALUE - LgLCDConstants.COLOR_TEMPERATURE_MIN_VALUE) / b + LgLCDConstants.COLOR_TEMPERATURE_MIN_VALUE;
		}
		return 0f;
	}

	/**
	 * Update the value for the control metric
	 *
	 * @param property is name of the metric
	 * @param value the value is value of properties
	 * @param extendedStatistics list statistics property
	 * @param advancedControllableProperties the advancedControllableProperties is list AdvancedControllableProperties
	 */
	private void updateValueForTheControllableProperty(String property, String value, Map<String, String> extendedStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		if (!advancedControllableProperties.isEmpty()) {
			for (AdvancedControllableProperty advancedControllableProperty : advancedControllableProperties) {
				if (advancedControllableProperty.getName().equals(property)) {
					extendedStatistics.put(property, value);
					advancedControllableProperty.setValue(value);
					break;
				}
			}
		}
	}

	/**
	 * Control property name by value
	 *
	 * @param command the command is command to send the request
	 * @param param the param is parameter of the request
	 */
	private void sendRequestToControlValue(commandNames command, byte[] param) {
		try {
			byte[] response = send(LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(command), param));
			String result = digestResponse(response, command).toString();
			if (LgLCDConstants.NONE.equals(result)) {
				throw new IllegalArgumentException(String.format("The response NG reply ", command.name()));
			}
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Property name %s does not support, The current model does not support control with the above option: " + e.getMessage(), command.name()), e);
		}
	}

	/**
	 * Populate controlling data
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void populateControllingData(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		retrieveFailOverGroupValue(controlStatistics, advancedControllableProperties);
		retrieveDisplayAndSoundGroupValue(controlStatistics, advancedControllableProperties);
		retrieveTileModeGroupValue(controlStatistics, advancedControllableProperties);
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.isControlType()) {
				String value = retrieveDataByCommandName(lgControllingCommand.getCommandNames(), commandNames.GET);
				populateDisplayPropertyGroup(value, lgControllingCommand, controlStatistics, advancedControllableProperties);
			}
		}
	}

	/**
	 * check Control Property Before Add New Property
	 *
	 * @param advancedControllableProperty the advancedControllableProperty is AdvancedControllableProperty instance
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void checkControlPropertyBeforeAddNewProperty(AdvancedControllableProperty advancedControllableProperty, List<AdvancedControllableProperty> advancedControllableProperties) {
		if (advancedControllableProperty != null) {
			advancedControllableProperties.add(advancedControllableProperty);
		}
	}

	/**
	 * Populate controlling property
	 *
	 * @param value the value is value of property
	 * @param lgControllingCommand the lgControllingCommand is LgControllingCommand enum instance
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void populateDisplayPropertyGroup(String value, LgControllingCommand lgControllingCommand, Map<String, String> controlStatistics,
			List<AdvancedControllableProperty> advancedControllableProperties) {
		String displayGroupName = LgLCDConstants.DISPLAY + LgLCDConstants.HASH;
		String soundGroupName = LgLCDConstants.SOUND + LgLCDConstants.HASH;
		String powerManagementGroupName = LgLCDConstants.POWER_MANAGEMENT + LgLCDConstants.HASH;
		switch (lgControllingCommand) {
			case ASPECT_RATIO:
				String[] aspectRatioDropdown = EnumTypeHandler.getEnumNames(AspectRatio.class);
				AdvancedControllableProperty aspectRatioControl = controlDropdown(controlStatistics, aspectRatioDropdown, displayGroupName + LgLCDConstants.ASPECT_RATIO, value);
				checkControlPropertyBeforeAddNewProperty(aspectRatioControl, advancedControllableProperties);
				break;
			case BRIGHTNESS_SIZE:
				String[] brightnessSizeDropdown = EnumTypeHandler.getEnumNames(BrightnessSize.class);
				AdvancedControllableProperty brightnessSizeControl = controlDropdown(controlStatistics, brightnessSizeDropdown, displayGroupName + LgLCDConstants.BRIGHTNESS_SIZE,
						value);
				checkControlPropertyBeforeAddNewProperty(brightnessSizeControl, advancedControllableProperties);
				break;
			case CONTRAST:
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.CONTRAST_VALUE);
				AdvancedControllableProperty controlContrast = createControlSlider(displayGroupName + LgLCDConstants.CONTRAST, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_CONTRAST));
				checkControlPropertyBeforeAddNewProperty(controlContrast, advancedControllableProperties);
				break;
			case PICTURE_MODE:
				String[] pictureModeDropdown = EnumTypeHandler.getEnumNames(PictureMode.class);
				AdvancedControllableProperty pictureModeControl = controlDropdown(controlStatistics, pictureModeDropdown, displayGroupName + LgLCDConstants.PICTURE_MODE, value);
				checkControlPropertyBeforeAddNewProperty(pictureModeControl, advancedControllableProperties);
				break;
			case BRIGHTNESS:
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.BRIGHTNESS_VALUE);
				AdvancedControllableProperty controlBrightness = createControlSlider(displayGroupName + LgLCDConstants.BRIGHTNESS, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_BRIGHTNESS));
				checkControlPropertyBeforeAddNewProperty(controlBrightness, advancedControllableProperties);
				break;
			case SHARPNESS:
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.SHARPNESS_VALUE);
				AdvancedControllableProperty controlSharpness = createControlSlider(displayGroupName + LgLCDConstants.SHARPNESS, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_SHARPNESS));
				checkControlPropertyBeforeAddNewProperty(controlSharpness, advancedControllableProperties);
				break;
			case SCREEN_COLOR:
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.SCREEN_COLOR_VALUE);
				AdvancedControllableProperty controlScreenColor = createControlSlider(displayGroupName + LgLCDConstants.SCREEN_COLOR, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_SCREEN_COLOR));
				checkControlPropertyBeforeAddNewProperty(controlScreenColor, advancedControllableProperties);
				break;
			case TINT:
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.TINT_VALUE);
				AdvancedControllableProperty controlTint = createControlSlider(displayGroupName + LgLCDConstants.TINT, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_TINT));
				checkControlPropertyBeforeAddNewProperty(controlTint, advancedControllableProperties);
				break;
			case COLOR_TEMPERATURE:
				Float colorTemperatureValue = convertFromApiValueToUIValue(value, LgLCDConstants.COLOR_TEMPERATURE_MAX_VALUE, LgLCDConstants.COLOR_TEMPERATURE_MIN_VALUE);
				if (colorTemperatureValue != 0f) {
					value = String.valueOf((int) Float.parseFloat(String.valueOf(colorTemperatureValue)));
				}
				checkNoneValueBeforePopulateData(value, controlStatistics, displayGroupName + LgLCDConstants.COLOR_TEMPERATURE_VALUE);
				AdvancedControllableProperty controlColorTemperature = createControlSlider(displayGroupName + LgLCDConstants.COLOR_TEMPERATURE, String.valueOf(value), controlStatistics,
						String.valueOf(LgLCDConstants.MIN_RANGE_COLOR_TEMPERATURE),
						String.valueOf(LgLCDConstants.MAX_RANGE_COLOR_TEMPERATURE));
				checkControlPropertyBeforeAddNewProperty(controlColorTemperature, advancedControllableProperties);
				break;
			case BALANCE:
				checkNoneValueBeforePopulateData(value, controlStatistics, soundGroupName + LgLCDConstants.BALANCE_VALUE);
				AdvancedControllableProperty controlBalance = createControlSlider(soundGroupName + LgLCDConstants.BALANCE, value, controlStatistics, String.valueOf(LgLCDConstants.ZERO),
						String.valueOf(LgLCDConstants.MAX_RANGE_BALANCE));
				checkControlPropertyBeforeAddNewProperty(controlBalance, advancedControllableProperties);
				break;
			case SOUND_MODE:
				String[] soundModeDropdown = EnumTypeHandler.getEnumNames(SoundMode.class);
				AdvancedControllableProperty soundModeControl = controlDropdown(controlStatistics, soundModeDropdown, soundGroupName + LgLCDConstants.SOUND_MODE, value);
				checkControlPropertyBeforeAddNewProperty(soundModeControl, advancedControllableProperties);
				break;
			case LANGUAGE:
				String[] languageDropdown = EnumTypeHandler.getEnumNames(Language.class);
				AdvancedControllableProperty languageControl = controlDropdown(controlStatistics, languageDropdown, LgLCDConstants.LANGUAGE, value);
				checkControlPropertyBeforeAddNewProperty(languageControl, advancedControllableProperties);
				break;
			case POWER_ON_STATUS:
				String[] powerDropdown = EnumTypeHandler.getEnumNames(PowerStatus.class);
				AdvancedControllableProperty powerControl = controlDropdown(controlStatistics, powerDropdown, powerManagementGroupName + LgLCDConstants.POWER_ON_STATUS, value);
				checkControlPropertyBeforeAddNewProperty(powerControl, advancedControllableProperties);
				break;
			case NO_SIGNAL_POWER_OFF:
				int noSignalPower = LgLCDConstants.ON.equalsIgnoreCase(value) ? 1 : 0;
				AdvancedControllableProperty controlNoSignalPower = controlSwitch(controlStatistics, powerManagementGroupName + LgLCDConstants.NO_SIGNAL_POWER_OFF, String.valueOf(noSignalPower),
						LgLCDConstants.OFF,
						LgLCDConstants.ON);
				checkControlPropertyBeforeAddNewProperty(controlNoSignalPower, advancedControllableProperties);
				break;
			case NO_IR_POWER_OFF:
				int noIRPower = LgLCDConstants.ON.equalsIgnoreCase(value) ? 1 : 0;
				AdvancedControllableProperty controlNoIRPower = controlSwitch(controlStatistics, powerManagementGroupName + LgLCDConstants.NO_IR_POWER_OFF, String.valueOf(noIRPower), LgLCDConstants.OFF,
						LgLCDConstants.ON);
				checkControlPropertyBeforeAddNewProperty(controlNoIRPower, advancedControllableProperties);
				break;
			default:
				logger.debug("the command name isn't supported" + lgControllingCommand.getName());
				break;
		}
	}

	/**
	 * Check None value before populate data
	 *
	 * @param value the value is value of property
	 * @param stats the stats is list of statistics
	 * @param property the property is property name
	 */
	private void checkNoneValueBeforePopulateData(String value, Map<String, String> stats, String property) {
		if (!LgLCDConstants.NONE.equals(value)) {
			stats.put(property, value);
		}
	}

	/**
	 * Populate monitoring data
	 *
	 * @param statistics the statistics are list of statistics
	 * @param dynamicStatistics the dynamicStatistics are list of dynamicStatistics
	 */
	private void populateMonitoringData(Map<String, String> statistics, Map<String, String> dynamicStatistics) {

		//The flow code is handled in the previous version
		String inputGroupName = LgLCDConstants.INPUT + LgLCDConstants.HASH;
		String signal = getSyncStatus().name();
		statistics.put(LgLCDConstants.SIGNAL, signal);
		statistics.put(inputGroupName + LgLCDConstants.SIGNAL, signal);

		String inputSignal = getInput().name();
		statistics.put(LgLCDConstants.INPUT_SELECT, inputSignal);
		statistics.put(inputGroupName + LgLCDConstants.INPUT_SELECT, inputSignal);

		statistics.put(LgLCDConstants.FAN, getFanStatus().name());
		retrieveTemperature(statistics, dynamicStatistics);

		//new feature
		retrieveDataByCommandName(commandNames.NETWORK_SETTING, commandNames.NETWORK_SETTING_PARAM);
		statistics.put(LgLCDConstants.DATE_TIME, String.format("%s %s", retrieveDataByCommandName(commandNames.DATE, commandNames.GET), retrieveDataByCommandName(commandNames.TIME, commandNames.GET)));
		statistics.put(LgLCDConstants.FAILOVER_STATUS, retrieveDataByCommandName(commandNames.FAILOVER, commandNames.GET));
		statistics.put(LgLCDConstants.SOFTWARE_VERSION, retrieveDataByCommandName(commandNames.SOFTWARE_VERSION, commandNames.GET));
		statistics.put(LgLCDConstants.TILE_MODE, retrieveDataByCommandName(commandNames.TILE_MODE_SETTINGS, commandNames.GET));
		statistics.put(LgLCDConstants.SERIAL_NUMBER, retrieveDataByCommandName(commandNames.SERIAL_NUMBER, commandNames.GET));
		statistics.put(LgLCDConstants.STAND_BY_MODE, retrieveDataByCommandName(commandNames.PMD, commandNames.GET));
		statistics.put(LgLCDConstants.IP_ADDRESS, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.IP_ADDRESS));
		statistics.put(LgLCDConstants.GATEWAY, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.GATEWAY));
		statistics.put(LgLCDConstants.SUB_NETMASK, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.SUB_NETMASK));
		statistics.put(LgLCDConstants.DNS_SERVER, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.DNS_SERVER));
		statistics.put(LgLCDConstants.IP_ADDRESS, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.IP_ADDRESS));
	}

	/**
	 * Retrieve tile mode group value
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveTileModeGroupValue(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String groupName = LgLCDConstants.TILE_MODE_SETTINGS + LgLCDConstants.HASH;
		//populate tile settings
		String tileMode = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.TILE_MODE);
		int tileModeValue = LgLCDConstants.ON.equalsIgnoreCase(tileMode) ? 1 : 0;
		AdvancedControllableProperty controlTileMode = controlSwitch(controlStatistics, groupName + LgLCDConstants.TILE_MODE, String.valueOf(tileModeValue), LgLCDConstants.OFF, LgLCDConstants.ON);
		advancedControllableProperties.add(controlTileMode);

		String tileModeColumn = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.TILE_MODE_COLUMN);
		controlStatistics.put(groupName + LgLCDConstants.TILE_MODE_COLUMN, StringUtils.isNullOrEmpty(tileModeColumn) ? LgLCDConstants.NONE : tileModeColumn);

		String tileModeRow = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.TILE_MODE_ROW);
		controlStatistics.put(groupName + LgLCDConstants.TILE_MODE_ROW, StringUtils.isNullOrEmpty(tileModeRow) ? LgLCDConstants.NONE : tileModeColumn);

		//NaturalMode
		if (LgLCDConstants.ON.equals(tileMode)) {

			//Retrieve Tile ID
			String tileModeID = retrieveDataByCommandName(commandNames.TILE_ID, commandNames.GET);
			if (!LgLCDConstants.NONE.equals(tileModeID)) {
				tileModeID = String.valueOf(Integer.parseInt(tileModeID));
			}
			controlStatistics.put(groupName + LgLCDConstants.TILE_MODE_ID, tileModeID);

			String naturalMode = retrieveDataByCommandName(commandNames.NATURAL_MODE, commandNames.GET);
			int naturalModeValue = !LgLCDConstants.NONE.equals(naturalMode) && LgLCDConstants.ZERO == Integer.parseInt(naturalMode) ? 0 : 1;
			AdvancedControllableProperty controlNaturalMode = controlSwitch(controlStatistics, groupName + LgLCDConstants.NATURAL_MODE, String.valueOf(naturalModeValue), LgLCDConstants.OFF,
					LgLCDConstants.ON);
			advancedControllableProperties.add(controlNaturalMode);

			if (naturalModeValue == 1) {
				controlStatistics.put(groupName + LgLCDConstants.NATURAL_SIZE, retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM));
			}
		}
	}

	/**
	 * Retrieve fail over group value
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveFailOverGroupValue(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String groupName = LgLCDConstants.FAILOVER + LgLCDConstants.HASH;
		String failOver = cacheMapOfPropertyNameAndValue.get(LgLCDConstants.FAILOVER_STATUS);
		retrieveDataByCommandName(commandNames.FAILOVER_INPUT_LIST, commandNames.GET);
		int failOverValue = LgLCDConstants.NUMBER_ONE;
		if (LgLCDConstants.OFF.equalsIgnoreCase(failOver)) {
			failOverValue = LgLCDConstants.ZERO;
		} else if (LgLCDConstants.AUTO.equalsIgnoreCase(failOver)) {
			AdvancedControllableProperty controlInputPriority = controlSwitch(controlStatistics, groupName + LgLCDConstants.INPUT_PRIORITY, String.valueOf(LgLCDConstants.ZERO), LgLCDConstants.AUTO,
					LgLCDConstants.MANUAL);
			advancedControllableProperties.add(controlInputPriority);
		} else {
			// failover is Manual
			AdvancedControllableProperty controlInputPriority = controlSwitch(controlStatistics, groupName + LgLCDConstants.INPUT_PRIORITY, String.valueOf(LgLCDConstants.NUMBER_ONE), LgLCDConstants.AUTO,
					LgLCDConstants.MANUAL);
			advancedControllableProperties.add(controlInputPriority);
			for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
				controlStatistics.put(groupName + entry.getKey(), entry.getValue());
			}
			controlStatistics.put(groupName + LgLCDConstants.PRIORITY_UP, LgLCDConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgLCDConstants.PRIORITY_UP, LgLCDConstants.UP, LgLCDConstants.UPPING, 0));

			controlStatistics.put(groupName + LgLCDConstants.PRIORITY_DOWN, LgLCDConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgLCDConstants.PRIORITY_DOWN, LgLCDConstants.DOWN, LgLCDConstants.DOWNING, 0));

			String[] inputSelected = cacheMapOfPriorityInputAndValue.values().toArray(new String[0]);
			String priorityInput = LgLCDConstants.NONE;
			Optional<Entry<String, String>> priorityInputOption = cacheMapOfPriorityInputAndValue.entrySet().stream().findFirst();
			if (priorityInputOption.isPresent()) {
				priorityInput = priorityInputOption.get().getValue();
			}
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.PRIORITY_INPUT, priorityInput);
			AdvancedControllableProperty controlInputSource = controlDropdown(controlStatistics, inputSelected, groupName + LgLCDConstants.PRIORITY_INPUT, priorityInput);
			advancedControllableProperties.add(controlInputSource);
		}
		AdvancedControllableProperty controlFailover = controlSwitch(controlStatistics, groupName + LgLCDConstants.FAILOVER_STATUS, String.valueOf(failOverValue), LgLCDConstants.OFF, LgLCDConstants.ON);
		advancedControllableProperties.add(controlFailover);
	}

	/**
	 * Retrieve display and sound group value
	 *
	 * @param statistics the statistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveDisplayAndSoundGroupValue(Map<String, String> statistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String backlight = retrieveDataByCommandName(commandNames.BACKLIGHT, commandNames.GET);
		String mute = retrieveDataByCommandName(commandNames.MUTE, commandNames.GET);
		String volume = retrieveDataByCommandName(commandNames.VOLUME, commandNames.GET);
		String displayGroupName = LgLCDConstants.DISPLAY + LgLCDConstants.HASH;
		String soundGroupName = LgLCDConstants.SOUND + LgLCDConstants.HASH;
		String inputGroupName = LgLCDConstants.INPUT + LgLCDConstants.HASH;
		String powerManagementGroupName = LgLCDConstants.POWER_MANAGEMENT + LgLCDConstants.HASH;

		statistics.put(displayGroupName + LgLCDConstants.BACKLIGHT_VALUE, backlight);
		AdvancedControllableProperty controlBacklight = createControlSlider(displayGroupName + LgLCDConstants.BACKLIGHT, backlight, statistics, String.valueOf(LgLCDConstants.ZERO),
				String.valueOf(LgLCDConstants.MAX_RANGE_BACKLIGHT));
		checkControlPropertyBeforeAddNewProperty(controlBacklight, advancedControllableProperties);

		mute = String.valueOf(LgLCDConstants.NUMBER_ONE).equals(mute) ? String.valueOf(LgLCDConstants.ZERO) : String.valueOf(LgLCDConstants.NUMBER_ONE);
		AdvancedControllableProperty controlMute = controlSwitch(statistics, soundGroupName + LgLCDConstants.MUTE, mute, LgLCDConstants.OFF, LgLCDConstants.ON);
		checkControlPropertyBeforeAddNewProperty(controlMute, advancedControllableProperties);

		statistics.put(soundGroupName + LgLCDConstants.VOLUME_VALUE, volume);
		AdvancedControllableProperty controlVolume = createControlSlider(soundGroupName + LgLCDConstants.VOLUME, volume, statistics, String.valueOf(LgLCDConstants.ZERO),
				String.valueOf(LgLCDConstants.MAX_RANGE_VOLUME));
		checkControlPropertyBeforeAddNewProperty(controlVolume, advancedControllableProperties);

		String[] inputDropdown = cacheMapOfPriorityInputAndValue.values().stream().sorted().collect(Collectors.toList()).toArray(new String[0]);
		AdvancedControllableProperty controlInputSource = controlDropdown(statistics, inputDropdown, inputGroupName + LgLCDConstants.INPUT_SELECT,
				cacheMapOfPropertyNameAndValue.get(LgLCDConstants.INPUT_SELECT));
		checkControlPropertyBeforeAddNewProperty(controlInputSource, advancedControllableProperties);

		String inputTypeValue = isInputTypePC ? LgLCDConstants.PC : LgLCDConstants.DTV;
		AdvancedControllableProperty controlInputType = controlDropdown(statistics, LgLCDConstants.INPUT_TYPE_DROPDOWN, inputGroupName + LgLCDConstants.INPUT_TYPE, inputTypeValue);
		checkControlPropertyBeforeAddNewProperty(controlInputType, advancedControllableProperties);

		String[] pmdDropdown = EnumTypeHandler.getEnumNames(PowerManagement.class);
		AdvancedControllableProperty controlPMD = controlDropdown(statistics, pmdDropdown, powerManagementGroupName + LgLCDConstants.STAND_BY_MODE,
				cacheMapOfPropertyNameAndValue.get(LgLCDConstants.STAND_BY_MODE));
		checkControlPropertyBeforeAddNewProperty(controlPMD, advancedControllableProperties);

		String pmdModeValue = retrieveDataByCommandName(commandNames.PMD_MODE, commandNames.PMD_MODE_PARAM);
		String[] pmdModeDropdown = EnumTypeHandler.getEnumNames(PowerManagementModeEnum.class);
		String pmdValue = EnumTypeHandler.getNameEnumByValue(PowerManagementModeEnum.class, pmdModeValue);
		AdvancedControllableProperty controlPMDMode = controlDropdown(statistics, pmdModeDropdown, powerManagementGroupName + LgLCDConstants.PM_MODE, pmdValue);
		checkControlPropertyBeforeAddNewProperty(controlPMDMode, advancedControllableProperties);
	}

	/**
	 * Retrieve data by command name
	 *
	 * @param command the command is command to send the request get the data
	 * @param param the param is param to send the request get the data
	 * @return String is data response from the device or None if response fail
	 */
	private String retrieveDataByCommandName(commandNames command, commandNames param) {
		try {
			byte[] response = send(LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(command), LgLCDConstants.commands.get(param)));
			return digestResponse(response, command).toString();
		} catch (Exception ex) {
			logger.debug("Error while retrieve monitoring data by name: " + command);
		}
		failedMonitor.add(command.name());
		return LgLCDConstants.NONE;
	}

	/**
	 * This method is used to get the current display power status
	 *
	 * @return powerStatus This returns the calculated xor checksum.
	 * @throws ResourceNotReachableException if the connection is reset many times
	 */
	private LgLCDConstants.powerStatusNames getPower() {

		try {
			byte[] response = send(LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.POWER), LgLCDConstants.commands.get(commandNames.GET)));

			LgLCDConstants.powerStatusNames power = (LgLCDConstants.powerStatusNames) digestResponse(response, LgLCDConstants.commandNames.POWER);

			if (power == null) {
				return LgLCDConstants.powerStatusNames.OFF;
			} else {
				return power;
			}
		} catch (ConnectException ce) {
			throw new ResourceNotReachableException("Connection time out", ce);
		} catch (SocketException | SocketTimeoutException se) {
			logger.error("error during getPower", se);
			return powerStatusNames.UNAVAILABLE;
		} catch (Exception e) {
			logger.error("error during getPower", e);
			failedMonitor.add(LgLCDConstants.POWER);
			return powerStatusNames.OFF;
		}
	}

	/**
	 * Control power on
	 */
	protected void powerON() {
		try {
			byte[] response = send(
					LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.POWER), LgLCDConstants.powerStatus.get(LgLCDConstants.powerStatusNames.ON)));

			digestResponse(response, LgLCDConstants.commandNames.POWER);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during power OFF send", e);
			}
		}
	}

	/**
	 * Control power off
	 */
	protected void powerOFF() {
		try {
			byte[] response = send(
					LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.POWER), LgLCDConstants.powerStatus.get(LgLCDConstants.powerStatusNames.OFF)));

			digestResponse(response, LgLCDConstants.commandNames.POWER);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during power ON send", e);
			}
		}
	}

	/**
	 * This method is used to get the current display input
	 *
	 * @return inputNames This returns the current input.
	 */
	private LgLCDConstants.inputNames getInput() {
		try {
			byte[] response = send(LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(commandNames.INPUT), LgLCDConstants.commands.get(commandNames.GET)));
			return (LgLCDConstants.inputNames) digestResponse(response, LgLCDConstants.commandNames.INPUT);
		} catch (SocketException | SocketTimeoutException e) {
			throw new ResourceNotReachableException("Connection time out", e);
		} catch (Exception ex) {
			logger.error("error during input", ex);
		}
		return LgLCDConstants.inputNames.OFF;
	}

	/**
	 * This method is used to get the current fan status
	 *
	 * @return fanStatusNames This returns the current display fan status.
	 */
	private LgLCDConstants.fanStatusNames getFanStatus() {
		try {
			byte[] response = send(
					LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.FAN_STATUS), LgLCDConstants.commands.get(LgLCDConstants.commandNames.GET)));
			return (LgLCDConstants.fanStatusNames) digestResponse(response, LgLCDConstants.commandNames.FAN_STATUS);
		} catch (SocketException | SocketTimeoutException e) {
			throw new ResourceNotReachableException("Connection time out", e);
		} catch (Exception ex) {
			logger.error("error during get fan status", ex);
		}
		failedMonitor.add(LgLCDConstants.FAN);
		return LgLCDConstants.fanStatusNames.NO_FAN;
	}

	/**
	 * This method is used to get the current display temperature
	 *
	 * @return int This returns the current display temperature.
	 */
	private Integer getTemperature() {
		try {
			byte[] response = send(
					LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.TEMPERATURE), LgLCDConstants.commands.get(LgLCDConstants.commandNames.GET)));
			return (Integer) digestResponse(response, LgLCDConstants.commandNames.TEMPERATURE);
		} catch (SocketException | SocketTimeoutException e) {
			throw new ResourceNotReachableException("Connection time out", e);
		} catch (Exception ex) {
			logger.error("error during get temperature", ex);
		}
		failedMonitor.add(LgLCDConstants.TEMPERATURE);
		return 0;
	}

	/**
	 * This method is used to get the current display sync status
	 *
	 * @return syncStatusNames This returns the current display sync status.
	 */
	private LgLCDConstants.syncStatusNames getSyncStatus() {
		try {
			byte[] response = send(LgLCDUtils.buildSendString((byte) monitorID, LgLCDConstants.commands.get(LgLCDConstants.commandNames.STATUS), LgLCDConstants.signalStatus));
			return (LgLCDConstants.syncStatusNames) digestResponse(response, LgLCDConstants.commandNames.STATUS);
		} catch (SocketException | SocketTimeoutException e) {
			throw new ResourceNotReachableException("Connection time out", e);
		} catch (Exception ex) {
			logger.error("error during get sync status", ex);
		}
		failedMonitor.add(LgLCDConstants.SIGNAL);
		return LgLCDConstants.syncStatusNames.NO_SYNC;
	}

	/**
	 * This method is used to digest the response received from the device
	 *
	 * @param response This is the response to be digested
	 * @param expectedResponse This is the expected response type to be compared with received
	 * @return Object This returns the result digested from the response.
	 */
	protected Object digestResponse(byte[] response, commandNames expectedResponse) {
		if (response[0] == LgLCDConstants.commands.get(expectedResponse)[1]) {

			byte[] responseStatus = Arrays.copyOfRange(response, 5, 7);

			if (Arrays.equals(responseStatus, LgLCDConstants.replyStatusCodes.get(replyStatusNames.OK))) {

				byte[] reply = Arrays.copyOfRange(response, 7, 9);

				switch (expectedResponse) {
					case NATURAL_MODE:
					case TILE_ID:
					case TILE_MODE_CONTROL:
						return convertByteToValue(reply);
					case NATURAL_SIZE:
						return Integer.parseInt(convertByteToValue(Arrays.copyOfRange(response, 9, 11)), 16);
					case BACKLIGHT:
					case MUTE:
					case VOLUME:
						return Integer.parseInt(convertByteToValue(reply), 16);
					case FAILOVER_INPUT_LIST:
						int len = response.length;
						reply = Arrays.copyOfRange(response, 7, len - 1);
						convertInputPriorityByValue(convertByteToValue(reply));
						return reply;
					case PMD_MODE:
						reply = Arrays.copyOfRange(response, 9, 11);
						return convertByteToValue(reply);
					case POWER:
						for (Map.Entry<LgLCDConstants.powerStatusNames, byte[]> entry : LgLCDConstants.powerStatus.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								return entry.getKey();
							}
						}
						break;
					case NETWORK_SETTING:
						reply = Arrays.copyOfRange(response, 10, response.length - 1);
						convertNetworkSettingByValue(convertByteToValue(reply));
						return reply;
					case INPUT_SOURCE:
					case INPUT:
						for (Map.Entry<LgLCDConstants.inputNames, byte[]> entry : LgLCDConstants.inputs.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								cacheMapOfPropertyNameAndValue.remove(LgLCDConstants.INPUT_SELECT);
								String inputValue = EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, convertByteToValue(entry.getValue()));
								if (LgLCDConstants.NONE.equalsIgnoreCase(inputValue)) {
									inputValue = EnumTypeHandler.getNameEnumByValue(InputSourceDropdown.class, convertByteToValue(entry.getValue()));
								}
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.INPUT_SELECT, inputValue);
								isInputTypePC = InputSourceDropdown.getTypeOfEnumByValue(entry.getValue().toString());
								return entry.getKey();
							}
						}
						break;
					case TEMPERATURE:
						return Integer.parseInt(new String(reply), 16);
					case FAN_STATUS:
						for (Map.Entry<LgLCDConstants.fanStatusNames, byte[]> entry : LgLCDConstants.fanStatusCodes.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								return entry.getKey();
							}
						}
						break;
					case STATUS:
						reply = Arrays.copyOfRange(response, 7, 11);
						for (Map.Entry<LgLCDConstants.syncStatusNames, byte[]> entry : LgLCDConstants.syncStatusCodes.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								return entry.getKey();
							}
						}
						break;
					case SERIAL_NUMBER:
						byte[] data = Arrays.copyOfRange(response, 7, 19);
						return convertByteToValue(data);
					case FAILOVER:
						String failOver = convertByteToValue(reply);
						for (FailOverEnum name : FailOverEnum.values()) {
							if (name.getValue().equals(failOver)) {
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.FAILOVER_STATUS, name.getName());
								return name.getName();
							}
						}
						break;
					case SOFTWARE_VERSION:
						data = Arrays.copyOfRange(response, 7, 13);
						String softwareVersion = convertByteToValue(data);
						//Custom software with format xx.xx.xx
						StringBuilder stringBuilder = new StringBuilder();
						for (int i = 0; i < softwareVersion.length(); i = i + 2) {
							stringBuilder.append(softwareVersion.substring(i, i + 2));
							if (i != softwareVersion.length() - 2) {
								stringBuilder.append(LgLCDConstants.DOT);
							}
						}
						return stringBuilder.toString();
					case PMD:
						String pdm = convertByteToValue(reply);
						for (PowerManagement name : PowerManagement.values()) {
							if (name.getValue().equals(pdm)) {
								if (!cacheMapOfPropertyNameAndValue.isEmpty() && cacheMapOfPropertyNameAndValue.get(LgLCDConstants.STAND_BY_MODE) != null) {
									cacheMapOfPropertyNameAndValue.remove(LgLCDConstants.STAND_BY_MODE);
								}
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.STAND_BY_MODE, name.getName());
								if (PowerManagement.OFF.getName().equals(name.getName())) {
									return name.getName();
								}
								return LgLCDConstants.ON;
							}
						}
						break;
					case DATE:
						data = Arrays.copyOfRange(response, 7, 13);
						return convertDateFormatByValue(data, false);
					case TIME:
						data = Arrays.copyOfRange(response, 7, 13);
						return convertDateFormatByValue(data, true);
					case TILE_MODE_SETTINGS:
						byte[] typeModeStatus = Arrays.copyOfRange(response, 7, 9);
						byte[] typeModeColumn = Arrays.copyOfRange(response, 9, 11);
						byte[] typeModeRow = Arrays.copyOfRange(response, 11, 13);
						cacheMapOfPropertyNameAndValue.put(LgLCDConstants.TILE_MODE_COLUMN, String.valueOf(Integer.parseInt(convertByteToValue(typeModeColumn), 16)));
						cacheMapOfPropertyNameAndValue.put(LgLCDConstants.TILE_MODE_ROW, String.valueOf(Integer.parseInt(convertByteToValue(typeModeRow), 16)));
						String tileMode = convertByteToValue(typeModeStatus);
						for (TileMode name : TileMode.values()) {
							if (name.isStatus() && name.getValue().equals(tileMode)) {
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.TILE_MODE, name.getName());
								return name.getName();
							}
						}
						break;
					case ASPECT_RATIO:
						return EnumTypeHandler.getNameEnumByValue(AspectRatio.class, convertByteToValue(reply));
					case BRIGHTNESS_SIZE:
						return EnumTypeHandler.getNameEnumByValue(BrightnessSize.class, convertByteToValue(reply));
					case PICTURE_MODE:
						return EnumTypeHandler.getNameEnumByValue(PictureMode.class, convertByteToValue(reply));
					case BRIGHTNESS:
					case CONTRAST:
					case SHARPNESS:
					case SCREEN_COLOR:
					case TINT:
					case COLOR_TEMPERATURE:
					case BALANCE:
						reply = Arrays.copyOfRange(response, 7, 9);
						return String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
					case SOUND_MODE:
						return EnumTypeHandler.getNameEnumByValue(SoundMode.class, convertByteToValue(reply));
					case NO_SIGNAL_POWER_OFF:
						String noSignal = String.valueOf(Integer.parseInt(convertByteToValue(reply)));
						String noSignalValue = LgLCDConstants.ON;
						if (String.valueOf(LgLCDConstants.ZERO).equals(noSignal)) {
							noSignalValue = LgLCDConstants.OFF;
						}
						return noSignalValue;
					case NO_IR_POWER_OFF:
						String noIRPower = String.valueOf(Integer.parseInt(convertByteToValue(reply)));
						String noIRPowerValue = LgLCDConstants.ON;
						if (String.valueOf(LgLCDConstants.ZERO).equals(noIRPower)) {
							noIRPowerValue = LgLCDConstants.OFF;
						}
						return noIRPowerValue;
					case LANGUAGE:
						return EnumTypeHandler.getNameEnumByValue(Language.class, convertByteToValue(reply));
					case POWER_ON_STATUS:
						return EnumTypeHandler.getNameEnumByValue(PowerStatus.class, convertByteToValue(reply));
					default:
						logger.debug("this command name is not supported" + expectedResponse);
				}
			} else if (Arrays.equals(responseStatus, LgLCDConstants.replyStatusCodes.get(replyStatusNames.NG))) {
				switch (expectedResponse) {
					case FAN_STATUS: {
						return LgLCDConstants.fanStatusNames.NOT_SUPPORTED;
					}
					default: {
						if (this.logger.isErrorEnabled()) {
							this.logger.error("error: NG reply: " + this.host + " port: " + this.getPort());
						}
						throw new ResourceNotReachableException("NG reply");
					}
				}
			}
		} else {
			if (this.logger.isErrorEnabled()) {
				this.logger.error("error: Unexpected reply: " + this.host + " port: " + this.getPort());
			}
			throw new RuntimeException("Error Unexpected reply");
		}

		return LgLCDConstants.NONE;
	}

	/**
	 * Convert input priority by value
	 *
	 * @param inputPriority the inputPriority is String value
	 */
	private void convertInputPriorityByValue(String inputPriority) {
		int index = 1;
		cacheMapOfPriorityInputAndValue = new HashMap<>();
		for (int i = 0; i < inputPriority.length(); i = i + 2) {
			String value = inputPriority.substring(i, i + 2);
			cacheMapOfPriorityInputAndValue.put(LgLCDConstants.PRIORITY + index, EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, value));
			index++;
		}
	}

	/**
	 * Convert network setting by value
	 *
	 * @param networkResponse the networkResponse is String value
	 */
	private void convertNetworkSettingByValue(String networkResponse) {
		String[] networkArray = networkResponse.split(LgLCDConstants.SPACE);
		StringBuilder stringBuilder = new StringBuilder();
		// value of network settings will be 172000001001 255255255000 172000001001 172000000003
		try {
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 4]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.IP_ADDRESS, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 3]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.SUB_NETMASK, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 2]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.GATEWAY, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 1]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.DNS_SERVER, stringBuilder.toString());
		} catch (Exception e) {
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.IP_ADDRESS, LgLCDConstants.NONE);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.SUB_NETMASK, LgLCDConstants.NONE);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.GATEWAY, LgLCDConstants.NONE);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.DNS_SERVER, LgLCDConstants.NONE);
		}
	}

	/**
	 * Convert data of network settings by value
	 *
	 * @param propertyName the propertyName is name of property
	 * @param networkValue the networkValue is value as String
	 */
	private void convertNetworkSettingToValue(StringBuilder propertyName, String networkValue) {
		//The network example value would be 192168000001, we will convert it to 192.168.0.1
		for (int i = 0; i < networkValue.length(); i = i + 3) {
			String value = networkValue.substring(i, i + 3);
			propertyName.append(Integer.parseInt(value));
			if (i != networkValue.length() - 3) {
				propertyName.append(LgLCDConstants.DOT);
			}
		}
	}

	/**
	 * Convert byte to value
	 *
	 * @param bytes is data represented as bytes
	 * @return String is data after converting byte to String
	 */
	private String convertByteToValue(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte byteValue : bytes) {
			stringBuilder.append((char) (byteValue));
		}
		return stringBuilder.toString();
	}

	/**
	 * Convert value to format month/day/year
	 *
	 * @param data the data is data of the response
	 * @param isTimeFormat the isTimeFormat is boolean value
	 * @return String is format of date
	 */
	private String convertDateFormatByValue(byte[] data, boolean isTimeFormat) {
		StringBuilder stringBuilder = new StringBuilder();
		StringBuilder dateValue = new StringBuilder();
		String year = LgLCDConstants.EMPTY_STRING;
		for (byte byteValue : data) {
			stringBuilder.append((char) (byteValue));
		}

		//The value example 173B00 with 17 is hours, 3B is minutes, and 00 is seconds
		//convert Hex to decimal data to 173B00 to 23:59:00
		String defaultTime = LgLCDConstants.AM;
		if (isTimeFormat) {
			for (int i = 0; i < stringBuilder.length() - 3; i = i + 2) {
				int hexValue = Integer.parseInt(stringBuilder.substring(i, i + 2), 16);
				if (i == 0) {
					if (hexValue == 0) {
						defaultTime = LgLCDConstants.PM;
						hexValue = 12;
					} else if (hexValue > 12) {
						defaultTime = LgLCDConstants.PM;
						hexValue = hexValue - 12;
					}
					dateValue.append(hexValue);
				} else {
					if (hexValue < 10) {
						dateValue.append(LgLCDConstants.COLON + LgLCDConstants.ZERO + hexValue);
					} else {
						dateValue.append(LgLCDConstants.COLON + hexValue);
					}
					dateValue.append(LgLCDConstants.SPACE + defaultTime);
				}
			}
			return dateValue.toString();
		}
		//The value example 0c011F with 0c is year, 01 is month, and 1F is day
		//convert Hex to decimal data to 0c011f to 1/31/2022
		//the year format = 2010 + 0c in(0c111F)
		for (int i = 0; i < stringBuilder.length() - 1; i = i + 2) {
			int hexValue = Integer.parseInt(stringBuilder.substring(i, i + 2), 16);
			if (i == 0) {
				year = String.valueOf(2010 + hexValue);
			} else {
				dateValue.append(hexValue + "/");
			}
		}
		return dateValue.append(year).toString();
	}

	/**
	 * Add switch is control property for metric
	 *
	 * @param stats list statistic
	 * @param name String name of metric
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty controlSwitch(Map<String, String> stats, String name, String value, String labelOff, String labelOn) {
		if (StringUtils.isNullOrEmpty(value)) {
			value = LgLCDConstants.NA;
		}
		stats.put(name, value);
		if (!LgLCDConstants.NONE.equals(value) && !StringUtils.isNullOrEmpty(value)) {
			return createSwitch(name, Integer.parseInt(value), labelOff, labelOn);
		}
		// if response data is null or none. Only display monitoring data not display controlling data
		return null;
	}

	/**
	 * Create switch is control property for metric
	 *
	 * @param name the name of property
	 * @param status initial status (0|1)
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty createSwitch(String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		AdvancedControllableProperty advancedControllableProperty = new AdvancedControllableProperty();
		advancedControllableProperty.setName(name);
		advancedControllableProperty.setValue(status);
		advancedControllableProperty.setType(toggle);
		advancedControllableProperty.setTimestamp(new Date());
		return advancedControllableProperty;
	}


	/**
	 * Create control slider is control property for the metric
	 *
	 * @param name the name of the metric
	 * @param value the value of the metric
	 * @param rangeStart is the starting number of the range
	 * @param rangeEnd is the end number of the range
	 * @return AdvancedControllableProperty slider instance
	 */
	private AdvancedControllableProperty createSlider(String name, Float value, String rangeStart, String rangeEnd) {
		AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
		slider.setLabelEnd(String.valueOf(rangeEnd));
		slider.setLabelStart(String.valueOf(rangeStart));
		slider.setRangeEnd(Float.valueOf(rangeEnd));
		slider.setRangeStart(Float.valueOf(rangeStart));

		return new AdvancedControllableProperty(name, new Date(), slider, value);
	}

	/**
	 * Create control slider is control property for the metric
	 *
	 * @param name name of the slider
	 * @param stats list of statistics
	 * @param rangeStart is the starting number of the range
	 * @param rangeEnd is the end number of the range
	 * @return AdvancedControllableProperty slider instance if add slider success else will is null
	 */
	private AdvancedControllableProperty createControlSlider(String name, String value, Map<String, String> stats, String rangeStart, String rangeEnd) {
		if (StringUtils.isNullOrEmpty(value) || LgLCDConstants.NONE.equals(value)) {
			stats.put(name, LgLCDConstants.NA);
			return null;
		}
		stats.put(name, value);
		return createSlider(name, Float.valueOf(value), rangeStart, rangeEnd);
	}

	/**
	 * Add dropdown is control property for metric
	 *
	 * @param stats list statistic
	 * @param options list select
	 * @param name String name of metric
	 * @return AdvancedControllableProperty dropdown instance if add dropdown success else will is null
	 */
	private AdvancedControllableProperty controlDropdown(Map<String, String> stats, String[] options, String name, String value) {
		if (StringUtils.isNullOrEmpty(value) || LgLCDConstants.NONE.equals(value)) {
			stats.put(name, LgLCDConstants.NA);
			return null;
		}
		stats.put(name, value);
		return createDropdown(name, options, value);
	}

	/***
	 * Create dropdown advanced controllable property
	 *
	 * @param name the name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty dropdown instance
	 */
	private AdvancedControllableProperty createDropdown(String name, String[] values, String initialValue) {
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values);
		dropDown.setLabels(values);

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @param gracePeriod grace period of button
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed, long gracePeriod) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(gracePeriod);
		return new AdvancedControllableProperty(name, new Date(), button, LgLCDConstants.EMPTY_STRING);
	}

	/**
	 * This method is used to validate input config management from user
	 *
	 * @return boolean is configManagement
	 */
	private void isValidConfigManagement() {
		isConfigManagement = StringUtils.isNotNullOrEmpty(this.configManagement) && this.configManagement.equalsIgnoreCase(LgLCDConstants.IS_VALID_CONFIG_MANAGEMENT);
	}
}

