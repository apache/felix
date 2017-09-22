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
package org.apache.felix.webconsole.plugins.ds.internal;

import org.osgi.framework.Bundle;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

public class MetatypeSupport
{

    public boolean check(final Object obj, final Bundle providingBundle, final String pid)
    {
        final MetaTypeService mts = (MetaTypeService)obj;
        final MetaTypeInformation mti = mts.getMetaTypeInformation(providingBundle);
        if (mti != null)
        {
            try {
                return mti.getObjectClassDefinition(pid, null) != null;
            } catch (final IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
}