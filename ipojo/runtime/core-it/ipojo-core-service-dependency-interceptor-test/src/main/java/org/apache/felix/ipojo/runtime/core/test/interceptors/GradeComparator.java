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

package org.apache.felix.ipojo.runtime.core.test.interceptors;

import org.osgi.framework.ServiceReference;

import java.util.Comparator;

public class GradeComparator implements Comparator<ServiceReference> {
    @Override
    public int compare(ServiceReference ref1, ServiceReference ref2) {
        Integer grade0;
        Integer grade1;

        grade0 = (Integer) ref1.getProperty("grade");
        grade1 = (Integer) ref2.getProperty("grade");

        return grade1.compareTo(grade0); // Best grade first.
    }
}
