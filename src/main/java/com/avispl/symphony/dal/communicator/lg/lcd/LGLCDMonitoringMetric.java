package com.avispl.symphony.dal.communicator.lg.lcd;

public enum LGLCDMonitoringMetric {
	POWER("power", false),
	FAN("fan", false),
	INPUT("input", false),
	TEMPERATURE("temperature", true),
	SIGNAL("signal",false);

	private String name;
	private boolean historical;

	LGLCDMonitoringMetric(String name, boolean historical){
		this.name = name;
		this.historical = historical;
	}

	public boolean isHistorical(){
		return this.historical;
	}

}
