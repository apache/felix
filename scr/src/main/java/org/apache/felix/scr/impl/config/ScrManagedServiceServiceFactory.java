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

<<<<<<< HEAD
import org.apache.felix.scr.impl.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * The <code>ScrManagedServiceServiceFactory</code> is the ServiceFactory
 * registered on behalf of the {@link ScrManagedService} (or
 * {@link ScrManagedServiceMetaTypeProvider}, resp.) to create the instance on
 * demand once it is used by the Configuration Admin Service or the MetaType
 * Service.
 * <p>
 * In contrast to the {@link ScrManagedService} and
 * {@link ScrManagedServiceMetaTypeProvider} classes, this class only requires
 * core OSGi API and thus may be instantiated without the Configuration Admin
 * and/or MetaType Service API actually available at the time of instantiation.
 */
public class ScrManagedServiceServiceFactory implements ServiceFactory
{
    private final ScrConfiguration scrConfiguration;

    public ScrManagedServiceServiceFactory(final ScrConfiguration scrConfiguration)
=======
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * The {@code ScrManagedServiceServiceFactory} is a {@code ServiceFactory} registered
 * on behalf of {@link ScrManagedService} to create a managed service instance
 * on demand once it is used by the Configuration Admin Service.
 * <p>
 * In contrast to the {@link ScrManagedService} class, this class only requires
 * core OSGi API and thus may be instantiated without the Configuration Admin
 * actually available at the time of instantiation.
 */
@SuppressWarnings("rawtypes")
public class ScrManagedServiceServiceFactory implements ServiceFactory
{
    private final ScrConfigurationImpl scrConfiguration;

    public ScrManagedServiceServiceFactory(final ScrConfigurationImpl scrConfiguration)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        this.scrConfiguration = scrConfiguration;
    }

<<<<<<< HEAD
    public Object getService(Bundle bundle, ServiceRegistration registration)
    {
        try
        {
            return ScrManagedServiceMetaTypeProvider.create( this.scrConfiguration );
        }
        catch ( Throwable t )
        {
            // assume MetaType Service API not available
            Activator
                .log(
                    LogService.LOG_ERROR,
                    null,
                    "Cannot create MetaType providing ManagedService; not providing Metatype information but just accepting configuration",
                    t );
        }
        return new ScrManagedService( this.scrConfiguration );
    }

    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service)
    {
        // nothing really todo; GC will do the rest
=======
    @Override
    public Object getService(final Bundle bundle, final ServiceRegistration registration)
    {
        return new ScrManagedService( this.scrConfiguration );
    }

    @Override
    public void ungetService(final Bundle bundle, final ServiceRegistration registration, final Object service)
    {
        // nothing really to do; GC will do the rest
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
