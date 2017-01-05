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
package org.apache.felix.scr.impl.inject;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;


/**
 * Utility methods for class handling used by method and field references.
 */
public class ClassUtils
{

    // name of the PackageAdmin class (this is a string to not create a reference to the class)
    private static final String PACKAGEADMIN_CLASS = "org.osgi.service.packageadmin.PackageAdmin";

    private static final Class<?> OBJECT_CLASS = Object.class;

    public static final Class<?> SERVICE_REFERENCE_CLASS = ServiceReference.class;

    public static final Class<?> COMPONENTS_SERVICE_OBJECTS_CLASS = ComponentServiceObjects.class;

    public static final Class<?> MAP_CLASS = Map.class;
    public static final Class<?> MAP_ENTRY_CLASS = Map.Entry.class;

    public static final Class<?> COLLECTION_CLASS = Collection.class;
    public static final Class<?> LIST_CLASS = List.class;

    // this bundle's context
    private static BundleContext m_context;
    // the package admin service (see BindMethod.getParameterClass)
    public static volatile ServiceTracker<?, ?> m_packageAdmin;

    /**
     * Returns the class object representing the class of the field reference
     * The class loader of the component class is used to load the service class.
     * <p>
     * It may well be possible, that the class loader of the target class cannot
     * see the service object class, for example if the service reference is
     * inherited from a component class of another bundle.
     *
     * @return The class object for the referred to service or <code>null</code>
     *      if the class loader of the <code>targetClass</code> cannot see that
     *      class.
     */
    public static Class<?> getClassFromComponentClassLoader(
            final Class<?> componentClass,
            final String className,
            final SimpleLogger logger )
    {
        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log(
                LogService.LOG_DEBUG,
                "getReferenceClass: Looking for interface class {0} through loader of {1}",
                    new Object[] {className, componentClass.getName()}, null );
        }

        try
        {
            // need the class loader of the target class, which may be the
            // system classloader, which case getClassLoader may retur null
            ClassLoader loader = componentClass.getClassLoader();
            if ( loader == null )
            {
                loader = ClassLoader.getSystemClassLoader();
            }

            final Class<?> referenceClass = loader.loadClass( className );
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "getParameterClass: Found class {0}", new Object[] {referenceClass.getName()}, null );
            }
            return referenceClass;
        }
        catch ( final ClassNotFoundException cnfe )
        {
            // if we can't load the class, perhaps the method is declared in a
            // super class so we try this class next
        }

        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log( LogService.LOG_DEBUG,
                "getParameterClass: Not found through component class, using PackageAdmin service", null );
        }

        // try to load the class with the help of the PackageAdmin service
        PackageAdmin pa = ( PackageAdmin ) getPackageAdmin();
        if ( pa != null )
        {
            final String referenceClassPackage = className.substring( 0, className
                .lastIndexOf( '.' ) );
            ExportedPackage[] pkg = pa.getExportedPackages( referenceClassPackage );
            if ( pkg != null )
            {
                for ( int i = 0; i < pkg.length; i++ )
                {
                    try
                    {
                        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
                        {
                            logger.log(
                                LogService.LOG_DEBUG,
                                "getParameterClass: Checking Bundle {0}/{1}",
                                    new Object[] {pkg[i].getExportingBundle().getSymbolicName(), pkg[i].getExportingBundle().getBundleId()}, null );
                        }

                        Class<?> referenceClass = pkg[i].getExportingBundle().loadClass( className );
                        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
                        {
                            logger.log( LogService.LOG_DEBUG,
                                    "getParameterClass: Found class {0}", new Object[] {referenceClass.getName()}, null );
                        }
                        return referenceClass;
                    }
                    catch ( ClassNotFoundException cnfe )
                    {
                        // exported package does not provide the interface !!!!
                    }
                }
            }
            else if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "getParameterClass: No bundles exporting package {0} found", new Object[] {referenceClassPackage}, null );
            }
        }
        else if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log( LogService.LOG_DEBUG,
                "getParameterClass: PackageAdmin service not available, cannot find class", null );
        }

        // class cannot be found, neither through the component nor from an
        // export, so we fall back to assuming Object
        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log( LogService.LOG_DEBUG,
                "getParameterClass: No class found, falling back to class Object", null );
        }
        return OBJECT_CLASS;
    }

    public static void setBundleContext( BundleContext bundleContext )
    {
        ClassUtils.m_context = bundleContext;
    }

    public static Object getPackageAdmin()
    {
        if (m_packageAdmin == null)
        {
            synchronized (ClassUtils.class)
            {
                if (m_packageAdmin == null)
                {
                    m_packageAdmin = new ServiceTracker(m_context, PACKAGEADMIN_CLASS,
                        null);
                    m_packageAdmin.open();
                }
            }
        }

        return m_packageAdmin.getService();
    }

    public static void close()
    {
        // close the PackageAdmin tracker now
        if (m_packageAdmin != null)
        {
            m_packageAdmin.close();
            m_packageAdmin = null;
        }

        // remove the reference to the component context
        m_context = null;
    }
}
