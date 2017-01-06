/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.integration;

import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;

@RunWith(JUnit4TestRunner.class)
public class Felix5276Test extends ComponentTestBase
{
    static
    {
        descriptorFile = "/integration_test_FELIX_5276.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".felix5276";
        DS_LOGLEVEL = "debug";
    }

    @Test
    public void test_servicePropsCauseDeactivation() throws Exception
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        Configuration configC = configure("C", null, props);
        log.log(LogService.LOG_INFO, "configC updated with empty props");
        delay();

        String componentNameA = "A";
        ComponentConfigurationDTO componentA = findComponentConfigurationByName(componentNameA,
            ComponentConfigurationDTO.ACTIVE);
        log.log(LogService.LOG_INFO, "A checked active");

        String componentNameB = "B";
        findComponentConfigurationByName(componentNameB, ComponentConfigurationDTO.ACTIVE);
        log.log(LogService.LOG_INFO, "B checked active");
        String componentNameC = "C";
        findComponentConfigurationByName(componentNameC, ComponentConfigurationDTO.ACTIVE);
        log.log(LogService.LOG_INFO, "C checked active");

        props.put("b.target", "(foo=bar)");
        configC.update(props);
        log.log(LogService.LOG_INFO, "configC updated with target filter");
        delay();

        findComponentConfigurationByName(componentNameC, ComponentConfigurationDTO.ACTIVE);
        log.log(LogService.LOG_INFO, "C checked active");

        disableAndCheck(componentA);

        log.log(LogService.LOG_INFO, "A disabled");
        findComponentConfigurationByName(componentNameC, ComponentConfigurationDTO.ACTIVE);
        log.log(LogService.LOG_INFO, "C checked active");
        findComponentConfigurationByName(componentNameB, ComponentConfigurationDTO.SATISFIED);
        log.log(LogService.LOG_INFO, "B checked satisfied");

        assertTrue("Expected no errors or warnings: " + log.foundWarnings(), log.foundWarnings().isEmpty());
    }
}
