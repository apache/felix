package org.apache.felix.dependencymanager.samples.dynamicdep.api;

import java.io.Serializable;

import org.osgi.service.log.LogService;

public class FileStorage implements Storage {
	volatile LogService log; // injected

	@Override
	public void store(String key, Serializable data) {
		log.log(LogService.LOG_WARNING, "FileStorage.store(" + key + "," + data + ")");
	}

	@Override
	public Serializable get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

}
