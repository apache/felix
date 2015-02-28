package org.apache.felix.dependencymanager.samples.dynamicdep.annot;

import java.io.Serializable;

public interface Storage {
	Serializable get(String key);
	void store(String key, Serializable data);
}
