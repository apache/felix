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
package org.apache.felix.scr.impl.helper;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.scr.impl.Activator;
import org.apache.felix.scr.impl.manager.AbstractComponentManager;
import org.apache.felix.scr.impl.manager.RefPair;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;


/**
 * Component method to be invoked on service (un)binding.
 */
public class BindMethod extends BaseMethod
{

    private static final Class OBJECT_CLASS = Object.class;

    private final String m_referenceClassName;

    private static final int SERVICE_REFERENCE = 1;
    private static final int SERVICE_OBJECT = 2;
    private static final int SERVICE_OBJECT_AND_MAP = 3;

    private int m_paramStyle;


    public BindMethod( final SimpleLogger logger, final String methodName,
            final Class componentClass, final String referenceClassName, final boolean isDS11, final boolean isDS12Felix )
    {
        super( logger, methodName, componentClass, isDS11, isDS12Felix );
        m_referenceClassName = referenceClassName;
    }


    /**
     * Finds the method named in the {@link #m_methodName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a method is found or the root
     * of the class hierarchy is reached without finding a method.
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    protected Method doFindMethod( Class targetClass, boolean acceptPrivate, boolean acceptPackage )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        // 112.3.1 The method is searched for using the following priority
        // 1 - Service reference parameter
        // 2 - Service object parameter
        // 3 - Service interface assignement compatible methods
        // 4 - same as 2, but with Map param (DS 1.1 only)
        // 5 - same as 3, but with Map param (DS 1.1 only)

        // flag indicating a suitable but inaccessible method has been found
        boolean suitableMethodNotAccessible = false;

        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log( LogService.LOG_DEBUG,
                "doFindMethod: Looking for method " + targetClass.getName() + "." + getMethodName(), null );
        }

        // Case 1 - Service reference parameter
        Method method;
        try
        {
            method = getServiceReferenceMethod( targetClass, acceptPrivate, acceptPackage );
            if ( method != null )
            {
                if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    getLogger().log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
                }
                m_paramStyle = SERVICE_REFERENCE;
                return method;
            }
        }
        catch ( SuitableMethodNotAccessibleException ex )
        {
            suitableMethodNotAccessible = true;
        }

        // for further methods we need the class of the service object
        final Class parameterClass = getParameterClass( targetClass );
        if ( parameterClass != null )
        {

            if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
            {
                getLogger().log(
                    LogService.LOG_DEBUG,
                    "doFindMethod: No method taking ServiceReference found, checking method taking "
                        + parameterClass.getName(), null );
            }

            // Case 2 - Service object parameter
            try
            {
                method = getServiceObjectMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                if ( method != null )
                {
                    m_paramStyle = SERVICE_OBJECT;
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 3 - Service interface assignement compatible methods
            try
            {
                method = getServiceObjectAssignableMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                if ( method != null )
                {
                    m_paramStyle = SERVICE_OBJECT;
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // signatures taking a map are only supported starting with DS 1.1
            if ( isDS11() )
            {

                // Case 4 - same as case 2, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectWithMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage );
                    if ( method != null )
                    {
                        m_paramStyle = SERVICE_OBJECT_AND_MAP;
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

                // Case 5 - same as case 3, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectAssignableWithMapMethod( targetClass, parameterClass, acceptPrivate,
                        acceptPackage );
                    if ( method != null )
                    {
                        m_paramStyle = SERVICE_OBJECT_AND_MAP;
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

            }

        }
        else if ( getLogger().isLogEnabled( LogService.LOG_WARNING ) )
        {
            getLogger().log(
                LogService.LOG_WARNING,
                "doFindMethod: Cannot check for methods taking parameter class " + m_referenceClassName + ": "
                    + targetClass.getName() + " does not see it", null );
        }

        // if at least one suitable method could be found but none of
        // the suitable methods are accessible, we have to terminate
        if ( suitableMethodNotAccessible )
        {
            getLogger().log( LogService.LOG_ERROR,
                "doFindMethod: Suitable but non-accessible method found in class {0}", new Object[]
                    { targetClass.getName() }, null );
            throw new SuitableMethodNotAccessibleException();
        }

        // no method found
        return null;
    }


    /**
     * Returns the class object representing the class of the service reference
     * named by the {@link #m_referenceClassName} field. The class loader of
     * the <code>targetClass</code> is used to load the service class.
     * <p>
     * It may well be possible, that the classloader of the target class cannot
     * see the service object class, for example if the service reference is
     * inherited from a component class of another bundle.
     *
     * @return The class object for the referred to service or <code>null</code>
     *      if the class loader of the <code>targetClass</code> cannot see that
     *      class.
     */
    private Class getParameterClass( final Class targetClass )
    {
        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log(
                LogService.LOG_DEBUG,
                "getParameterClass: Looking for interface class " + m_referenceClassName + "through loader of "
                    + targetClass.getName(), null );
        }

