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

import java.util.ArrayList;
import java.util.List;

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
		if(m_validated) {
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
}
