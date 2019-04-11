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
package org.apache.felix.hc.generalchecks.scrutil;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.Result.Status;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Minimal bridge to root cause in order to allow making that dependency optional. */
@Component(service = DsRootCauseAnalyzer.class, immediate = true)
public class DsRootCauseAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(DsRootCauseAnalyzer.class);

    private DsRootCauseAdapter dsRootCauseAdapter;

    @Reference(policyOption = ReferencePolicyOption.GREEDY)
    private ServiceComponentRuntime scr;

    @Activate
    public void activate() throws InterruptedException {

        String rootCauseClassName = "org.apache.felix.rootcause.DSRootCause";
        try {
            Class<?> rootCauseClass = Class.forName(rootCauseClassName);
            LOG.debug("Class {} could be loaded", rootCauseClass);
            dsRootCauseAdapter = new DsRootCauseAdapter(scr);
        } catch (ClassNotFoundException e) {
            LOG.debug("Class {} could NOT be loaded", rootCauseClassName, e);
            dsRootCauseAdapter = null;
        }
    }

    public void logMissingService(FormattingResultLog log, String missingServiceName, Status status) {
        if (dsRootCauseAdapter != null) {
            dsRootCauseAdapter.logMissingService(log, missingServiceName, status);
        }
    }

    public void logNotEnabledComponent(FormattingResultLog log, ComponentDescriptionDTO desc, Status status) {
        if (dsRootCauseAdapter != null) {
            dsRootCauseAdapter.logNotEnabledComponent(log, desc, status);
        }
    }
}
