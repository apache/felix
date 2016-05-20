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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ComponentServiceObjectsHelper;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ReadOnlyDictionary;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
public class BindMethod extends BaseMethod<BindParameters>
implements org.apache.felix.scr.impl.helper.ReferenceMethod
{
    private final String m_referenceClassName;

    private enum ParamType {
        serviceReference,
        serviceObjects,
        serviceType,
        map
    }

    //initialized for cases where there is no method.
    private volatile List<ParamType> m_paramTypes = Collections.emptyList();


    public BindMethod( final String methodName,
            final Class<?> componentClass, final String referenceClassName, final DSVersion dsVersion, final boolean configurableServiceProperties )
    {
        super( methodName, componentClass, dsVersion, configurableServiceProperties );
        m_referenceClassName = referenceClassName;
    }


    /**
     * Finds the method named in the {@link #m_methodName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a method is found or the root
     * of the class hierarchy is reached without finding a method.
     *
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    @Override
    protected Method doFindMethod( final Class<?> targetClass, 
    		final boolean acceptPrivate, 
    		final boolean acceptPackage, 
    		final SimpleLogger logger )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        // 112.3.1 The method is searched for using the following priority
        //  1 - ServiceReference single parameter
        //  2 - DS 1.3+ : ComponentServiceObjects single parameter
        //  3 - Service object single parameter
        //  4 - Service interface assignment compatible single parameter
        //  5 - DS 1.3+ : Single argument with Map
        //  6 - DS 1.1/DS 1.2 : two parameters, first the type of or assignment compatible with the service, the second Map
        //  7 - DS 1.3+ : one or more parameters of types ServiceReference, ServiceObjects, interface type, 
    	//                or assignment compatible to interface type, in any order.

    	// flag indicating a suitable but inaccessible method has been found
        boolean suitableMethodNotAccessible = false;

        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log( LogService.LOG_DEBUG,
                "doFindMethod: Looking for method " + targetClass.getName() + "." + getMethodName(), null );
        }

        // Case 1 - Service reference parameter
        Method method;
        try
        {
            method = getServiceReferenceMethod( targetClass, acceptPrivate, acceptPackage, logger );
            if ( method != null )
            {
                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
                }
                m_paramTypes = Collections.singletonList(ParamType.serviceReference);
                return method;
            }
        }
        catch ( SuitableMethodNotAccessibleException ex )
        {
            suitableMethodNotAccessible = true;
        }

        // Case 2 - ComponentServiceObjects parameter
        if ( getDSVersion().isDS13() )
        {
	        try
	        {
	            method = getComponentObjectsMethod( targetClass, acceptPrivate, acceptPackage, logger );
	            if ( method != null )
	            {
	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
	                {
	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
	                }
	                m_paramTypes = Collections.singletonList(ParamType.serviceObjects);
	                return method;
	            }
	        }
	        catch ( SuitableMethodNotAccessibleException ex )
	        {
	            suitableMethodNotAccessible = true;
	        }
        }
        
        // for further methods we need the class of the service object
        final Class<?> parameterClass = ClassUtils.getClassFromComponentClassLoader( targetClass, m_referenceClassName, logger );
        if ( parameterClass != null )
        {

            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log(
                    LogService.LOG_DEBUG,
                    "doFindMethod: No method taking ServiceReference found, checking method taking "
                        + parameterClass.getName(), null );
            }

            // Case 3 - Service object parameter
            try
            {
                method = getServiceObjectMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                if ( method != null )
                {
	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
	                {
	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
	                }
	                m_paramTypes = Collections.singletonList(ParamType.serviceType);
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 4 - Service interface assignment compatible methods
            try
            {
                method = getServiceObjectAssignableMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                if ( method != null )
                {
	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
	                {
	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
	                }
                    m_paramTypes = Collections.singletonList(ParamType.serviceType);
                    return method;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                suitableMethodNotAccessible = true;
            }

            // Case 5 - DS 1.3+ : Single argument with Map
            if ( getDSVersion().isDS13() )
            {
                try
                {
                    method = getMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                    if ( method != null )
                    {
    	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
    	                {
    	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
    	                }
                        m_paramTypes = Collections.singletonList(ParamType.map);
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }            	
            }
            
            // signatures taking a map are only supported starting with DS 1.1
            if ( getDSVersion().isDS11() && !getDSVersion().isDS13() )
            {

                // Case 6 - same as case 3, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectWithMapMethod( targetClass, parameterClass, acceptPrivate, acceptPackage, logger );
                    if ( method != null )
                    {
    	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
    	                {
    	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
    	                }
                        List<ParamType> paramTypes = new ArrayList<ParamType>(2);
                        paramTypes.add(ParamType.serviceType);
                        paramTypes.add(ParamType.map);
                        m_paramTypes = paramTypes;
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

                // Case 6 - same as case 4, but + Map param (DS 1.1 only)
                try
                {
                    method = getServiceObjectAssignableWithMapMethod( targetClass, parameterClass, acceptPrivate,
                        acceptPackage );
                    if ( method != null )
                    {
    	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
    	                {
    	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + method, null );
    	                }
                        List<ParamType> paramTypes = new ArrayList<ParamType>(2);
                        paramTypes.add(ParamType.serviceType);
                        paramTypes.add(ParamType.map);
                        m_paramTypes = paramTypes;
                        return method;
                    }
                }
                catch ( SuitableMethodNotAccessibleException ex )
                {
                    suitableMethodNotAccessible = true;
                }

            }
            // Case 7 - Multiple parameters
            if ( getDSVersion().isDS13() )
            {
                for (Method m: targetClass.getDeclaredMethods())
                {
                    if (getMethodName().equals(m.getName())) {
                        Class<?>[] parameterTypes = m.getParameterTypes();
                        boolean matches = true;
                        boolean specialMatch = true;
                        List<ParamType> paramTypes = new ArrayList<ParamType>(parameterTypes.length);
                        for (Class<?> paramType: parameterTypes) {
                            if (paramType == ClassUtils.SERVICE_REFERENCE_CLASS)
                            {
                                if (specialMatch && parameterClass == ClassUtils.SERVICE_REFERENCE_CLASS)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ParamType.serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ParamType.serviceReference);
                                }
                            }
                            else if (paramType == ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS)
                            {
                                if (specialMatch && parameterClass == ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ParamType.serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ParamType.serviceObjects);
                                }
                            }
                            else if (paramType == Map.class)
                            {
                                if (specialMatch && parameterClass == Map.class)
                                {
                                    specialMatch = false;
                                    paramTypes.add(ParamType.serviceType);
                                }
                                else
                                {
                                    paramTypes.add(ParamType.map);
                                }
                            }
                            else if (paramType.isAssignableFrom( parameterClass ) )
                            {
                                paramTypes.add(ParamType.serviceType);
                            }
                            else
                            {
                                matches = false;
                                break;
                            }
                        }
                        if (matches)
                        {
                            if ( accept( m, acceptPrivate, acceptPackage, returnValue() ) )
                            {
            	                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            	                {
            	                    logger.log( LogService.LOG_DEBUG, "doFindMethod: Found Method " + m, null );
            	                }
                                m_paramTypes = paramTypes;
                                return m;
                            }
                            suitableMethodNotAccessible = true;
                        }
                    }
                }
            }
        }
        else if ( logger.isLogEnabled( LogService.LOG_WARNING ) )
        {
            logger.log(
                LogService.LOG_WARNING,
                "doFindMethod: Cannot check for methods taking parameter class " + m_referenceClassName + ": "
                    + targetClass.getName() + " does not see it", null );
        }

        // if at least one suitable method could be found but none of
        // the suitable methods are accessible, we have to terminate
        if ( suitableMethodNotAccessible )
        {
            logger.log( LogService.LOG_ERROR,
                "doFindMethod: Suitable but non-accessible method found in class {0}", new Object[]
                    { targetClass.getName() }, null );
            throw new SuitableMethodNotAccessibleException();
        }

        // no method found
        return null;
    }

    /**
     * Returns a method taking a single <code>ServiceReference</code> object
     * as a parameter or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceReferenceMethod( final Class<?> targetClass, boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { ClassUtils.SERVICE_REFERENCE_CLASS }, acceptPrivate, acceptPackage, logger );
    }

    private Method getComponentObjectsMethod( final Class<?> targetClass, boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod(targetClass, getMethodName(),
            new Class[] { ClassUtils.COMPONENTS_SERVICE_OBJECTS_CLASS }, acceptPrivate, acceptPackage,
            logger);
    }


    /**
     * Returns a method taking a single parameter of the exact type declared
     * for the service reference or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectMethod( final Class<?> targetClass, final Class<?> parameterClass, boolean acceptPrivate,
            boolean acceptPackage, SimpleLogger logger ) throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass }, acceptPrivate, acceptPackage, logger );
    }


    /**
     * Returns a method taking a single object whose type is assignment
     * compatible with the declared service type or <code>null</code> if no
     * such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     */
    private Method getServiceObjectAssignableMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger ) throws SuitableMethodNotAccessibleException
    {
        // Get all potential bind methods
        Method candidateBindMethods[] = targetClass.getDeclaredMethods();
        boolean suitableNotAccessible = false;

        if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
        {
            logger.log(
                LogService.LOG_DEBUG,
                "getServiceObjectAssignableMethod: Checking " + candidateBindMethods.length
                    + " declared method in class " + targetClass.getName(), null );
        }

        // Iterate over them
        for ( int i = 0; i < candidateBindMethods.length; i++ )
        {
            Method method = candidateBindMethods[i];
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG, "getServiceObjectAssignableMethod: Checking " + method, null );
            }

            // Get the parameters for the current method
            Class[] parameters = method.getParameterTypes();

            // Select only the methods that receive a single
            // parameter
            // and a matching name
            if ( parameters.length == 1 && method.getName().equals( getMethodName() ) )
            {

                if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    logger.log( LogService.LOG_DEBUG, "getServiceObjectAssignableMethod: Considering " + method, null );
                }

                // Get the parameter type
                final Class<?> theParameter = parameters[0];

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
                else if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
                {
                    logger.log(
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
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getServiceObjectWithMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger ) throws SuitableMethodNotAccessibleException,
        InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { parameterClass, ClassUtils.MAP_CLASS }, acceptPrivate, acceptPackage, logger );
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
    private Method getServiceObjectAssignableWithMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
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
                if ( parameters[0].isAssignableFrom( parameterClass ) && parameters[1] == ClassUtils.MAP_CLASS )
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

    /**
     * Returns a method taking a single map parameter
     * or <code>null</code> if no such method exists.
     *
     *
     * @param targetClass The class in which to look for the method. Only this
     *      class is searched for the method.
     * @param acceptPrivate <code>true</code> if private methods should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private methods should
     *      be considered.
     * @param logger
     * @return The requested method or <code>null</code> if no acceptable method
     *      can be found in the target class.
     * @throws SuitableMethodNotAccessibleException If a suitable method was
     *      found which is not accessible
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested method.
     */
    private Method getMapMethod( final Class<?> targetClass, final Class<?> parameterClass,
            boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger ) throws SuitableMethodNotAccessibleException,
        InvocationTargetException
    {
        return getMethod( targetClass, getMethodName(), new Class[]
            { ClassUtils.MAP_CLASS }, acceptPrivate, acceptPackage, logger );
    }

    public <S, T> boolean getServiceObject( ComponentContextImpl<S> key, RefPair<S, T> refPair, BundleContext context, SimpleLogger logger )
    {
        //??? this resolves which we need.... better way?
        if ( refPair.getServiceObject(key) == null && methodExists( logger ) )
        {
            if ( m_paramTypes.contains(ParamType.serviceType) ) {
                return refPair.getServiceObject(key, context, logger);
            }
        }
        return true;
    }

    public MethodResult invoke(Object componentInstance, ComponentContextImpl<?> componentContext, RefPair<?, ?> refPair, MethodResult methodCallFailureResult, SimpleLogger logger) {
        return invoke(componentInstance, new BindParameters(componentContext, refPair), methodCallFailureResult, logger);
    }

    @Override
    protected Object[] getParameters( Method method, BindParameters bp )
    {
        ComponentContextImpl key = bp.getComponentContext();
        Object[] result = new Object[ m_paramTypes.size()];
        RefPair<?, ?> refPair = bp.getRefPair();
        int i = 0;
        for ( ParamType pt: m_paramTypes ) {
            switch (pt) {
                case serviceReference:
                    result[i++] = refPair.getRef();
                    break;

                case serviceObjects:
                    result[i++] = ((ComponentServiceObjectsHelper)bp.getComponentContext().getComponentServiceObjectsHelper()).getServiceObjects(refPair.getRef());
                    break;

                case map:
                    result[i++] = new ReadOnlyDictionary<String, Object>( refPair.getRef() );
                    break;

                case serviceType:
                    result[i++] = refPair.getServiceObject(key);
                    break;

                default: throw new IllegalStateException("unexpected ParamType: " + pt);

            }
        }
        return result;
    }


    @Override
    protected String getMethodNamePrefix()
    {
        return "bind";
    }

}
