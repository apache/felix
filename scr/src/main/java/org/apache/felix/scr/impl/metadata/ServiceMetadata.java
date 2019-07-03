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
package org.apache.felix.scr.impl.metadata;

import static org.apache.felix.scr.impl.metadata.MetadataStoreHelper.addString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataReader;
import org.apache.felix.scr.impl.metadata.MetadataStoreHelper.MetaDataWriter;

/**
 * This class contains the metadata associated to a service that is provided
 * by a component
 *
 */
public class ServiceMetadata {

    public enum Scope { singleton, bundle, prototype}

    // 112.4.6 Flag that indicates if the service is a ServiceFactory
    private Boolean m_serviceFactory;

    private String m_scopeName;
    private Scope m_scope = Scope.singleton;

    // List of provided interfaces
    private List<String> m_provides = new ArrayList<String>();

    // Flag that indicates if this metadata has been validated and has become immutable
    private boolean m_validated = false;

    /**
     * Setter for the servicefactory attribute of the service element
     *
     * @param serviceFactory
     */
    public void setServiceFactory(boolean serviceFactory) {
        if (m_validated) {
            return;
        }

        m_serviceFactory = serviceFactory;
    }

    public void setScope(String scopeName) {
        if(m_validated) {
            return;
        }
        this.m_scopeName = scopeName;
    }



    public Scope getScope() {
        return m_scope;
    }

    /**
     * Add a provided interface to this service
     *
     * @param provide a String containing the name of the provided interface
     */
    public void addProvide(String provide) {
        if(m_validated) {
            return;
        }

        m_provides.add(provide);
    }

    /**
     * Returns the implemented interfaces
     *
     * @return the implemented interfaces as a string array
     */
    public String [] getProvides() {
        return m_provides.toArray( new String[m_provides.size()] );
    }

    /**
     * Verify if the semantics of this metadata are correct
     *
     */
    void validate( ComponentMetadata componentMetadata )
    {
        if ( m_provides.size() == 0 )
        {
            throw componentMetadata
            .validationFailure( "At least one provided interface must be declared in the service element" );
        }
        for ( String provide: m_provides )
        {
            if ( provide == null )
            {
                throw componentMetadata
                .validationFailure( "Null provides.  Possibly service is not specified as value of attribute 'interface'" );
            }
        }
        if (m_serviceFactory != null)
        {
            if ( componentMetadata.getDSVersion().isDS13() )
            {
                throw componentMetadata.validationFailure("service-factory can only be specified in version 1.2 and earlier");
            }
            m_scope = m_serviceFactory? Scope.bundle: Scope.singleton;
        }
        if ( m_scopeName != null )
        {
            if ( !componentMetadata.getDSVersion().isDS13() )
            {
                throw componentMetadata.validationFailure("service scope can only be specified in version 1.3 and later");
            }
            try
            {
                m_scope = Scope.valueOf(m_scopeName);
            }
            catch (IllegalArgumentException e)
            {
                throw componentMetadata.validationFailure("Service scope may be only 'singleton' 'bundle' or 'prototype' not " + m_scopeName);
            }
        }
        m_validated = true;
    }

    void collectStrings(Set<String> strings)
    {
        for (String s : m_provides)
        {
            addString(s, strings);
        }
        addString(m_scopeName, strings);
        addString(m_scope.toString(), strings);
    }

    void store(DataOutputStream out, MetaDataWriter metaDataWriter) throws IOException
    {
        out.writeInt(m_provides.size());
        for (String s : m_provides)
        {
            metaDataWriter.writeString(s, out);
        }
        metaDataWriter.writeString(m_scopeName, out);
        metaDataWriter.writeString(m_scope.toString(), out);
        out.writeBoolean(m_serviceFactory != null);
        if (m_serviceFactory != null)
        {
            out.writeBoolean(m_serviceFactory.booleanValue());
        }
    }

    static ServiceMetadata load(DataInputStream in, MetaDataReader metaDataReader)
        throws IOException
    {
        ServiceMetadata result = new ServiceMetadata();
        int providerSize = in.readInt();
        for (int i = 0; i < providerSize; i++)
        {
            result.addProvide(metaDataReader.readString(in));
        }
        result.m_scopeName = metaDataReader.readString(in);
        result.m_scope = Scope.valueOf(metaDataReader.readString(in));
        if (in.readBoolean())
        {
            result.m_serviceFactory = in.readBoolean();
        }
        // only stored valid metadata
        result.m_validated = true;
        return result;
    }
}
