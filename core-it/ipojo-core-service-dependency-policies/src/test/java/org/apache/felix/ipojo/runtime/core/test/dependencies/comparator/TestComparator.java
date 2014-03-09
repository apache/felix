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

package org.apache.felix.ipojo.runtime.core.test.dependencies.comparator;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestComparator extends Common {

    String gradeFactory = "COMPARATOR-gradedFooProvider";
    String dynamic = "COMPARATOR-DynamicCheckService";
    String dynamicpriority = "COMPARATOR-DynamicPriorityCheckService";


    ComponentInstance dynInstance;
    ComponentInstance dpInstance;

    @Before
    public void setUp() {
        dynInstance = ipojoHelper.createComponentInstance(dynamic, (Properties) null);
        dpInstance = ipojoHelper.createComponentInstance(dynamicpriority, (Properties) null);
    }

    @After
    public void tearDown() {
        ipojoHelper.dispose();
    }

    @Test
    public void testDynamic() {
        createGrade(1);
        ComponentInstance grade2 = createGrade(2);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), dynInstance.getInstanceName());
        assertNotNull("CS availability", ref);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref);
        Properties result = cs.getProps();
        int fsGrade = (Integer) result.get("fs");
        int fs2Grade = (Integer) result.get("fs2");
        int[] fssGrades = (int[]) result.get("fss");

        // We should have been injected with the highest one.
        assertEquals("fs grade -1", 2, fsGrade);
        assertEquals("fs2 grade -1", 2, fs2Grade);
        assertEquals("fss grade size -1", 2, fssGrades.length);


        assertEquals("fss grade[0] -1", 2, fssGrades[0]);
        assertEquals("fss grade[1] -1", 1, fssGrades[1]);

        createGrade(3);
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");

        assertEquals("fs grade -2", 2, fsGrade);
        assertEquals("fs2 grade -2", 2, fs2Grade);
        assertEquals("fss grade size -2", 3, fssGrades.length);
        assertEquals("fss grade[0] -2", 2, fssGrades[0]);
        assertEquals("fss grade[1] -2", 1, fssGrades[1]);
        assertEquals("fss grade[2] -2", 3, fssGrades[2]);

        grade2.stop();

        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");

        assertEquals("fs grade -3", 3, fsGrade);
        assertEquals("fs2 grade -3", 3, fs2Grade);
        assertEquals("fss grade size -3", 2, fssGrades.length);
        assertEquals("fss grade[0] -3", 1, fssGrades[0]);
        assertEquals("fss grade[1] -3", 3, fssGrades[1]);
    }

    @Test
    public void testDynamicPriority() {
        createGrade(1);
        ComponentInstance grade2 = createGrade(2);

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), dpInstance.getInstanceName());
        assertNotNull("CS availability", ref);

        CheckService cs = (CheckService) osgiHelper.getRawServiceObject(ref);
        Properties result = cs.getProps();
        int fsGrade = (Integer) result.get("fs");
        int fs2Grade = (Integer) result.get("fs2");
        int[] fssGrades = (int[]) result.get("fss");

        assertEquals("fs grade -1", 2, fsGrade);
        assertEquals("fs2 grade -1", 2, fs2Grade);
        assertEquals("fss grade size -1", 2, fssGrades.length);
        assertEquals("fss grade[0] -1", 2, fssGrades[0]);
        assertEquals("fss grade[1] -1", 1, fssGrades[1]);

        createGrade(3);
        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");

        assertEquals("fs grade -2", 3, fsGrade);
        assertEquals("fs2 grade -2", 3, fs2Grade);
        assertEquals("fss grade size -2", 3, fssGrades.length);
        assertEquals("fss grade[0] -2", 3, fssGrades[0]);
        assertEquals("fss grade[1] -2", 2, fssGrades[1]);
        assertEquals("fss grade[2] -2", 1, fssGrades[2]);

        grade2.stop();

        result = cs.getProps();
        fsGrade = ((Integer) result.get("fs")).intValue();
        fs2Grade = ((Integer) result.get("fs2")).intValue();
        fssGrades = (int[]) result.get("fss");

        assertEquals("fs grade -3", 3, fsGrade);
        assertEquals("fs2 grade -3", 3, fs2Grade);
        assertEquals("fss grade size -3", 2, fssGrades.length);
        assertEquals("fss grade[0] -3", 3, fssGrades[0]);
        assertEquals("fss grade[1] -3", 1, fssGrades[1]);
    }

    private ComponentInstance createGrade(int grade) {
        Properties props = new Properties();
        props.put("grade", grade);
        return ipojoHelper.createComponentInstance(gradeFactory, props);
    }

}
