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
import java.util.TreeMap;

import org.apache.felix.scr.impl.helper.ConstructorMethod;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.DependencyManager;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;
import org.osgi.service.log.LogService;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ConstructorMethodImpl<T> implements ConstructorMethod<T> 
{
    private volatile boolean m_initialized = false;
    
    private volatile FieldUtils.ValueType[] m_paramTypes;
    private volatile Field[] m_fields;
    
    private volatile Constructor<T> m_constructor;
    private volatile FieldUtils.ValueType[] m_constructorArgTypes;
    
	@SuppressWarnings("unchecked")
	@Override
	public <S> T newInstance(Class<T> componentClass, 
             			 ComponentContextImpl<T> componentContext, 
                         TreeMap<Integer, DependencyManager<S, ?>> parameterMap,
			             SimpleLogger logger)
    throws Exception
	{
		if ( !m_initialized ) {
			// activation fields
			if ( componentContext.getComponentMetadata().getActivationFields() != null )
			{
				m_paramTypes = new FieldUtils.ValueType[componentContext.getComponentMetadata().getActivationFields().size()];
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
							m_paramTypes[index] = FieldUtils.getValueType(result.field.getType());
							m_fields[index] = result.field;
						}
						else
						{
							m_paramTypes[index] = FieldUtils.ValueType.ignore;
							m_fields[index] = null;
						}
					}
					index++;
				}
			}
			else
			{
				m_paramTypes = FieldUtils.EMPTY_TYPES;
				m_fields = null;
			}
			// constructor injection
			if ( ComponentMetadata.CONSTRUCTOR_MARKER.equals(componentContext.getComponentMetadata().getActivate() ) ) 
			{
				// TODO search constructor
				m_constructor = null;
				
				boolean hasFailure = false;
				final Class<?>[] argTypes = m_constructor.getParameterTypes();
				m_constructorArgTypes = new FieldUtils.ValueType[argTypes.length];
				for(int i=0;i<m_constructorArgTypes.length;i++)
				{
					final DependencyManager<S, ?> dm = parameterMap.get(i);
					ReferenceMetadata reference = dm == null ? null : dm.getReferenceMetadata();
					if ( reference == null )
					{
						m_constructorArgTypes[i] = FieldUtils.getValueType(argTypes[i]);
					}
					else 
					{
						m_constructorArgTypes[i] = FieldUtils.getReferenceValueType(componentClass, reference, argTypes[i], null, logger);
					}
					if ( m_constructorArgTypes[i] == FieldUtils.ValueType.ignore )
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
		for(final FieldUtils.ValueType t : m_paramTypes)
		{
			if ( t == null )
			{
				throw new InstantiationException("Field not found; Component will fail");
			}
		}

		// constructor
		final T component;
		if ( ComponentMetadata.CONSTRUCTOR_MARKER.equals(componentContext.getComponentMetadata().getActivate() ) ) 
		{
			if ( m_constructor == null )
			{
				throw new InstantiationException("Constructor not found; Component will fail");				
			}
			final Object[] args = new Object[m_constructorArgTypes.length];
			for(int i=0; i<args.length; i++) 
			{
				final DependencyManager<S, ?> dm = parameterMap.get(i);
				args[i] = FieldUtils.getValue(m_constructorArgTypes[i], 
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
	    	if ( m_paramTypes[i] != FieldUtils.ValueType.ignore )
	    	{
		        final Object value = FieldUtils.getValue(m_paramTypes[i], 
		        		m_fields[i].getType(), 
		        		(ComponentContextImpl<T>) componentContext, 
		        		null);
				this.setField(m_fields[i], component, value, logger);
	    	}
		}
		
		return component;
	}

   
    /**
     * Set the field, type etc.
     * @param f The field
     * @param logger The logger
     */
	private void setField( final Field f, 
    		final T component,
    		final Object value,
    		final SimpleLogger logger )
    {
        try
        {
            f.set(component, value);
        }
        catch ( final IllegalArgumentException iae )
        {
            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} can't be set", new Object[]
                    {f.getName(), component.getClass().getName()}, iae );
        }
        catch ( final IllegalAccessException iae )
        {
            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} can't be set", new Object[]
                    {f.getName(), component.getClass().getName()}, iae );
        }
    }
    
}
