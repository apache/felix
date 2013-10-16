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
package org.apache.felix.scr.impl.config;


import java.util.Dictionary;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.impl.BundleComponentActivator;
import org.apache.felix.scr.impl.TargetedPID;
import org.apache.felix.scr.impl.manager.SingleComponentManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;


/**
 * The <code>ComponentHolder</code> interface provides the API for supporting
 * component instances configured through either singleton configurations (or
 * no configuration at all) and factory configurations.
 * <p>
 * Instances of this interface are managed by the {@link ConfigurationSupport}
 * class on behalf of the
 * {@link org.apache.felix.scr.impl.BundleComponentActivator} and the
 * {@link org.apache.felix.scr.impl.ComponentRegistry}.
 */
public interface ComponentHolder
{

    /**
     * Returns the {@link BundleComponentActivator} owning this component
     * holder.
     */
    BundleComponentActivator getActivator();


    /**
     * Returns the {@link ComponentMetadata} describing and declaring this
     * component.
     */
    ComponentMetadata getComponentMetadata();


    /**
     * The configuration with the given PID has been deleted from the
     * Configuration Admin service.
     *
     * @param pid The PID of the deleted configuration
     */
    void configurationDeleted( String pid );


    /**
     * Configure a component with configuration from the given PID.
     *
     * @param pid The PID of the configuration used to configure the component.
     * @param props the property dictionary from the configuration.
     * @param changeCount change count of the configuration, or R4 imitation.
     * @param targetedPid Targeted PID for the configuration
     * @return true if a new component is created for a factory PID, false if an existing factory pid configuration is updated or 
     * we have no factory pid
     */
    boolean configurationUpdated( String pid, Dictionary<String, Object> props, long changeCount, TargetedPID targetedPid );
    
    /**
     * Change count (or fake R4 imitation)
     * @param pid PID of the component we are interested in.
     * @return the last change count from a configurationUpdated call for the given pid.
     */
    long getChangeCount( String pid );
    
    /**
     * Returns the targeted PID used to configure this component
     * @param pid a targetedPID containing the service pid for the component desired (the rest of the targeted pid is ignored)
     * @return the complete targeted pid actually used to configure the comonent.
     */
    TargetedPID getConfigurationTargetedPID(TargetedPID pid);

    /**
     * Returns all <code>Component</code> instances held by this holder.
     */
    Component[] getComponents();

    /**
     * Enables all components of this holder and if satisifed activates
     * them.
     *
     * @param async Whether the actual activation should take place
     *      asynchronously or not.
     */
    void enableComponents( boolean async );


    /**
     * Disables all components of this holder.
     *
     * @param async Whether the actual deactivation should take place
     *      asynchronously or not.
     */
    void disableComponents( boolean async );


    /**
     * Disposes off all components of this holder.
     * @param reason
     */
    void disposeComponents( int reason );


    /**
     * Informs the holder that the component has been disposed as a result of
     * calling the dispose method.
     */
    void disposed( SingleComponentManager component );
}
