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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.inject.field.FieldUtils;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.service.log.LogService;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ComponentConstructor<S>
{
    private final Field[] activationFields;
    private final ValueType[] activationFieldTypes;

    private final Constructor<S> constructor;
    private final ValueType[] constructorArgTypes;
    private final ReferenceMetadata[] constructorRefs;

    @SuppressWarnings("unchecked")
    public ComponentConstructor(final ComponentMetadata componentMetadata,
            final Class<S> componentClass,
            final ComponentLogger logger)
    {
        // constructor injection
        // get reference parameter map
        final Map<Integer, List<ReferenceMetadata>> paramMap = ( componentMetadata.getNumberOfConstructorParameters() > 0 ? new HashMap<Integer, List<ReferenceMetadata>>() : null);
        for ( final ReferenceMetadata refMetadata : componentMetadata.getDependencies())
        {
            if ( refMetadata.getParameterIndex() != null )
            {
                final int index = refMetadata.getParameterIndex();
                if ( index > componentMetadata.getNumberOfConstructorParameters() )
                {
                    // if the index (starting at 0) is equal or higher than the number of constructor arguments
                    // we log an error and ignore the reference
                    logger.log(LogService.LOG_ERROR,
                            "Ignoring reference {0} for constructor injection. Parameter index is too high.", null,
                            refMetadata.getName() );
                }
                else if ( !refMetadata.isStatic() )
                {
                    // if the reference is dynamic, we log an error and ignore the reference
                    logger.log(LogService.LOG_ERROR,
                            "Ignoring reference {0} for constructor injection. Reference is dynamic.", null,
                            refMetadata.getName() );
                }
                List<ReferenceMetadata> list = paramMap.get(index);
                if ( list == null )
                {
                    list = new ArrayList<>();
                    paramMap.put(index, list);
                }
                list.add(refMetadata);
            }
        }

        // Search constructor
        Constructor<S> found = null;
        ValueType[] foundTypes = null;
        ReferenceMetadata[] foundRefs = null;

        final Constructor<?>[] constructors = componentClass.getConstructors();
        for(final Constructor<?> c : constructors)
        {
            // we try each constructor with the right number of arguments
            if ( c.getParameterTypes().length == componentMetadata.getNumberOfConstructorParameters() )
            {
                final Constructor<S> check = (Constructor<S>) c;
                logger.log(LogService.LOG_DEBUG,
                        "Checking constructor {0}", null,
                        check );
                // check argument types
                if ( componentMetadata.getNumberOfConstructorParameters() > 0 )
                {
                    boolean hasFailure = false;
                    final Class<?>[] argTypes = check.getParameterTypes();
                    foundTypes = new ValueType[argTypes.length];
                    foundRefs = new ReferenceMetadata[argTypes.length];
                    for(int i=0; i<foundTypes.length;i++)
                    {
                        final List<ReferenceMetadata> refs = paramMap.get(i);
                        if ( refs == null )
                        {
                            foundTypes[i] = ValueUtils.getValueType(argTypes[i]);
                            if ( foundTypes[i] == ValueType.ignore )
                            {
                                logger.log(LogService.LOG_DEBUG,
                                        "Constructor argument type {0} not supported by constructor injection: {1}", null,
                                        i, argTypes[i] );
                            }
                        }
                        else
                        {
                            for(final ReferenceMetadata ref : refs)
                            {
                                final ValueType t = ValueUtils.getReferenceValueType(componentClass, ref, argTypes[i], null, logger);
                                if ( t != null )
                                {
                                    foundTypes[i] = t;
                                    foundRefs[i] = ref;
                                    break;
                                }
                            }
                            if ( foundTypes[i] == null )
                            {
                                foundTypes[i] = ValueType.ignore;
                            }
                            else
                            {
                                if ( refs.size() > 1 )
                                {
                                    logger.log(LogService.LOG_ERROR,
                                            "Several references for constructor injection of parameter {0}. Only {1} will be used out of: {2}.", null,
                                            i, foundRefs[i].getName(), getNames(refs) );
                                }
                            }
                        }

                        if ( foundTypes[i] == ValueType.ignore )
                        {
                            hasFailure = true;
                            break;
                        }
                    }
                    if ( !hasFailure )
                    {
                        found = check;
                        break;
                    }
                }
                else
                {
                    found = (Constructor<S>) c;
                    break;
                }
            }
        }

        this.constructor = found;
        this.constructorArgTypes = foundTypes;
        this.constructorRefs = foundRefs;

        // activation fields
        if ( componentMetadata.getActivationFields() != null )
        {
            activationFieldTypes = new ValueType[componentMetadata.getActivationFields().size()];
            activationFields = new Field[activationFieldTypes.length];

            int index = 0;
            for(final String fieldName : componentMetadata.getActivationFields() )
            {
                final FieldUtils.FieldSearchResult result = FieldUtils.searchField(componentClass, fieldName, logger);
                if ( result == null || result.field == null )
                {
                    activationFieldTypes[index] = null;
                    activationFields[index] = null;
                }
                else
                {
                    if ( result.usable )
                    {
                        activationFieldTypes[index] = ValueUtils.getValueType(result.field.getType());
                        activationFields[index] = result.field;
                    }
                    else
                    {
                        activationFieldTypes[index] = ValueType.ignore;
                        activationFields[index] = null;
                    }
                }

                index++;
            }
        }
        else
        {
            activationFieldTypes = ValueUtils.EMPTY_VALUE_TYPES;
            activationFields = null;
        }

        if ( constructor == null )
        {
            logger.log(LogService.LOG_ERROR,
                    "Constructor with {0} arguments not found. Component will fail.", null,
                    componentMetadata.getNumberOfConstructorParameters() );
        }
        else
        {
            logger.log(LogService.LOG_DEBUG,
                    "Found constructor with {0} arguments : {1}", null,
                    componentMetadata.getNumberOfConstructorParameters(), found );
        }
    }

    /**
     * Create a new instance
     * @param componentContext The component context
     * @param parameterMap A map of reference parameters for handling references in the
     *                     constructor
     * @return The instance
     * @throws Exception If anything goes wrong, like constructor can't be found etc.
     */
    public <T> S newInstance(final ComponentContextImpl<S> componentContext,
            final Map<ReferenceMetadata, DependencyManager.OpenStatus<S, ?>> parameterMap)
    throws Exception
    {
        // no constructor -> throw
        if ( constructor == null )
        {
            throw new InstantiationException("Constructor not found.");
        }

        final Object[] args;
        if ( constructorArgTypes == null )
        {
            args = null;
        }
        else
        {
            args = new Object[constructorArgTypes.length];
            for(int i=0; i<args.length; i++)
            {
                final ReferenceMetadata refMetadata = this.constructorRefs[i];
                final DependencyManager.OpenStatus<S, ?> status = refMetadata == null ? null : parameterMap.get(refMetadata);

                if ( refMetadata == null )
                {
                    args[i] = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                            constructorArgTypes[i],
                            constructor.getParameterTypes()[i],
                            componentContext,
                            null);
                }
                else
                {
                    final List<Object> refs = refMetadata.isMultiple() ? new ArrayList<>() : null;
                    Object ref = null;
                    for(final RefPair<S, ?> refPair : status.refs)
                    {
                        if ( !refPair.isDeleted() && !refPair.isFailed() )
                        {
                            if ( refPair.getServiceObject(componentContext) == null
                                    && (constructorArgTypes[i] == ValueType.ref_serviceType || constructorArgTypes[i] == ValueType.ref_tuple ) )
                            {
                                refPair.getServiceObject(componentContext, componentContext.getBundleContext());
                            }
                            ref = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                                    constructorArgTypes[i],
                                    constructor.getParameterTypes()[i],
                                    componentContext,
                                    refPair);
                            if ( refMetadata.isMultiple() && ref != null )
                            {
                                refs.add(ref);
                            }
                        }
                    }
                    if ( !refMetadata.isMultiple())
                    {
                        if ( ref == null )
                        {
                            throw new InstantiationException("Unable to get service for reference " + refMetadata.getName());
                        }
                        args[i] = ref;
                    }
                    else
                    {
                        args[i] = refs;
                    }
                }
            }
        }
        final S component = constructor.newInstance(args);

        // activation fields
        for(int i = 0; i<activationFieldTypes.length; i++)
        {
            if ( activationFieldTypes[i] != null && activationFieldTypes[i] != ValueType.ignore )
            {
                final Object value = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                        activationFieldTypes[i],
                        activationFields[i].getType(),
                        componentContext,
                        null); // null is ok as activation fields are not references
                FieldUtils.setField(activationFields[i], component, value, componentContext.getLogger());
            }
        }

        return component;
    }

    private String getNames(final List<ReferenceMetadata> refs)
    {
        final StringBuilder sb = new StringBuilder();
        for(final ReferenceMetadata refMetadata : refs)
        {
            if ( sb.length() == 0 )
            {
                sb.append(refMetadata.getName());
            }
            else
            {
                sb.append(", ").append(refMetadata.getName());
            }
        }
        return sb.toString();
    }
}
