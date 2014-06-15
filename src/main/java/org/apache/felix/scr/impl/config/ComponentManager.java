package org.apache.felix.scr.impl.config;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;


public interface ComponentManager<S> {
	
	int STATE_SATISFIED = ComponentConfigurationDTO.SATISFIED;
	int STATE_UNSATISFIED = ComponentConfigurationDTO.UNSATISFIED;
	int STATE_ACTIVE = ComponentConfigurationDTO.ACTIVE;
	int STATE_FACTORY = 8;
	int STATE_FACTORY_INSTANCE = 16;
	int STATE_DISPOSED = 32;
	int STATE_DISABLED = 64; //TODO????

	Map<String, Object> getProperties();

	long getId();

	int getState();
	
	List<? extends ReferenceManager<S, ?>> getReferenceManagers();
	
}