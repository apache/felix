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
package org.apache.felix.framework.util;

public interface FelixConstants extends org.osgi.framework.Constants
{
    String SYSTEM_BUNDLE_SYMBOLICNAME = "org.apache.felix.framework";
    // Framework constants and values.
    String FRAMEWORK_VERSION_VALUE = "1.8";
    String FRAMEWORK_VENDOR_VALUE = "Apache Software Foundation";

    // Framework constants and values.
    String FELIX_VERSION_PROPERTY = "felix.version";

    // Miscellaneous manifest constants.
    String DIRECTIVE_SEPARATOR = ":=";
    String ATTRIBUTE_SEPARATOR = "=";
    String CLASS_PATH_SEPARATOR = ",";
    String CLASS_PATH_DOT = ".";
    String PACKAGE_SEPARATOR = ";";
    String VERSION_SEGMENT_SEPARATOR = ".";
    int VERSION_SEGMENT_COUNT = 3;
    String BUNDLE_NATIVECODE_OPTIONAL = "*";

    // Miscellaneous OSGi constants.
    String BUNDLE_URL_PROTOCOL = "bundle";

    // Miscellaneous framework configuration property names.
    String FRAMEWORK_BUNDLECACHE_IMPL = "felix.bundlecache.impl";
    String LOG_LEVEL_PROP = "felix.log.level";
    String LOG_LOGGER_PROP = "felix.log.logger";
    String SYSTEMBUNDLE_ACTIVATORS_PROP = "felix.systembundle.activators";
    String BUNDLE_STARTLEVEL_PROP = "felix.startlevel.bundle";
    String SERVICE_URLHANDLERS_PROP = "felix.service.urlhandlers";
    String IMPLICIT_BOOT_DELEGATION_PROP = "felix.bootdelegation.implicit";
    String BOOT_CLASSLOADERS_PROP = "felix.bootdelegation.classloaders";
    String USE_LOCALURLS_PROP = "felix.jarurls";
    String NATIVE_OS_NAME_ALIAS_PREFIX = "felix.native.osname.alias";
    String NATIVE_PROC_NAME_ALIAS_PREFIX = "felix.native.processor.alias";
    String USE_CACHEDURLS_PROPS = "felix.bundlecodesource.usecachedurls";

    // Missing OSGi constant for resolution directive.
    String RESOLUTION_DYNAMIC = "dynamic";

    // Start level-related constants.
    int FRAMEWORK_INACTIVE_STARTLEVEL = 0;
    int FRAMEWORK_DEFAULT_STARTLEVEL = 1;
    int SYSTEMBUNDLE_DEFAULT_STARTLEVEL = 0;
    int BUNDLE_DEFAULT_STARTLEVEL = 1;

    // Miscellaneous properties values.
    String FAKE_URL_PROTOCOL_VALUE = "location:";
    String FELIX_EXTENSION_ACTIVATOR = "Felix-Activator";
    String SECURITY_DEFAULT_POLICY = "felix.security.defaultpolicy";
    String FELIX_EXTENSIONS_DISABLE = "felix.extensions.disable";
}
