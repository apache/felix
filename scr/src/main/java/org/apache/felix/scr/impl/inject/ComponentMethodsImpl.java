/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.felix.scr.impl.inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.impl.inject.field.FieldMethods;
import org.apache.felix.scr.impl.inject.methods.ActivateMethod;
import org.apache.felix.scr.impl.inject.methods.BindMethods;
import org.apache.felix.scr.impl.inject.methods.DeactivateMethod;
import org.apache.felix.scr.impl.inject.methods.ModifiedMethod;
import org.apache.felix.scr.impl.logger.ComponentLogger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * @version $Rev$ $Date$
 * @param <T>
 */
public class ComponentMethodsImpl<T> implements ComponentMethods<T>
{
    private LifecycleMethod m_activateMethod;
    private LifecycleMethod m_modifiedMethod;
    private LifecycleMethod m_deactivateMethod;
    private ComponentConstructor<T> m_constructor;

    private final Map<String, ReferenceMethods> bindMethodMap = new HashMap<>();

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public synchronized void initComponentMethods(
	        final ComponentMetadata componentMetadata,
	        final Class<T> implementationObjectClass,
	        final ComponentLogger logger)
    {
        if (m_activateMethod != null)
        {
            // do init only once
            return;
        }
        DSVersion dsVersion = componentMetadata.getDSVersion();
        boolean configurableServiceProperties = componentMetadata.isConfigurableServiceProperties();
        boolean supportsInterfaces = componentMetadata.isConfigureWithInterfaces();

        m_activateMethod = new ActivateMethod(
        		componentMetadata.getActivate(),
        		componentMetadata.isActivateDeclared(),
        		implementationObjectClass,
        		dsVersion,
        		configurableServiceProperties,
        		supportsInterfaces);
        m_deactivateMethod = new DeactivateMethod( componentMetadata.getDeactivate(),
                componentMetadata.isDeactivateDeclared(), implementationObjectClass, dsVersion, configurableServiceProperties, supportsInterfaces );

        m_modifiedMethod = new ModifiedMethod( componentMetadata.getModified(), implementationObjectClass, dsVersion, configurableServiceProperties, supportsInterfaces );

        for ( ReferenceMetadata referenceMetadata: componentMetadata.getDependencies() )
        {
            final String refName = referenceMetadata.getName();
            final List<ReferenceMethods> methods = new ArrayList<>();
            if ( referenceMetadata.getField() != null )
            {
                methods.add(new FieldMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties));
            }
            if ( referenceMetadata.getBind() != null )
            {
                methods.add(new BindMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties));
            }

            if ( methods.isEmpty() )
            {
            	    bindMethodMap.put( refName, ReferenceMethods.NOPReferenceMethod );
            }
            else if ( methods.size() == 1 )
            {
            	    bindMethodMap.put( refName, methods.get(0) );
            }
            else
            {
            	    bindMethodMap.put( refName, new DuplexReferenceMethods(methods) );
            }
        }

    	    m_constructor = new ComponentConstructor(componentMetadata, implementationObjectClass, logger);
    }

	@Override
    public LifecycleMethod getActivateMethod()
    {
        return m_activateMethod;
    }

	@Override
    public LifecycleMethod getDeactivateMethod()
    {
        return m_deactivateMethod;
    }

	@Override
    public LifecycleMethod getModifiedMethod()
    {
        return m_modifiedMethod;
    }

	@Override
    public ReferenceMethods getBindMethods(String refName )
    {
        return bindMethodMap.get( refName );
    }

	@Override
	public ComponentConstructor<T> getConstructor()
	{
		return m_constructor;
	}
}
