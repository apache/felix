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
package org.apache.felix.scrplugin.om;

import org.apache.felix.scrplugin.Options;
import org.apache.felix.scrplugin.Project;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.helper.IssueLog;

public class Context {

    private Project project;

    private IssueLog issueLog;

    private SpecVersion specVersion;

    private ClassDescription classDescription;

    private Options options;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public IssueLog getIssueLog() {
        return issueLog;
    }

    public void setIssueLog(IssueLog issueLog) {
        this.issueLog = issueLog;
    }

    public SpecVersion getSpecVersion() {
        return specVersion;
    }

    public void setSpecVersion(SpecVersion specVersion) {
        this.specVersion = specVersion;
    }

    public ClassDescription getClassDescription() {
        return classDescription;
    }

    public void setClassDescription(ClassDescription classDescription) {
        this.classDescription = classDescription;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }
}
