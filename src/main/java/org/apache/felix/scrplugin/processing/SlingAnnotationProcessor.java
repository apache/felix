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
package org.apache.felix.scrplugin.processing;

import java.util.List;

import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ClassAnnotation;
import org.apache.felix.scrplugin.annotations.ScannedClass;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.ServiceDescription;

/**
 * This is the processor for the Apache Felix Sling annotations.
 */
public class SlingAnnotationProcessor implements AnnotationProcessor {

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getName()
     */
    public String getName() {
        return "Apache Sling Annotation Processor";
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    public void process(final ScannedClass scannedClass,
                        final ClassDescription describedClass)
    throws SCRDescriptorFailureException, SCRDescriptorException {

        final List<ClassAnnotation> servlets = scannedClass.getClassAnnotations(SlingServlet.class.getName());
        scannedClass.processed(servlets);

        for(final ClassAnnotation cad : servlets) {
            processSlingServlet(cad, describedClass);
        }

        final List<ClassAnnotation> filters = scannedClass.getClassAnnotations(SlingFilter.class.getName());
        scannedClass.processed(filters);

        for(final ClassAnnotation cad : filters) {
            processSlingFilter(cad, describedClass);
        }

    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getRanking()
     */
    public int getRanking() {
        return 500;
    }

    /**
     * Process SlingServlet
     */
    private void processSlingServlet(final ClassAnnotation cad, final ClassDescription classDescription) {
        // generate ComponentDescription if required
        final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
        if (generateComponent) {
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("name", classDescription.getDescribedClass().getName()));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);

            cd.setLabel(cad.getStringValue("label", null));
            cd.setDescription(cad.getStringValue("description", null));

            cd.setCreateMetatype(cad.getBooleanValue("metatype", false));

            classDescription.add(cd);
        }

        // generate ServiceDescription if required
        final boolean generateService = cad.getBooleanValue("generateService", true);
        if (generateService) {
            final ServiceDescription sd = new ServiceDescription(cad);
            sd.addInterface("javax.servlet.Servlet");
            classDescription.add(sd);
        }

        // generate PropertyDescriptions
        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_PATHS}
        final String[] paths = (String[])cad.getValue("paths");
        if ( paths != null ) {
            final PropertyDescription pd = new PropertyDescription(cad);
            pd.setName("sling.servlet.paths");
            pd.setMultiValue(paths);
            pd.setType(PropertyType.String);
            pd.setPrivate(true);
            classDescription.add(pd);
        }

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES}
        final String[] resourceTypes = (String[])cad.getValue("resourceTypes");
        if ( resourceTypes != null ) {
            final PropertyDescription pd = new PropertyDescription(cad);
            pd.setName("sling.servlet.resourceTypes");
            pd.setMultiValue(resourceTypes);
            pd.setType(PropertyType.String);
            pd.setPrivate(true);
            classDescription.add(pd);
        }

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS}
        final String[] selectors = (String[])cad.getValue("selectors");
        if (selectors != null ) {
            final PropertyDescription pd = new PropertyDescription(cad);
            pd.setName("sling.servlet.selectors");
            pd.setMultiValue(selectors);
            pd.setType(PropertyType.String);
            pd.setPrivate(true);
            classDescription.add(pd);
        }

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_EXTENSIONS}
        final String[] extensions = (String[])cad.getValue("extensions");
        if (extensions != null ) {
            final PropertyDescription pd = new PropertyDescription(cad);
            pd.setName("sling.servlet.extensions");
            pd.setMultiValue(extensions);
            pd.setType(PropertyType.String);
            pd.setPrivate(true);
            classDescription.add(pd);
        }

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_METHODS}
        final String[] methods = (String[])cad.getValue("methods");
        if (methods != null ) {
            final PropertyDescription pd = new PropertyDescription(cad);
            pd.setName("sling.servlet.methods");
            pd.setMultiValue(methods);
            pd.setType(PropertyType.String);
            pd.setPrivate(true);
            classDescription.add(pd);
        }
    }

    /**
     * Process SlingFilter
     */
    private void processSlingFilter(final ClassAnnotation cad, final ClassDescription classDescription) {
        // generate ComponentDescription if required
        final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
        if (generateComponent) {
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("name", classDescription.getDescribedClass().getName()));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);

            cd.setLabel(cad.getStringValue("label", null));
            cd.setDescription(cad.getStringValue("description", null));

            cd.setCreateMetatype(cad.getBooleanValue("metatype", false));

            classDescription.add(cd);
        }

        // generate ServiceDescription if required
        final boolean generateService = cad.getBooleanValue("generateService", true);
        if (generateService) {
            final ServiceDescription sd = new ServiceDescription(cad);
            sd.addInterface("javax.servlet.Filter");
            classDescription.add(sd);
        }

        // generate PropertyDescriptions
        // property order = service.ranking
        final int order = cad.getIntegerValue("order", 0);
        final PropertyDescription pd = new PropertyDescription(cad);
        pd.setName("service.ranking");
        pd.setValue(String.valueOf(order));
        pd.setType(PropertyType.Integer);
        pd.setPrivate(true);
        classDescription.add(pd);

        // property scope
        final String scope = cad.getEnumValue("scope", SlingFilterScope.REQUEST.getScope());
        final PropertyDescription pd2 = new PropertyDescription(cad);
        pd2.setName("sling.filter.scope");
        pd2.setValue(scope);
        pd2.setType(PropertyType.String);
        pd2.setPrivate(true);
        classDescription.add(pd2);
    }
}
