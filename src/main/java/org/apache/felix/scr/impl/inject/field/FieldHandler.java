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
package org.apache.felix.scr.impl.inject.field;


import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.inject.BindParameters;
import org.apache.felix.scr.impl.inject.ClassUtils;
import org.apache.felix.scr.impl.inject.InitReferenceMethod;
import org.apache.felix.scr.impl.inject.MethodResult;
import org.apache.felix.scr.impl.inject.ReferenceMethod;
import org.apache.felix.scr.impl.inject.ValueUtils;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
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
    /** The reference metadata. */
    private final ReferenceMetadata metadata;

    /** The component class. */
    private final Class<?> componentClass;

    /** The field used for the injection. */
    private volatile Field field;

    /** Value type. */
    private volatile ValueType valueType;

    /** State handling. */
    private volatile State state;

    /** Mapping of ref pairs to value bound */
    private final Map<RefPair<?, ?>, Object> boundValues = new TreeMap<RefPair<?,?>, Object>(
        new Comparator<RefPair<?, ?>>()
        {

            public int compare(final RefPair<?, ?> o1, final RefPair<?, ?> o2)
            {
                return o1.getRef().compareTo(o2.getRef());
            }
        });

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

    private enum METHOD_TYPE
    {
        BIND,
        UNBIND,
        UPDATED
    };


    private boolean initField(final Object componentInstance,
            final SimpleLogger logger )
    {
    	if ( valueType == ValueType.ignore )
    	{
    		return true;
    	}
        try
        {
            if ( metadata.isMultiple() )
            {
                if ( metadata.isReplace()  )
                {
                    this.setFieldValue(componentInstance, new CopyOnWriteArrayList<Object>());
                }
                else
                {
                    final Class<?> fieldType = this.field.getType();

                    // update strategy: if DS implementation provides collection implementation
                    //                  only list and collection are allowed, field must not be final
                    final Object providedImpl = this.getFieldValue(componentInstance);
                    if ( providedImpl == null)
                    {
                        if ( Modifier.isFinal(this.field.getModifiers()) )
                        {
                            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must not be declared as final", new Object[]
                                    {metadata.getField(), this.componentClass}, null );
                            valueType = ValueType.ignore;
                            return true;
                        }
                        if ( fieldType != ClassUtils.LIST_CLASS && fieldType != ClassUtils.COLLECTION_CLASS )
                        {
                            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} has unsupported type {2}."+
                                " It must be one of java.util.Collection or java.util.List.",
                                new Object[] {metadata.getField(), this.componentClass, fieldType.getName()}, null );
                            valueType = ValueType.ignore;
                            return true;
                        }
                        if ( fieldType == ClassUtils.LIST_CLASS )
                        {
                        	this.setFieldValue(componentInstance, new CopyOnWriteArrayList<Object>());
                        }
                        else
                        {
                        	this.setFieldValue(componentInstance, new CopyOnWriteArraySet<Object>());
                        }
                    }
                }
            }
            else
            {
            	// only optional field need initialization
            	if ( metadata.isOptional() )
            	{
	            	// null the field if optional and unary
	            	this.setFieldValue(componentInstance, null);
	            }
            }
        }
        catch ( final InvocationTargetException ite)
        {
            valueType = ValueType.ignore;

            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} can't be initialized.",
                    new Object[] {metadata.getField(), this.componentClass}, ite );
            return false;

        }
        return true;
    }

    private Collection<Object> getReplaceCollection()
    {
        final List<Object> objects = new ArrayList<Object>();
        for(final Object val : this.boundValues.values())
        {
            objects.add(val);
        }
        return objects;
    }

    private MethodResult updateField(final METHOD_TYPE mType,
                                     final Object componentInstance,
                                     final BindParameters bp,
                                     final SimpleLogger logger )
        throws InvocationTargetException
    {
        @SuppressWarnings("rawtypes")
		final ComponentContextImpl key = bp.getComponentContext();
        final RefPair<?, ?> refPair = bp.getRefPair();

        if ( !this.metadata.isMultiple() )
        {
            // unary references

        	// unbind needs only be done, if reference is dynamic and optional
            if ( mType == METHOD_TYPE.UNBIND )
            {
                if ( this.metadata.isOptional() && !this.metadata.isStatic() )
                {
                    // we only reset if it was previously set with this value
                    if ( this.boundValues.size() == 1 )
                    {
                        this.setFieldValue(componentInstance, null);
                    }
                }
                this.boundValues.remove(refPair);
            }
            // updated needs only be done, if the value type is map or tuple
            // If it's a dynamic reference, the value can be updated
            // for a static reference we need a reactivation
            else if ( mType == METHOD_TYPE.UPDATED )
            {
            	if ( this.valueType == ValueType.ref_map || this.valueType == ValueType.ref_tuple )
            	{
            		if ( this.metadata.isStatic() )
            		{
            			return MethodResult.REACTIVATE;
            		}
                    final Object obj = ValueUtils.getValue(valueType, field.getType(), key, refPair);
                    this.setFieldValue(componentInstance, obj);
                    this.boundValues.put(refPair, obj);
            	}
            }
            // bind needs always be done
            else
            {
                final Object obj = ValueUtils.getValue(valueType, field.getType(), key, refPair);
                this.setFieldValue(componentInstance, obj);
                this.boundValues.put(refPair, obj);
            }
        }
        else
        {
            // multiple references

            // bind: replace or update the field
            if ( mType == METHOD_TYPE.BIND )
            {
                final Object obj = ValueUtils.getValue(valueType, field.getType(), key, refPair);
                this.boundValues.put(refPair, obj);
                if ( metadata.isReplace() )
                {
                    this.setFieldValue(componentInstance, getReplaceCollection());
                }
                else
                {
                    @SuppressWarnings("unchecked")
                    final Collection<Object> col = (Collection<Object>)this.getFieldValue(componentInstance);
                    col.add(obj);
                }
            }
            // unbind needs only be done, if reference is dynamic
            else if ( mType == METHOD_TYPE.UNBIND)
            {
                if ( !metadata.isStatic() )
                {
                    final Object obj = this.boundValues.remove(refPair);
                    if ( metadata.isReplace() )
                    {
                        this.setFieldValue(componentInstance, getReplaceCollection());
                    }
                    else
                    {
                        @SuppressWarnings("unchecked")
                        final Collection<Object> col = (Collection<Object>)this.getFieldValue(componentInstance);
                        col.remove(obj);
                    }
                }
            }
            // updated needs only be done, if the value type is map or tuple
            else if ( mType == METHOD_TYPE.UPDATED)
            {
            	if ( this.valueType == ValueType.ref_map || this.valueType == ValueType.ref_tuple )
            	{
                    if ( !this.metadata.isStatic() )
                    {
	                    final Object obj = ValueUtils.getValue(valueType, field.getType(), key, refPair);
	                    final Object oldObj = this.boundValues.put(refPair, obj);

	                    if ( metadata.isReplace() )
	                    {
	                        this.setFieldValue(componentInstance, getReplaceCollection());
	                    }
	                    else
	                    {
	                        @SuppressWarnings("unchecked")
	                        final Collection<Object> col = (Collection<Object>)this.getFieldValue(componentInstance);
	                        col.add(obj);
	                        col.remove(oldObj);
	                    }
                    }
                    else
                    {
                    	// if it's static we need to reactivate
                    	return MethodResult.REACTIVATE;
                    }
                }
            }
        }

        return MethodResult.VOID;
    }

    private void setFieldValue(final Object componentInstance, final Object value)
    throws InvocationTargetException
    {
        try
        {
            field.set(componentInstance, value);
        }
        catch ( final IllegalArgumentException iae )
        {
            throw new InvocationTargetException(iae);
        }
        catch ( final IllegalAccessException iae )
        {
            throw new InvocationTargetException(iae);
        }
    }

    private Object getFieldValue(final Object componentInstance)
    throws InvocationTargetException
    {
        try
        {
            return field.get(componentInstance);
        }
        catch ( final IllegalArgumentException iae )
        {
            throw new InvocationTargetException(iae);
        }
        catch ( final IllegalAccessException iae )
        {
            throw new InvocationTargetException(iae);
        }
    }




    /**
     * Internal state interface.
     */
    private static interface State
    {

        MethodResult invoke( final FieldHandler handler,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        throws InvocationTargetException;

        boolean fieldExists( final FieldHandler handler, final SimpleLogger logger);
    }

    /**
     * Initial state.
     */
    private static class NotResolved implements State
    {
        private static final State INSTANCE = new NotResolved();

        private synchronized void resolve( final FieldHandler handler, final SimpleLogger logger )
        {
            logger.log( LogService.LOG_DEBUG, "getting field: {0}", new Object[]
                    {handler.metadata.getField()}, null );

            // resolve the field
        	final FieldUtils.FieldSearchResult result = FieldUtils.searchField( handler.componentClass, handler.metadata.getField(), logger );
        	if ( result == null ) 
        	{
        		handler.field = null;         
        		handler.valueType = null;
        		handler.state = NotFound.INSTANCE;
                logger.log(LogService.LOG_ERROR, "Field [{0}] not found; Component will fail",
                    new Object[] { handler.metadata.getField() }, null);
        	}
        	else 
        	{
        		handler.field = result.field;
            	if ( !result.usable ) 
            	{
            		handler.valueType = ValueType.ignore;
            	}
            	else
            	{
            		handler.valueType = ValueUtils.getReferenceValueType( 
            				handler.componentClass,
            				handler.metadata,
            				result.field.getType(),
            				result.field, 
            				logger );
            	}
                handler.state = Resolved.INSTANCE;
                logger.log( LogService.LOG_DEBUG, "Found field: {0}",
                        new Object[] { result.field }, null );
        	}
        }

        public MethodResult invoke( final FieldHandler handler,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                SimpleLogger logger )
        throws InvocationTargetException
        {
            resolve( handler, logger );
            return handler.state.invoke( handler, mType, componentInstance, rawParameter, logger );
        }

        public boolean fieldExists( final FieldHandler handler, final SimpleLogger logger)
        {
            resolve( handler, logger );
            return handler.state.fieldExists( handler, logger );
        }
    }

    /**
     * Final state of field couldn't be found or errors occurred.
     */
    private static class NotFound implements State
    {
        private static final State INSTANCE = new NotFound();

        public MethodResult invoke( final FieldHandler handler,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
        {
            logger.log( LogService.LOG_ERROR, "Field [{0}] not found", new Object[]
                { handler.metadata.getField() }, null );
            return null;
        }

        public boolean fieldExists( final FieldHandler handler, final SimpleLogger logger)
        {
            return false;
        }
    }

    /**
     * Final state of field could be found and is valid.
     */
    private static class Resolved implements State
    {
        private static final State INSTANCE = new Resolved();

        public MethodResult invoke( final FieldHandler handler,
                final METHOD_TYPE mType,
                final Object componentInstance,
                final BindParameters rawParameter,
                final SimpleLogger logger )
            throws InvocationTargetException
        {
            return handler.updateField( mType, componentInstance, rawParameter, logger );
        }

        public boolean fieldExists( final FieldHandler handler, final SimpleLogger logger)
        {
            return true;
        }
    }

    public boolean fieldExists( SimpleLogger logger )
    {
        return this.state.fieldExists( this, logger );
    }

    public static final class ReferenceMethodImpl
        implements ReferenceMethod
    {

        private final METHOD_TYPE methodType;

        private final FieldHandler handler;

        public ReferenceMethodImpl(final METHOD_TYPE mt, final FieldHandler handler)
        {
            this.methodType = mt;
            this.handler = handler;
        }

        public <S, T> MethodResult invoke(final Object componentInstance,
                final BindParameters rawParameter,
                final MethodResult methodCallFailureResult,
                final SimpleLogger logger)
        {
            if ( handler.valueType == ValueType.ignore )
            {
                return MethodResult.VOID;
            }

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

        public <S, T> boolean getServiceObject(
                final BindParameters rawParameter,
                final BundleContext context,
                final SimpleLogger logger)
        {
            if ( methodType != METHOD_TYPE.UNBIND )
            {
                //??? this resolves which we need.... better way?
                if ( rawParameter.getRefPair().getServiceObject(rawParameter.getComponentContext()) == null
                  && handler.fieldExists( logger )
                  && (handler.valueType == ValueType.ref_serviceType || handler.valueType == ValueType.ref_tuple ) )
                {
                    return rawParameter.getRefPair().getServiceObject(rawParameter.getComponentContext(), context, logger);
                }
            }
            return true;
        }
    }

    public ReferenceMethod getBind()
    {
        return new ReferenceMethodImpl(METHOD_TYPE.BIND, this);
    }

    public ReferenceMethod getUnbind()
    {
        return new ReferenceMethodImpl(METHOD_TYPE.UNBIND, this);
    }

    public ReferenceMethod getUpdated()
    {
        return new ReferenceMethodImpl(METHOD_TYPE.UPDATED, this);
    }

    public InitReferenceMethod getInit()
    {
        if ( valueType == ValueType.ignore )
        {
            return null;
        }
        return new InitReferenceMethod()
        {

            public boolean init(final Object componentInstance, final SimpleLogger logger)
            {
                if ( fieldExists( logger ) )
                {
                    return initField(componentInstance, logger);
                }
                return false;
            }
        };
    }
}
