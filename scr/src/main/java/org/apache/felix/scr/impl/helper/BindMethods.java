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


import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * @version $Rev:$ $Date:$
 */
public class BindMethods
{
    private final BindMethod m_bind;
    private final UpdatedMethod m_updated;
    private final UnbindMethod m_unbind;

    BindMethods( SimpleLogger logger, ReferenceMetadata m_dependencyMetadata, Class instanceClass,
            final boolean isDS11, final boolean isDS12Felix )
    {
        m_bind = new BindMethod( logger,
                m_dependencyMetadata.getBind(),
                instanceClass,
                m_dependencyMetadata.getInterface(),
                isDS11, isDS12Felix
        );
        m_updated = new UpdatedMethod( logger,
                m_dependencyMetadata.getUpdated(),
                instanceClass,
                m_dependencyMetadata.getName(),
                m_dependencyMetadata.getInterface(),
                isDS11, isDS12Felix
        );
        m_unbind = new UnbindMethod( logger,
                m_dependencyMetadata.getUnbind(),
                instanceClass,
                m_dependencyMetadata.getName(),
                m_dependencyMetadata.getInterface(),
                isDS11, isDS12Felix
        );
    }

    public BindMethod getBind()
    {
        return m_bind;
    }

    public UnbindMethod getUnbind()
    {
        return m_unbind;
    }

    public UpdatedMethod getUpdated()
    {
        return m_updated;
    }
}
