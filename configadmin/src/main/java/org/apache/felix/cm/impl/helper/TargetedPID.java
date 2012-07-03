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
package org.apache.felix.cm.impl.helper;


import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


/**
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


    public TargetedPID( final String rawPid )
    {
        this.rawPid = rawPid;

        if ( rawPid.indexOf( '|' ) < 0 )
        {
            this.servicePid = rawPid;
            this.symbolicName = null;
            this.version = null;
            this.location = null;
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
                }
                else
                {
                    this.version = rawPid.substring( start );
                    this.location = null;
                }
            }
            else
            {
                this.symbolicName = rawPid.substring( start );
                this.version = null;
                this.location = null;
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
     * @param reference <code>ServiceReference</code> to the registered
     *      service
     * @return <code>true</code> if the referenced service matches the
     *      target of this PID.
     */
    public boolean matchesTarget( ServiceReference<?> reference )
    {
        // already unregistered
        final Bundle serviceBundle = reference.getBundle();
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
        Object v = serviceBundle.getHeaders().get( Constants.BUNDLE_VERSION );
        Version s = ( v == null ) ? Version.emptyVersion : new Version( v.toString() );
        if ( !this.version.equals( s.toString() ) )
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
     * Returns how string this <code>TargetedPID</code> matches the
     * given <code>ServiceReference</code>. The return value is one of
     * those listed in the table:
     *
     * <table>
     * <tr><th>level</th><th>Description</th></tr>
     * <tr><td>-1</td><td>The targeted PID does not match at all. This means
     *      that either the service PID, the bundle's symbolic name, the
     *      bundle's version or bundle's location does not match the
     *      respective non-<code>null</code> parts of the targeted PID.
     *      This value is also returned if the raw PID fully matches the
     *      service PID.</td></tr>
     * <tr><td>0</td><td>The targeted PID only has the service PID which
     *      also matches. The bundle's symbolic name, version, and
     *      location are not considered.</td></tr>
     * <tr><td>1</td><td>The targeted PID only has the service PID and
     *      bundle symbolic name which both match. The bundle's version and
     *      location are not considered.</td></tr>
     * <tr><td>2</td><td>The targeted PID only has the service PID, bundle
     *      symbolic name and version which all match. The bundle's
     *      location is not considered.</td></tr>
     * <tr><td>3</td><td>The targeted PID has the service PID as well as
     *      the bundle symbolic name, version, and location which all
     *      match.</td></tr>
     * </table>
     *
     * @param ref
     * @return
     */
    public int matchLevel( final ServiceReference ref )
    {

        // TODO: this fails on multi-value PID properties !
        final Object servicePid = ref.getProperty( Constants.SERVICE_PID );

        // in case the service PID contains | characters, this allows
        // it to match the raw version of the targeted PID
        if ( this.rawPid.equals( servicePid ) )
        {
            return 1;
        }

        if ( !this.servicePid.equals( servicePid ) )
        {
            return -1;
        }

        if ( this.symbolicName == null )
        {
            return 0;
        }

        final Bundle refBundle = ref.getBundle();
        if ( !this.symbolicName.equals( refBundle.getSymbolicName() ) )
        {
            return -1;
        }

        if ( this.version == null )
        {
            return 1;
        }

        if ( !this.version.equals( refBundle.getHeaders().get( Constants.BUNDLE_VERSION ) ) )
        {
            return -1;
        }

        if ( this.location == null )
        {
            return 2;
        }

        if ( !this.location.equals( refBundle.getLocation() ) )
        {
            return -1;
        }

        return 3;
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
            return true;
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
}
