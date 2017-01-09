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

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ConstructorMethod;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * This implementation is used to construct a component instance object,
 * call the constructor and set the activation fields.
 */
public class ConstructorMethodImpl<T> implements ConstructorMethod<T> 
{
	/** If the activate method has this value, a constructor is used instead. */
	public static final String CONSTRUCTOR_MARKER = "-init-";
	
    private enum ParamType {
        failure,
        ignore,
        componentContext,
        bundleContext,
        map,
        annotation
    }

    private final ComponentMetadata m_metadata;
    
    private final ParamType[] EMPTY_PARAMS = new ParamType[0];
    
    private volatile ParamType[] m_paramTypes;
    private volatile Field[] m_fields;
    
	public ConstructorMethodImpl( final ComponentMetadata metadata)
	{
		this.m_metadata = metadata;

	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T newInstance(Class<T> componentClass, ComponentContext componentContext, SimpleLogger logger)
			throws Exception
	{
		if ( m_paramTypes == null ) {
			if ( m_metadata.getActivationFields() != null )
			{
				m_paramTypes = new ParamType[m_metadata.getActivationFields().size()];
				m_fields = new Field[m_paramTypes.length];
			
				int index = 0;
				for(final String fieldName : m_metadata.getActivationFields() ) 
				{
					final FieldUtils.FieldSearchResult result = FieldUtils.findField(componentClass, fieldName, logger);
					if ( result == null )
					{
						m_paramTypes[index] = ParamType.failure;
						m_fields[index] = null;
					}
					else
					{
						if ( result.usable )
						{
							m_paramTypes[index] = getFieldType(result.field);
							m_fields[index] = result.field;
						}
						else
						{
							m_paramTypes[index] = ParamType.ignore;
							m_fields[index] = null;
						}
					}
					index++;
				}
			}
			else
			{
				m_paramTypes = EMPTY_PARAMS;
				m_fields = null;
			}
		}
		
		// if we have fields and one is in state failure we can directly throw
		for(final ParamType t : m_paramTypes)
		{
			if ( t == ParamType.failure )
			{
				throw new InstantiationException("Field not found; Component will fail");
			}
		}

		final T component;
		if ( CONSTRUCTOR_MARKER.equals(m_metadata.getActivate() ) ) 
		{
		    component = null;
		    throw new IllegalStateException("Constructor init not implemented yet");
		}
		else
		{
		    component = (T)ConstructorMethod.DEFAULT.newInstance((Class<Object>)componentClass, componentContext, logger);
		}
		for(int i = 0; i<m_paramTypes.length; i++)
		{
			setField(componentClass, m_fields[i], m_paramTypes[i], component, componentContext, logger);
		}
		
		return component;
	}

	/**
     * Get the field parameter type.
     * @param f The field
     * @return The parameter type of the field
     */
    private ParamType getFieldType( final Field f)
    {
        final Class<?> fieldType = f.getType();
        if ( fieldType == ClassUtils.COMPONENT_CONTEXT_CLASS )
        {
        	return ParamType.componentContext;
        }
        else if ( fieldType == ClassUtils.BUNDLE_CONTEXT_CLASS )
        {
        	return ParamType.bundleContext;
        }
        else if ( fieldType == ClassUtils.MAP_CLASS )
        {
        	return ParamType.map;
        }
        else
        {
        	return ParamType.annotation;
        }
    }
    
    /**
     * Set the field, type etc.
     * @param f The field
     * @param logger The logger
     */
    @SuppressWarnings("unchecked")
	private void setField( final Class<T> componentClass, 
    		final Field f, 
    		final ParamType type,
    		final T component,
    		final ComponentContext componentContext,
    		final SimpleLogger logger )
    {
    	if ( type == ParamType.ignore )
    	{
    		return;
    	}
        final Object value;
        if ( type == ParamType.componentContext )
        {
    	    value = componentContext;
        }
        else if ( type == ParamType.bundleContext )
        {
            value = componentContext.getBundleContext();
        }
        else if ( type == ParamType.map )
        {
            // note: getProperties() returns a ReadOnlyDictionary which is a Map
        	value = componentContext.getProperties();
        }
        else
        {
        	value = Annotations.toObject(f.getType(),
                (Map<String, Object>) componentContext.getProperties(),
                componentContext.getBundleContext().getBundle(), m_metadata.isConfigureWithInterfaces());
        }
        try
        {
            f.set(component, value);
        }
        catch ( final IllegalArgumentException iae )
        {
            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} can't be set", new Object[]
                    {f.getName(), componentClass}, iae );
        }
        catch ( final IllegalAccessException iae )
        {
            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} can't be set", new Object[]
                    {f.getName(), componentClass}, iae );
        }
    }
}
