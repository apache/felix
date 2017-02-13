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
package org.apache.felix.webconsole.plugins.packageadmin.internal;

import java.util.Comparator;

import org.osgi.service.packageadmin.ExportedPackage;

final class ExportedPackageComparator implements Comparator<ExportedPackage>
{

    public int compare(ExportedPackage o1, ExportedPackage o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        int name = o1.getName().compareTo(o2.getName());
        if (name != 0)
        {
            return name;
        }

        int version = o1.getVersion().compareTo(o2.getVersion());
        if (version != 0)
        {
            return version;
        }

        final long o1bid = o1.getExportingBundle().getBundleId();
        final long o2bid = o2.getExportingBundle().getBundleId();
        return (o1bid < o2bid ? -1 : (o1bid == o2bid ? 0 : 1));
    }

}