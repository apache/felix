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

package org.apache.felix.ipojo.runtime.core.test.components.comparator;

import org.osgi.framework.ServiceReference;

import java.util.Comparator;

public class GradeComparator implements Comparator {

    public int compare(Object arg0, Object arg1) {
        ServiceReference ref0 = null;
        ServiceReference ref1 = null;
        Integer grade0 = null;
        Integer grade1 = null;
        if (arg0 instanceof ServiceReference) {
            ref0 = (ServiceReference)  arg0;
            grade0 = (Integer) ref0.getProperty("grade");
        }
        if (arg1 instanceof ServiceReference) {
            ref1 = (ServiceReference) arg1;
            grade1 = (Integer) ref1.getProperty("grade");
        }
        
        if (ref0 != null && ref1 != null
                && grade0 != null && grade1 != null) {
            return grade1.compareTo(grade0); // Best grade first.
        } else {
            return 0; // Equals
        }
    }

}
