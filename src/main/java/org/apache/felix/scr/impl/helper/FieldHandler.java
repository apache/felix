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
import java.util.Collections;
import java.util.Map;

import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Handler for field references
 */
public class FieldHandler
{
    private enum ParamType {
        serviceReference,
        serviceObjects,
        serviceType,
        map,
        tuple
    }

    /** The reference metadata. */
    private final ReferenceMetadata metadata;

    /** The component class. */
    private final Class<?> componentClass;

    /** The field used for the injection. */
    private volatile Field field;

    /** Value type. */
    private volatile ParamType valueType;

    /** State handling. */
    private volatile State state;

    /**
     * Create a new field handler
     * @param fieldName name of the field
     * @param componentClass component class
     * @param referenceClassName service class name
     */
    public FieldHandler( final ReferenceMetadata metadata,
            final Class<?> componentClass)
    {
        this.metadata = metadata;
        this.componentClass = componentClass;
        this.state = NotResolved.INSTANCE;
    }

    /**
     * Set the field.
     * If the field is found, the state transitions to resolved, if the field is
     * {@code null} the state transitions to not found.
     * @param f The field or {@code null}.
     * @param logger The logger
     */
    private void setField( final Field f, final SimpleLogger logger )
    {
        this.field = f;

        if ( f != null )
        {
            state = Resolved.INSTANCE;
            logger.log( LogService.LOG_DEBUG, "Found field: {0}",
                    new Object[] { field }, null );
        }
        else
        {
            state = NotFound.INSTANCE;
            logger.log(LogService.LOG_ERROR, "Field [{0}] not found; Component will fail",
                new Object[] { this.metadata.getField() }, null);
        }
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
    private Field findField( final SimpleLogger logger )
    throws InvocationTargetException
    {
        final Class<?> targetClass = this.componentClass;
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
                    "Locating field " + this.metadata.getField() + " in class " + theClass.getName(), null );
            }

            try
            {
                final Field field = getField( theClass, acceptPrivate, acceptPackage, logger );
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
                        { this.metadata.getField(), theClass.getName(), targetClass.getName() }, null );
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
     * Finds the field named in the {@link #fieldName} field in the given
     * <code>targetClass</code>. If the target class has no acceptable field
     * the class hierarchy is traversed until a field is found or the root
     * of the class hierarchy is reached without finding a field.
     *
     *
     * @param targetClass The class in which to look for the method
     * @param acceptPrivate <code>true</code> if private fields should be
     *      considered.
     * @param acceptPackage <code>true</code> if package private fields should
     *      be considered.
     * @param logger
     * @return The requested field or <code>null</code> if no acceptable field
     *      can be found in the target class or any super class.
     * @throws InvocationTargetException If an unexpected Throwable is caught
     *      trying to find the requested field.
     */
    private Field getField( final Class<?> clazz,
            final boolean acceptPrivate,
            final boolean acceptPackage,
            final SimpleLogger logger )
    throws SuitableMethodNotAccessibleException, InvocationTargetException
    {
        try
        {
            // find the declared field in this class
            final Field field = clazz.getDeclaredField( this.metadata.getField() );

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
                    { clazz.getName(), this.metadata.getField() }, null );
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
                buf.append( "Failure loooking up field " ).append( this.metadata.getField() );
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
            throw new InvocationTargetException( throwable, "Unexpected problem trying to get field " + this.metadata.getField() );
        }

        // caught and ignored exception, assume no field and continue search
        return null;
    }

    /**
     * Validate the field, type etc.
     * @param f The field
     * @param logger The logger
     * @return The field if it's valid, {@code null} otherwise.
     */
    private Field validateField( final Field f, final SimpleLogger logger )
    {
        final Class<?> fieldType = f.getType();
        final Class<?> referenceType = ClassUtils.getClassFromComponentClassLoader(
                this.componentClass, metadata.getInterface(), logger);

        // unary reference
        if ( !metadata.isMultiple() )
        {
            if ( fieldType.isAssignableFrom(referenceType) )
            {
                valueType = ParamType.serviceType;
            }
            else if ( fieldType == ClassUtils.SERVICE_REFERENCE_CLASS )
            {
                valueType = ParamType.serviceReference;
            }
            else if ( fieldType == ClassUtils.SERVICE_OBJECTS_CLASS )
            {
                valueType = ParamType.serviceObjects;
            }
            else if ( fieldType == ClassUtils.MAP_CLASS )
            {
                valueType = ParamType.map;
            }
            else if ( fieldType == ClassUtils.MAP_ENTRY_CLASS )
            {
                valueType = ParamType.tuple;
            }
            else
            {
                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}", new Object[]
                        {metadata.getField(), this.componentClass, fieldType.getName()}, null );
                return null;
            }

