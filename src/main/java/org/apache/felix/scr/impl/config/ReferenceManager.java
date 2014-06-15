package org.apache.felix.scr.impl.config;

import java.util.List;

import org.osgi.framework.ServiceReference;

public interface ReferenceManager<S, T> {

	/**
	 * Returns an array of <code>ServiceReference</code> instances of all
	 * services this instance is bound to or <code>null</code> if no services
	 * are actually bound.
	 */
	List<ServiceReference<?>> getServiceReferences();

	/**
	 * Returns the name of the service reference.
	 */
	String getName();

	/**
	 * Returns the target filter of this dependency as a string or
	 * <code>null</code> if this dependency has no target filter set.
	 *
	 * @return The target filter of this dependency or <code>null</code> if
	 *      none is set.
	 */
	String getTarget();

}