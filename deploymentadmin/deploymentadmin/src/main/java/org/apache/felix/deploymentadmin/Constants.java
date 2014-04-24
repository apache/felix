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
package org.apache.felix.deploymentadmin;

import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public interface Constants extends org.osgi.framework.Constants {

    // manifest main attribute header constants
    String DEPLOYMENTPACKAGE_SYMBOLICMAME = "DeploymentPackage-SymbolicName";
    String DEPLOYMENTPACKAGE_VERSION = "DeploymentPackage-Version";
    String DEPLOYMENTPACKAGE_FIXPACK = "DeploymentPackage-FixPack";
    String DEPLOYMENTPACKAGE_NAME = "DeploymentPackage-Name";
    String DEPLOYMENTPACKAGE_ICON = "DeploymentPackage-Icon";

    // manifest 'name' section header constants
    String RESOURCE_PROCESSOR = "Resource-Processor";
    String DEPLOYMENTPACKAGE_MISSING = "DeploymentPackage-Missing";
    String DEPLOYMENTPACKAGE_CUSTOMIZER = "DeploymentPackage-Customizer";

    // event topics and properties
    String EVENTTOPIC_INSTALL = "org/osgi/service/deployment/INSTALL";
    String EVENTTOPIC_UNINSTALL = "org/osgi/service/deployment/UNINSTALL";
    String EVENTTOPIC_COMPLETE = "org/osgi/service/deployment/COMPLETE";

    String EVENTPROPERTY_DEPLOYMENTPACKAGE_NAME = DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NAME;
    String EVENTPROPERTY_DEPLOYMENTPACKAGE_READABLENAME = DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_READABLENAME;
    String EVENTPROPERTY_DEPLOYMENTPACKAGE_CURRENTVERSION = DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_CURRENTVERSION;
    String EVENTPROPERTY_DEPLOYMENTPACKAGE_NEXTVERSION = DeploymentPackage.EVENT_DEPLOYMENTPACKAGE_NEXTVERSION;
    String EVENTPROPERTY_SUCCESSFUL = "successful";

    // miscellaneous constants
    String BUNDLE_LOCATION_PREFIX = "osgi-dp:";

    // inlined constants for convenience & readability
    int CODE_CANCELLED = DeploymentException.CODE_CANCELLED;
    int CODE_NOT_A_JAR = DeploymentException.CODE_NOT_A_JAR;
    int CODE_ORDER_ERROR = DeploymentException.CODE_ORDER_ERROR;
    int CODE_MISSING_HEADER = DeploymentException.CODE_MISSING_HEADER;
    int CODE_BAD_HEADER = DeploymentException.CODE_BAD_HEADER;
    int CODE_MISSING_FIXPACK_TARGET = DeploymentException.CODE_MISSING_FIXPACK_TARGET;
    int CODE_MISSING_BUNDLE = DeploymentException.CODE_MISSING_BUNDLE;
    int CODE_MISSING_RESOURCE = DeploymentException.CODE_MISSING_RESOURCE;
    int CODE_SIGNING_ERROR = DeploymentException.CODE_SIGNING_ERROR;
    int CODE_BUNDLE_NAME_ERROR = DeploymentException.CODE_BUNDLE_NAME_ERROR;
    int CODE_FOREIGN_CUSTOMIZER = DeploymentException.CODE_FOREIGN_CUSTOMIZER;
    int CODE_BUNDLE_SHARING_VIOLATION = DeploymentException.CODE_BUNDLE_SHARING_VIOLATION;
    int CODE_RESOURCE_SHARING_VIOLATION = DeploymentException.CODE_RESOURCE_SHARING_VIOLATION;
    int CODE_COMMIT_ERROR = DeploymentException.CODE_COMMIT_ERROR;
    int CODE_OTHER_ERROR = DeploymentException.CODE_OTHER_ERROR;
    int CODE_PROCESSOR_NOT_FOUND = DeploymentException.CODE_PROCESSOR_NOT_FOUND;
    int CODE_TIMEOUT = DeploymentException.CODE_TIMEOUT;

    String BUNDLE_SYMBOLICNAME = org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
    String BUNDLE_VERSION = org.osgi.framework.Constants.BUNDLE_VERSION;
    String SERVICE_PID = org.osgi.framework.Constants.SERVICE_PID;

}