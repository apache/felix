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
package org.apache.felix.metatype.internal;


import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeProvider;


public class ServiceMetaTypeInformation extends MetaTypeInformationImpl
{

    public ServiceMetaTypeInformation( Bundle bundle )
    {
        super( bundle );
    }


    protected void addService( String[] pids, boolean isSingleton, boolean isFactory, MetaTypeProvider mtp )
    {
        if ( pids != null )
        {
            if ( isSingleton )
            {
                addSingletonMetaTypeProvider( pids, mtp );
            }

            if ( isFactory )
            {
                addFactoryMetaTypeProvider( pids, mtp );
            }
        }
    }


    protected void removeService( String[] pids, boolean isSingleton, boolean isFactory )
    {
        if ( pids != null )
        {
            if ( isSingleton )
            {
                removeSingletonMetaTypeProvider( pids );
            }

            if ( isFactory )
            {
                removeFactoryMetaTypeProvider( pids );
            }
        }
    }
}
