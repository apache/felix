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
import org.apache.felix.scrplugin.helper.StringUtils;

/**
 * This is the processor for the Apache Felix Sling annotations.
 */
public class SlingAnnotationProcessor implements AnnotationProcessor {

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getName()
     */
    @Override
    public String getName() {
        return "Apache Sling Annotation Processor";
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    @Override
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
    @Override
    public int getRanking() {
        return 500;
    }

    /**
     * Process SlingServlet
     */
    private void processSlingServlet(final ClassAnnotation cad, final ClassDescription classDescription) {
        // generate ComponentDescription if required
        final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
        final boolean metatype = cad.getBooleanValue("metatype", !generateComponent);

        if (generateComponent) {
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("name", classDescription.getDescribedClass().getName()));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);

            cd.setLabel(cad.getStringValue("label", null));
            cd.setDescription(cad.getStringValue("description", null));

            cd.setCreateMetatype(metatype);

            cd.setCreatePid(false); // always set to false

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
        generateStringPropertyDescriptor(cad, classDescription, metatype, "paths", "sling.servlet.paths");

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES}
        generateStringPropertyDescriptor(cad, classDescription, metatype, "resourceTypes",
                "sling.servlet.resourceTypes");

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_SELECTORS}
        generateStringPropertyDescriptor(cad, classDescription, metatype, "selectors", "sling.servlet.selectors");

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_EXTENSIONS}
        generateStringPropertyDescriptor(cad, classDescription, metatype, "extensions", "sling.servlet.extensions");

        // {@see org.apache.sling.servlets.resolver.internal.ServletResolverConstants.SLING_SERVLET_METHODS}
        generateStringPropertyDescriptor(cad, classDescription, metatype, "methods", "sling.servlet.methods");
    }

    /**
     * Generates a property descriptor of type {@link PropertyType#String}
     */
    private void generateStringPropertyDescriptor(final ClassAnnotation cad, final ClassDescription classDescription,
            final boolean metatype, String annotationName, String propertyDescriptorName) {

        final String[] values = (String[]) cad.getValue(annotationName);
        if (values == null) {
            return;
        }

        final PropertyDescription pd = new PropertyDescription(cad);
        pd.setName(propertyDescriptorName);
        pd.setMultiValue(values);
        pd.setType(PropertyType.String);
        if (metatype) {
            pd.setPrivate(true);
        }
        classDescription.add(pd);
    }

    /**
     * Process SlingFilter
     */
    private void processSlingFilter(final ClassAnnotation cad, final ClassDescription classDescription) {
        // generate ComponentDescription if required
        final boolean generateComponent = cad.getBooleanValue("generateComponent", true);
        final boolean metatype = cad.getBooleanValue("metatype", !generateComponent);

        if (generateComponent) {
            final ComponentDescription cd = new ComponentDescription(cad);
            cd.setName(cad.getStringValue("name", classDescription.getDescribedClass().getName()));
            cd.setConfigurationPolicy(ComponentConfigurationPolicy.OPTIONAL);

            cd.setLabel(cad.getStringValue("label", null));
            cd.setDescription(cad.getStringValue("description", null));

            cd.setCreateMetatype(metatype);

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
        if (metatype) {
            pd.setPrivate(true);
        }
        classDescription.add(pd);

        // property pattern = sling.filter.pattern
        final String pattern = cad.getStringValue("pattern", "");
        if(!StringUtils.isEmpty(pattern)) {
            final PropertyDescription pdPattern = new PropertyDescription(cad);
            pdPattern.setName("sling.filter.pattern");
            pdPattern.setValue(pattern);
            pdPattern.setType(PropertyType.String);
            if (metatype) {
            	pdPattern.setPrivate(true);
            }
            classDescription.add(pdPattern);
        }

        // property scope
        final String[] scopes;
        final Object val = cad.getValue("scope");
        if ( val != null ) {
            if ( val instanceof String[] ) {
                final String[] arr = (String[])val;
                scopes = new String[arr.length / 2];
                int i = 0;
                int index = 0;
                while ( i < arr.length) {
                    scopes[index] = arr[i];
                    i+=2;
                    index++;
                }
            } else if ( val instanceof String[][] ) {
                final String[][] arr = (String[][])val;
                scopes = new String[arr.length];
                int index = 0;
                while ( index < arr.length) {
                    scopes[index] = arr[index][1];
                    index++;
                }
            } else {
                scopes = new String[] { val.toString()};
            }
        } else {
            scopes = new String[] {SlingFilterScope.REQUEST.getScope()};
        }

        final PropertyDescription pd2 = new PropertyDescription(cad);
        pd2.setName("sling.filter.scope");
        if ( scopes.length == 1 ) {
            pd2.setValue(scopes[0]);
        } else {
            pd2.setMultiValue(scopes);
        }
        pd2.setType(PropertyType.String);
        if (metatype) {
            pd2.setPrivate(true);
        }
        classDescription.add(pd2);
    }
}
