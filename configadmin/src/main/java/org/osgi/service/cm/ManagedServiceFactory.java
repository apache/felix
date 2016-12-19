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
 * Manage multiple service instances. Bundles registering this interface are
 * giving the Configuration Admin service the ability to create and configure a
 * number of instances of a service that the implementing bundle can provide.
 * For example, a bundle implementing a DHCP server could be instantiated
 * multiple times for different interfaces using a factory.
 * <p>
 * Each of these <i>service instances </i> is represented, in the persistent
 * storage of the Configuration Admin service, by a factory
 * {@code Configuration} object that has a PID. When such a
 * {@code Configuration} is updated, the Configuration Admin service calls the
 * {@code ManagedServiceFactory} updated method with the new properties. When
 * {@code updated} is called with a new PID, the Managed Service Factory should
 * create a new factory instance based on these configuration properties. When
 * called with a PID that it has seen before, it should update that existing
 * service instance with the new configuration information.
 * <p>
 * In general it is expected that the implementation of this interface will
 * maintain a data structure that maps PIDs to the factory instances that it has
 * created. The semantics of a factory instance are defined by the Managed
 * Service Factory. However, if the factory instance is registered as a service
 * object with the service registry, its PID should match the PID of the
 * corresponding {@code Configuration} object (but it should <b>not </b> be
 * registered as a Managed Service!).
 * <p>
 * An example that demonstrates the use of a factory. It will create serial
 * ports under command of the Configuration Admin service.
 *
 * <pre>
 *
 *   class SerialPortFactory
 *     implements ManagedServiceFactory {
 *     ServiceRegistration registration;
 *     Hashtable ports;
 *     void start(BundleContext context) {
 *       Hashtable properties = new Hashtable();
 *       properties.put( Constants.SERVICE_PID,
 *         &quot;com.acme.serialportfactory&quot; );
 *       registration = context.registerService(
 *         ManagedServiceFactory.class.getName(),
 *         this,
 *         properties
 *       );
 *     }
 *     public void updated( String pid,
 *       Dictionary properties  ) {
 *       String portName = (String) properties.get(&quot;port&quot;);
 *       SerialPortService port =
 *         (SerialPort) ports.get( pid );
 *       if ( port == null ) {
 *         port = new SerialPortService();
 *         ports.put( pid, port );
 *         port.open();
 *       }
 *       if ( port.getPortName().equals(portName) )
 *         return;
 *       port.setPortName( portName );
 *     }
 *     public void deleted( String pid ) {
 *       SerialPortService port =
 *         (SerialPort) ports.get( pid );
 *       port.close();
 *       ports.remove( pid );
 *     }
 *     ...
 *   }
 * </pre>
 *
 * @author $Id$
 * @ThreadSafe
 */
@ConsumerType
public interface ManagedServiceFactory {
	/**
	 * Return a descriptive name of this factory.
	 *
	 * @return the name for the factory, which might be localized
	 */
	public String getName();

	/**
	 * Create a new instance, or update the configuration of an existing
	 * instance.
	 *
	 * If the PID of the {@code Configuration} object is new for the Managed
	 * Service Factory, then create a new factory instance, using the
	 * configuration {@code properties} provided. Else, update the service
	 * instance with the provided {@code properties}.
	 *
	 * <p>
	 * If the factory instance is registered with the Framework, then the
	 * configuration {@code properties} should be copied to its registry
	 * properties. This is not mandatory and security sensitive properties
	 * should obviously not be copied.
	 *
	 * <p>
	 * If this method throws any {@code Exception}, the Configuration Admin
	 * service must catch it and should log it.
	 *
	 * <p>
	 * When the implementation of updated detects any kind of error in the
	 * configuration properties, it should create a new
	 * {@link ConfigurationException} which describes the problem.
	 *
	 * <p>
	 * The Configuration Admin service must call this method asynchronously.
	 * This implies that implementors of the {@code ManagedServiceFactory} class
	 * can be assured that the callback will not take place during registration
	 * when they execute the registration in a synchronized method.
	 *
	 * <p>
	 * If the security allows multiple managed service factories to be called
	 * back for a single configuration then the callbacks must occur in service
	 * ranking order.
	 *
	 * <p>
	 * It is valid to create multiple factory instances that are bound to
	 * different locations. Managed Service Factory services must only be
	 * updated with configurations that are bound to their location or that
	 * start with the {@code ?} prefix and for which they have permission.
	 * Changes in the location must be reflected by deleting the corresponding
	 * configuration if the configuration is no longer visible or updating when
	 * it becomes visible.
	 *
	 * @param pid The PID for this configuration.
	 * @param properties A copy of the configuration properties. This argument
	 *        must not contain the service.bundleLocation" property. The value
	 *        of this property may be obtained from the
	 *        {@code Configuration.getBundleLocation} method.
	 * @throws ConfigurationException when the configuration properties are
	 *         invalid.
	 * @security ConfigurationPermission[c.location,TARGET] Required by the
	 *           bundle that registered this service
	 */
	public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException;

	/**
	 * Remove a factory instance.
	 *
	 * Remove the factory instance associated with the PID. If the instance was
	 * registered with the service registry, it should be unregistered. The
	 * Configuration Admin must call deleted for each instance it received in
	 * {@link #updated(String, Dictionary)}.
	 *
	 * <p>
	 * If this method throws any {@code Exception}, the Configuration Admin
	 * service must catch it and should log it.
	 * <p>
	 * The Configuration Admin service must call this method asynchronously.
	 *
	 * @param pid the PID of the service to be removed
	 */
	public void deleted(String pid);
}
