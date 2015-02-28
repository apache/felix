package org.apache.felix.dependencymanager.samples.dynamicdep.annot;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.service.log.LogService;

@Component(properties={@Property(name="type", value="mapdb")})
public class MapDBStorage implements Storage {
	@ServiceDependency
	volatile LogService log; // injected

	@Override
	public void store(String key, Serializable data) {
		log.log(LogService.LOG_WARNING, "MapDBStorage.store(" + key + "," + data + ")");
	}

	@Override
	public Serializable get(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}
