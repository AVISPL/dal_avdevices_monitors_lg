/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

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
 *
 * Static Monitored Statistics
 * <li>
 * Input
 * Power
 * Fan
 * SyncStatus
 * </li>
 *
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
	private Map<String, String> cacheMapOfPriorityInputAndValue;
	private ExtendedStatistics localExtendedStatistics;

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
		cacheMapOfPriorityInputAndValue.clear();
		cacheMapOfPropertyNameAndValue.clear();
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
		return String.join(",", this.historicalProperties);
	}

	/**
	 * Sets {@link #historicalProperties} value
	 *
	 * @param historicalProperties new value of {@link #historicalProperties}
	 */
	public void setHistoricalProperties(String historicalProperties) {
		this.historicalProperties.clear();
		Arrays.asList(historicalProperties.split(",")).forEach(propertyName -> this.historicalProperties.add(propertyName.trim()));
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
			if (this.localExtendedStatistics == null) {
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
						dataConvert = EnumTypeHandler.getNameEnumByValue(InputSourceDropdown.class, value);
						sendRequestToControlValue(commandNames.INPUT_SOURCE, dataConvert.getBytes(StandardCharsets.UTF_8));
						break;
					case PMD_MODE:
						dataConvert = LgLCDConstants.BYTE_COMMAND + EnumTypeHandler.getNameEnumByValue(PMDModeEnum.class, value);
						sendRequestToControlValue(commandNames.PMD_MODE, dataConvert.getBytes(StandardCharsets.UTF_8));
						break;
					case PMD:
						dataConvert = EnumTypeHandler.getNameEnumByValue(PowerManagement.class, value);
						sendRequestToControlValue(commandNames.PMD, dataConvert.getBytes(StandardCharsets.UTF_8));
						break;
					case FAILOVER_MODE:
						String inputPriority = group + LgLCDConstants.INPUT_PRIORITY;
						String priorityInput = group + LgLCDConstants.PRIORITY_INPUT;
						String priorityInputDown = group + LgLCDConstants.PRIORITY_DOWN;
						String priorityInputUp = group + LgLCDConstants.PRIORITY_UP;
						int failOverStatus = Integer.parseInt(value);
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
							//cacheMapOfPropertyControlAndValue
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8));
							updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);

							AdvancedControllableProperty controlInputPriority = controlSwitch(stats, group + LgLCDConstants.INPUT_PRIORITY, String.valueOf(LgLCDConstants.ZERO),
									LgLCDConstants.AUTO,
									LgLCDConstants.MANUAL);
							advancedControllableProperties.add(controlInputPriority);
						}
						break;
					case INPUT_PRIORITY:
						if (String.valueOf(LgLCDConstants.ZERO).equals(value)) {
							if (cacheMapOfPriorityInputAndValue != null) {
								for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
									stats.remove(group + input.getKey());
								}
							}
							priorityInputDown = group + LgLCDConstants.PRIORITY_DOWN;
							priorityInputUp = group + LgLCDConstants.PRIORITY_UP;

							stats.remove(priorityInputDown);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

							stats.remove(priorityInputUp);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8));
						} else {
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
							AdvancedControllableProperty controlInputSource = controlDropdown(stats, inputSelected, group + LgLCDConstants.PRIORITY_INPUT,
									cacheMapOfPriorityInputAndValue.entrySet().stream().findFirst().get().getValue());
							advancedControllableProperties.add(controlInputSource);
						}
						AdvancedControllableProperty controlPower = controlSwitch(stats, group + LgLCDConstants.FAILOVER_STATUS, String.valueOf(value), LgLCDConstants.OFF, LgLCDConstants.ON);
						advancedControllableProperties.add(controlPower);
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
							stringBuilder.append(EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgLCDConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8));
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
							stringBuilder.append(EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgLCDConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8));
						for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
							stats.remove(group + entry.getKey());
							stats.put(group + entry.getKey(), entry.getValue());
						}
						break;
					case TILE_MODE:
						String naturalModeKey = group + LgLCDConstants.NATURAL_MODE;
						String naturalSize = group + LgLCDConstants.NATURAL_SIZE;
						String paramTileMode;
						if (String.valueOf(LgLCDConstants.ZERO).equals(value)) {
							stats.remove(naturalModeKey);
							stats.remove(naturalSize);
							advancedControllableProperties.removeIf(item -> item.getName().equals(naturalModeKey));
							paramTileMode = String.valueOf(LgLCDConstants.ZERO) + LgLCDConstants.ZERO;
						} else {
							paramTileMode =
									Integer.toHexString(Integer.parseInt(stats.get(group + LgLCDConstants.TILE_MODE_COLUMN))) + Integer.toHexString(Integer.parseInt(stats.get(group + LgLCDConstants.TILE_MODE_ROW)));
							sendRequestToControlValue(commandNames.TILE_MODE_CONTROL, paramTileMode.getBytes(StandardCharsets.UTF_8));
							String naturalMode = retrieveDataByCommandName(commandNames.NATURAL_MODE, commandNames.GET);
							int naturalModeValue = LgLCDConstants.NONE.equals(naturalMode) && LgLCDConstants.ZERO == Integer.parseInt(naturalMode) ? 0 : 1;
							AdvancedControllableProperty controlNaturalMode = controlSwitch(stats, group + LgLCDConstants.NATURAL_MODE, String.valueOf(naturalModeValue), LgLCDConstants.OFF,
									LgLCDConstants.ON);
							advancedControllableProperties.add(controlNaturalMode);
							if (naturalModeValue == 1) {
								stats.put(group + LgLCDConstants.NATURAL_SIZE, retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM));
							}
						}
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
					default:
						logger.debug(String.format("Property name %s doesn't support", propertyKey));
				}
			}
			updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);
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
				populateMonitoringData(statistics, dynamicStatistics);
				populateControllingData(controlStatistics, advancedControllableProperties);
				destroyChannel();
				isValidConfigManagement();
				if (isConfigManagement) {
					extendedStatistics.setControllableProperties(advancedControllableProperties);
					statistics.putAll(controlStatistics);
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
	 * Retrieve signal status
	 *
	 * @param statistics the statistics list of statistics.
	 */
	private void retrieveSignal(Map<String, String> statistics) {
		//getting sync status from device
		try {
			statistics.put(LgLCDConstants.SIGNAL, getSyncStatus().name());
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during getSyncStatus", e);
			}
			throw e;
		}
	}

	/**
	 * Retrieve fan status
	 *
	 * @param statistics the statistics list of statistics.
	 */
	private void retrieveFanStatus(Map<String, String> statistics) {
		//getting fan status from device
		try {
			statistics.put(LgLCDConstants.FAN, getFanStatus().name());
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during getFanStatus", e);
			}
			throw e;
		}
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
				this.logger.debug("error during getInput", e);
			}
			throw e;
		}
	}

	/**
	 * Retrieve input source status
	 *
	 * @param statistics the statistics list of statistics.
	 */
	private void retrieveInputSource(Map<String, String> statistics) {
		//getting input status from device
		try {
			statistics.put(LgLCDConstants.INPUT, getInput().name());
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during getInput", e);
			}
			throw e;
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
	 * Update the value for the control metric
	 *
	 * @param property is name of the metric
	 * @param value the value is value of properties
	 * @param extendedStatistics list statistics property
	 * @param advancedControllableProperties the advancedControllableProperties is list AdvancedControllableProperties
	 */
	private void updateValueForTheControllableProperty(String property, String value, Map<String, String> extendedStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (AdvancedControllableProperty advancedControllableProperty : advancedControllableProperties) {
			if (advancedControllableProperty.getName().equals(property)) {
				extendedStatistics.put(property, value);
				advancedControllableProperty.setValue(value);
				break;
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
			digestResponse(response, command);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during property name" + command.name(), e);
			}
		}
	}

	/**
	 * Populate controlling data
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void populateControllingData(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		retrievePowerStatus(advancedControllableProperties, controlStatistics);

		retrieveFailOverGroupValue(controlStatistics, advancedControllableProperties);
		retrieveDisplayAndSoundGroupValue(controlStatistics, advancedControllableProperties);
		retrieveTileModeGroupValue(controlStatistics, advancedControllableProperties);
	}

	/**
	 * Populate monitoring data
	 *
	 * @param statistics the statistics are list of statistics
	 */
	private void populateMonitoringData(Map<String, String> statistics, Map<String, String> dynamicStatistics) {
		//The flow code is handled in the previous version
		retrieveInputSource(statistics);
		retrieveFanStatus(statistics);
		retrieveSignal(statistics);
		retrieveTemperature(statistics, dynamicStatistics);

		//new feature
		retrieveDataByCommandName(commandNames.NETWORK_SETTING, commandNames.NETWORK_SETTING_PARAM);
		statistics.put(LgLCDConstants.DATE_TIME, String.format("%s %s", retrieveDataByCommandName(commandNames.DATE, commandNames.GET), retrieveDataByCommandName(commandNames.TIME, commandNames.GET)));
		statistics.put(LgLCDConstants.FAILOVER_STATUS, retrieveDataByCommandName(commandNames.FAILOVER, commandNames.GET));
		statistics.put(LgLCDConstants.SOFTWARE_VERSION, retrieveDataByCommandName(commandNames.SOFTWARE_VERSION, commandNames.GET));
		statistics.put(LgLCDConstants.TILE_MODE, retrieveDataByCommandName(commandNames.TILE_MODE_SETTINGS, commandNames.GET));
		statistics.put(LgLCDConstants.SERIAL_NUMBER, retrieveDataByCommandName(commandNames.SERIAL_NUMBER, commandNames.GET));
		statistics.put(LgLCDConstants.DPM_STATUS, retrieveDataByCommandName(commandNames.PMD, commandNames.GET));
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
			String priorityInput = cacheMapOfPriorityInputAndValue.entrySet().stream().findFirst().get().getValue();

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
		String groupName = LgLCDConstants.DISPLAY_AND_SOUND + LgLCDConstants.HASH;

		statistics.put(groupName + LgLCDConstants.BACKLIGHT_VALUE, backlight);
		AdvancedControllableProperty controlBacklight = createControlSlider(groupName + LgLCDConstants.BACKLIGHT, backlight, statistics, String.valueOf(LgLCDConstants.ZERO),
				String.valueOf(LgLCDConstants.MAX_RANGE_BACKLIGHT));
		advancedControllableProperties.add(controlBacklight);

		mute = String.valueOf(LgLCDConstants.NUMBER_ONE).equals(mute) ? String.valueOf(LgLCDConstants.ZERO) : String.valueOf(LgLCDConstants.NUMBER_ONE);
		AdvancedControllableProperty controlMute = controlSwitch(statistics, groupName + LgLCDConstants.MUTE, mute, LgLCDConstants.OFF, LgLCDConstants.ON);
		advancedControllableProperties.add(controlMute);

		statistics.put(groupName + LgLCDConstants.VOLUME_VALUE, volume);
		AdvancedControllableProperty controlVolume = createControlSlider(groupName + LgLCDConstants.VOLUME, volume, statistics, String.valueOf(LgLCDConstants.ZERO),
				String.valueOf(LgLCDConstants.MAX_RANGE_VOLUME));
		advancedControllableProperties.add(controlVolume);

		String[] inputDropdown = cacheMapOfPriorityInputAndValue.values().stream().sorted().collect(Collectors.toList()).toArray(new String[0]);
		AdvancedControllableProperty controlInputSource = controlDropdown(statistics, inputDropdown, groupName + LgLCDConstants.INPUT_SOURCE,
				cacheMapOfPropertyNameAndValue.get(LgLCDConstants.INPUT_SOURCE));
		advancedControllableProperties.add(controlInputSource);

		String[] pmdDropdown = EnumTypeHandler.getEnumNames(PowerManagement.class);
		AdvancedControllableProperty controlPMD = controlDropdown(statistics, pmdDropdown, groupName + LgLCDConstants.DPM, cacheMapOfPropertyNameAndValue.get(LgLCDConstants.DPM_STATUS));
		advancedControllableProperties.add(controlPMD);

		String pmdModeValue = retrieveDataByCommandName(commandNames.PMD_MODE, commandNames.PMD_MODE_PARAM);
		String[] pmdModeDropdown = EnumTypeHandler.getEnumNames(PMDModeEnum.class);
		String pmdValue = EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, pmdModeValue);
		AdvancedControllableProperty controlPMDMode = controlDropdown(statistics, pmdModeDropdown, groupName + LgLCDConstants.PMD_MODE, pmdValue);
		advancedControllableProperties.add(controlPMDMode);
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
		} catch (Exception e) {
			logger.debug("Error while retrieve monitoring data by name: " + command);
		}
		return LgLCDConstants.NONE;
	}

	/**
	 * This method is used to get the current display power status
	 *
	 * @return powerStatus This returns the calculated xor checksum.
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
		} catch (Exception e) {
			if (this.logger.isErrorEnabled()) {
				this.logger.error("error during get power send", e);
			}
		}
		return null;
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
		} catch (Exception e) {
			this.logger.error("Connect exception", e);
			return LgLCDConstants.inputNames.OFF;
		}
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
		} catch (Exception e) {
			this.logger.error("Connect exception", e);
			return LgLCDConstants.fanStatusNames.NO_FAN;
		}
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
		} catch (Exception e) {
			this.logger.error("Connect exception", e);
			return 0;
		}
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
		} catch (Exception e) {
			this.logger.error("Connect exception", e);
			return LgLCDConstants.syncStatusNames.NO_SYNC;
		}
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
						reply = Arrays.copyOfRange(response, 13, response.length - 1);
						convertNetworkSettingByValue(convertByteToValue(reply));
						return reply;
					case INPUT:
						for (Map.Entry<LgLCDConstants.inputNames, byte[]> entry : LgLCDConstants.inputs.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								cacheMapOfPropertyNameAndValue.remove(LgLCDConstants.INPUT_SOURCE);
								String inputValue = EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, convertByteToValue(entry.getValue()));
								if (LgLCDConstants.NONE.equalsIgnoreCase(inputValue)) {
									inputValue = EnumTypeHandler.getNameEnumByValue(InputSourceDropdown.class, convertByteToValue(entry.getValue()));
								}
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.INPUT_SOURCE, inputValue);
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
						return convertByteToValue(data);
					case PMD:
						String pdm = convertByteToValue(reply);
						for (PowerManagement name : PowerManagement.values()) {
							if (name.getValue().equals(pdm)) {
								if (!cacheMapOfPropertyNameAndValue.isEmpty() && cacheMapOfPropertyNameAndValue.get(LgLCDConstants.DPM_STATUS) != null) {
									cacheMapOfPropertyNameAndValue.remove(LgLCDConstants.DPM_STATUS);
								}
								cacheMapOfPropertyNameAndValue.put(LgLCDConstants.DPM_STATUS, name.getName());
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

		return null;
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
		if (networkArray.length == 4) {
			convertNetworkSettingToValue(stringBuilder, networkArray[0]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.IP_ADDRESS, stringBuilder.toString());

			convertNetworkSettingToValue(stringBuilder, networkArray[1]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.SUB_NETMASK, stringBuilder.toString());

			convertNetworkSettingToValue(stringBuilder, networkArray[2]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.GATEWAY, stringBuilder.toString());

			convertNetworkSettingToValue(stringBuilder, networkArray[3]);
			cacheMapOfPropertyNameAndValue.put(LgLCDConstants.DNS_SERVER, stringBuilder.toString());
		} else {
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
					dateValue.append(LgLCDConstants.COLON + hexValue);
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
			value = LgLCDConstants.NONE;
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

