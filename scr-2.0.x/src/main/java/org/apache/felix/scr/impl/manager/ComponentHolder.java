/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.manager;


import java.util.Dictionary;
import java.util.List;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.TargetedPID;
import org.osgi.util.promise.Promise;


/**
 * The <code>ComponentHolder</code> interface provides the API for supporting
 * component instances configured through either singleton configurations (or
 * no configuration at all) and factory configurations.
 * <p>
 * Instances of this interface are managed by the {@link RegionConfigurationSupport}
 * class on behalf of the
 * {@link org.apache.felix.scr.impl.BundleComponentActivator} and the
 * {@link org.apache.felix.scr.impl.ComponentRegistry}.
 */
public interface ComponentHolder<S>
{

    /**
     * Returns the {@link ComponentActivator} owning this component
     * holder. (overlaps ComponentContaienr)
     */
    ComponentActivator getActivator();

    /**
     * Returns the {@link ComponentMetadata} describing and declaring this
     * component. (overlaps ComponentContaienr)
     */
    ComponentMetadata getComponentMetadata();

    /**
     * The configuration with the given PID has been deleted from the
     * Configuration Admin service.
     *
     * @param pid The PID of the deleted configuration
     * @param factoryPid The factory PID of the deleted configuration
     */
    void configurationDeleted(TargetedPID pid, TargetedPID factoryPid );


    /**
     * Configure a component with configuration from the given PID.
     * @param targetedPid Targeted PID for the configuration
     * @param factoryTargetedPid the (targeted) factory pid or null for a singleton pid
     * @param props the property dictionary from the configuration.
     * @param changeCount change count of the configuration, or R4 imitation.
     *
     * @return true if a new component is created for a factory PID, false if an existing factory pid configuration is updated or 
     * we have no factory pid
     */
    boolean configurationUpdated( TargetedPID targetedPid, TargetedPID factoryTargetedPid, Dictionary<String, Object> props, long changeCount );
    
    /**
     * Returns the targeted PID used to configure this component
     * @param pid a targetedPID containing the service pid for the component desired (the rest of the targeted pid is ignored)
     * @param factoryPid a targetedPID containing the factory pid for the component desired.
     * @return the complete targeted pid actually used to configure the comonent.
     */
    TargetedPID getConfigurationTargetedPID(TargetedPID pid, TargetedPID factoryPid);

    /**
     * Returns all <code>Component</code> instances held by this holder.
     */
    List<? extends ComponentManager<?>> getComponents();

    /**
     * Enables all components of this holder and if satisfied activates
     * them.
     *
     * @param async Whether the actual activation should take place
     *      asynchronously or not.
     */
    Promise<Void> enableComponents( boolean async );


    /**
     * Disables all components of this holder.
     *
     * @param async Whether the actual deactivation should take place
     *      asynchronously or not.
     */
    Promise<Void> disableComponents( boolean async );
    
    /**
     * whether the component is currently enabled
     * @return whether the component is enabled
     */
    boolean isEnabled();


    /**
     * Disposes off all components of this holder.
     * @param reason
     */
    void disposeComponents( int reason );

}
