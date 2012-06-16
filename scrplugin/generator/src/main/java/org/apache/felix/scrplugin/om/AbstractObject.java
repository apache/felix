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

import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
import org.apache.felix.scrplugin.helper.IssueLog;

/**
 * The <code>AbstractObject</code> is the base class for the all classes of the scr om.
 */
public abstract class AbstractObject {

    private final String annotationPrefix;

    private final String sourceLocation;

    protected AbstractObject(final ScannedAnnotation annotation, final String sourceLocation) {
        if ( annotation == null ) {
            this.annotationPrefix = "";
        } else {
            this.annotationPrefix = "@" + annotation.getSimpleName()  + " : ";
        }
        this.sourceLocation = sourceLocation;
    }

    public void logWarn(IssueLog iLog, String message) {
        iLog.addWarning(this.annotationPrefix + message, sourceLocation);
    }

    public void logError(IssueLog iLog, String message) {
        iLog.addError(this.annotationPrefix + message, sourceLocation);
    }
}
