/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.lcd;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;

/**
 * ClassTypeHandler  class defined the enum for monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/13/2022
 * @since 1.0.0
 */
public class EnumTypeHandler {

	/**
	 * Get an array of all enum names
	 *
	 * @param enumType the enumtype is enum class
	 */
	public static <T extends Enum<T>> String[] getEnumNames(Class<T> enumType) {
		List<String> names = new ArrayList<>();
		for (T c : enumType.getEnumConstants()) {
			try {
				Method method = c.getClass().getMethod("getName");
				String name = (String) method.invoke(c); // getName executed
				names.add(name);
			} catch (Exception e) {
				throw new ResourceNotReachableException("Error to convert enum " + enumType.getSimpleName() + " to names", e);
			}
		}
		return names.toArray(new String[names.size()]);
	}

}