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


import org.apache.felix.scr.impl.metadata.DSVersion;
import org.apache.felix.scr.impl.metadata.ReferenceMetadata;

/**
 * @version $Rev$ $Date$
 */
public class FieldMethods implements ReferenceMethods
{
    private final FieldHandler handler;

    public FieldMethods( final ReferenceMetadata m_dependencyMetadata,
            final Class<?> instanceClass,
            final DSVersion dsVersion,
            final boolean configurableServiceProperties )
    {
        handler = new FieldHandler(
                m_dependencyMetadata,
                instanceClass
        );
    }

    public ReferenceMethod getBind()
    {
        return handler.getBind();
    }

    public ReferenceMethod getUnbind()
    {
        return handler.getUnbind();
    }

    public ReferenceMethod getUpdated()
    {
        return handler.getUpdated();
    }
}
