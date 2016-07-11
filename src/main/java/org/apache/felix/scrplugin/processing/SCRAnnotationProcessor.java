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

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.AutoDetect;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Services;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.annotations.ClassAnnotation;
import org.apache.felix.scrplugin.annotations.FieldAnnotation;
import org.apache.felix.scrplugin.annotations.MethodAnnotation;
import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
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

/**
 * This is the processor for the Apache Felix SCR annotations.
 */
public class SCRAnnotationProcessor implements AnnotationProcessor {

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getName()
     */
    @Override
    public String getName() {
        return "Apache Felix SCR Annotation Processor";
    }

    /**
     * @throws SCRDescriptorException
     * @throws SCRDescriptorFailureException
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#process(org.apache.felix.scrplugin.annotations.ScannedClass, org.apache.felix.scrplugin.description.ClassDescription)
     */
    @Override
    public void process(final ScannedClass scannedClass, final ClassDescription describedClass)
                    throws SCRDescriptorFailureException, SCRDescriptorException {

        final List<ClassAnnotation> componentTags = scannedClass.getClassAnnotations(Component.class.getName());
        scannedClass.processed(componentTags);

        for (final ClassAnnotation cad : componentTags) {
            describedClass.add(createComponent(cad, scannedClass));
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
                }
            }

        }

        // service tags
        final List<ClassAnnotation> allServiceTags = new ArrayList<ClassAnnotation>();
        final List<ClassAnnotation> serviceTags = scannedClass.getClassAnnotations(Service.class.getName());
        if (serviceTags.size() > 0) {
            scannedClass.processed(serviceTags);
            allServiceTags.addAll(serviceTags);
        }
        // services tags - class level
        final List<ClassAnnotation> servicesTags = scannedClass.getClassAnnotations(Services.class.getName());
        if (servicesTags.size() > 0) {
            scannedClass.processed(servicesTags);
            for (final ClassAnnotation cad : servicesTags) {
                final ClassAnnotation[] values = (ClassAnnotation[]) cad.getValue("value");
                if (values != null) {
                    allServiceTags.addAll(Arrays.asList(values));
                }
            }
        }
        if (allServiceTags.size() > 0) {
            describedClass.add(createService(allServiceTags, scannedClass));
        }

        // references - class level
        final List<ClassAnnotation> referencesClassTags = scannedClass.getClassAnnotations(References.class.getName());
        scannedClass.processed(referencesClassTags);
        for (final ClassAnnotation cad : referencesClassTags) {
            final ClassAnnotation[] values = (ClassAnnotation[]) cad.getValue("value");
            if (values != null) {
                createReferences(Arrays.asList(values), describedClass);
            }
        }

        // reference - class level
        final List<ClassAnnotation> refClassTags = scannedClass.getClassAnnotations(Reference.class.getName());
        scannedClass.processed(refClassTags);
        createReferences(refClassTags, describedClass);

        // reference - field level
        final List<FieldAnnotation> refFieldTags = scannedClass.getFieldAnnotations(Reference.class.getName());
        scannedClass.processed(refFieldTags);
        createReferences(refFieldTags, describedClass);

        // properties - class level
        final List<ClassAnnotation> propsClassTags = scannedClass.getClassAnnotations(Properties.class.getName());
        scannedClass.processed(propsClassTags);
        for (final ClassAnnotation cad : propsClassTags) {
            final ClassAnnotation[] values = (ClassAnnotation[]) cad.getValue("value");
            if (values != null) {
                createProperties(Arrays.asList(values), describedClass);
            }
        }

        // property - class level
        final List<ClassAnnotation> propClassTags = scannedClass.getClassAnnotations(Property.class.getName());
        scannedClass.processed(propClassTags);
        createProperties(propClassTags, describedClass);

        // property - field level
        final List<FieldAnnotation> propFieldTags = scannedClass.getFieldAnnotations(Property.class.getName());
        scannedClass.processed(propFieldTags);
        createProperties(propFieldTags, describedClass);
    }

    /**
     * @see org.apache.felix.scrplugin.annotations.AnnotationProcessor#getRanking()
     */
    @Override
    public int getRanking() {
        return 1000;
    }

    /**
     * Create a component description.
     *
     * @param cad
     *            The component annotation for the class.
     * @param scannedClass
     *            The scanned class.
     */
    private ComponentDescription createComponent(final ClassAnnotation cad, final ScannedClass scannedClass) {
        final ComponentDescription component = new ComponentDescription(cad);
        final boolean classIsAbstract = Modifier.isAbstract(scannedClass.getScannedClass().getModifiers());
        component.setAbstract(cad.getBooleanValue("componentAbstract", classIsAbstract));

        component.setCreatePid(false); // always set to false

        component.setName(cad.getStringValue("name", scannedClass.getScannedClass().getName()));

        component.setLabel(cad.getStringValue("label", null));
        component.setDescription(cad.getStringValue("description", null));

        component.setCreateDs(cad.getBooleanValue("ds", true));

        component.setCreateMetatype(cad.getBooleanValue("metatype", false));

        if (cad.getValue("enabled") != null) {
            component.setEnabled(cad.getBooleanValue("enabled", true));
        }
        if (cad.getValue("specVersion") != null) {
            component.setSpecVersion(SpecVersion.fromName(cad.getValue("specVersion").toString()));
        }
        component.setFactory(cad.getStringValue("factory", null));
        // FELIX-593: immediate attribute does not default to true all the
        // times hence we only set it if declared in the tag
        if (cad.getValue("immediate") != null) {
            component.setImmediate(cad.getBooleanValue("immediate", false));
        }
        component.setInherit(cad.getBooleanValue("inherit", true));
        component.setConfigurationPolicy(ComponentConfigurationPolicy.valueOf(cad.getEnumValue("policy",
                        ComponentConfigurationPolicy.OPTIONAL.name())));
        component.setSetMetatypeFactoryPid(cad.getBooleanValue("configurationFactory", false));

        // Version 1.2
        component.setConfigurationPid(cad.getStringValue("configurationPid", null));

        return component;
    }

    /**
     * Create a service description
     *
     * @param descs
     *            The service annotations.
     * @param scannedClass
     *            The scanned class
     */
    private ServiceDescription createService(final List<ClassAnnotation> descs, final ScannedClass scannedClass) {
        final ServiceDescription service = new ServiceDescription(descs.get(0));

        final List<String> listedInterfaces = new ArrayList<String>();
        for (final ClassAnnotation d : descs) {
            if (d.getBooleanValue("serviceFactory", false)) {
                service.setServiceFactory(true);
            }
            if (d.getValue("value") != null) {
                final String[] interfaces = (String[]) d.getValue("value");
                for (String t : interfaces) {
                    listedInterfaces.add(t);
                }
            }
        }

        if (listedInterfaces.size() > 0 && !listedInterfaces.contains(AutoDetect.class.getName())) {
            for (final String i : listedInterfaces) {
                service.addInterface(i);
            }
        } else {
            // auto detection
            addInterfaces(service, scannedClass.getScannedClass());
        }

        return service;
    }

    /**
     * Recursively add interfaces to the service.
     */
    private void addInterfaces(final ServiceDescription service, final Class<?> javaClass) {
        if (javaClass != null) {
            final Class<?>[] interfaces = javaClass.getInterfaces();
            for (final Class<?> i : interfaces) {
                service.addInterface(i.getName());
                // recursivly add interfaces implemented by this interface
                this.addInterfaces(service, i);
            }

            // try super class
            this.addInterfaces(service, javaClass.getSuperclass());
        }
    }

    /**
     * Create reference descriptions
     *
     * @param descs
     *            List of reference annotations.s
     * @param describedClass
     *            The described class.
     */
    private void createReferences(final List<? extends ScannedAnnotation> descs, final ClassDescription describedClass) {
        for (final ScannedAnnotation ad : descs) {
            final ReferenceDescription ref = new ReferenceDescription(ad);

            // check for field annotation
            final FieldAnnotation fieldAnnotation;
            if (ad instanceof FieldAnnotation) {
                fieldAnnotation = (FieldAnnotation) ad;
                ref.setField(fieldAnnotation.getAnnotatedField());
            } else {
                fieldAnnotation = null;
            }

            ref.setName(ad.getStringValue("name",
                            (fieldAnnotation != null ? fieldAnnotation.getAnnotatedField().getName() : null)));
            String defaultInterfaceName = null;
            if ( fieldAnnotation != null ) {
                if ( fieldAnnotation.getAnnotatedField().getType().isArray() ) {
                    defaultInterfaceName = fieldAnnotation.getAnnotatedField().getType().getComponentType().getName();
                } else {
                    defaultInterfaceName = fieldAnnotation.getAnnotatedField().getType().getName();
                }
            }
            ref.setInterfaceName(ad.getStringValue("referenceInterface", defaultInterfaceName));
            ref.setTarget(ad.getStringValue("target", null));
            ref.setCardinality(ReferenceCardinality.valueOf(ad.getEnumValue("cardinality",
                            ReferenceCardinality.MANDATORY_UNARY.name())));
            ref.setPolicy(ReferencePolicy.valueOf(ad.getEnumValue("policy", ReferencePolicy.STATIC.name())));
            ref.setPolicyOption(ReferencePolicyOption.valueOf(ad.getEnumValue("policyOption", ReferencePolicyOption.RELUCTANT.name())));
            ref.setStrategy(ReferenceStrategy.valueOf(ad.getEnumValue("strategy", ReferenceStrategy.EVENT.name())));

            ref.setBind(ad.getStringValue("bind", null));
            ref.setUnbind(ad.getStringValue("unbind", null));
            ref.setUpdated(ad.getStringValue("updated", null));

            describedClass.add(ref);
        }
    }

    private static final String[] PROPERTY_VALUE_PROCESSING = new String[] { "String", "value", "String", "classValue", "Long",
            "longValue", "Double", "doubleValue", "Float", "floatValue", "Integer", "intValue", "Byte", "byteValue", "Char",
            "charValue", "Boolean", "boolValue", "Short", "shortValue", "Password", "passwordValue" };

    /**
     * Create properties descriptions
     *
     * @throws SCRDescriptorException
     * @throws SCRDescriptorFailureException
     */
    private void createProperties(final List<? extends ScannedAnnotation> descs, final ClassDescription describedClass)
                    throws SCRDescriptorFailureException, SCRDescriptorException {
        for (final ScannedAnnotation ad : descs) {
            final PropertyDescription prop = new PropertyDescription(ad);

            // check for field annotation
            final FieldAnnotation fieldAnnotation;
            if (ad instanceof FieldAnnotation) {
                fieldAnnotation = (FieldAnnotation) ad;
            } else {
                fieldAnnotation = null;
            }

            // Detect values from annotation
            String type = null;
            String[] values = null;
            int index = 0;
            while (type == null && index < PROPERTY_VALUE_PROCESSING.length) {
                final String propType = PROPERTY_VALUE_PROCESSING[index];
                final String propName = PROPERTY_VALUE_PROCESSING[index + 1];
                final Object propValue = ad.getValue(propName);
                if (propValue != null && propValue.getClass().isArray()) {
                    type = propType;
                    values = new String[Array.getLength(propValue)];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = Array.get(propValue, i).toString();
                    }
                }
                index += 2;
            }

            String name = ad.getStringValue("name", null);

            if (values != null) {
                prop.setType(PropertyType.valueOf(type));
                if (values.length == 1) {
                    prop.setValue(values[0]);
                } else {
                    prop.setMultiValue(values);
                }
                if ( name == null ) {
                    final Object value = fieldAnnotation.getAnnotatedFieldValue();
                    if (value != null) {
                        name = value.toString();
                    }
                }

            } else if (fieldAnnotation != null) {
                // Detect values from field
                if ( name != null ) {
                    final Object value = fieldAnnotation.getAnnotatedFieldValue();
                    if (value != null) {
                        if (value.getClass().isArray()) {
                            final String[] newValues = new String[Array.getLength(value)];
                            for (int i = 0; i < newValues.length; i++) {
                                newValues[i] = Array.get(value, i).toString();
                            }
                            prop.setMultiValue(newValues);
                            prop.setType(PropertyType.from(fieldAnnotation.getAnnotatedField().getType().getComponentType()));
                        } else {
                            prop.setType(PropertyType.from(value.getClass()));
                            prop.setValue(value.toString());
                        }
                    }
                } else {
                    if ( Modifier.isStatic(fieldAnnotation.getAnnotatedField().getModifiers()) ) {
                        final Object value = fieldAnnotation.getAnnotatedFieldValue();
                        if (value != null) {
                            name = value.toString();
                        }
                    } else {
                        // non static, no name, no value (FELIX-4393)
                        name = fieldAnnotation.getAnnotatedField().getName();
                        final Object value = fieldAnnotation.getAnnotatedFieldValue();
                        if (value != null) {
                            if (value.getClass().isArray()) {
                                final String[] newValues = new String[Array.getLength(value)];
                                for (int i = 0; i < newValues.length; i++) {
                                    newValues[i] = Array.get(value, i).toString();
                                }
                                prop.setMultiValue(newValues);
                                prop.setType(PropertyType.from(fieldAnnotation.getAnnotatedField().getType().getComponentType()));
                            } else {
                                prop.setType(PropertyType.from(value.getClass()));
                                prop.setValue(value.toString());
                            }
                        }

                    }
                }
            }

            prop.setName(name);
            prop.setLabel(ad.getStringValue("label", null));
            prop.setDescription(ad.getStringValue("description", null));

            // check type
            if ( prop.getType() == null ) {
                prop.setType(PropertyType.String);
            }

            // private
            if ( ad.getValue("propertyPrivate") != null ) {
                prop.setPrivate(ad.getBooleanValue("propertyPrivate", false));
            }

            // cardinality handling
            final PropertyUnbounded pu = PropertyUnbounded
                            .valueOf(ad.getEnumValue("unbounded", PropertyUnbounded.DEFAULT.name()));
            prop.setUnbounded(pu);

            if (pu == PropertyUnbounded.DEFAULT) {
                prop.setCardinality(ad.getIntegerValue("cardinality", 0));
                if (prop.getMultiValue() != null && prop.getCardinality() == 0) {
                    prop.setUnbounded(PropertyUnbounded.ARRAY);
                }
            } else {
                prop.setCardinality(0);
            }

            if ( prop.getValue() != null ) {
                if ( prop.getUnbounded() == PropertyUnbounded.ARRAY || prop.getUnbounded() == PropertyUnbounded.VECTOR ) {
                    prop.setMultiValue(new String[] {prop.getValue()});
                } else if ( prop.getCardinality() < -1 || prop.getCardinality() > 1 ) {
                    prop.setMultiValue(new String[] {prop.getValue()});
                }
            }
            // options
            final ScannedAnnotation[] options = (ScannedAnnotation[])ad.getValue("options");
            if (options != null) {
                final List<String> propertyOptions = new ArrayList<String>();
                for(final ScannedAnnotation po : options) {
                    propertyOptions.add(po.getStringValue("name", ""));
                    propertyOptions.add(po.getStringValue("value", ""));
                }
                prop.setOptions(propertyOptions.toArray(new String[propertyOptions.size()]));
            }

            describedClass.add(prop);
        }
    }
}
