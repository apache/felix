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
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.inject.field.FieldUtils;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.service.log.LogService;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ComponentConstructorImpl<T> implements ComponentConstructor<T>
{
    private final ValueType[] paramTypes;
    private final Field[] fields;

    private final Constructor<T> constructor;
    private final ValueType[] constructorArgTypes;

    @SuppressWarnings("unchecked")
    public ComponentConstructorImpl(final ComponentMetadata componentMetadata,
            final Class<T> componentClass,
            final SimpleLogger logger)
    {
        // constructor injection
        // get reference parameter map
        final Map<Integer, ReferenceMetadata> paramMap = ( componentMetadata.getNumberOfConstructorParameters() > 0 ? new HashMap<Integer, ReferenceMetadata>() : null);
        for ( final ReferenceMetadata ref : componentMetadata.getDependencies())
        {
            if ( ref.getParameterIndex() != null )
            {
                final int index = ref.getParameterIndex();
                if ( index > componentMetadata.getNumberOfConstructorParameters() )
                {
                    // if the index (starting at 0) is equal or higher than the number of constructor arguments
                    // we log a warning and ignore the reference
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Parameter index is too high.",
                            new Object[] { ref.getName(), componentMetadata.getName() }, null );
                }
                else if ( !ref.isStatic() )
                {
                    // if the reference is dynamic, we log a warning and ignore the reference
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Reference is dynamic.",
                            new Object[] { ref.getName(), componentMetadata.getName() }, null );
                }
                else if ( paramMap.get(index) != null )
                {
                    // duplicate reference for the same index, we log a warning and ignore the duplicates
                    logger.log(LogService.LOG_WARNING,
                            "Ignoring reference {0} in component {1} for constructor injection. Another reference has the same index.",
                            new Object[] { ref.getName(), componentMetadata.getName() }, null );
                }
                else
                {
                    paramMap.put(index, ref);
                }

            }
        }

        // Search constructor
        Constructor<T> found = null;
        ValueType[] foundTypes = null;
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

        // activation fields
        if ( componentMetadata.getActivationFields() != null )
        {
            paramTypes = new ValueType[componentMetadata.getActivationFields().size()];
            fields = new Field[paramTypes.length];

            int index = 0;
            for(final String fieldName : componentMetadata.getActivationFields() )
            {
                final FieldUtils.FieldSearchResult result = FieldUtils.searchField(componentClass, fieldName, logger);
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
    }

    @Override
    public <S> T newInstance(final ComponentContextImpl<T> componentContext,
            final Map<Integer, ReferencePair<S>> parameterMap)
    throws Exception
    {
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
                args[i] = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
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
                final Object value = ValueUtils.getValue(constructor.getDeclaringClass().getName(),
                        paramTypes[i],
                        fields[i].getType(),
                        componentContext,
                        null); // null is ok as activation fields are not references
                FieldUtils.setField(fields[i], component, value, componentContext.getLogger());
            }
        }

        // TODO - add field initialization for references

        return component;
    }
}
