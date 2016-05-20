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
package org.apache.felix.scr.impl.metadata;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * Copied with modifications from felix configadmin.
 *
 * The <code>TargetedPID</code> class represents a targeted PID as read
 * from a configuration object.
 * <p>
 * For a factory configuration the <code>TargetedPID</code> represents
 * the factory PID of the configuration. Otherwise it represents the
 * PID itself of the configuration.
 */
public class TargetedPID
{

    private final String rawPid;

    private final String servicePid;

    private final String symbolicName;
    private final String version;
    private final String location;

    /**
     * The level of binding of this targeted PID:
     * <ul>
     * <li><code>0</code> -- this PID is not targeted at all</li>
     * <li><code>1</code> -- this PID is targeted by the symbolic name</li>
     * <li><code>2</code> -- this PID is targeted by the symbolic name and version</li>
     * <li><code>3</code> -- this PID is targeted by the symbolic name, version, and location</li>
     * </ul>
     */
    private final short bindingLevel;


    /**
     * Returns the bundle's version as required for targeted PIDs: If the
     * bundle has a version the string representation of the version
     * string converted to a Version object is returned. Otherwise the
     * string representation of <code>Version.emptyVersion</code> is
     * returned.
     * <p>
     * To remain compatible with pre-R4.2 (Framework API < 1.5) we cannot
     * use the <code>Bundle.getVersion()</code> method.
     *
     * @param bundle The bundle whose version is to be returned.
     */
    public static String getBundleVersion( final Bundle bundle )
    {
        Object vHeader = bundle.getHeaders().get( Constants.BUNDLE_VERSION );
        Version version = ( vHeader == null ) ? Version.emptyVersion : new Version( vHeader.toString() );
        return version.toString();
    }


    public TargetedPID( final String rawPid )
    {
        this.rawPid = rawPid;

        if ( rawPid.indexOf( '|' ) < 0 )
        {
            this.servicePid = rawPid;
            this.symbolicName = null;
            this.version = null;
            this.location = null;
            this.bindingLevel = 0;
        }
        else
        {
            int start = 0;
            int end = rawPid.indexOf( '|' );
            this.servicePid = rawPid.substring( start, end );

            start = end + 1;
            end = rawPid.indexOf( '|', start );
            if ( end >= 0 )
            {
                this.symbolicName = rawPid.substring( start, end );
                start = end + 1;
                end = rawPid.indexOf( '|', start );
                if ( end >= 0 )
                {
                    this.version = rawPid.substring( start, end );
                    this.location = rawPid.substring( end + 1 );
                    this.bindingLevel = 3;
                }
                else
                {
                    this.version = rawPid.substring( start );
                    this.location = null;
                    this.bindingLevel = 2;
                }
            }
            else
            {
                this.symbolicName = rawPid.substring( start );
                this.version = null;
                this.location = null;
                this.bindingLevel = 1;
            }
        }
    }


    /**
     * Returns true if the target of this PID (bundle symbolic name,
     * version, and location) match the bundle registering the referenced
     * service.
     * <p>
     * This method just checks the target not the PID value itself, so
     * this method returning <code>true</code> does not indicate whether
     * the service actually is registered with a service PID equal to the
     * raw PID of this targeted PID.
     * <p>
     * This method also returns <code>false</code> if the service has
     * concurrently been unregistered and the registering bundle is now
     * <code>null</code>.
     *
     * @param serviceBundle <code>Bundle</code> to the registered
     *      service
     * @return <code>true</code> if the referenced service matches the
     *      target of this PID.
     */
    public boolean matchesTarget( Bundle serviceBundle )
    {
        // already unregistered
        if ( serviceBundle == null )
        {
            return false;
        }

        // This is not really targeted
        if ( this.symbolicName == null )
        {
            return true;
        }

        // bundle symbolic names don't match
        if ( !this.symbolicName.equals( serviceBundle.getSymbolicName() ) )
        {
            return false;
        }

        // no more specific target
        if ( this.version == null )
        {
            return true;
        }

        // bundle version does not match

        if ( !this.version.equals( getBundleVersion( serviceBundle ) ) )
        {
            return false;
        }

        // assert bundle location match
        return this.location == null || this.location.equals( serviceBundle.getLocation() );
    }


    /**
     * Gets the raw PID with which this instance has been created.
     * <p>
     * If an actual service PID contains pipe symbols that PID might be
     * considered being targeted PID without it actually being one. This
     * method provides access to the raw PID to allow for such services to
     * be configured.
     */
    public String getRawPid()
    {
        return rawPid;
    }


    /**
     * Returns the service PID of this targeted PID which basically is
     * the targeted PID without the targeting information.
     */
    public String getServicePid()
    {
        return servicePid;
    }


    /**
     * Returns <code>true</code> if this targeted PID binds stronger than
     * the <code>other</code> {@link TargetedPID}.
     * <p>
     * This method assumes both targeted PIDs have already been checked for
     * suitability for the bundle encoded in the targetting.
     *
     * @param other The targeted PID to check whether it is binding stronger
     *      or not.
     * @return <code>true</code> if the <code>other</code> targeted PID
     *      is binding strong.
     */
    public boolean bindsStronger( final TargetedPID other )
    {
        return other == null || this.bindingLevel > other.bindingLevel;
    }


    @Override
    public int hashCode()
    {
        return this.rawPid.hashCode();
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
        {
            return false;
        }
        else if ( obj == this )
        {
            return true;
        }

        // assume equality if same class and raw PID equals
        if ( this.getClass() == obj.getClass() )
        {
            return this.rawPid.equals( ( ( TargetedPID ) obj ).rawPid );
        }

        // not the same class or different raw PID
        return false;
    }


    @Override
    public String toString()
    {
        return this.rawPid;
    }
}
