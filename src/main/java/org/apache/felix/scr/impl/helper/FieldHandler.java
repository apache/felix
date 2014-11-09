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


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata.ReferenceScope;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;


/**
 * Component method to be invoked on service (un)binding.
 */
public class FieldHandler
{

    // class references to simplify parameter checking
    protected static final Class<?> MAP_CLASS = Map.class;

    private final String fieldName;
    private final Class<?> componentClass;

    private volatile Field field;

    private volatile State state;

    public FieldHandler( final String fieldName, final Class<?> componentClass,
            final String referenceClassName, final ReferenceScope referenceScope)
    {
        this.fieldName = fieldName;
        this.componentClass = componentClass;
        this.state = NotResolved.INSTANCE;
    }

    protected final String getFieldName()
    {
        return this.fieldName;
    }

    final Field getField()
    {
        return this.field;
    }

    protected final Class<?> getComponentClass()
    {
        return this.componentClass;
    }


    void setField( final Field f, final SimpleLogger logger )
    {
        this.field = f;

        if ( f != null )
        {
            state = Resolved.INSTANCE;
            logger.log( LogService.LOG_DEBUG, "Found field: {0}", new Object[]
                { field }, null );
        }
        else
        {
            state = NotFound.INSTANCE;
            logger.log(LogService.LOG_ERROR, "Field [{0}] not found; Component will fail",
                new Object[]
                    { getFieldName() }, null);
        }
    }


    State getState()
    {
        return state;
    }


    /**
     * Finds the field named in the {@link #fieldName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable method
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     *
     * @return The requested field or <code>null</code> if no acceptable field
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested field.
     * @param logger
     */
    private Field findField( final SimpleLogger logger ) throws InvocationTargetException
    {
        final Class<?> targetClass = getComponentClass();
        final ClassLoader targetClasslLoader = targetClass.getClassLoader();
        final String targetPackage = getPackageName( targetClass );
        Class<?> theClass = targetClass;
        boolean acceptPrivate = true;
        boolean acceptPackage = true;
        while (true)
        {

            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG,
                    "Locating field " + getFieldName() + " in class " + theClass.getName(), null );
            }

            try
            {
                final Field field = doFindField( theClass, acceptPrivate, acceptPackage, logger );
                if ( field != null )
                {
                    return field;
                }
            }
            catch ( SuitableMethodNotAccessibleException ex )
            {
                // log and return null
                logger.log( LogService.LOG_ERROR,
                    "findField: Suitable but non-accessible field {0} found in class {1}, subclass of {2}", new Object[]
                        { getFieldName(), theClass.getName(), targetClass.getName() }, null );
                break;
            }

            // if we get here, we have no field, so check the super class
            theClass = theClass.getSuperclass();
            if ( theClass == null )
            {
                break;
            }

            // super class field check ignores private fields and accepts
            // package fields only if in the same package and package
            // fields are (still) allowed
            acceptPackage &= targetClasslLoader == theClass.getClassLoader()
                && targetPackage.equals( getPackageName( theClass ) );

            // private fields will not be accepted any more in super classes
            acceptPrivate = false;
        }

        // nothing found after all these years ...
        return null;
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
    private Field doFindField( Class<?> targetClass, boolean acceptPrivate, boolean acceptPackage, SimpleLogger logger )
        throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        // TODO - check field type(!)
        return getField( targetClass, this.getFieldName(), acceptPrivate, acceptPackage, logger);
    }

    private Field getField( final Class<?> clazz, final String name, final boolean acceptPrivate,
            final boolean acceptPackage, final SimpleLogger logger )
    throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        try
        {
            // find the declared field in this class
            final Field field = clazz.getDeclaredField( name );

            // accept public and protected fields only and ensure accessibility
            if ( accept( field, acceptPrivate, acceptPackage ) )
            {
                return field;
            }

            // the method would fit the requirements but is not acceptable
            throw new SuitableMethodNotAccessibleException();
        }
        catch ( NoSuchFieldException nsfe )
        {
            // thrown if no field is declared with the given name and
            // parameters
            if ( logger.isLogEnabled( LogService.LOG_DEBUG ) )
            {
                logger.log( LogService.LOG_DEBUG, "Declared Field {0}.{1} not found", new Object[]
                    { clazz.getName(), name }, null );
            }
        }
        catch ( NoClassDefFoundError cdfe )
        {
            // may be thrown if a method would be found but the signature
            // contains throws declaration for an exception which cannot
            // be loaded
            if ( logger.isLogEnabled( LogService.LOG_WARNING ) )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "Failure loooking up field " ).append( name );
                buf.append( " in class class " ).append( clazz.getName() ).append( ". Assuming no such field." );
                logger.log( LogService.LOG_WARNING, buf.toString(), cdfe );
            }
        }
        catch ( SuitableMethodNotAccessibleException e)
        {
            throw e;
        }
        catch ( Throwable throwable )
        {
            // unexpected problem accessing the field, don't let everything
            // blow up in this situation, just throw a declared exception
            throw new InvocationTargetException( throwable, "Unexpected problem trying to get field " + name );
        }

        // caught and ignored exception, assume no field and continue search
        return null;
    }
