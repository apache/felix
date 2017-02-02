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
package org.apache.felix.scr.impl.xml;

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.scr.impl.metadata.DSVersion;

/**
 * Constants for the component XML
 */
public abstract class XmlConstants
{
    // Empty Namespace URI maps to DS 1.0
    public static final String NAMESPACE_URI_EMPTY = "";

    // Namespace URI of DS 1.0
    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    // Namespace URI of DS 1.1
    public static final String NAMESPACE_URI_1_1 = "http://www.osgi.org/xmlns/scr/v1.1.0";

    // Namespace URI of DS 1.1-felix (see FELIX-1893)
    public static final String NAMESPACE_URI_1_1_FELIX = "http://felix.apache.org/xmlns/scr/v1.1.0-felix";

    // Namespace URI of DS 1.2
    public static final String NAMESPACE_URI_1_2 = "http://www.osgi.org/xmlns/scr/v1.2.0";

    // Namespace URI of DS 1.2-felix (see FELIX-3377)
    public static final String NAMESPACE_URI_1_2_FELIX = "http://felix.apache.org/xmlns/scr/v1.2.0-felix";

    // Namespace URI of DS 1.3
    public static final String NAMESPACE_URI_1_3 = "http://www.osgi.org/xmlns/scr/v1.3.0";

    // Namespace URI of Felix DS extensions 1.0
    public static final String NAMESPACE_URI_1_0_FELIX_EXTENSIONS = "http://felix.apache.org/xmlns/scr/extensions/v1.0.0";

    // Namespace URI of DS 1.4
    public static final String NAMESPACE_URI_1_4 = "http://www.osgi.org/xmlns/scr/v1.4.0";

    // Elements
    public static final String EL_COMPONENT = "component";
    public static final String EL_COMPONENTS = "components";
    public static final String EL_FACTORY_PROPERTY = "factory-property";
    public static final String EL_FACTORY_PROPERTIES = "factory-properties";
    public static final String EL_IMPL = "implementation";
    public static final String EL_PROPERTY = "property";
    public static final String EL_PROPERTIES = "properties";
    public static final String EL_PROVIDE = "provide";
    public static final String EL_REF = "reference";
    public static final String EL_SERVICE = "service";

    // Attributes
    public static final String ATTR_ACTIVATE = "activate";
    public static final String ATTR_ACTIVATION_FIELDS = "activation-fields";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_CONFIG_PID = "configuration-pid";
    public static final String ATTR_CONFIG_POLICY = "configuration-policy";
    public static final String ATTR_DEACTIVATE = "deactivate";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_ENTRY = "entry";
    public static final String ATTR_FACTORY = "factory";
    public static final String ATTR_IMMEDIATE = "immediate";
    public static final String ATTR_INTERFACE = "interface";
    public static final String ATTR_MODIFIED = "modified";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_VALUE = "value";

    // Extension features
    public static final String ATTR_CONFIGURABLE_SERVICE_PROPERTIES = "configurableServiceProperties";

    public static final String ATTR_PERSISTENT_FACTORY_COMPONENT = "persistentFactoryComponent";

    public static final String ATTR_DELETE_CALLS_MODIFY = "deleteCallsModify";

    public static final String ATTR_OBSOLETE_FACTORY_COMPONENT_FACTORY = "obsoleteFactoryComponentFactory";

    public static final String ATTR_CONFIGURE_WITH_INTERFACES = "configureWithInterfaces";

    public static final String ATTR_DELAYED_KEEP_INSTANCES = "delayedKeepInstances";

    // mapping of namespace URI to namespace code
    public static final Map<String, DSVersion> NAMESPACE_CODE_MAP;


    static
    {
        NAMESPACE_CODE_MAP = new HashMap<String, DSVersion>();
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_EMPTY, DSVersion.DS10 );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI, DSVersion.DS10 );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_1, DSVersion.DS11 );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_1_FELIX, DSVersion.DS11Felix );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_2, DSVersion.DS12 );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_2_FELIX, DSVersion.DS12Felix );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_3, DSVersion.DS13 );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_4, DSVersion.DS14 );
    }
}
