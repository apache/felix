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


package org.apache.felix.scr.impl.helper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;


/**
 * @version $Rev:$ $Date:$
 */
public class ComponentMethods
{
    private ActivateMethod m_activateMethod;
    private ModifiedMethod m_modifiedMethod;
    private DeactivateMethod m_deactivateMethod;

    private final Map bindMethodMap = new HashMap();//<String, BindMethods>

    public synchronized void initComponentMethods( ComponentMetadata componentMetadata, Class implementationObjectClass )
    {
        if (m_activateMethod != null)
        {
            return;
        }
        boolean isDS11 = componentMetadata.isDS11();
        boolean isDS12Felix = componentMetadata.isDS12Felix();
        m_activateMethod = new ActivateMethod( componentMetadata.getActivate(), componentMetadata
                .isActivateDeclared(), implementationObjectClass, isDS11, isDS12Felix );
        m_deactivateMethod = new DeactivateMethod( componentMetadata.getDeactivate(),
                componentMetadata.isDeactivateDeclared(), implementationObjectClass, isDS11, isDS12Felix );

        m_modifiedMethod = new ModifiedMethod( componentMetadata.getModified(), implementationObjectClass, isDS11, isDS12Felix );

        for ( ReferenceMetadata referenceMetadata: componentMetadata.getDependencies() )
        {
            String refName = referenceMetadata.getName();
            BindMethods bindMethods = new BindMethods( referenceMetadata, implementationObjectClass, isDS11, isDS12Felix);
            bindMethodMap.put( refName, bindMethods );
        }
    }

    public ActivateMethod getActivateMethod()
    {
        return m_activateMethod;
    }

    public DeactivateMethod getDeactivateMethod()
    {
        return m_deactivateMethod;
    }

    public ModifiedMethod getModifiedMethod()
    {
        return m_modifiedMethod;
    }

    public BindMethods getBindMethods(String refName )
    {
        return ( BindMethods ) bindMethodMap.get( refName );
    }

}
