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

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.inject.field.FieldUtils;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.service.log.LogService;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ComponentConstructorImpl<T> implements ComponentConstructor<T>
{
    private final Field[] activationFields;
    private final ValueType[] activationFieldTypes;

    private final Constructor<T> constructor;
    private final ValueType[] constructorArgTypes;
    private final ReferenceMetadata[] constructorRefs;

    @SuppressWarnings("unchecked")
    public ComponentConstructorImpl(final ComponentMetadata componentMetadata,
            final Class<T> componentClass,
            final SimpleLogger logger)
    {
        // constructor injection
        // get reference parameter map
        final Map<Integer, ReferenceMetadata> paramMap = ( componentMetadata.getNumberOfConstructorParameters() > 0 ? new HashMap<Integer, ReferenceMetadata>() : null);
        for ( final ReferenceMetadata refMetadata : componentMetadata.getDependencies())
        {
            if ( refMetadata.getParameterIndex() != null )
            {
                final int index = refMetadata.getParameterIndex();
                if ( index > componentMetadata.getNumberOfConstructorParameters() )
                {
                    // if the index (starting at 0) is equal or higher than the number of constructor arguments
                    // we log a warning and ignore the reference
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Parameter index is too high.",
                            new Object[] { refMetadata.getName(), componentMetadata.getName() }, null );
                }
                else if ( !refMetadata.isStatic() )
                {
                    // if the reference is dynamic, we log a warning and ignore the reference
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Reference is dynamic.",
                            new Object[] { refMetadata.getName(), componentMetadata.getName() }, null );
                }
                else if ( paramMap.get(index) != null )
                {
                    // duplicate reference for the same index, we log a warning and ignore the duplicates
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Another reference has the same index.",
                            new Object[] { refMetadata.getName(), componentMetadata.getName() }, null );
                }
                else
                {
                    paramMap.put(index, refMetadata);
                }

            }
        }

        // Search constructor
        Constructor<T> found = null;
        ValueType[] foundTypes = null;
        ReferenceMetadata[] foundRefs = null;

        final Constructor<?>[] constructors = componentClass.getConstructors();
        for(final Constructor<?> c : constructors)
        {
            // we try each constructor with the right number of arguments
            if ( c.getParameterTypes().length == componentMetadata.getNumberOfConstructorParameters() )
            {
                final Constructor<T> check = (Constructor<T>) c;
                // check argument types
                if ( componentMetadata.getNumberOfConstructorParameters() > 0 )
                {
                    boolean hasFailure = false;
                    final Class<?>[] argTypes = check.getParameterTypes();
                    foundTypes = new ValueType[argTypes.length];
                    foundRefs = new ReferenceMetadata[argTypes.length];
                    for(int i=0; i<foundTypes.length;i++)
                    {
                        final ReferenceMetadata ref = paramMap.get(i);
                        if ( ref == null )
                        {
                            foundTypes[i] = ValueUtils.getValueType(argTypes[i]);
                        }
                        else
                        {
                            foundTypes[i] = ValueUtils.getReferenceValueType(componentClass, ref, argTypes[i], null, logger);
                            foundRefs[i] = ref;
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
                    found = (Constructor<T>) c;
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
    }

    @Override
    public <S> T newInstance(final ComponentContextImpl<T> componentContext,
            final Map<ReferenceMetadata, ReferencePair<S>> parameterMap)
    throws Exception
    {
        // no constructor -> throw
        if ( constructor == null )
        {
            throw new InstantiationException("Constructor not found; Component will fail");
        }

        // if we have fields and one is in state failure (type == null) we can directly throw
        int index = 0;
        for(final ValueType t : activationFieldTypes)
        {
            if ( t == null )
            {
                throw new InstantiationException("Field " + componentContext.getComponentMetadata().getActivationFields().get(index)
                        + " not found; Component will fail");
            }
            index++;
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
                final ReferencePair<S> pair = refMetadata == null ? null : parameterMap.get(refMetadata);

                if ( refMetadata == null || !refMetadata.isMultiple())
                {
                    // TODO - can we assume that pair.openStatus.refs.iterator().next() always returns a value?
                    // and is this the correct (highest?)
                    args[i] = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                            constructorArgTypes[i],
                            constructor.getParameterTypes()[i],
                            componentContext,
                            pair != null ? pair.openStatus.refs.iterator().next() : null);
                }
                else
                {
                    // reference of cardinality multiple
                    final List<Object> collection = new ArrayList<>();
                    for(final RefPair<S,?> refPair : pair.openStatus.refs)
                    {
                        // TODO - do we need further checks?
                        final Object value = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                                constructorArgTypes[i],
                                constructor.getParameterTypes()[i],
                                componentContext,
                                refPair);
                        collection.add( value );
                    }
                    args[i] = collection;
                }
            }
        }
        final T component = constructor.newInstance(args);

        // activation fields
        for(int i = 0; i<activationFieldTypes.length; i++)
        {
            if ( activationFieldTypes[i] != ValueType.ignore )
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
}
