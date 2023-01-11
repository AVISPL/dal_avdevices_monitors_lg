/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import java.util.ArrayList;
import java.util.List;

/**
 * Class support build String to byte
 */
public class LgLCDUtils {

	static byte[] buildSendString(byte monitorID, byte[] command, byte[] param) {
		List<Byte> bytes = new ArrayList<>();

		for (int i = 0; i < command.length; i++) {
			bytes.add(command[i]);
		}
		bytes.add((byte) ' ');
		if (monitorID < 17) {
			bytes.add((byte) '0');
		}

		String ID = Integer.toHexString(monitorID);

		for (byte b : ID.getBytes()) {
			bytes.add(b);
		}

		bytes.add((byte) ' ');
		for (int i = 0; i < param.length; i++) {
			bytes.add(param[i]);
		}
		bytes.add((byte) '\r');

		byte[] byteArray = new byte[bytes.size()];
		for (int i = 0; i < bytes.size(); i++) {
			byteArray[i] = bytes.get(i);
		}

		return byteArray;
	}
}
