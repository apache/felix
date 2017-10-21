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
import java.util.Map;

import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.inject.field.FieldUtils;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ComponentConstructorImpl<T> implements ComponentConstructor<T>
{
    private volatile boolean initialized = false;

    private volatile ValueType[] paramTypes;
    private volatile Field[] fields;

    private volatile Constructor<T> constructor;
    private volatile ValueType[] constructorArgTypes;

    @SuppressWarnings("unchecked")
    @Override
    public <S> T newInstance(final Class<T> componentClass,
            final ComponentContextImpl<T> componentContext,
            final Map<Integer, ReferencePair<S>> parameterMap)
    throws Exception
    {
        if ( !initialized ) {
            // activation fields
            if ( componentContext.getComponentMetadata().getActivationFields() != null )
            {
                paramTypes = new ValueType[componentContext.getComponentMetadata().getActivationFields().size()];
                fields = new Field[paramTypes.length];

                int index = 0;
                for(final String fieldName : componentContext.getComponentMetadata().getActivationFields() )
                {
                    final FieldUtils.FieldSearchResult result = FieldUtils.searchField(componentClass, fieldName, componentContext.getLogger());
                    if ( result == null || result.field == null )
                    {
                        paramTypes[index] = null;
                        fields[index] = null;
                    }
                    else
                    {
                        if ( result.usable )
                        {
                            paramTypes[index] = ValueUtils.getValueType(result.field.getType());
                            fields[index] = result.field;
                        }
                        else
                        {
                            paramTypes[index] = ValueType.ignore;
                            fields[index] = null;
                        }
                    }
                    index++;
                }
            }
            else
            {
                paramTypes = ValueUtils.EMPTY_VALUE_TYPES;
                fields = null;
            }

            // Search constructor
            final Constructor<?>[] constructors = componentClass.getConstructors();
            for(final Constructor<?> c : constructors)
            {
                // we pick the first one with the right number of arguments
                if ( c.getParameterTypes().length == componentContext.getComponentMetadata().getNumberOfConstructorParameters() )
                {
                    constructor = (Constructor<T>) c;
                    break;
                }
                if ( constructor != null && componentContext.getComponentMetadata().getNumberOfConstructorParameters() > 0 )
                {
                    boolean hasFailure = false;
                    final Class<?>[] argTypes = constructor.getParameterTypes();
                    constructorArgTypes = new ValueType[argTypes.length];
                    for(int i=0;i<constructorArgTypes.length;i++)
                    {
                        final ReferencePair<S> pair = parameterMap.get(i);
                        ReferenceMetadata reference = pair == null ? null : pair.dependencyManager.getReferenceMetadata();
                        if ( reference == null )
                        {
                            constructorArgTypes[i] = ValueUtils.getValueType(argTypes[i]);
                        }
                        else
                        {
                            constructorArgTypes[i] = ValueUtils.getReferenceValueType(componentClass, reference, argTypes[i], null, componentContext.getLogger());
                        }
                        if ( constructorArgTypes[i] == ValueType.ignore )
                        {
                            hasFailure = true;
                            break;
                        }
                    }
                    if ( hasFailure )
                    {
                        constructor = null;
                    }
                }
            }

            // done
            initialized = true;
        }

        // if we have fields and one is in state failure (type == null) we can directly throw
        int index = 0;
        for(final ValueType t : paramTypes)
        {
            if ( t == null )
            {
                throw new InstantiationException("Field " + componentContext.getComponentMetadata().getActivationFields().get(index)
                        + " not found; Component will fail");
            }
            index++;
        }

        // constructor
        if ( constructor == null )
        {
            throw new InstantiationException("Constructor not found; Component will fail");
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
                // TODO - handle reference cardinality multiple
                // TODO - can we assume that pair.openStatus.refs.iterator().next() always returns a value?
                final ReferencePair<S> pair = parameterMap.get(i);
                args[i] = ValueUtils.getValue(componentClass.getName(),
                        constructorArgTypes[i],
                        constructor.getParameterTypes()[i],
                        componentContext,
                        pair != null ? pair.openStatus.refs.iterator().next() : null);
            }
        }
        final T component = constructor.newInstance(args);

        // activation fields
        for(int i = 0; i<paramTypes.length; i++)
        {
            if ( paramTypes[i] != ValueType.ignore )
            {
                final Object value = ValueUtils.getValue(componentClass.getName(),
                        paramTypes[i],
                        fields[i].getType(),
                        componentContext,
                        null); // null is ok as activation fields are not references
                FieldUtils.setField(fields[i], component, value, componentContext.getLogger());
            }
        }

        return component;
    }
}