        try
        {
            // need the class loader of the target class, which may be the
            // system classloader, which case getClassLoader may retur null
            ClassLoader loader = targetClass.getClassLoader();
            if ( loader == null )
            {
                loader = ClassLoader.getSystemClassLoader();
            }

            final Class referenceClass = loader.loadClass( m_referenceClassName );
            if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
            {
                getLogger().log( LogService.LOG_DEBUG,
                    "getParameterClass: Found class " + referenceClass.getName(), null );
            }
            return referenceClass;
        }
        catch ( ClassNotFoundException cnfe )
        {
            // if we can't load the class, perhaps the method is declared in a
            // super class so we try this class next
        }

        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log( LogService.LOG_DEBUG,
                "getParameterClass: Not found through component class, using PackageAdmin service", null );
        }

        // try to load the class with the help of the PackageAdmin service
        PackageAdmin pa = ( PackageAdmin ) Activator.getPackageAdmin();
        if ( pa != null )
        {
            final String referenceClassPackage = m_referenceClassName.substring( 0, m_referenceClassName
                .lastIndexOf( '.' ) );
            ExportedPackage[] pkg = pa.getExportedPackages( referenceClassPackage );
            if ( pkg != null )
            {
                for ( int i = 0; i < pkg.length; i++ )
                {
                    try
                    {
                        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
                        {
                            getLogger().log(
                                LogService.LOG_DEBUG,
                                "getParameterClass: Checking Bundle " + pkg[i].getExportingBundle().getSymbolicName()
                                    + "/" + pkg[i].getExportingBundle().getBundleId(), null );
                        }

                        Class referenceClass = pkg[i].getExportingBundle().loadClass( m_referenceClassName );
                        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
                        {
                            getLogger().log( LogService.LOG_DEBUG,
                                "getParameterClass: Found class " + referenceClass.getName(), null );
                        }
                        return referenceClass;
                    }
                    catch ( ClassNotFoundException cnfe )
                    {
                        // exported package does not provide the interface !!!!
                    }
                }
            }
            else if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
            {
                getLogger().log( LogService.LOG_DEBUG,
                    "getParameterClass: No bundles exporting package " + referenceClassPackage + " found ", null );
            }
        }
        else if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log( LogService.LOG_DEBUG,
                "getParameterClass: PackageAdmin service not available, cannot find class", null );
        }

        // class cannot be found, neither through the component nor from an
        // export, so we fall back to assuming Object
        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log( LogService.LOG_DEBUG,
                "getParameterClass: No class found, falling back to class Object", null );
        }
        return OBJECT_CLASS;
    }


    /**
     * Returns a method taking a single <code>ServiceReference</code> object
     * as a parameter or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceReferenceMethod( final Class targetClass, boolean acceptPrivate, boolean acceptPackage )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { SERVICE_REFERENCE_CLASS }, acceptPrivate, acceptPackage );
    }


    /**
     * Returns a method taking a single parameter of the exact type declared
     * for the service reference or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectMethod( final Class targetClass, final Class parameterClass, boolean acceptPrivate,
        boolean acceptPackage ) throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass }, acceptPrivate, acceptPackage );
    }


    /**
     * Returns a method taking a single object whose type is assignment
     * compatible with the declared service type or <code>null</code> if no
     * such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
        {
            getLogger().log(
                LogService.LOG_DEBUG,
                "getServiceObjectAssignableMethod: Checking " + candidateBindMethods.length
                    + " declared method in class " + targetClass.getName(), null );
        }

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            Method method = candidateBindMethods[i];
            if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
            {
                getLogger().log( LogService.LOG_DEBUG, "getServiceObjectAssignableMethod: Checking " + method, null );
            }

            // Get the parameters for the current method
            Class[] parameters = method.getParameterTypes();

            // Select only the methods that receive a single
            // parameter
            // and a matching name
            if ( parameters.length == 1 && method.getName().equals( getMethodName() ) )
            {

                if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    getLogger().log( LogService.LOG_DEBUG, "getServiceObjectAssignableMethod: Considering " + method, null );
                }

                // Get the parameter type
                final Class theParameter = parameters[0];

                // Check if the parameter type is ServiceReference
                // or is assignable from the type specified by the
                // reference's interface attribute
                if ( theParameter.isAssignableFrom( parameterClass ) )
                {
                    if ( accept( method, acceptPrivate, acceptPackage, false ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
                else if ( getLogger().isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    getLogger().log(
                        LogService.LOG_DEBUG,
                        "getServiceObjectAssignableMethod: Parameter failure: Required " + theParameter + "; actual "
                            + parameterClass.getName(), null );
                }

            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }


    /**
     * Returns a method taking two parameters, the first being of the exact
     * type declared for the service reference and the second being a
     * <code>Map</code> or <code>null</code> if no such method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectWithMapMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException,
        InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass, MAP_CLASS }, acceptPrivate, acceptPackage );
    }


    /**
     * Returns a method taking two parameters, the first being an object
     * whose type is assignment compatible with the declared service type and
     * the second being a <code>Map</code> or <code>null</code> if no such
     * method exists.
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableWithMapMethod( final Class targetClass, final Class parameterClass,
        boolean acceptPrivate, boolean acceptPackage ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            final Method method = candidateBindMethods[i];
            final Class[] parameters = method.getParameterTypes();
            if ( parameters.length == 2 && method.getName().equals( getMethodName() ) )
            {

                // parameters must be refclass,map
                if ( parameters[0].isAssignableFrom( parameterClass ) && parameters[1] == MAP_CLASS )
                {
                    if ( accept( method, acceptPrivate, acceptPackage, false ) )
                    {
                        return method;
                    }

                    // suitable method is not accessible, flag for exception
                    suitableNotAccessible = true;
                }
            }
        }

        // if one or more suitable methods which are not accessible is/are
        // found an exception is thrown
        if ( suitableNotAccessible )
        {
            throw new SuitableMethodNotAccessibleException();
        }

        // no method with assignment compatible argument found
        return null;
    }

    public boolean getServiceObject( RefPair refPair, BundleContext context )
    {
        //??? this resolves which we need.... better way?
        if ( refPair.getServiceObject() == null && methodExists() )
        {
            if (m_paramStyle == SERVICE_OBJECT || m_paramStyle == SERVICE_OBJECT_AND_MAP) {
                Object service = context.getService( refPair.getRef() );
                if ( service == null )
                {
                    refPair.setFailed();
                    getLogger().log(
                         LogService.LOG_WARNING,
                         "Could not get service from ref " + refPair.getRef(), null );
                    return false;
                }
                refPair.setServiceObject( service );
                return true;
            }
        }
        return true;
    }

    protected Object[] getParameters( Method method, Object rawParameter )
    {
        RefPair refPair = ( RefPair ) rawParameter;
        if (m_paramStyle == SERVICE_REFERENCE )
        {
            return new Object[] {refPair.getRef()};
        }
        if (m_paramStyle == SERVICE_OBJECT)
        {
            return new Object[] {refPair.getServiceObject()};
        }
        if (m_paramStyle == SERVICE_OBJECT_AND_MAP  )
        {
            return new Object[] {refPair.getServiceObject(), new ReadOnlyDictionary( refPair.getRef() )};
        }
        throw new IllegalStateException( "Unexpected m_paramStyle of " + m_paramStyle );
    }


    protected String getMethodNamePrefix()
    {
        return "bind";
    }

    //---------- Service abstraction ------------------------------------

    public static interface Service
    {

        ServiceReference getReference();


        Object getInstance();

    }

}
