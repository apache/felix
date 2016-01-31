package org.apache.felix.dm.lambda;

import org.apache.felix.dm.Dependency;

/**
 * Base class for all dependency builders
 * @param <T> the dependency type.
 */
public interface DependencyBuilder<T extends Dependency> {
	/**
	 * Builds a DependencyManager dependency. 
	 * @return a real DependencyManager dependency
	 */
    T build();
}
