/*
 * Copyright (c) OSGi Alliance (2001, 2016). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.cm;

import java.util.Dictionary;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A service that can receive configuration data from a Configuration Admin
 * service.
 * <p>
 * A Managed Service is a service that needs configuration data. Such an object
 * should be registered with the Framework registry with the {@code service.pid}
 * property set to some unique identifier called a PID.
 * <p>
 * If the Configuration Admin service has a {@code Configuration} object
 * corresponding to this PID, it will callback the {@code updated()} method of
 * the {@code ManagedService} object, passing the properties of that
 * {@code Configuration} object.
 * <p>
 * If it has no such {@code Configuration} object, then it calls back with a
 * {@code null} properties argument. Registering a Managed Service will always
 * result in a callback to the {@code updated()} method provided the
 * Configuration Admin service is, or becomes active. This callback must always
 * be done asynchronously.
 * <p>
 * Else, every time that either of the {@code updated()} methods is called on
 * that {@code Configuration} object, the {@code ManagedService.updated()}
 * method with the new properties is called. If the {@code delete()} method is
 * called on that {@code Configuration} object, {@code ManagedService.updated()}
 * is called with a {@code null} for the properties parameter. All these
 * callbacks must be done asynchronously.
 * <p>
 * The following example shows the code of a serial port that will create a port
 * depending on configuration information.
 *
 * <pre>
 *
 *   class SerialPort implements ManagedService {
 *
 *     ServiceRegistration registration;
 *     Hashtable configuration;
 *     CommPortIdentifier id;
 *
 *     synchronized void open(CommPortIdentifier id,
 *     BundleContext context) {
 *       this.id = id;
 *       registration = context.registerService(
 *         ManagedService.class.getName(),
 *         this,
 *         getDefaults()
 *       );
 *     }
 *
 *     Hashtable getDefaults() {
 *       Hashtable defaults = new Hashtable();
 *       defaults.put( &quot;port&quot;, id.getName() );
 *       defaults.put( &quot;product&quot;, &quot;unknown&quot; );
 *       defaults.put( &quot;baud&quot;, &quot;9600&quot; );
 *       defaults.put( Constants.SERVICE_PID,
 *         &quot;com.acme.serialport.&quot; + id.getName() );
 *       return defaults;
 *     }
 *
 *     public synchronized void updated(
 *       Dictionary configuration  ) {
 *       if ( configuration == null )
 *         registration.setProperties( getDefaults() );
 *       else {
 *         setSpeed( configuration.get(&quot;baud&quot;) );
 *         registration.setProperties( configuration );
 *       }
 *     }
 *     ...
 *   }
 * </pre>
 * <p>
 * As a convention, it is recommended that when a Managed Service is updated, it
 * should copy all the properties it does not recognize into the service
 * registration properties. This will allow the Configuration Admin service to
 * set properties on services which can then be used by other applications.
 * <p>
 * Normally, a single Managed Service for a given PID is given the configuration
 * dictionary, this is the configuration that is bound to the location of the
 * registering bundle. However, when security is on, a Managed Service can have
 * Configuration Permission to also be updated for other locations.
 *
 * @author $Id$
 * @ThreadSafe
 */
@ConsumerType
public interface ManagedService {
	/**
	 * Update the configuration for a Managed Service.
	 *
	 * <p>
	 * When the implementation of {@code updated(Dictionary)} detects any kind
	 * of error in the configuration properties, it should create a new
	 * {@code ConfigurationException} which describes the problem. This can
	 * allow a management system to provide useful information to a human
	 * administrator.
	 *
	 * <p>
	 * If this method throws any other {@code Exception}, the Configuration
	 * Admin service must catch it and should log it.
	 * <p>
	 * The Configuration Admin service must call this method asynchronously with
	 * the method that initiated the callback. This implies that implementors of
	 * Managed Service can be assured that the callback will not take place
	 * during registration when they execute the registration in a synchronized
	 * method.
	 *
	 * <p>
	 * If the location allows multiple managed services to be called back for a
	 * single configuration then the callbacks must occur in service ranking
	 * order. Changes in the location must be reflected by deleting the
	 * configuration if the configuration is no longer visible and updating when
	 * it becomes visible.
	 *
	 * <p>
	 * If no configuration exists for the corresponding PID, or the bundle has
	 * no access to the configuration, then the bundle must be called back with
	 * a {@code null} to signal that CM is active but there is no data.
	 *
	 * @param properties A copy of the Configuration properties, or {@code null}
	 *        . This argument must not contain the "service.bundleLocation"
	 *        property. The value of this property may be obtained from the
	 *        {@code Configuration.getBundleLocation} method.
	 * @throws ConfigurationException when the update fails
	 * @security ConfigurationPermission[c.location,TARGET] Required by the
	 *           bundle that registered this service
	 */
	public void updated(Dictionary<String, ?> properties) throws ConfigurationException;
}
