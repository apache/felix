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
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ConstructorMethod;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

public class ConstructorMethodImpl implements ConstructorMethod 
{

	private final ComponentMetadata m_metadata;
	
	public ConstructorMethodImpl( final ComponentMetadata metadata)
	{
		m_metadata = metadata;
	}
	
	@Override
	public <T> T newInstance(Class<T> componentClass, ComponentContext componentContext, SimpleLogger logger)
			throws Exception
	{
		final T component = ConstructorMethod.DEFAULT.newInstance(componentClass, componentContext, logger);
		
		if ( m_metadata.getActivationFields() != null )
		{
			for(final String fieldName : m_metadata.getActivationFields() ) 
			{
				Field field = FieldUtils.findField(componentClass, fieldName, logger);
				if ( field != null )
				{
					setField(componentClass, field, component, componentContext, logger);
				}
			}
		}
		
		return component;
	}
	
    /**
     * Validate and set the field, type etc.
     * @param f The field
     * @param logger The logger
     */
    private <T> void setField( final Class<T> componentClass, 
    		final Field f, 
    		final T component,
    		final ComponentContext componentContext,
    		final SimpleLogger logger )
    {

        // ignore static fields
        if ( Modifier.isStatic(f.getModifiers()))
        {
            logger.log( LogService.LOG_ERROR, "Field {0} in component {1} must not be static", new Object[]
                    {f.getName(), componentClass}, null );
        }
        else
        {
            final Class<?> fieldType = f.getType();
            final Object value;
            if ( fieldType == ClassUtils.COMPONENT_CONTEXT_CLASS )
            {
        	    value = componentContext;
            }
            else if ( fieldType == ClassUtils.BUNDLE_CONTEXT_CLASS )
            {
                value = componentContext.getBundleContext();
            }
            else if ( fieldType == ClassUtils.MAP_CLASS )
            {
                // note: getProperties() returns a ReadOnlyDictionary which is a Map
            	value = componentContext.getProperties();
            }
            else
            {
            	value = Annotations.toObject(fieldType,
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
}
