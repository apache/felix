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
package org.apache.felix.scrplugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.scrplugin.annotations.AnnotationProcessor;
import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentConfigurationPolicy;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.PropertyType;
import org.apache.felix.scrplugin.description.PropertyUnbounded;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ReferencePolicyOption;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.description.ServiceDescription;
import org.apache.felix.scrplugin.helper.AnnotationProcessorManager;
import org.apache.felix.scrplugin.helper.ClassModifier;
import org.apache.felix.scrplugin.helper.ClassScanner;
import org.apache.felix.scrplugin.helper.ComponentContainer;
import org.apache.felix.scrplugin.helper.DescriptionContainer;
import org.apache.felix.scrplugin.helper.IssueLog;
import org.apache.felix.scrplugin.helper.MetatypeAttributeDefinition;
import org.apache.felix.scrplugin.helper.MetatypeContainer;
import org.apache.felix.scrplugin.helper.StringUtils;
import org.apache.felix.scrplugin.helper.Validator;
import org.apache.felix.scrplugin.xml.ComponentDescriptorIO;
import org.apache.felix.scrplugin.xml.MetaTypeIO;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The <code>SCRDescriptorGenerator</code> class does the hard work of
 * generating the SCR descriptors. This class is being instantiated and
 * configured by clients and the {@link #execute()} method called to generate
 * the descriptor files.
 * <p>
 * When using this class carefully consider calling <i>all</i> setter methods to properly configure the generator. All setter
 * method document, which default value is assumed for the respective property if the setter is not called.
 * <p>
 * Instances of this class are not thread save and should not be reused.
 */
public class SCRDescriptorGenerator {

    private final Log logger;

    /** The project. */
    private Project project;

    /** The options. */
    private Options options = new Options();

    /** The annotation scanner. */
    private ClassScanner scanner;

    /** The issue log. */
    private IssueLog iLog;

    /**
     * Create an instance of this generator using the given {@link Log} instance
     * of logging.
     */
    public SCRDescriptorGenerator(final Log logger) {
        this.logger = logger;
    }

    /**
     * Set the project. This is required.
     */
    public void setProject(final Project p) {
        this.project = p;
    }

    /**
     * Set the options.
     */
    public void setOptions(final Options p) {
        this.options = p;
    }

    /**
     * Actually generates the Declarative Services and Metatype descriptors
     * scanning the java sources provided by the {@link #setProject(Project)}
     *
     * @return A list of generated file names, relative to the output directory
     *
     * @throws SCRDescriptorException
     * @throws SCRDescriptorFailureException
     */
    public Result execute() throws SCRDescriptorException, SCRDescriptorFailureException {

        this.logger.debug("Starting SCR Descriptor Generator....");
        if (this.project == null) {
            throw new SCRDescriptorFailureException("Project has not been set!");
        }
        if (this.options == null) {
            // use default options
            this.options = new Options();
        }
        if (this.options.getOutputDirectory() == null) {
            throw new SCRDescriptorFailureException("Output directory has not been set!");
        }

        this.logger.debug("..using output directory: " + this.options.getOutputDirectory());
        this.logger.debug("..strict mode: " + this.options.isStrictMode());
        this.logger.debug("..generating accessors: " + this.options.isGenerateAccessors());

        // check speck version configuration
        SpecVersion specVersion = options.getSpecVersion();
        if (specVersion == null) {
            this.logger.debug("..auto detecting spec version");
        } else {
            this.logger.debug("..using spec version " + specVersion.getName());
        }

        // create a log
        this.iLog = new IssueLog(this.options.isStrictMode());

        // create the annotation processor manager
        final AnnotationProcessor aProcessor = new AnnotationProcessorManager(this.logger,
                        this.project.getClassLoader());

        // create the class scanner - and start scanning
        this.scanner = new ClassScanner(logger, iLog, project, aProcessor);
        final List<ClassDescription> scannedDescriptions = scanner.scanSources();

        // create the result to hold the list of processed source files
        final Result result = new Result();
        final List<ComponentContainer> processedContainers = new ArrayList<ComponentContainer>();
        for (final ClassDescription desc : scannedDescriptions) {
            this.logger.debug("Processing component class " + desc.getSource());
            result.addProcessedSourceFile(desc.getSource());

            // check if there is more than one component definition
            if (desc.getDescriptions(ComponentDescription.class).size() > 1) {
                iLog.addError("Class has more than one component definition." +
                             " Check the annotations and merge the definitions to a single definition.",
                                desc.getSource());
            } else {
                final ComponentContainer container = this.createComponent(desc, iLog);

                if (container.getComponentDescription().getSpecVersion() != null) {
                    if ( specVersion == null ) {
                        specVersion = container.getComponentDescription().getSpecVersion();
                        logger.debug("Setting used spec version to " + specVersion);
                    } else if (container.getComponentDescription().getSpecVersion().ordinal() > specVersion.ordinal() ) {
                        if ( this.options.getSpecVersion() != null) {
                            // if a spec version has been configured and a component requires a higher
                            // version, this is considered an error!
                            iLog.addError("Component " + container + " requires spec version " + container.getComponentDescription().getSpecVersion().name()
                                            + " but plugin is configured to use version " + this.options.getSpecVersion(),
                                            desc.getSource());
                        } else {
                            specVersion = container.getComponentDescription().getSpecVersion();
                            logger.debug("Setting used spec version to " + specVersion);
                        }
                    }
                } else {
                    if ( this.options.getSpecVersion() != null ) {
                        container.getComponentDescription().setSpecVersion(options.getSpecVersion());
                    } else {
                        container.getComponentDescription().setSpecVersion(SpecVersion.VERSION_1_0);
                    }
                }
                processedContainers.add(container);
            }
        }
        // if spec version is still not set, we're using lowest available
        if ( specVersion == null ) {
            specVersion = SpecVersion.VERSION_1_0;
            logger.debug("Using default spec version " + specVersion);
        }
        this.logger.debug("Generating descriptor for spec version: " + specVersion);
        options.setSpecVersion(specVersion);

        // before we can validate we should check the references for bind/unbind method
        // in order to create them if possible
        if ( this.options.isGenerateAccessors() ) {
            for(final ComponentContainer container : processedContainers) {
                this.generateMethods(container);
            }
        }

        // now validate
        final DescriptionContainer module = new DescriptionContainer(this.options);
        for (final ComponentContainer container : processedContainers) {
            final int errorCount = iLog.getNumberOfErrors();

            final Validator validator = new Validator(container, project, options, iLog);
            validator.validate();

            // ignore component if it has errors
            if (iLog.getNumberOfErrors() == errorCount) {
                module.add(container);
            }
        }
        // log issues
        iLog.logMessages(logger);

        // after checking all classes, throw if there were any failures
        if (iLog.hasErrors()) {
            throw new SCRDescriptorFailureException("SCR Descriptor parsing had failures (see log)");
        }

        // and generate files
        result.setMetatypeFiles(MetaTypeIO.generateDescriptors(module, this.project, this.options, this.logger));
        result.setScrFiles(ComponentDescriptorIO.generateDescriptorFiles(module, this.options, logger));

        return result;
    }

    private void generateMethods(final ComponentContainer container) throws SCRDescriptorException {
        for (final ReferenceDescription ref : container.getReferences().values()) {
            // skip refs without a interface name (validate will be called next)
            if (StringUtils.isEmpty(ref.getInterfaceName())) {
                continue;
            }

            // if this is a field with a single cardinality,
            // we look for the bind/unbind methods
            // and create them if they are not availabe
            if (ref.getStrategy() != ReferenceStrategy.LOOKUP && ref.getField() != null
                && ref.getField().getDeclaringClass().getName().equals(container.getClassDescription().getDescribedClass().getName())
                && (ref.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || ref.getCardinality() == ReferenceCardinality.MANDATORY_UNARY)) {

                final String bindValue = ref.getBind();
                final String unbindValue = ref.getUnbind();
                final String name = ref.getName();
                final String type = ref.getInterfaceName();

                boolean createBind = false;
                boolean createUnbind = false;

                // Only create method if no bind name has been specified
                if (bindValue == null && Validator.findMethod(this.project, this.options, container.getClassDescription(), ref, "bind") == null) {
                    // create bind method
                    createBind = true;
                }
                if (unbindValue == null && Validator.findMethod(this.project, this.options, container.getClassDescription(), ref, "unbind") == null) {
                    // create unbind method
                    createUnbind = true;
                }
                if (createBind || createUnbind) {
                    // logging
                    if ( createBind && createUnbind ) {
                        this.logger.debug("Generating bind and unbind method for " + name + " in " + container.getClassDescription().getDescribedClass().getName());
                    } else if ( createBind ) {
                        this.logger.debug("Generating bind method for " + name + " in " + container.getClassDescription().getDescribedClass().getName());
                    } else {
                        this.logger.debug("Generating unbind method for " + name + " in " + container.getClassDescription().getDescribedClass().getName());

                    }
                    ClassModifier.addMethods(container.getClassDescription().getDescribedClass().getName(),
                                    name,
                                    ref.getField().getName(),
                                    type,
                                    createBind,
                                    createUnbind,
                                    this.project.getClassLoader(),
                                    this.project.getClassesDirectory(),
                                    this.logger);
                    // set a flag for validation
                    ref.setBindMethodCreated(createBind);
                    ref.setUnbindMethodCreated(createUnbind);
                }
            }
        }
    }

    /**
     * Create the SCR objects based on the descriptions
     */
    private ComponentContainer createComponent(final ClassDescription desc,
                    final IssueLog iLog)
    throws SCRDescriptorException {
        final ComponentDescription componentDesc = desc.getDescription(ComponentDescription.class);

        final SpecVersion intitialComponentSpecVersion = componentDesc.getSpecVersion();

        // configuration pid in 1.2
        if ( componentDesc.getConfigurationPid() != null && !componentDesc.getConfigurationPid().equals(componentDesc.getName())) {
            componentDesc.setSpecVersion(SpecVersion.VERSION_1_2);
        }

        final ComponentContainer container = new ComponentContainer(desc, componentDesc);

        // Create metatype (if required)
        final MetatypeContainer ocd;
        if ( !componentDesc.isAbstract() && componentDesc.isCreateMetatype() ) {
            // OCD
            ocd = new MetatypeContainer();
            container.setMetatypeContainer( ocd );
            ocd.setId( componentDesc.getName() );
            if ( componentDesc.getLabel() != null ) {
                ocd.setName( componentDesc.getLabel() );
            }
            if ( componentDesc.getDescription() != null ) {
                ocd.setDescription( componentDesc.getDescription() );
            }

            // Factory pid
            if ( componentDesc.isSetMetatypeFactoryPid() ) {
                if ( componentDesc.getFactory() == null ) {
                    ocd.setFactoryPid( componentDesc.getName() );
                } else {
                    iLog.addWarning( "Component factory " + componentDesc.getName()
                        + " should not set metatype factory pid.", desc.getSource() );
                }
            }
        } else {
            ocd = null;
        }
        // metatype checks if metatype is not generated (FELIX-4033)
        if ( !componentDesc.isAbstract() && !componentDesc.isCreateMetatype() ) {
            if ( componentDesc.getLabel() != null && componentDesc.getLabel().trim().length() > 0 ) {
                iLog.addWarning(" Component " + componentDesc.getName() + " has set a label. However metatype is set to false. This label is ignored.",
                        desc.getSource());
            }
            if ( componentDesc.getDescription() != null && componentDesc.getDescription().trim().length() > 0 ) {
                iLog.addWarning(" Component " + componentDesc.getName() + " has set a description. However metatype is set to false. This description is ignored.",
                        desc.getSource());
            }
        }

        ClassDescription current = desc;
        boolean inherit;
        do {
            final ComponentDescription cd = current.getDescription(ComponentDescription.class);
            inherit = (cd == null ? true : cd.isInherit());

            if ( cd != null ) {
                if ( current != desc ) {
                    iLog.addWarning(" Component " + componentDesc.getName() + " is using the " +
                                    "deprecated inheritance feature and inherits from " + current.getDescribedClass().getName() +
                                    ". This feature will be removed in future versions.",
                                    desc.getSource());
                }
                // handle enabled and immediate
                if ( componentDesc.getEnabled() == null ) {
                    componentDesc.setEnabled(cd.getEnabled());
                }
                if ( componentDesc.getImmediate() == null ) {
                    componentDesc.setImmediate(cd.getImmediate());
                }

                // lifecycle methods
                if ( componentDesc.getActivate() == null && cd.getActivate() != null ) {
                    componentDesc.setActivate(cd.getActivate());
                }
                if ( componentDesc.getDeactivate() == null && cd.getDeactivate() != null ) {
                    componentDesc.setDeactivate(cd.getDeactivate());
                }
                if ( componentDesc.getModified() == null && cd.getModified() != null ) {
                    componentDesc.setModified(cd.getModified());
                }
                if ( componentDesc.getActivate() != null || componentDesc.getDeactivate() != null || componentDesc.getModified() != null ) {
                    // spec version must be at least 1.1
                    componentDesc.setSpecVersion(SpecVersion.VERSION_1_1);
                }
                if ( componentDesc.getConfigurationPolicy() != ComponentConfigurationPolicy.OPTIONAL ) {
                    // policy requires 1.1
                    componentDesc.setSpecVersion(SpecVersion.VERSION_1_1);
                }

            }
            // services, properties, references
            this.processServices(current, container);
            this.processProperties(current, container, ocd);
            this.processReferences(current, container);


            // go up in the class hierarchy
            if ( !inherit || current.getDescribedClass().getSuperclass() == null ) {
                current = null;
            } else {
                try {
                    current = this.scanner.getDescription(current.getDescribedClass().getSuperclass());
                } catch ( final SCRDescriptorFailureException sde) {
                    this.logger.debug(sde.getMessage(), sde);
                    iLog.addError(sde.getMessage(), current.getSource());
                } catch ( final SCRDescriptorException sde) {
                    this.logger.debug(sde.getSourceLocation() + " : " + sde.getMessage(), sde);
                    iLog.addError(sde.getMessage(), sde.getSourceLocation());
                }
            }
        } while ( current != null);

        // check service interfaces for properties
        if ( container.getServiceDescription() != null ) {
            for(final String interfaceName : container.getServiceDescription().getInterfaces()) {
                try {
                    final Class<?> interfaceClass = project.getClassLoader().loadClass(interfaceName);
                    final ClassDescription interfaceDesc = this.scanner.getDescription(interfaceClass);
                    if ( interfaceDesc != null ) {
                        this.processProperties(interfaceDesc, container, ocd);
                    }
                } catch ( final SCRDescriptorFailureException sde) {
                    this.logger.debug(sde.getMessage(), sde);
                    iLog.addError(sde.getMessage(), interfaceName);
                } catch ( final SCRDescriptorException sde) {
                    this.logger.debug(sde.getSourceLocation() + " : " + sde.getMessage(), sde);
                    iLog.addError(sde.getMessage(), sde.getSourceLocation());
                } catch (ClassNotFoundException e) {
                    this.logger.debug(e.getMessage(), e);
                    iLog.addError(e.getMessage(), interfaceName);
                }
            }
        }

        // global properties
        this.processGlobalProperties(desc, container.getProperties());

        // check lifecycle methods
        if ( componentDesc.getActivate() == null ) {
            final Validator.MethodResult result = Validator.findLifecycleMethod(project, container, "activate", true);
            if ( result.method != null ) {
                componentDesc.setSpecVersion(result.requiredSpecVersion);
            }
        }
        if ( componentDesc.getDeactivate() == null ) {
            final Validator.MethodResult result = Validator.findLifecycleMethod(project, container, "deactivate", false);
            if ( result.method != null ) {
                componentDesc.setSpecVersion(result.requiredSpecVersion);
            }
        }

        // check if component has spec version configured but requires a higher one
        if ( intitialComponentSpecVersion != null && componentDesc.getSpecVersion().ordinal() > intitialComponentSpecVersion.ordinal() ) {
            iLog.addError("Component " + container + " requires spec version " + container.getComponentDescription().getSpecVersion().name()
                    + " but component is configured to use version " + intitialComponentSpecVersion.name(),
                    desc.getSource());
        }
        return container;
    }

    /**
     * Process service directives
     */
    private void processServices(final ClassDescription current, final ComponentContainer component) {

        final ServiceDescription serviceDesc = current.getDescription(ServiceDescription.class);
        if ( serviceDesc != null ) {
            ServiceDescription service = component.getServiceDescription();
            if ( service == null ) {
                service = new ServiceDescription(serviceDesc.getAnnotation());
                service.setServiceFactory(false);
                component.setServiceDescription(service);
            }
            if ( serviceDesc.isServiceFactory() ) {
                service.setServiceFactory(true);
            }
            for(final String className : serviceDesc.getInterfaces()) {
                service.addInterface(className);
            }
        }
    }

    private boolean isPrivateProperty(final String name) {
        final boolean isPrivate;
        if (org.osgi.framework.Constants.SERVICE_RANKING.equals(name)
                || org.osgi.framework.Constants.SERVICE_PID.equals(name)
                || org.osgi.framework.Constants.SERVICE_DESCRIPTION.equals(name)
                || org.osgi.framework.Constants.SERVICE_VENDOR.equals(name)
                || ConfigurationAdmin.SERVICE_BUNDLELOCATION.equals(name)
                || ConfigurationAdmin.SERVICE_FACTORYPID.equals(name) ) {
                isPrivate = true;
            } else {
                isPrivate = false;
            }
        return isPrivate;
    }

    /**
     * Process property directives
     */
    private void processProperties(
                    final ClassDescription current,
                    final ComponentContainer component,
                    final MetatypeContainer ocd) {
        for(final PropertyDescription pd : current.getDescriptions(PropertyDescription.class)) {

            if ( this.testProperty(current, component.getProperties(), pd, current == component.getClassDescription()) ) {
                final String name = pd.getName();
                if ( org.osgi.framework.Constants.SERVICE_ID.equals(name) ) {
                    iLog.addError("Class " + current.getDescribedClass().getName() + " is declaring " +
                                  "the protected property 'service.id'.", current.getSource() );
                    continue;

                }
                if ( ocd != null) {

                    // metatype - is this property private?
                    final boolean isPrivate;
                    if ( pd.isPrivate() != null ) {
                        isPrivate = pd.isPrivate();
                    } else {
                        if (isPrivateProperty(name) ) {
                            isPrivate = true;
                        } else {
                            isPrivate = false;
                        }
                    }
                    if ( !isPrivate ) {
                        final MetatypeAttributeDefinition ad = new MetatypeAttributeDefinition();
                        ocd.getProperties().add(ad);
                        ad.setId(pd.getName());
                        ad.setType(pd.getType().name());

                        if (pd.getLabel() != null ) {
                            ad.setName(pd.getLabel());
                        }
                        if (pd.getDescription() != null ) {
                            ad.setDescription(pd.getDescription());
                        }

                        if ( pd.getUnbounded() == PropertyUnbounded.DEFAULT ) {
                            if ( pd.getCardinality() != 0 ) {
                                ad.setCardinality(pd.getCardinality());
                            }
                        } else if ( pd.getUnbounded() == PropertyUnbounded.ARRAY ) {
                            // unlimited array
                            ad.setCardinality(new Integer(Integer.MAX_VALUE));
                        } else {
                            // unlimited vector
                            ad.setCardinality(new Integer(Integer.MIN_VALUE));
                        }

                        ad.setDefaultValue(pd.getValue());
                        ad.setDefaultMultiValue(pd.getMultiValue());

                        // check options
                        final String[] parameters = pd.getOptions();
                        if ( parameters != null && parameters.length > 0 ) {
                            final Map<String, String> options = new LinkedHashMap<String, String>();
                            for (int j=0; j < parameters.length; j=j+2) {
                                final String optionLabel = parameters[j];
                                final String optionValue = (j < parameters.length-1) ? parameters[j+1] : null;
                                if (optionValue != null) {
                                    options.put(optionLabel, optionValue);
                                }
                            }
                            ad.setOptions(options);
                        }
                    }
                } else {
                    // additional metatype checks (FELIX-4033)
                    if ( pd.isPrivate() != null && pd.isPrivate() ) {
                        iLog.addWarning("Property " + pd.getName() + " in class "
                                + current.getDescribedClass().getName() + " is set as private. " +
                                "This is redundant as no metatype will be generated.", current.getSource() );
                    }
                }
            }
        }
    }

    /**
     * Add global properties (if not already defined in the component)
     */
    private void processGlobalProperties(final ClassDescription desc,
                    final Map<String, PropertyDescription> allProperties) {
        // apply pre configured global properties
        if ( this.options.getProperties() != null ) {
            for(final Map.Entry<String, String> entry : this.options.getProperties().entrySet()) {
                final String propName = entry.getKey();
                final String value = entry.getValue();
                // check if the service already provides this property
                if ( value != null && !allProperties.containsKey(propName) ) {

                    final PropertyDescription p = new PropertyDescription(null);
                    p.setName(propName);
                    p.setValue(value);
                    p.setType(PropertyType.String);

                    allProperties.put(propName, p);
                }
            }
        }
    }

    /**
     * Test a newly found property
     */
    private boolean testProperty(final ClassDescription current,
                    final Map<String, PropertyDescription> allProperties,
                    final PropertyDescription newProperty,
                    final boolean isInspectedClass ) {
        final String propName = newProperty.getName();

        if ( !StringUtils.isEmpty(propName) ) {
            if ( allProperties.containsKey(propName) ) {
                // if the current class is the class we are currently inspecting, we
                // have found a duplicate definition
                if ( isInspectedClass ) {
                    iLog.addError("Duplicate definition for property " + propName + " in class "
                                    + current.getDescribedClass().getName(), current.getSource() );
                }
                return false;
            }
            allProperties.put(propName, newProperty);

        } else {
            // no name - generate a unique one
            allProperties.put(UUID.randomUUID().toString(), newProperty);
        }
        return true;
    }

    /**
     * Process reference directives
     * @throws SCRDescriptorException
     */
    private void processReferences(final ClassDescription current,
                    final ComponentContainer component) {
        for(final ReferenceDescription rd : current.getDescriptions(ReferenceDescription.class)) {
            if ( rd.getPolicyOption() != ReferencePolicyOption.RELUCTANT ) {
                component.getComponentDescription().setSpecVersion(SpecVersion.VERSION_1_2);
            }
            if ( rd.getUpdated() != null ) {
                // updated requires 1.2 or 1.1_FELIX, if nothing is set, we use 1.2
                if ( component.getComponentDescription().getSpecVersion() == null
                     || component.getComponentDescription().getSpecVersion().ordinal() < SpecVersion.VERSION_1_1_FELIX.ordinal() ) {
                    component.getComponentDescription().setSpecVersion(SpecVersion.VERSION_1_2);
                }
            }

            this.testReference(current, component.getReferences(), rd, component.getClassDescription() == current);

            // check for method signature - if interface name is set (empty interface name will fail during validate)
            if (!StringUtils.isEmpty(rd.getInterfaceName())) {

                try {
                    final Validator.MethodResult bindMethod = Validator.findMethod(this.project, this.options, current, rd,
                            rd.getBind() == null ? "bind" : rd.getBind());
                    if ( bindMethod != null ) {
                        component.getComponentDescription().setSpecVersion(bindMethod.requiredSpecVersion);
                    }

                    final Validator.MethodResult unbindMethod = Validator.findMethod(this.project, this.options, current, rd,
                            rd.getUnbind() == null ? "unbind" : rd.getUnbind());
                    if ( unbindMethod != null ) {
                        component.getComponentDescription().setSpecVersion(unbindMethod.requiredSpecVersion);
                    }

                } catch (final SCRDescriptorException sde) {
                    // this happens only if a class not found exception occurs, so we can ignore this at this point!
                }
            }
        }
    }

    /**
     * Test a newly found reference
     */
    private void testReference(final ClassDescription current,
                    final Map<String, ReferenceDescription> allReferences,
                    final ReferenceDescription newReference,
                    final boolean isInspectedClass ) {
        String refName = newReference.getName();
        if ( refName == null) {
            refName = newReference.getInterfaceName();
        }

        if ( refName != null ) {
            if ( allReferences.containsKey( refName ) ) {
                // if the current class is the class we are currently inspecting, we
                // have found a duplicate definition
                if ( isInspectedClass ) {
                    iLog.addError("Duplicate definition for reference " + refName + " in class "
                        + current.getDescribedClass().getName(), current.getSource() );
                }
            } else {
                allReferences.put(refName, newReference);
            }
        } else {
            // no name - generate a unique one
            allReferences.put(UUID.randomUUID().toString(), newReference);
        }
    }
}