            // if the field is dynamic and optional it has to be volatile
            if ( !metadata.isStatic() && metadata.isOptional() )
            {
                if ( !Modifier.isVolatile(f.getModifiers()) )
                {
                    logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must be declared volatile to handle a dynamic reference", new Object[]
                            {metadata.getField(), this.componentClass}, null );
                    return null;
                }
            }
        }
        else
        {
            // multiple cardinality, field type must be collection or subtype
            if ( !ClassUtils.COLLECTION_CLASS.isAssignableFrom(fieldType) )
            {
                logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}", new Object[]
                        {metadata.getField(), this.componentClass, fieldType.getName()}, null );
                return null;
            }

        }
        return f;
    }

    private enum METHOD_TYPE {
        BIND,
        UNBIND,
        UPDATED
    };

    private MethodResult updateField( final METHOD_TYPE mType,
            final Object componentInstance,
            final BindParameters bp,
            final SimpleLogger logger )
        throws InvocationTargetException
    {
        final ComponentContextImpl key = bp.getComponentContext();
        final RefPair<?, ?> refPair = bp.getRefPair();

        final Object obj;
        switch ( this.valueType ) {
            case serviceType : obj = refPair.getServiceObject(key); break;
            case serviceReference : obj = refPair.getRef(); break;
            case serviceObjects : obj = refPair.getServiceObjects(); break;
            case map : obj = new ReadOnlyDictionary<String, Object>( refPair.getRef() ); break;
            case tuple : final Object tupleKey = new ReadOnlyDictionary<String, Object>( refPair.getRef() );
                         final Object tupleValue = refPair.getServiceObject(key);
                         final Map<?, ?> tupleMap = Collections.singletonMap(tupleKey, tupleValue);
                         obj = tupleMap.entrySet().iterator().next();
                         break;
            default: obj = null;
        }

        try {
            if ( mType == METHOD_TYPE.BIND ) {
                field.set(componentInstance, obj);
            } else if ( mType == METHOD_TYPE.UNBIND ) {
                field.set(componentInstance, null);
            }
        } catch ( final IllegalArgumentException iae ) {
            throw new InvocationTargetException(iae);
        } catch ( final IllegalAccessException iae ) {
            throw new InvocationTargetException(iae);
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

    /**
     * Internal state interface.
     */
    private static interface State
    {

        MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        throws InvocationTargetException;
    }

    /**
     * Initial state.
     */
    private static class NotResolved implements State
    {
        private static final State INSTANCE = new NotResolved();

        private synchronized void resolve( final FieldHandler baseMethod, final SimpleLogger logger )
        {
            logger.log( LogService.LOG_DEBUG, "getting field: {0}", new Object[]
                    {baseMethod.metadata.getField()}, null );

            // resolve the field
            Field field = null;
            try
            {
                field = baseMethod.findField( logger );
                field = baseMethod.validateField( field, logger );
            }
            catch ( final InvocationTargetException ex )
            {
                logger.log( LogService.LOG_WARNING, "{0} cannot be found", new Object[]
                        {baseMethod.metadata.getField()}, ex.getTargetException() );
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
            return baseMethod.state.invoke( baseMethod, mType, componentInstance, rawParameter, logger );
        }
    }

    /**
     * Final state of field couldn't be found or errors occured.
     */
    private static class NotFound implements State
    {
        private static final State INSTANCE = new NotFound();


        public MethodResult invoke( final FieldHandler baseMethod,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        {
            logger.log( LogService.LOG_ERROR, "Field [{0}] not found", new Object[]
                { baseMethod.metadata.getField() }, null );
            return null;
        }
    }

    /**
     * Final state of field could be found and is valid.
     */
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
            return baseMethod.updateField( mType, componentInstance, rawParameter, logger );
        }
    }

    public static final class ReferenceMethodImpl implements ReferenceMethod {

        private final METHOD_TYPE methodType;

        private final FieldHandler handler;

        public ReferenceMethodImpl(final METHOD_TYPE mt, final FieldHandler handler) {
            this.methodType = mt;
            this.handler = handler;
        }

        public MethodResult invoke(final Object componentInstance,
                final BindParameters rawParameter,
                final MethodResult methodCallFailureResult,
                final SimpleLogger logger) {
            try
            {
                return handler.state.invoke( handler,
                        methodType,
                        componentInstance,
                        rawParameter,
                        logger );
            }
            catch ( final InvocationTargetException ite )
            {
                logger.log( LogService.LOG_ERROR, "The {0} field has thrown an exception", new Object[]
                    { handler.metadata.getField() }, ite.getCause() );
            }

            return methodCallFailureResult;
        }

        public <S, T> boolean getServiceObject(final ComponentContextImpl<S> key,
                final RefPair<S, T> refPair,
                final BundleContext context,
                final SimpleLogger logger) {
            if ( methodType != METHOD_TYPE.UNBIND )
            {
                //??? this resolves which we need.... better way?
                if ( refPair.getServiceObject(key) == null
                    )
                    // TODO: && (handler.valueType == ParamType.serviceType || handler.valueType == ParamType.tuple ) )
                {
                    return refPair.getServiceObject(key, context, logger);
                }
            }
            return true;
        }

    }
    public ReferenceMethod getBind() {
        return new ReferenceMethodImpl(METHOD_TYPE.BIND, this);
    }

    public ReferenceMethod getUnbind() {
        return new ReferenceMethodImpl(METHOD_TYPE.UNBIND, this);
    }

    public ReferenceMethod getUpdated() {
        return new ReferenceMethodImpl(METHOD_TYPE.UPDATED, this);
    }
}
