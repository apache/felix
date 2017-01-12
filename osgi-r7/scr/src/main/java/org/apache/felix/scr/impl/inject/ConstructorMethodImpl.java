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

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.inject.ValueUtils.ValueType;
import org.apache.felix.scr.impl.inject.field.FieldUtils;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ConstructorMethodImpl<T> implements ConstructorMethod<T> 
{
    private volatile boolean m_initialized = false;
    
    private volatile ValueType[] m_paramTypes;
    private volatile Field[] m_fields;
    
    private volatile Constructor<T> m_constructor;
    private volatile ValueType[] m_constructorArgTypes;
    
	@SuppressWarnings("unchecked")
	@Override
	public <S> T newInstance(Class<T> componentClass, 
             			 ComponentContextImpl<T> componentContext, 
                         Map<Integer, ReferencePair<S>> parameterMap,
			             SimpleLogger logger)
    throws Exception
	{
		if ( !m_initialized ) {
			// activation fields
			if ( componentContext.getComponentMetadata().getActivationFields() != null )
			{
				m_paramTypes = new ValueType[componentContext.getComponentMetadata().getActivationFields().size()];
				m_fields = new Field[m_paramTypes.length];
			
				int index = 0;
				for(final String fieldName : componentContext.getComponentMetadata().getActivationFields() ) 
				{
					final FieldUtils.FieldSearchResult result = FieldUtils.searchField(componentClass, fieldName, logger);
					if ( result == null || result.field == null )
					{
						m_paramTypes[index] = null;
						m_fields[index] = null;
					}
					else
					{
						if ( result.usable )
						{
							m_paramTypes[index] = ValueUtils.getValueType(result.field.getType());
							m_fields[index] = result.field;
						}
						else
						{
							m_paramTypes[index] = ValueType.ignore;
							m_fields[index] = null;
						}
					}
					index++;
				}
			}
			else
			{
				m_paramTypes = ValueUtils.EMPTY_VALUE_TYPES;
				m_fields = null;
			}
			// constructor injection
			if ( componentContext.getComponentMetadata().isActivateConstructor() ) 
			{
				// TODO search constructor
				m_constructor = null;
				
				boolean hasFailure = false;
				final Class<?>[] argTypes = m_constructor.getParameterTypes();
				m_constructorArgTypes = new ValueType[argTypes.length];
				for(int i=0;i<m_constructorArgTypes.length;i++)
				{
					final ReferencePair<S> pair = parameterMap.get(i);
					ReferenceMetadata reference = pair == null ? null : pair.dependencyManager.getReferenceMetadata();
					if ( reference == null )
					{
						m_constructorArgTypes[i] = ValueUtils.getValueType(argTypes[i]);
					}
					else 
					{
						m_constructorArgTypes[i] = ValueUtils.getReferenceValueType(componentClass, reference, argTypes[i], null, logger);
					}
					if ( m_constructorArgTypes[i] == ValueType.ignore )
					{
						hasFailure = true;
						break;
					}
				}
				if ( hasFailure )
				{
					m_constructor = null;
				}
			}
			
			// done
			m_initialized = true;
		}
		
		// if we have fields and one is in state failure (type == null) we can directly throw
		for(final ValueType t : m_paramTypes)
		{
			if ( t == null )
			{
				throw new InstantiationException("Field not found; Component will fail");
			}
		}

		// constructor
		final T component;
		if ( componentContext.getComponentMetadata().isActivateConstructor() ) 
		{
			if ( m_constructor == null )
			{
				throw new InstantiationException("Constructor not found; Component will fail");				
			}
			final Object[] args = new Object[m_constructorArgTypes.length];
			for(int i=0; i<args.length; i++) 
			{
				// TODO - get ref pair
				final ReferencePair<S> pair = parameterMap.get(i);
				args[i] = ValueUtils.getValue(m_constructorArgTypes[i], 
						m_constructor.getParameterTypes()[i], 
						componentContext, 
						null);
			}
		    component = m_constructor.newInstance(args);
		}
		else
		{
		    component = (T)ConstructorMethod.DEFAULT.newInstance((Class<Object>)componentClass, 
		    		(ComponentContextImpl<Object>)componentContext, null, logger);
		}
		
		// activation fields
		for(int i = 0; i<m_paramTypes.length; i++)
		{
	    	if ( m_paramTypes[i] != ValueType.ignore )
	    	{
		        final Object value = ValueUtils.getValue(m_paramTypes[i], 
		        		m_fields[i].getType(), 
		        		(ComponentContextImpl<T>) componentContext, 
		        		null);
				FieldUtils.setField(m_fields[i], component, value, logger);
	    	}
		}
		
		return component;
	}    
}
