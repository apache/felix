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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.impl.helper.ComponentMethod;
import org.apache.felix.scr.impl.helper.ComponentMethods;
import org.apache.felix.scr.impl.helper.ReferenceMethods;
import org.apache.felix.scr.impl.inject.ActivateMethod;
import org.apache.felix.scr.impl.inject.BindMethods;
import org.apache.felix.scr.impl.inject.DeactivateMethod;
import org.apache.felix.scr.impl.inject.DuplexReferenceMethods;
import org.apache.felix.scr.impl.inject.FieldMethods;
import org.apache.felix.scr.impl.inject.ModifiedMethod;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * @version $Rev$ $Date$
 */
public class ComponentMethodsImpl implements ComponentMethods
{
    private ActivateMethod m_activateMethod;
    private ModifiedMethod m_modifiedMethod;
    private DeactivateMethod m_deactivateMethod;

    private final Map<String, ReferenceMethods> bindMethodMap = new HashMap<String, ReferenceMethods>();

    public synchronized void initComponentMethods( ComponentMetadata componentMetadata, Class<?> implementationObjectClass )
    {
        if (m_activateMethod != null)
        {
            return;
        }
        DSVersion dsVersion = componentMetadata.getDSVersion();
        boolean configurableServiceProperties = componentMetadata.isConfigurableServiceProperties();
        boolean supportsInterfaces = componentMetadata.isConfigureWithInterfaces();
        m_activateMethod = new ActivateMethod( componentMetadata.getActivate(), componentMetadata
                .isActivateDeclared(), implementationObjectClass, dsVersion, configurableServiceProperties, supportsInterfaces );
        m_deactivateMethod = new DeactivateMethod( componentMetadata.getDeactivate(),
                componentMetadata.isDeactivateDeclared(), implementationObjectClass, dsVersion, configurableServiceProperties, supportsInterfaces );

        m_modifiedMethod = new ModifiedMethod( componentMetadata.getModified(), implementationObjectClass, dsVersion, configurableServiceProperties, supportsInterfaces );

        for ( ReferenceMetadata referenceMetadata: componentMetadata.getDependencies() )
        {
            final String refName = referenceMetadata.getName();
            final ReferenceMethods methods;
            if ( referenceMetadata.getField() != null && referenceMetadata.getBind() != null)
            {
                methods = new DuplexReferenceMethods(
                        new FieldMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties),
                        new BindMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties));
            }
            else if ( referenceMetadata.getField() != null )
            {
                methods = new FieldMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties);
            }
            else
            {
                methods = new BindMethods( referenceMetadata, implementationObjectClass, dsVersion, configurableServiceProperties);
            }
            bindMethodMap.put( refName, methods );
        }
    }

    public ComponentMethod getActivateMethod()
    {
        return m_activateMethod;
    }

    public ComponentMethod getDeactivateMethod()
    {
        return m_deactivateMethod;
    }

    public ComponentMethod getModifiedMethod()
    {
        return m_modifiedMethod;
    }

    public ReferenceMethods getBindMethods(String refName )
    {
        return bindMethodMap.get( refName );
    }

}
