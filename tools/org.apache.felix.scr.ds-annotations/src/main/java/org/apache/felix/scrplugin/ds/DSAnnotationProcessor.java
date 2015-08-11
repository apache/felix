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
package org.apache.felix.scrplugin.ds;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ClassAnnotation;
import org.apache.felix.scrplugin.annotations.MethodAnnotation;
import org.apache.felix.scrplugin.annotations.ScannedClass;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.PropertyUnbounded;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferencePolicyOption;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.description.ServiceDescription;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * This is the processor for the DS annotations.
 */
public class DSAnnotationProcessor implements AnnotationProcessor {

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getName()
     */
    public String getName() {
        return "DS Annotation Processor";
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    public void process(final ScannedClass scannedClass,
                        final ClassDescription describedClass)
    throws SCRDescriptorFailureException, SCRDescriptorException {
        final List<ClassAnnotation> componentTags = scannedClass.getClassAnnotations(Component.class.getName());
        scannedClass.processed(componentTags);

        for (final ClassAnnotation cad : componentTags) {
            this.createComponent(cad, describedClass, scannedClass);
        }

        // search for the component descriptions and use the first one
        final List<ComponentDescription> componentDescs = describedClass.getDescriptions(ComponentDescription.class);
        ComponentDescription found = null;
        if (!componentDescs.isEmpty()) {
            found = componentDescs.get(0);
        }

        if (found != null) {
            final ComponentDescription cd = found;

            // search for methods
            final List<MethodAnnotation> methodTags = scannedClass.getMethodAnnotations(null);
            for (final MethodAnnotation m : methodTags) {
                if (m.getName().equals(Activate.class.getName())) {
                    cd.setActivate(m.getAnnotatedMethod().getName());
                    scannedClass.processed(m);
                } else if (m.getName().equals(Deactivate.class.getName())) {
                    cd.setDeactivate(m.getAnnotatedMethod().getName());
                    scannedClass.processed(m);
                } else if (m.getName().equals(Modified.class.getName())) {
                    cd.setModified(m.getAnnotatedMethod().getName());
                    scannedClass.processed(m);
                } else if (m.getName().equals(Reference.class.getName()) ) {
                    this.processReference(describedClass, m);
                    scannedClass.processed(m);
                }
            }

        }
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getRanking()
     */
    public int getRanking() {
        return 300;
    }

    /**
     * Create a component description.
     *
     * @param cad The component annotation for the class.
     * @param scannedClass The scanned class.
     */
    private ComponentDescription createComponent(final ClassAnnotation cad,
                    final ClassDescription describedClass,
                    final ScannedClass scannedClass)
    throws SCRDescriptorException {
        final ComponentDescription component = new ComponentDescription(cad);
        describedClass.add(component);

        // Although not defined in the spec, we support abstract classes.
        final boolean classIsAbstract = Modifier.isAbstract(scannedClass.getClass().getModifiers());
        component.setAbstract(classIsAbstract);

        // name
        component.setName(cad.getStringValue("name", scannedClass.getScannedClass().getName()));

        // services
        final List<String> listedInterfaces = new ArrayList<String>();
        if (cad.hasValue("service") ) {
            final String[] interfaces = (String[]) cad.getValue("service");
            if ( interfaces != null ) {
                for (final String t : interfaces) {
                    listedInterfaces.add(t);
                }
            }
        } else {
            // scan directly implemented interfaces
            this.searchInterfaces(listedInterfaces, scannedClass.getScannedClass());
        }
        if ( listedInterfaces.size() > 0 ) {
            final ServiceDescription serviceDesc = new ServiceDescription(cad);
            describedClass.add(serviceDesc);

            for(final String name : listedInterfaces) {
                serviceDesc.addInterface(name);
            }
            serviceDesc.setServiceFactory(cad.getBooleanValue("servicefactory", false));
        }

        // factory
        component.setFactory(cad.getStringValue("factory", null));

        // enabled
        if (cad.getValue("enabled") != null) {
            component.setEnabled(cad.getBooleanValue("enabled", true));
        }

        // immediate
        if (cad.getValue("immediate") != null) {
            component.setImmediate(cad.getBooleanValue("immediate", false));
        }

        // property
        final String[] property = (String[])cad.getValue("property");
        if ( property != null ) {
            // TODO - what do we do if the value is invalid?
            for(final String propDef : property) {
                final int pos = propDef.indexOf('=');
                if ( pos != -1 ) {
                    final String prefix = propDef.substring(0, pos);
                    final String value = propDef.substring(pos + 1);
                    final int typeSep = prefix.indexOf(':');
                    final String key = (typeSep == -1 ? prefix : prefix.substring(0, typeSep));
                    final String type = (typeSep == -1 ? PropertyType.String.name() : prefix.substring(typeSep + 1));

                    final PropertyType propType = PropertyType.valueOf(type);
                    // FELIX-4159 : check if this is a multi value prop
                    final List<PropertyDescription> existingProps = describedClass.getDescriptions(PropertyDescription.class);
                    PropertyDescription found = null;
                    for(final PropertyDescription current : existingProps) {
                        if ( current.getName().equals(key) ) {
                            found = current;
                            break;
                        }
                    }
                    if ( found == null ) {
                        final PropertyDescription pd = new PropertyDescription(cad);
                        describedClass.add(pd);
                        pd.setName(key);
                        pd.setValue(value);
                        pd.setType(propType);
                        pd.setUnbounded(PropertyUnbounded.DEFAULT);
                    } else {
                        if ( propType != found.getType() ) {
                            throw new SCRDescriptorException("Multi value property '" + key + "' has different types: "
                                    + found.getType() + " & " + propType,
                                    describedClass.getSource());
                        }
                        if ( found.getValue() != null ) {
                            final String[] values = new String[2];
                            values[0] = found.getValue();
                            values[1] = value;
                            found.setMultiValue(values);
                        } else {
                            final String[] oldValues = found.getMultiValue();
                            final String[] newValues = new String[oldValues.length + 1];
                            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                            newValues[oldValues.length] = value;
                            found.setMultiValue(newValues);
                        }
                    }
                }
            }
        }
        // TODO: properties

        // xmlns
        if (cad.getValue("xmlns") != null) {
            final SpecVersion spec = SpecVersion.fromNamespaceUrl(cad.getValue("xmlns").toString());
            if ( spec == null ) {
                throw new SCRDescriptorException("Unknown xmlns attribute value: " + cad.getValue("xmlns"),
                                describedClass.getSource());
            }
            component.setSpecVersion(spec);
        }

        // configuration policy
        component.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(cad.getEnumValue("configurationPolicy",
                        ComponentConfigurationPolicy.OPTIONAL.name())));

        // configuration pid
        component.setConfigurationPid(cad.getStringValue("configurationPid", null));
        component.setCreatePid(false);

        // no inheritance
        component.setInherit(false);

        return component;
    }

    /**
     * Get all directly implemented interfaces
     */
    private void searchInterfaces(final List<String> interfaceList, final Class<?> javaClass) {
        final Class<?>[] interfaces = javaClass.getInterfaces();
        for (final Class<?> i : interfaces) {
            interfaceList.add(i.getName());
        }
    }

    /**
     * Process a reference
     */
    private void processReference(final ClassDescription describedClass, final MethodAnnotation ad) {
        final ReferenceDescription ref = new ReferenceDescription(ad);
        describedClass.add(ref);

        ref.setStrategy(ReferenceStrategy.EVENT);

        final String methodName = ad.getAnnotatedMethod().getName();
        final String defaultUnbindMethodName;
        final String refNameByMethod;
        if ( methodName.startsWith("add") ) {
            refNameByMethod = methodName.substring(3);
            defaultUnbindMethodName = "remove" + refNameByMethod;
        } else if ( methodName.startsWith("set") ) {
            refNameByMethod = methodName.substring(3);
            defaultUnbindMethodName = "unset" + refNameByMethod;
        } else if ( methodName.startsWith("bind") ) {
            refNameByMethod = methodName.substring(4);
            defaultUnbindMethodName = "unbind" + refNameByMethod;
        } else {
            refNameByMethod = methodName;
            defaultUnbindMethodName = "un" + refNameByMethod;
        }
        final String defaultUpdateMethodName = "updated" + refNameByMethod;

        // bind method
        ref.setBind(ad.getAnnotatedMethod().getName());
        // unbind method
        final String unbind = ad.getStringValue("unbind",
                        this.hasMethod(describedClass, defaultUnbindMethodName) ? defaultUnbindMethodName : "-");
        if ( !unbind.equals("-") ) {
            ref.setUnbind(unbind);
        }
        // update method
        final String updated = ad.getStringValue("updated",
                        this.hasMethod(describedClass, defaultUpdateMethodName) ? defaultUpdateMethodName : "-");
        if ( !updated.equals("-") ) {
            ref.setUpdated(updated);
        }

        // name
        ref.setName(ad.getStringValue("name", refNameByMethod));
        // service
        final String serviceName = ad.getStringValue("service", null);
        if ( serviceName != null ) {
            ref.setInterfaceName(serviceName);
        } else {
            final Class<?>[] params = ad.getAnnotatedMethod().getParameterTypes();
            if ( params != null && params.length > 0 ) {
                ref.setInterfaceName(params[0].getName());
            }
        }
        // cardinality
        final String cardinality = ad.getEnumValue("cardinality", "MANDATORY");
        if ( cardinality.equals("OPTIONAL") ) {
            ref.setCardinality(ReferenceCardinality.OPTIONAL_UNARY);
        } else if ( cardinality.equals("MULTIPLE") ) {
            ref.setCardinality(ReferenceCardinality.OPTIONAL_MULTIPLE);
        } else if ( cardinality.equals("AT_LEAST_ONE") ) {
            ref.setCardinality(ReferenceCardinality.MANDATORY_MULTIPLE);
        } else {
            ref.setCardinality(ReferenceCardinality.MANDATORY_UNARY);
        }

        // policy
        ref.setPolicy(ReferencePolicy.valueOf(ad.getEnumValue("policy", ReferencePolicy.STATIC.name())));
        // policy option
        ref.setPolicyOption(ReferencePolicyOption.valueOf(ad.getEnumValue("policyOption", ReferencePolicyOption.RELUCTANT.name())));
        // target
        ref.setTarget(ad.getStringValue("target", null));
    }

    private boolean hasMethod(final ClassDescription classDescription, final String name) {
        final Method[] allMethods = classDescription.getDescribedClass().getDeclaredMethods();
        for(final Method m : allMethods) {
            if ( m.getName().equals(name) ) {
                return true;
            }
        }
        return false;
    }
}
