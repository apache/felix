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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.BundleAdapterService;
import org.apache.felix.dm.annotation.api.BundleDependency;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Composition;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.Init;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.LifecycleController;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Registered;
import org.apache.felix.dm.annotation.api.RepeatableProperty;
import org.apache.felix.dm.annotation.api.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.ResourceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.Unregistered;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;

import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Verifier;

/**
 * This is the scanner which does all the annotation parsing on a given class.
 * To start the parsing, just invoke the parseClassFileWithCollector and finish methods.
 * Once parsed, the corresponding component descriptors can be built using the "writeTo" method.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationCollector extends ClassDataCollector
{
    private final static String A_INIT = Init.class.getName();
    private final static String A_START = Start.class.getName();
    private final static String A_STOP = Stop.class.getName();
    private final static String A_DESTROY = Destroy.class.getName();
    private final static String A_COMPOSITION = Composition.class.getName();
    private final static String A_LIFCLE_CTRL = LifecycleController.class.getName();

    private final static String A_COMPONENT = Component.class.getName();
    private final static String A_PROPERTY = Property.class.getName();
    private final static String A_REPEATABLE_PROPERTY = RepeatableProperty.class.getName();
    private final static String A_SERVICE_DEP = ServiceDependency.class.getName();
    private final static String A_CONFIGURATION_DEPENDENCY = ConfigurationDependency.class.getName();
    private final static String A_BUNDLE_DEPENDENCY = BundleDependency.class.getName();
    private final static String A_RESOURCE_DEPENDENCY = ResourceDependency.class.getName();
    private final static String A_ASPECT_SERVICE = AspectService.class.getName();
    private final static String A_ADAPTER_SERVICE = AdapterService.class.getName();
    private final static String A_BUNDLE_ADAPTER_SERVICE = BundleAdapterService.class.getName();
    private final static String A_RESOURCE_ADAPTER_SERVICE = ResourceAdapterService.class.getName();
    private final static String A_FACTORYCONFIG_ADAPTER_SERVICE = FactoryConfigurationAdapterService.class.getName();
    private final static String A_INJECT = Inject.class.getName();
    private final static String A_REGISTERED = Registered.class.getName();
    private final static String A_UNREGISTERED = Unregistered.class.getName();

    private Logger m_logger;
    private String m_className;
    private String[] m_interfaces;
    private boolean m_isField;
    private String m_field;
    private String m_method;
    private String m_descriptor;
    private Set<String> m_dependencyNames = new HashSet<String>();
    private List<EntryWriter> m_writers = new ArrayList<EntryWriter>();
    private MetaType m_metaType;
    private String m_startMethod;
    private String m_stopMethod;
    private String m_initMethod;
    private String m_destroyMethod;
    private String m_compositionMethod;
    private String m_starter;
    private String m_stopper;
    private Set<String> m_importService = new HashSet<String>();
    private Set<String> m_exportService = new HashSet<String>();
    private String m_bundleContextField;
    private String m_dependencyManagerField;
    private String m_componentField;
    private String m_registeredMethod;
    private String m_unregisteredMethod;
    
    /**
     * When more than one @Property annotation are declared on a component type (outside of the @Component annotation), then a @Repeatable 
     * annotation is used as the container for the @Property annotations. When such annotation is found, it is stored in this attribute, which 
     * will be parsed in our finish() method.
     */
    private Annotation m_repeatableProperty; 
    
    /**
     * If a Single @Property is declared on the component type (outside of the @Component annotation), then there is no @Repeatable annotation.
     * When such single @Property annotation is found, it is stored in this attribute, which will be parsed in our finish() method.
     */
    private Annotation m_singleProperty; 
    
    /**
     * List of all possible DM components.
     */
    private final List<EntryType> m_componentTypes = Arrays.asList(EntryType.Component, EntryType.AspectService, EntryType.AdapterService,
        EntryType.BundleAdapterService, EntryType.ResourceAdapterService, EntryType.FactoryConfigurationAdapterService);

    /**
     * Makes a new Collector for parsing a given class.
     * @param reporter the object used to report logs.
     */
    public AnnotationCollector(Logger reporter, MetaType metaType)
    {
        m_logger = reporter;
        m_metaType = metaType;
    }

    /**
     * Parses the name of the class.
     * @param access the class access
     * @param name the class name (package are "/" separated).
     */
    @Override
    public void classBegin(int access, TypeRef name)
    {
        m_className = name.getFQN();
        m_logger.debug("class name: %s", m_className);
    }

    /**
     * Parses the implemented interfaces ("/" separated).
     */
    @Override
    public void implementsInterfaces(TypeRef[] interfaces)
    {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < interfaces.length; i++)
        {
            if (!interfaces[i].getBinary().equals("scala/ScalaObject"))
            {
                result.add(interfaces[i].getFQN());
            }
        }
         
        m_interfaces = result.toArray(new String[result.size()]);
        m_logger.debug("implements: %s", Arrays.toString(m_interfaces));
    }

    /**
     * Parses a method. Always invoked BEFORE eventual method annotation.
     */
    @Override
    public void method(Clazz.MethodDef method)
    {
        m_logger.debug("Parsed method %s, descriptor=%s", method.getName(), method.getDescriptor());
        m_isField = false;
        m_method = method.getName();
        m_descriptor = method.getDescriptor().toString();
    }

    /**
     * Parses a field. Always invoked BEFORE eventual field annotation
     */
    @Override
    public void field(Clazz.FieldDef field)
    {
        m_logger.debug("Parsed field %s, descriptor=%s", field.getName(), field.getDescriptor());
        m_isField = true;
        m_field = field.getName();
        m_descriptor = field.getDescriptor().toString();
    }

    /** 
     * An annotation has been parsed. Always invoked AFTER the "method"/"field"/"classBegin" callbacks. 
     */
    @Override
    public void annotation(Annotation annotation)
    {
        m_logger.debug("Parsing annotation: %s", annotation.getName());

        if (annotation.getName().getFQN().equals(A_COMPONENT))
        {
            parseComponentAnnotation(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_ASPECT_SERVICE))
        {
            parseAspectService(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_ADAPTER_SERVICE))
        {
            parseAdapterService(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_BUNDLE_ADAPTER_SERVICE))
        {
            parseBundleAdapterService(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_RESOURCE_ADAPTER_SERVICE))
        {
            parseResourceAdapterService(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_FACTORYCONFIG_ADAPTER_SERVICE))
        {
            parseFactoryConfigurationAdapterService(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_INIT))
        {
            m_initMethod = m_method;
        } 
        else if (annotation.getName().getFQN().equals(A_START))
        {
            m_startMethod = m_method;
        } 
        else if (annotation.getName().getFQN().equals(A_REGISTERED))
        {
            m_registeredMethod = m_method;
        }
        else if (annotation.getName().getFQN().equals(A_STOP))
        {
            m_stopMethod = m_method;
        }
        else if (annotation.getName().getFQN().equals(A_UNREGISTERED))
        {
            m_unregisteredMethod = m_method;
        }
        else if (annotation.getName().getFQN().equals(A_DESTROY))
        {
            m_destroyMethod = m_method;
        }
        else if (annotation.getName().getFQN().equals(A_COMPOSITION))
        {
            Patterns.parseMethod(m_method, m_descriptor, Patterns.COMPOSITION);
            m_compositionMethod = m_method;
        } else if (annotation.getName().getFQN().equals(A_LIFCLE_CTRL)) 
        {
            parseLifecycleAnnotation(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_SERVICE_DEP))
        {
            parseServiceDependencyAnnotation(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_CONFIGURATION_DEPENDENCY))
        {
            parseConfigurationDependencyAnnotation(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_BUNDLE_DEPENDENCY))
        {
            parseBundleDependencyAnnotation(annotation);
        }
        else if (annotation.getName().getFQN().equals(A_RESOURCE_DEPENDENCY))
        {
            parseRersourceDependencyAnnotation(annotation);
        } 
        else if (annotation.getName().getFQN().equals(A_INJECT))
        {
            parseInject(annotation);
        } 
        else if (annotation.getName().getFQN().equals(A_REPEATABLE_PROPERTY))
        {
            parseRepeatableProperties(annotation);
        } 
        else if (annotation.getName().getFQN().equals(A_PROPERTY))
        {
        	m_singleProperty = annotation;
        } 
    }

    /**
     * Finishes up the class parsing. This method must be called once the parseClassFileWithCollector method has returned.
     * @return true if some annotations have been parsed, false if not.
     */
    public boolean finish()
    {
        if (m_writers.size() == 0)
        {
            m_logger.info("No components found for class " + m_className);
            return false;
        }

        // We must have at least a valid component annotation type (component, aspect, or adapters)
        
        EntryWriter componentWriter = m_writers.stream()
            .filter(writer -> m_componentTypes.indexOf(writer.getEntryType()) != -1)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(": the class " + m_className + " must be annotated with either one of the following types: " + m_componentTypes));                   
        
        // Add any repeated @Property annotations to the component (or to the aspect, or adapter).
                
        if (m_repeatableProperty != null)
        {
            Object[] properties = m_repeatableProperty.get("value");
            for (Object property : properties)
            {
                // property is actually a @Property annotation.
                parseProperty((Annotation) property, componentWriter);
            }
        }
        
        // Handle a single Property declared on the component type (in this case, there is no @Repeatable annotation).
        
        if (m_singleProperty != null) {
            parseProperty(m_singleProperty, componentWriter);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Parsed annotation for class ");
        sb.append(m_className);
        for (int i = m_writers.size() - 1; i >= 0; i--)
        {
            sb.append("\n\t").append(m_writers.get(i).toString());
        }
        m_logger.info(sb.toString());
        return true;
    }

    /**
     * Writes the generated component descriptor in the given print writer.
     * The first line must be the service (@Service or AspectService).
     * @param pw the writer where the component descriptor will be written.
     */
    public void writeTo(PrintWriter pw)
    {
        // The last element our our m_writers list contains either the Service, or the AspectService descriptor.
        for (int i = m_writers.size() - 1; i >= 0; i--)
        {
            pw.println(m_writers.get(i).toString());
        }
    }
        
    /**
     * Returns list of all imported services. Imported services are deduced from every
     * @ServiceDependency annotations.
     * @return the list of imported services, or null
     */
    public Set<String> getImportService()
    {
        return m_importService;
    }

    /**
     * Returns list of all exported services. Imported services are deduced from every
     * annotations which provides a service (@Component, etc ...)
     * @return the list of exported services, or null
     */
    public Set<String> getExportService()
    {
        return m_exportService;
    }
    
    /**
     * Parses a Property annotation that is applied on the component class.
     * @param property the Property annotation.
     */
    private void parseRepeatableProperties(Annotation repeatedProperties)
    {
        m_repeatableProperty = repeatedProperties;
    }
    
    private void parseComponentAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.Component);
        m_writers.add(writer);

        // Register previously parsed annotations common to services (Init/Start/...)
        addCommonServiceParams(writer);

        // impl attribute
        writer.put(EntryParam.impl, m_className);

        // properties attribute
        parseProperties(annotation, writer);

        // provides attribute.
        if (writer.putClassArray(annotation, EntryParam.provides, m_interfaces, m_exportService) == 0)
        {
            // no service provided: check if @Registered/@Unregistered annotation are used
            // and raise an error if true.
            checkRegisteredUnregisteredNotPresent();
        }

        // factorySet attribute (deprecated, replaced by factoryName)
        String factorySetName = writer.putString(annotation, EntryParam.factorySet, null);
        if (factorySetName != null)
        {
            // When a component defines a factorySet, it means that a java.util.Set will 
            // be provided into the OSGi registry, in order to let anoter component add
            // some component instance configurations into it.
            // So, we have to indicate that the Set is provided as a service, in the Export-Serviec
            // header.
            m_exportService.add("java.util.Set");
        }

        // factoryName attribute
        String factoryName = writer.putString(annotation, EntryParam.factoryName, null);
        if (factoryName != null)
        {
            // When a component defines a factoryName, it means that a ComponentFactory will 
            // be provided into the OSGi registry, in order to let another component create some component instances.
            // So, we have to indicate that the ComponentFactory is provided as a service, in the Export-Serviec
            // header.
            m_exportService.add("org.apache.felix.dependencymanager.runtime.api.ComponentFactory");
        }

        // factoryConfigure attribute
        writer.putString(annotation, EntryParam.factoryConfigure, null);
        
        // factoryMethod attribute
        writer.putString(annotation, EntryParam.factoryMethod, null);
    }

    private void addCommonServiceParams(EntryWriter writer)
    {
        if (m_initMethod != null)
        {
            writer.put(EntryParam.init, m_initMethod);
        }

        if (m_startMethod != null)
        {
            writer.put(EntryParam.start, m_startMethod);
        }
        
        if (m_registeredMethod != null)
        {
            writer.put(EntryParam.registered, m_registeredMethod);
        }

        if (m_stopMethod != null)
        {
            writer.put(EntryParam.stop, m_stopMethod);
        }
        
        if (m_unregisteredMethod != null)
        {
            writer.put(EntryParam.unregistered, m_unregisteredMethod);
        }

        if (m_destroyMethod != null)
        {
            writer.put(EntryParam.destroy, m_destroyMethod);
        }

        if (m_compositionMethod != null)
        {
            writer.put(EntryParam.composition, m_compositionMethod);
        }       
        
        if (m_starter != null) 
        {
            writer.put(EntryParam.starter, m_starter);
        }
        
        if (m_stopper != null)
        {
            writer.put(EntryParam.stopper, m_stopper);
            if (m_starter == null)
            {
                throw new IllegalArgumentException("Can't use a @LifecycleController annotation for stopping a service without declaring a " +
                                                   "@LifecycleController that starts the component in class " + m_className);
            }
        }   

        if (m_bundleContextField != null)
        {
            writer.put(EntryParam.bundleContextField, m_bundleContextField);
        }
        
        if (m_dependencyManagerField != null)
        {
            writer.put(EntryParam.dependencyManagerField, m_dependencyManagerField);
        }
        
        if (m_componentField != null)
        {
            writer.put(EntryParam.componentField, m_componentField);
        }
    }

    /**
     * Parses a ServiceDependency Annotation.
     * @param annotation the ServiceDependency Annotation.
     */
    private void parseServiceDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ServiceDependency);
        m_writers.add(writer);

        // service attribute
        String service = parseClassAttrValue(annotation.get(EntryParam.service.toString()));
        if (service == null)
        {
            if (m_isField)
            {
                service = Patterns.parseClass(m_descriptor, Patterns.CLASS, 1);
            }
            else
            {
            	// parse "bind(Component, ServiceReference, Service)" signature
            	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS1, 3, false);            		
            	
            	if (service == null) {
                	// parse "bind(Component, Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS2, 2, false);            		
            	}
            	
            	if (service == null) {
            		// parse "bind(Component, Map, Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS3, 3, false);            		
            	}
            	
            	if (service == null) {
            		// parse "bind(ServiceReference, Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS4, 2, false);            		
            	}

            	if (service == null) {
            		// parse "bind(Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS5, 1, false);            		
            	}

            	if (service == null) {
            		// parse "bind(Service, Map)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS6, 1, false);            		
            	}

            	if (service == null) {
            		// parse "bind(Map, Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS7, 2, false);            		
            	}

            	if (service == null) {
            		// parse "bind(Service, Dictionary)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS8, 1, false);            		
            	}

            	if (service == null) {
            		// parse "bind(Dictionary, Service)" signature
                	service = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS9, 2, true);            		
            	}
            }
        }
        writer.put(EntryParam.service, service);

        // Store this service in list of imported services.
        m_importService.add(service);
        
        // autoConfig attribute
        if (m_isField)
        {
            writer.put(EntryParam.autoConfig, m_field);
        }

        // filter attribute
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // defaultImpl attribute
        writer.putClass(annotation, EntryParam.defaultImpl);

        // added callback
        writer.putString(annotation, EntryParam.added, (!m_isField) ? m_method : null);

        // timeout parameter
        writer.putString(annotation, EntryParam.timeout, null);
        Long t = (Long) annotation.get(EntryParam.timeout.toString());
        if (t != null && t.longValue() < -1)
        {
            throw new IllegalArgumentException("Invalid timeout value " + t + " in ServiceDependency annotation from class " + m_className);
        }
        
        // required attribute (not valid if parsing a temporal service dependency)
        writer.putString(annotation, EntryParam.required, null);

        // changed callback
        writer.putString(annotation, EntryParam.changed, null);

        // removed callback
        writer.putString(annotation, EntryParam.removed, null); 
        
        // name attribute
        parseDependencyName(writer, annotation);
        
        // propagate attribute
        writer.putString(annotation, EntryParam.propagate, null);
    }
        
    /**
     * Parse the value of a given annotation attribute (which is of type 'class').
     * This method is compatible with bndtools 2.4.1 (where the annotation.get() method returns a String of the form "Lfull/class/name;"),
     * and with bndtools 3.x.x (where the annotation.get() method returns a TypeRef).
     * 
     * @param annot the annotation which contains the given attribute
     * @param attr the attribute name (of 'class' type).
     * @return the annotation class attribute value
     */
    public static String parseClassAttrValue(Object value) {
    	if (value instanceof String)
    	{
            return Patterns.parseClass((String) value, Patterns.CLASS, 1);
    	}
    	else if (value instanceof TypeRef) 
    	{
    		return ((TypeRef) value).getFQN();
    	} 
    	else if (value == null) {
    		return null;
    	}
    	else {
    		throw new IllegalStateException("can't parse class attribute value from " + value);
    	}
    }

    /**
     * Parses a ConfigurationDependency annotation.
     * @param annotation the ConfigurationDependency annotation.
     */
    private void parseConfigurationDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ConfigurationDependency);
        m_writers.add(writer);

        // The pid is either:
        //
        // - the fqdn of the configuration proxy type, if the callback accepts an interface (not a Dictionary).
        // - or the fqdn of the class specified by the pidFromClass attribute 
        // - or the value of the pid attribute
        // - or by default the fdqn of the class where the annotation is found

        String pidFromClass = parseClassAttrValue(annotation.get(EntryParam.pidClass.toString()));
        String pid = pidFromClass != null ? pidFromClass : get(annotation, EntryParam.pid.toString(), null);

        // Check if annotation is applied on "updated(ConfigProxyType)"
        String confProxyType =   Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS5, 1, false);
        if (confProxyType != null)
        {
            if (! Dictionary.class.getName().equals(confProxyType)) 
            {
                // It's a conf proxy type.
                writer.put(EntryParam.configType, confProxyType);
            }
            else
            {
                confProxyType = null;
            }
            
        } 
        else
        {
            // Check if annotation is applied on "updated(Component, ConfigProxyType)"
            confProxyType = Patterns.parseClass(m_descriptor, Patterns.BIND_CLASS2, 2, false); 
            if (! Dictionary.class.getName().equals(confProxyType)) 
            {
                // It's a conf proxy type.
                writer.put(EntryParam.configType, confProxyType);
            }
            else
            {
                confProxyType = null;
            }
        }
        
        if (pid == null) 
        {
            if (confProxyType != null)
            {
                pid = confProxyType;
            }
            else 
            {
                pid = m_className;
            }
        }

        writer.put(EntryParam.pid, pid);
        
        // the method on which the annotation is applied
        writer.put(EntryParam.updated, m_method);

        // propagate attribute
        writer.putString(annotation, EntryParam.propagate, null);

        // Property Meta Types
        parseMetaTypes(annotation, pid, false);
        
        // name attribute
        parseDependencyName(writer, annotation);
    }

    /**
     * Parses an AspectService annotation.
     * @param annotation
     */
    private void parseAspectService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.AspectService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Parse service filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse service aspect ranking
        Integer ranking = annotation.get(EntryParam.ranking.toString());
        writer.put(EntryParam.ranking, ranking.toString());

        // Generate Aspect Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse Aspect properties.
        parseProperties(annotation, writer);
        
        // Parse field/added/changed/removed attributes
        parseAspectOrAdapterCallbackMethods(annotation, writer);

        // Parse service interface this aspect is applying to
        Object service = annotation.get(EntryParam.service.toString());
        if (service == null)
        {
            if (m_interfaces == null)
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className
                    + " does not implement any interfaces");
            }
            if (m_interfaces.length != 1)
            {
                throw new IllegalStateException("Invalid AspectService annotation: " +
                    "the service attribute has not been set and the class " + m_className
                    + " implements more than one interface");
            }

            writer.put(EntryParam.service, m_interfaces[0]);
        }
        else
        {
            writer.putClass(annotation, EntryParam.service);
        }
        
        // Parse factoryMethod attribute
        writer.putString(annotation, EntryParam.factoryMethod, null);
    }

    private void parseAspectOrAdapterCallbackMethods(Annotation annotation, EntryWriter writer)
    {
        String field = annotation.get(EntryParam.field.toString());
        String added = annotation.get(EntryParam.added.toString());
        String changed = annotation.get(EntryParam.changed.toString());
        String removed = annotation.get(EntryParam.removed.toString());
        String swap = annotation.get(EntryParam.swap.toString());

        // "field" and "added/changed/removed/swap" attributes can't be mixed
        if (field != null && (added != null || changed != null || removed != null || swap != null))
        {
            throw new IllegalStateException("Annotation " + annotation + "can't applied on " + m_className
                    + " can't mix \"field\" attribute with \"added/changed/removed\" attributes");
        }
                
        // Parse aspect impl field where to inject the original service.
        writer.putString(annotation, EntryParam.field, null);
        
        // Parse aspect impl callback methods.
        writer.putString(annotation, EntryParam.added, null);
        writer.putString(annotation, EntryParam.changed, null);
        writer.putString(annotation, EntryParam.removed, null);
        writer.putString(annotation, EntryParam.swap, null);
    }

    /**
     * Parses an AspectService annotation.
     * @param annotation
     */
    private void parseAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.AdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse adaptee filter
        String adapteeFilter = annotation.get(EntryParam.adapteeFilter.toString());
        if (adapteeFilter != null)
        {
            Verifier.verifyFilter(adapteeFilter, 0);
            writer.put(EntryParam.adapteeFilter, adapteeFilter);
        }

        // Parse the mandatory adapted service interface.
        writer.putClass(annotation, EntryParam.adapteeService);

        // Parse Adapter properties.
        parseProperties(annotation, writer);

        // Parse the provided adapter service (use directly implemented interface by default).
        if (writer.putClassArray(annotation, EntryParam.provides, m_interfaces, m_exportService) == 0)
        {
            checkRegisteredUnregisteredNotPresent();
        }
        
        // Parse factoryMethod attribute
        writer.putString(annotation, EntryParam.factoryMethod, null);
        
        // Parse propagate flag.
        // Parse factoryMethod attribute
        writer.putString(annotation, EntryParam.propagate, null);

        // Parse field/added/changed/removed attributes
        parseAspectOrAdapterCallbackMethods(annotation, writer);
    }

    /**
     * Parses a BundleAdapterService annotation.
     * @param annotation
     */
    private void parseBundleAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.BundleAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse bundle filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse stateMask attribute
        writer.putString(annotation, EntryParam.stateMask, Integer.valueOf(
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE).toString());

        // Parse Adapter properties.
        parseProperties(annotation, writer);

        // Parse the optional adapter service (use directly implemented interface by default).
        if (writer.putClassArray(annotation, EntryParam.provides, m_interfaces, m_exportService) == 0)
        {
            checkRegisteredUnregisteredNotPresent();
        }

        // Parse propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());
        
        // Parse factoryMethod attribute
        writer.putString(annotation, EntryParam.factoryMethod, null);
    }

    /**
     * Parses a BundleAdapterService annotation.
     * @param annotation
     */
    private void parseResourceAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ResourceAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // Parse resource filter
        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        // Parse Adapter properties.
        parseProperties(annotation, writer);

        // Parse the provided adapter service (use directly implemented interface by default).
        if (writer.putClassArray(annotation, EntryParam.provides, m_interfaces, m_exportService) == 0)
        {
            checkRegisteredUnregisteredNotPresent();
        }

        // Parse propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());
        
        // Parse changed attribute
        writer.putString(annotation, EntryParam.changed, null);
    }

    /**
     * Parses a Factory Configuration Adapter annotation.
     * @param annotation
     */
    private void parseFactoryConfigurationAdapterService(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.FactoryConfigurationAdapterService);
        m_writers.add(writer);

        // Register previously parsed Init/Start/Stop/Destroy/Composition annotations
        addCommonServiceParams(writer);

        // Generate Adapter Implementation
        writer.put(EntryParam.impl, m_className);

        // factory pid attribute (can be specified using the factoryPid attribute, or using the factoryPidClass attribute)
        String factoryPidClass = parseClassAttrValue(annotation.get(EntryParam.factoryPidClass.toString()));
        
        // Test if a type safe configuration type is provided.
        String configType = parseClassAttrValue(annotation.get(EntryParam.configType.toString()));
        
        if (configType != null) {
            writer.put(EntryParam.configType, configType);
        }
        
        String factoryPid = null;
        
        factoryPid = get(annotation, EntryParam.factoryPid.toString(), factoryPidClass);
        if (factoryPid == null) {
            factoryPid = configType != null ? configType : m_className;
        }
        
        writer.put(EntryParam.factoryPid, factoryPid);

        // Parse updated callback
        writer.putString(annotation, EntryParam.updated, "updated");

        // propagate attribute
        writer.putString(annotation, EntryParam.propagate, Boolean.FALSE.toString());

        // Parse the provided adapter service (use directly implemented interface by default).
        if (writer.putClassArray(annotation, EntryParam.provides, m_interfaces, m_exportService) == 0)
        {
            checkRegisteredUnregisteredNotPresent();
        }

        // Parse Adapter properties.
        parseProperties(annotation, writer);

        // Parse optional meta types for configuration description.
        parseMetaTypes(annotation, factoryPid, true);
        
        // Parse factoryMethod attribute
        writer.putString(annotation, EntryParam.factoryMethod, null);
    }

    private void parseBundleDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.BundleDependency);
        m_writers.add(writer);

        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }

        writer.putString(annotation, EntryParam.added, m_method);
        writer.putString(annotation, EntryParam.changed, null);
        writer.putString(annotation, EntryParam.removed, null);
        writer.putString(annotation, EntryParam.required, null);
        writer.putString(annotation, EntryParam.stateMask, null);
        writer.putString(annotation, EntryParam.propagate, null);
        parseDependencyName(writer, annotation);
    }

    private void parseRersourceDependencyAnnotation(Annotation annotation)
    {
        EntryWriter writer = new EntryWriter(EntryType.ResourceDependency);
        m_writers.add(writer);

        String filter = annotation.get(EntryParam.filter.toString());
        if (filter != null)
        {
            Verifier.verifyFilter(filter, 0);
            writer.put(EntryParam.filter, filter);
        }
        
        if (m_isField)
        {
            writer.put(EntryParam.autoConfig, m_field);
        }

        writer.putString(annotation, EntryParam.added, (!m_isField) ? m_method : null);
        writer.putString(annotation, EntryParam.changed, null);
        writer.putString(annotation, EntryParam.removed, null);
        writer.putString(annotation, EntryParam.required, null);
        writer.putString(annotation, EntryParam.propagate, null);
        writer.putString(annotation, EntryParam.factoryMethod, null);
        parseDependencyName(writer, annotation);
    }

    /**
     * Parse the name of a given dependency.
     * @param writer The writer where to write the dependency name
     * @param annotation the dependency to be parsed
     */
    private void parseDependencyName(EntryWriter writer, Annotation annotation)
    {
        String name = annotation.get(EntryParam.name.toString());
        if (name != null) 
        {
            if(! m_dependencyNames.add(name))
            {
                throw new IllegalArgumentException("Duplicate dependency name " + name + " in Dependency " + annotation + " from class " + m_className);
            }
            writer.put(EntryParam.name, name);
        }
    }
    
    private void parseLifecycleAnnotation(Annotation annotation)
    {
        Patterns.parseField(m_field, m_descriptor, Patterns.RUNNABLE);
        if ("true".equals(get(annotation,EntryParam.start.name(), "true")))
        {
            if (m_starter != null) {
                throw new IllegalStateException("Lifecycle annotation already defined on field " + 
                                                m_starter + " in class " + m_className);
            }
            m_starter = m_field;
        } else {
            if (m_stopper != null) {
                throw new IllegalStateException("Lifecycle annotation already defined on field " + 
                                                m_stopper + " in class " + m_className);
            }
            m_stopper = m_field;
        }
    }

    /**
     * Parse optional meta types annotation attributes
     * @param annotation
     */
    private void parseMetaTypes(Annotation annotation, String pid, boolean factory)
    {
        if (annotation.get("metadata") != null)
        {
            String propertiesHeading = annotation.get("heading");
            String propertiesDesc = annotation.get("description");

            MetaType.OCD ocd = new MetaType.OCD(pid, propertiesHeading, propertiesDesc);
            for (Object p: (Object[]) annotation.get("metadata"))
            {
                Annotation property = (Annotation) p;
                String heading = property.get("heading");
                String id = property.get("id");
                String type = parseClassAttrValue(property.get("type"));
                Object[] defaults = (Object[]) property.get("defaults");
                String description = property.get("description");
                Integer cardinality = property.get("cardinality");
                Boolean required = property.get("required");

                MetaType.AD ad = new MetaType.AD(id, type, defaults, heading, description,
                    cardinality, required);

                Object[] optionLabels = property.get("optionLabels");
                Object[] optionValues = property.get("optionValues");

                if (optionLabels == null
                    && optionValues != null
                    ||
                    optionLabels != null
                    && optionValues == null
                    ||
                    (optionLabels != null && optionValues != null && optionLabels.length != optionValues.length))
                {
                    throw new IllegalArgumentException("invalid option labels/values specified for property "
                        + id +
                        " in PropertyMetadata annotation from class " + m_className);
                }

                if (optionValues != null)
                {
                    for (int i = 0; i < optionValues.length; i++)
                    {
                        ad.add(new MetaType.Option(optionValues[i].toString(), optionLabels[i].toString()));
                    }
                }

                ocd.add(ad);
            }

            m_metaType.add(ocd);
            MetaType.Designate designate = new MetaType.Designate(pid, factory);
            m_metaType.add(designate);
            m_logger.info("Parsed MetaType Properties from class " + m_className);
        }
    }

    /**
     * Parses a Property annotation (which represents a list of key-value pair).
     * The properties are encoded using the following json form:
     * 
     * properties       ::= key-value-pair*
     * key-value-pair   ::= key value
     * value            ::= String | String[] | value-type
     * value-type       ::= jsonObject with value-type-info
     * value-type-info  ::= "type"=primitive java type
     *                      "value"=String|String[]
     *                      
     * Exemple:
     * 
     *  "properties" : {
     *      "string-param" : "string-value",
     *      "string-array-param" : ["str1", "str2],
     *      "long-param" : {"type":"java.lang.Long", "value":"1"}}
     *      "long-array-param" : {"type":"java.lang.Long", "value":["1"]}}
     *  }
     * }
     * 
     * @param component the component annotation which contains a "properties" attribute. The component can be either a @Component, or an aspect, or an adapter.
     * @param writer the object where the parsed attributes are written.
     */
    private void parseProperties(Annotation component, EntryWriter writer)
    {            
        Object[] properties = component.get(EntryParam.properties.toString());
        if (properties != null)
        {
            for (Object property : properties)
            {
                Annotation propertyAnnotation = (Annotation) property;
                parseProperty(propertyAnnotation, writer);
            }             
        }
    }
    
    /**
     * Parses a Property annotation. The result is added to the associated writer object
     * @param annotation the @Property annotation.
     * @param writer the writer object where the parsed property will be added to.
     */
    private void parseProperty(Annotation property, EntryWriter writer)
    {
        EntryParam attribute = EntryParam.properties;
        try
        {
            JSONObject properties = writer.getJsonObject(attribute);
            if (properties == null) {
                properties = new JSONObject();
            }
                        
            String name = (String) property.get("name");
            String type = parseClassAttrValue(property.get("type"));
            Class<?> classType;
            try
            {
                classType = (type == null) ? String.class : Class.forName(type);
            }
            catch (ClassNotFoundException e)
            {
                // Theorically impossible
                throw new IllegalArgumentException("Invalid Property type " + type
                    + " from annotation " + property + " in class " + m_className);
            }

            Object[] values;

            if ((values = property.get("value")) != null)
            {
                values = checkPropertyType(name, classType, values);
                addProperty(properties, name, values, classType);
            }
            else if ((values = property.get("values")) != null)
            { // deprecated
                values = checkPropertyType(name, classType, values);
                addProperty(properties, name, values, classType);
            }
            else if ((values = property.get("longValue")) != null)
            {
                addProperty(properties, name, values, Long.class);
            }
            else if ((values = property.get("doubleValue")) != null)
            {
                addProperty(properties, name, values, Double.class);
            }
            else if ((values = property.get("floatValue")) != null)
            {
                addProperty(properties, name, values, Float.class);
            }
            else if ((values = property.get("intValue")) != null)
            {
                addProperty(properties, name, values, Integer.class);
            }
            else if ((values = property.get("byteValue")) != null)
            {
                addProperty(properties, name, values, Byte.class);
            }
            else if ((values = property.get("charValue")) != null)
            {
                addProperty(properties, name, values, Character.class);
            }
            else if ((values = property.get("booleanValue")) != null)
            {
                addProperty(properties, name, values, Boolean.class);
            }
            else if ((values = property.get("shortValue")) != null)
            {
                addProperty(properties, name, values, Short.class);
            }
            else
            {
                throw new IllegalArgumentException(
                    "Missing Property value from annotation " + property + " in class " + m_className);
            }

            if (properties.length() > 0) {
                writer.putJsonObject(attribute, properties);
            }
        }
        catch (JSONException e)
        {
            throw new IllegalArgumentException("Unexpected exception while parsing Property from class " + m_className, e);
        }
    }

    /**
     * Checks if a property contains values that are compatible with a give primitive type.
     * 
     * @param name the property name
     * @param values the values for the property name
     * @param type the primitive type.
     * @return the same property values, possibly modified if the type is 'Character' (the strings are converted to their character integer values). 
     */
    private Object[] checkPropertyType(String name, Class<?> type, Object ... values)
    {
        if (type.equals(String.class))
        {
            return values;
        } else if (type.equals(Long.class)) {
            for (Object value : values) {
                try {
                    Long.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Long value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Double.class)) {
            for (Object value : values) {
                try {
                    Double.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Double value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Float.class)) {
            for (Object value : values) {
                try {
                    Float.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Float value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Integer.class)) {
            for (Object value : values) {
                try {
                    Integer.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Integer value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Byte.class)) {
            for (Object value : values) {
                try {
                    Byte.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Byte value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Character.class)) {
            for (int i = 0; i < values.length; i++)
            {
                try
                {
                    // If the string is already an integer, don't modify it
                    Integer.valueOf(values[i].toString());
                }
                catch (NumberFormatException e)
                {
                    // Converter the character string to its corresponding integer code.
                    if (values[i].toString().length() != 1)
                    {
                        throw new IllegalArgumentException("Property \"" + name + "\" in class "
                            + m_className + " does not contain a valid Character value (" + values[i] + ")");
                    }
                    try
                    {
                        values[i] = Integer.valueOf(values[i].toString().charAt(0));
                    }
                    catch (NumberFormatException e2)
                    {
                        throw new IllegalArgumentException("Property \"" + name + "\" in class "
                            + m_className + " does not contain a valid Character value (" + values[i].toString()
                            + ")");
                    }
                }
            }
        } else if (type.equals(Boolean.class)) {
            for (Object value : values) {
                if (! value.toString().equalsIgnoreCase("false") && ! value.toString().equalsIgnoreCase("true")) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Boolean value (" + value.toString() + ")");
                }
            }
        } else if (type.equals(Short.class)) {
            for (Object value : values) {
                try {
                    Short.valueOf(value.toString());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Property \"" + name + "\" in class " + m_className
                        + " does not contain a valid Short value (" + value.toString() + ")");
                }
            }
        }

        return values;
    }

    /**
     * Adds a key/value(s) pair in a properties map
     * @param properties the target properties map
     * @param name the property name
     * @param value the property value(s)
     * @param type the property type
     * @throws JSONException 
     */
    private void addProperty(JSONObject props, String name, Object value, Class<?> type) throws JSONException {
        if (value.getClass().isArray())
        {
            Object[] array = (Object[]) value;
            if (array.length == 1)
            {
                value = array[0];
            }
        }

        if (type.equals(String.class))
        {
            props.put(name, value.getClass().isArray() ? new JSONArray(value) : value);
        }
        else
        {
            JSONObject jsonValue = new JSONObject();
            jsonValue.put("type", type.getName());
            jsonValue.put("value", value.getClass().isArray() ? new JSONArray(value) : value);
            props.put(name, jsonValue);
        }
    }

    /**
     * Parse Inject annotation, used to inject some special classes in some fields
     * (BundleContext/DependencyManager etc ...)
     * @param annotation the Inject annotation
     */
    private void parseInject(Annotation annotation)
    {      
        if (Patterns.BUNDLE_CONTEXT.matcher(m_descriptor).matches())
        {
            m_bundleContextField = m_field;
        }
        else if (Patterns.DEPENDENCY_MANAGER.matcher(m_descriptor).matches())
        {
            m_dependencyManagerField = m_field;
        }
        else if (Patterns.COMPONENT.matcher(m_descriptor).matches())
        {
            m_componentField = m_field;
        }
        else
        {
            throw new IllegalArgumentException("@Inject annotation can't be applied on the field \"" + m_field
                                               + "\" in class " + m_className);
        }
    }
    
    /**
     * This method checks if the @Registered and/or @Unregistered annotations have been defined
     * while they should not, because the component does not provide a service.
     */
    private void checkRegisteredUnregisteredNotPresent()
    {
        if (m_registeredMethod != null)
        {
            throw new IllegalStateException("@Registered annotation can't be used on a Component " +
                                            " which does not provide a service (class=" + m_className + ")");

        }
        
        if (m_unregisteredMethod != null)
        {
            throw new IllegalStateException("@Unregistered annotation can't be used on a Component " +
                                            " which does not provide a service (class=" + m_className + ")");

        }
    }

    /**
     * Get an annotation attribute, and return a default value if its not present.
     * @param <T> the type of the variable which is assigned to the return value of this method.
     * @param annotation The annotation we are parsing
     * @param name the attribute name to get from the annotation
     * @param defaultValue the default value to return if the attribute is not found in the annotation
     * @return the annotation attribute value, or the defaultValue if not found
     */
    @SuppressWarnings("unchecked")
    private <T> T get(Annotation annotation, String name, T defaultValue)
    {
        T value = (T) annotation.get(name);
        return value != null ? value : defaultValue;
    }
}