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
package de.kalpatec.pojosr.framework.felix.framework.capabilityset;

import java.util.List;

public interface Capability
{
    static final String MODULE_NAMESPACE = "module";
    static final String HOST_NAMESPACE = "host";
    static final String PACKAGE_NAMESPACE = "package";
    static final String SINGLETON_NAMESPACE = "singleton";

    public static final String PACKAGE_ATTR = "package";
    public static final String VERSION_ATTR = "version";

    String getNamespace();

    Directive getDirective(String name);

    List<Directive> getDirectives();

    Attribute getAttribute(String name);

    List<Attribute> getAttributes();

    List<String> getUses();
}