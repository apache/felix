/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.installer.impl;

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

class TestLogService implements LogService {

	List<String> logged = new LinkedList<>();

	@Override
	public void log(int level, String message) {
		log(null, level, message, null);
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		log(null, level, message, exception);
	}

	@SuppressWarnings("rawtypes")
    @Override
	public void log(ServiceReference sr, int level, String message) {
		log(sr, level, message, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void log(ServiceReference sr, int level, String message, Throwable exception) {
		String levelStr;
		switch (level) {
		case LogService.LOG_DEBUG:
			levelStr = "DEBUG";
			break;
		case LogService.LOG_INFO:
			levelStr = "INFO";
			break;
		case LogService.LOG_WARNING:
			levelStr = "WARNING";
			break;
		case LogService.LOG_ERROR:
			levelStr = "ERROR";
			break;
		default:
			levelStr = "UNKNOWN";
		}
		String formatted = String.format("%s: %s %s", levelStr, message, exception != null ? exception : "");
		this.logged.add(formatted);
	}

}