/*
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
                    result[i++] = refPair.getServiceObjects();
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
*/
    private enum METHOD_TYPE {
        BIND,
        UNBIND,
        UPDATED
    };

    private MethodResult invokeMethod( final METHOD_TYPE mType,
            final Object componentInstance,
            final BindParameters bp,
            final SimpleLogger logger )
        throws InvocationTargetException
    {
        final ComponentContextImpl key = bp.getComponentContext();
        final RefPair<?, ?> refPair = bp.getRefPair();

        final Object serviceObject = refPair.getServiceObject(key);

        try {
            if ( mType == METHOD_TYPE.BIND ) {
                field.set(componentInstance, serviceObject);
            } else if ( mType == METHOD_TYPE.UNBIND ) {
                field.set(componentInstance, null);
            }
        } catch ( final IllegalArgumentException iae ) {
            iae.printStackTrace();
        } catch ( final IllegalAccessException iae ) {
            iae.printStackTrace();

        }
        return MethodResult.VOID;
    }

    /**
     * Returns <code>true</code> if the field is acceptable to be returned from the
     * {@link #getField(Class, String, boolean, boolean, SimpleLogger)} and also
     * makes the field accessible.
     * <p>
     * This method returns <code>true</code> if the field:
     * <ul>
     * <li>Is not static</li>
     * <li>Is public or protected</li>
     * <li>Is private and <code>acceptPrivate</code> is <code>true</code></li>
     * <li>Is package private and <code>acceptPackage</code> is <code>true</code></li>
     * </ul>
     * <p>
     *
     * @param field The field to check
     * @param acceptPrivate Whether a private field is acceptable
     * @param acceptPackage Whether a package private field is acceptable
     * @return whether the field is acceptable
     */
    private static boolean accept( final Field field, boolean acceptPrivate, boolean acceptPackage )
    {
        // check modifiers now
        int mod = field.getModifiers();

        // no static fields
        if ( Modifier.isStatic( mod ) )
        {
            return false;
        }

        // accept public and protected fields
        if ( Modifier.isPublic( mod ) || Modifier.isProtected( mod ) )
        {
            setAccessible( field );
            return true;
        }

        // accept private if accepted
        if ( Modifier.isPrivate( mod ) )
        {
            if ( acceptPrivate )
            {
                setAccessible( field );
                return true;
            }

            return false;
        }

        // accept default (package)
        if ( acceptPackage )
        {
            setAccessible( field );
            return true;
        }

        // else don't accept
        return false;
    }

    private static void setAccessible(final Field field)
    {
        AccessController.doPrivileged( new PrivilegedAction<Object>()
        {
            public Object run()
            {
                field.setAccessible( true );
                return null;
            }
        } );
    }


    /**
     * Returns the name of the package to which the class belongs or an
     * empty string if the class is in the default package.
     */
    public static String getPackageName( Class<?> clazz )
    {
        String name = clazz.getName();
        int dot = name.lastIndexOf( '.' );
        return ( dot > 0 ) ? name.substring( 0, dot ) : "";
    }

    private static interface State
    {

        MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        throws InvocationTargetException;
    }

    private static class NotResolved implements State
    {
        private static final State INSTANCE = new NotResolved();

        private synchronized void resolve( final FieldHandler baseMethod, SimpleLogger logger )
        {
            logger.log( LogService.LOG_DEBUG, "getting field: {0}", new Object[]
                    {baseMethod.getFieldName()}, null );

            // resolve the field
            Field field = null;
            try
            {
                field = baseMethod.findField( logger );
            }
            catch ( InvocationTargetException ex )
            {
                logger.log( LogService.LOG_WARNING, "{0} cannot be found", new Object[]
                        {baseMethod.getFieldName()}, ex.getTargetException() );
            }

            baseMethod.setField( field, logger );
        }


        public MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                SimpleLogger logger )
        throws InvocationTargetException
        {
            resolve( baseMethod, logger );
            return baseMethod.getState().invoke( baseMethod, mType, componentInstance, rawParameter, logger );
        }
    }

    private static class NotFound implements State
    {
        private static final State INSTANCE = new NotFound();


        public MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        {
            logger.log( LogService.LOG_ERROR, "Field [{1}] not found", new Object[]
                { baseMethod.getFieldName() }, null );
            return null;
        }
    }

    private static class Resolved implements State
    {
        private static final State INSTANCE = new Resolved();


        public MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
            throws InvocationTargetException
        {
            return baseMethod.invokeMethod( mType, componentInstance, rawParameter, logger );
        }
    }

    public ReferenceMethod getBind() {
        return new ReferenceMethod() {

            public MethodResult invoke(Object componentInstance,
                    BindParameters rawParameter,
                    MethodResult methodCallFailureResult, SimpleLogger logger) {
                try
                {
                    return state.invoke( FieldHandler.this, METHOD_TYPE.BIND, componentInstance, rawParameter, logger );
                }
                catch ( InvocationTargetException ite )
                {
                    logger.log( LogService.LOG_ERROR, "The {0} field has thrown an exception", new Object[]
                        { getFieldName() }, ite.getCause() );
                }

                return methodCallFailureResult;
            }

            public <S, T> boolean getServiceObject(ComponentContextImpl<S> key,
                    RefPair<S, T> refPair, BundleContext context,
                    SimpleLogger logger) {
                //??? this resolves which we need.... better way?
                if ( refPair.getServiceObject(key) == null )
                {
                    return refPair.getServiceObject(key, context, logger);
                }
                return true;
            }
        };
    }

    public ReferenceMethod getUnbind() {
        return new ReferenceMethod() {

            public MethodResult invoke(Object componentInstance,
                    BindParameters rawParameter,
                    MethodResult methodCallFailureResult, SimpleLogger logger) {
                try
                {
                    return state.invoke( FieldHandler.this, METHOD_TYPE.UNBIND, componentInstance, rawParameter, logger );
                }
                catch ( InvocationTargetException ite )
                {
                    logger.log( LogService.LOG_ERROR, "The {0} field has thrown an exception", new Object[]
                        { getFieldName() }, ite.getCause() );
                }

                return methodCallFailureResult;
            }

            public <S, T> boolean getServiceObject(ComponentContextImpl<S> key,
                    RefPair<S, T> refPair, BundleContext context,
                    SimpleLogger logger) {
                // TODO ?!?
                return true;
            }
        };
    }

    public ReferenceMethod getUpdated() {
        return new ReferenceMethod() {

            public MethodResult invoke(Object componentInstance,
                    BindParameters rawParameter,
                    MethodResult methodCallFailureResult, SimpleLogger logger) {
                try
                {
                    return state.invoke( FieldHandler.this, METHOD_TYPE.UPDATED, componentInstance, rawParameter, logger );
                }
                catch ( InvocationTargetException ite )
                {
                    logger.log( LogService.LOG_ERROR, "The {0} field has thrown an exception", new Object[]
                        { getFieldName() }, ite.getCause() );
                }

                return methodCallFailureResult;
            }

            public <S, T> boolean getServiceObject(ComponentContextImpl<S> key,
                    RefPair<S, T> refPair, BundleContext context,
                    SimpleLogger logger) {
                //??? this resolves which we need.... better way?
                if ( refPair.getServiceObject(key) == null )
                {
                    return refPair.getServiceObject(key, context, logger);
                }
                return true;
            }
        };
    }
}
