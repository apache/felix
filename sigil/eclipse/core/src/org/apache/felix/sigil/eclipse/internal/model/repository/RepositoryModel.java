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

package org.apache.felix.sigil.eclipse.internal.model.repository;

import java.util.Properties;

import org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;

public class RepositoryModel implements IRepositoryModel
{
    public static final String NAME = "name";

    private String id;

    private IRepositoryType type;

    private Properties properties = new Properties();
    
    private Throwable throwable;

    public RepositoryModel(String id, IRepositoryType type)
    {
        this.id = id;
        this.type = type;
    }

    /**
     * @param exception 
     * @param id2
     * @param type2
     * @param properties2
     */
    public RepositoryModel(String id, IRepositoryType type, Properties properties, Exception exception)
    {
        this.id = id;
        this.type = type;
        this.properties = properties;
        this.throwable = exception;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public IRepositoryType getType()
    {
        return type;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        if ( getType().isDynamic() ) {
            String name = properties.getProperty(NAME);
            if ( name == null ) {
                name = id;
            }
            return name;
        }
        else {
            return getType().getName();
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        try
        {
            RepositoryModel e = (RepositoryModel) obj;
            return id.equals(e.id);
        }
        catch (ClassCastException e)
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return id.hashCode();
    }

    public String toString()
    {
        return type.getId() + ":" + id + ":" + getName();
    }

    /* (non-Javadoc)
     * @see org.apache.felix.sigil.eclipse.model.repository.IRepositoryModel#getProblem()
     */
    public Throwable getProblem()
    {
        return throwable;
    }

}