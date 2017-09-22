/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.ds.internal;

import java.util.Comparator;

import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

class Util
{

    static final Comparator<ComponentConfigurationDTO> COMPONENT_COMPARATOR = new Comparator<ComponentConfigurationDTO>()
    {
        public int compare(ComponentConfigurationDTO c0, ComponentConfigurationDTO c1)
        {
            final int nameCmp = c0.description.name.compareTo(c1.description.name);
            if (nameCmp != 0)
            {
                return nameCmp;
            }
            return (c0.id < c1.id) ? -1 : ((c0.id > c1.id) ? 1 : 0);
        }
    };

    private Util()
    {
        // prevent instantiation
    }

}
