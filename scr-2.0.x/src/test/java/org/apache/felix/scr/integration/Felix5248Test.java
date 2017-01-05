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

import java.util.Hashtable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.log.LogService;

@RunWith(JUnit4TestRunner.class)
public class Felix5248Test extends ComponentTestBase
{
    static
    {
        descriptorFile = "/integration_test_FELIX_5248.xml";
        COMPONENT_PACKAGE = COMPONENT_PACKAGE + ".felix5248";
        DS_LOGLEVEL = "debug";
    }

    @Test
    public void test_reconfigurationActivates() throws Exception
    {
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put( "FAIL", true );
        Configuration configC = configure( "Component", null, props );
        log.log( LogService.LOG_INFO, "Component updated with FAIL props" );
        delay();

        String componentName = "Component";
        ComponentConfigurationDTO component = findComponentConfigurationByName( componentName,
            ComponentConfigurationDTO.SATISFIED );
        log.log( LogService.LOG_INFO, "A checked satisfied (not active)" );

        props.clear();
        configC.update( props );
        log.log( LogService.LOG_INFO, "Component updated with no props" );
        delay();

        findComponentConfigurationByName( componentName, ComponentConfigurationDTO.ACTIVE );
        log.log( LogService.LOG_INFO, "C checked active" );

    }
}
