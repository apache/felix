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
package org.apache.felix.bundlerepository.impl;

import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.RequirementBuilder;

public class RequirementBuilderImpl implements RequirementBuilder {

    private class RequirementCollector {
        final String namespace;
        Resource resource;
        final Map<String, Object> attributesMap = new HashMap<>();
        final Map<String, String> directivesMap = new HashMap<>();

        public RequirementCollector(String pNamespace) {
            namespace = pNamespace;
        }
    }

    private final RequirementCollector requirementCollector;

    public RequirementBuilderImpl(String pNamespace) {
        requirementCollector = new RequirementCollector(pNamespace);
    }

    @Override
    public RequirementBuilder addAttribute(String pName, Object pValue) {
        this.requirementCollector.attributesMap.put(pName, pValue);
        return this;
    }

    @Override
    public RequirementBuilder addDirective(String pName, String pValue) {
        this.requirementCollector.directivesMap.put(pName, pValue);
        return this;
    }

    @Override
    public Requirement build() {
        return new org.apache.felix.utils.resource.RequirementImpl(
                requirementCollector.resource, requirementCollector.namespace,
                requirementCollector.directivesMap,
                requirementCollector.attributesMap);
    }

    @Override
    public IdentityExpression buildExpression() {
        return new IdentityExpressionImpl(build());
    }

    @Override
    public RequirementBuilder setAttributes(Map<String, Object> pAttributes) {
        this.requirementCollector.attributesMap.clear();
        this.requirementCollector.attributesMap.putAll(pAttributes);
        return this;
    }

    @Override
    public RequirementBuilder setDirectives(Map<String, String> pDirectives) {
        this.requirementCollector.directivesMap.clear();
        this.requirementCollector.directivesMap.putAll(pDirectives);
        return this;
    }

    @Override
    public RequirementBuilder setResource(Resource pResource) {
        this.requirementCollector.resource = pResource;
        return this;
    }

}
