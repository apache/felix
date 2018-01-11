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
package org.apache.felix.scr.impl;

import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.apache.felix.scr.info.ScrInfo;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.*;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.*;

import java.text.MessageFormat;
import java.util.*;

public class ComponentCommands {

    private static final String INDENT_1 = "  ";
    private static final String INDENT_2 = INDENT_1 + INDENT_1;
    private static final String INDENT_3 = INDENT_2 + INDENT_1;
    private static final String INDENT_4 = INDENT_3 + INDENT_1;

    private final BundleContext context;
    private final ServiceComponentRuntime scr;
    private final ScrConfiguration scrConfig;

    private final Comparator<ComponentConfigurationDTO> configDtoComparator = new Comparator<ComponentConfigurationDTO>() {
        @Override
        public int compare(ComponentConfigurationDTO o1, ComponentConfigurationDTO o2) {
            long diff = o1.id - o2.id;
            return diff == 0L ? 0 : (int) (diff / Math.abs(diff));
        }
    };
    private final Comparator<ServiceReferenceDTO> serviceRefDtoComparator = new Comparator<ServiceReferenceDTO>() {
        @Override
        public int compare(ServiceReferenceDTO o1, ServiceReferenceDTO o2) {
            long diff = o1.id - o2.id;
            return diff == 0L ? 0 : (int) (diff / Math.abs(diff));
        }
    };

    private ServiceRegistration<ComponentCommands> commandsReg = null;
    private ServiceRegistration<?> converterReg = null;
    private ServiceRegistration<ScrInfo> scrInfoReg = null;

    synchronized void register() {
        if (commandsReg != null) {
            throw new IllegalStateException("Component Commands already registered");
        }

        Dictionary<String, Object> svcProps;

        svcProps = new Hashtable<>();
        svcProps.put("osgi.command.scope", "scr");
        svcProps.put("osgi.command.function", new String[] {
                "config",
                "disable",
                "enable",
                "info",
                "list"
        });
        svcProps.put(Constants.SERVICE_DESCRIPTION, "SCR Gogo Shell Support");
        svcProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        commandsReg = context.registerService(ComponentCommands.class, this, svcProps);

        svcProps = new Hashtable<>();
        svcProps.put("osgi.converter.classes", new String[] {
                ComponentDescriptionDTO.class.getName(),
                ComponentConfigurationDTO.class.getName()
        });
        svcProps.put(Constants.SERVICE_DESCRIPTION, "SCR Runtime DTO Converter");
        svcProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        converterReg = context.registerService("org.apache.felix.service.command.Converter", new ComponentConverterFactory(this), svcProps);
    }

    synchronized void unregister() {
        safeUnregister(converterReg);
        safeUnregister(commandsReg);
        safeUnregister(scrInfoReg);
    }

    public synchronized void updateProvideScrInfoService(boolean register) {
        if (register) {
            if (scrInfoReg == null) {
                Dictionary<String, Object> svcProps = new Hashtable<>();
                svcProps.put(Constants.SERVICE_DESCRIPTION, "SCR Info Service");
                svcProps.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
                scrInfoReg = context.registerService(ScrInfo.class, new ComponentCommandsScrInfo(this, context), svcProps);
            }
        } else {
            safeUnregister(scrInfoReg);
            scrInfoReg = null;
        }
    }

    protected ComponentCommands(BundleContext context, ServiceComponentRuntime scr, ScrConfiguration scrConfig) {
        this.context = context;
        this.scr = scr;
        this.scrConfig = scrConfig;
    }

    @Descriptor("List all components")
    public ComponentDescriptionDTO[] list() {
        return scr.getComponentDescriptionDTOs().toArray(new ComponentDescriptionDTO[0]);
    }

    @Descriptor("List components of a specific bundle")
    public ComponentDescriptionDTO[] list(@Descriptor("ID of the bundle") long bundleId) {
        Bundle bundle = context.getBundle(bundleId);
        return bundle != null ? scr.getComponentDescriptionDTOs(bundle).toArray(new ComponentDescriptionDTO[0]) : null;
    }

    private List<ComponentDescriptionDTO> findComponents(String name) {
        String lowerName = name.toLowerCase();
        List<ComponentDescriptionDTO> matches = new LinkedList<>();
        for (ComponentDescriptionDTO dto : scr.getComponentDescriptionDTOs()) {
            if (dto.name.equalsIgnoreCase(name)) {
                // Exact match, return only this component.
                return Collections.singletonList(dto);
            }
            if (dto.name.toLowerCase().contains(lowerName))
                matches.add(dto);
        }
        return matches;
    }

    @Descriptor("Dump information of a component")
    public ComponentDescriptionDTO info(@Descriptor("Name of the component") String name) {
        List<ComponentDescriptionDTO> matches = findComponents(name);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format("No component description matching \"{0}\".", name));
        } else if (matches.size() > 1) {
            StringBuilder partialMatchesStr = new StringBuilder();
            for (Iterator<ComponentDescriptionDTO> iter = matches.iterator(); iter.hasNext(); ) {
                partialMatchesStr.append(iter.next().name);
                if (iter.hasNext()) partialMatchesStr.append(", ");
            }
            throw new IllegalArgumentException(MessageFormat.format("Multiple components matching \"{0}\": [{1}]", name, partialMatchesStr));
        }
        return matches.get(0);
    }

    @Descriptor("Dump information of a component configuration")
    public ComponentConfigurationDTO info(@Descriptor("ID of the component configuration") long id) {
        for (ComponentDescriptionDTO descDto : scr.getComponentDescriptionDTOs()) {
            for (ComponentConfigurationDTO configDto : scr.getComponentConfigurationDTOs(descDto)) {
                if (configDto.id == id)
                    return configDto;
            }
        }
        return null;
    }

    @Descriptor("Enable a disabled component")
    public boolean enable(@Descriptor("Name of the component") final String name) {
        boolean changed = false;
        for (ComponentDescriptionDTO comp : findComponents(name)) {
            if (!scr.isComponentEnabled(comp)) {
                scr.enableComponent(comp);
                changed = true;
            }
        }
        return changed;
    }

    @Descriptor("Disable an enabled component")
    public boolean disable(@Descriptor("Name of the component") final String name) {
        boolean changed = false;
        for (ComponentDescriptionDTO comp : findComponents(name)) {
            if (scr.isComponentEnabled(comp)) {
                scr.disableComponent(comp);
                changed = true;
            }
        }
        return changed;
    }

    @Descriptor("Show the current SCR configuration")
    public String config() {
        Map<String,String> out = new LinkedHashMap<>();
        out.put("Log Level", Integer.toString(scrConfig.getLogLevel()));
        out.put("Obsolete Component Factory with Factory Configuration", Boolean.toString(scrConfig.isFactoryEnabled()));
        out.put("Keep instances with no references", scrConfig.keepInstances() ? "Supported" : "Unsupported");
        out.put("Lock timeout ms", Long.toString(scrConfig.lockTimeout()));
        out.put("Stop timeout ms", Long.toString(scrConfig.stopTimeout()));
        out.put("Global extender", Boolean.toString(scrConfig.globalExtender()));
        out.put("Info Service registered", scrConfig.infoAsService() ? "Supported" : "Unsupported");

        StringBuilder builder = new StringBuilder();
        printColumnsAligned("SCR Configuration", out, '=', builder);
        return builder.toString();
    }

    public Object convert(Class<?> desiredType, Object in) throws Exception {
        throw new UnsupportedOperationException("Not implemented");
    }

    public CharSequence format(Object target, int level) throws Exception {
        final CharSequence result;
        if (target instanceof ComponentDescriptionDTO[]) {
            result = format((ComponentDescriptionDTO[]) target, level);
        } else if (target instanceof ComponentDescriptionDTO) {
            result = format((ComponentDescriptionDTO) target, level);
        } else if (target instanceof ComponentConfigurationDTO) {
            result = format((ComponentConfigurationDTO) target, level);
        } else {
            result = null;
        }
        return result;
    }

    CharSequence format(ComponentDescriptionDTO[] dtoArray, int level) throws Exception {
        StringBuilder sb = new StringBuilder();
        if (dtoArray == null || dtoArray.length == 0) {
            sb.append("No component descriptions found");
        } else {
            for (int i = 0; i < dtoArray.length; i++) {
                if (i > 0) sb.append('\n');
                sb.append(format(dtoArray[i], Converter.LINE));
            }
        }
        return sb;
    }

    CharSequence format(ComponentDescriptionDTO dto, int level) throws Exception {
        final StringBuilder builder = new StringBuilder();

        // Get the child ComponentConfigurationDTOs and sort by id field.
        final List<ComponentConfigurationDTO> children;
        Collection<ComponentConfigurationDTO> childrenTmp = scr.getComponentConfigurationDTOs(dto);
        if (childrenTmp == null) {
            children = Collections.emptyList();
        } else {
            children = new ArrayList<>(childrenTmp);
            Collections.sort(children, configDtoComparator);
        }

        switch (level) {
            case Converter.LINE:
                builder.append(MessageFormat.format("{0} in bundle {1} ({2}:{3}) {4}, {5,choice,0#0 instances|1#1 instance|1<{5} instances}.",
                        dto.name,
                        dto.bundle.id,
                        dto.bundle.symbolicName,
                        dto.bundle.version,
                        dto.defaultEnabled ? "enabled" : "disabled",
                        children.size()
                ));

                for (ComponentConfigurationDTO child : children)
                    builder.append("\n").append(INDENT_2).append(format(child, Converter.LINE));
                break;
            case Converter.INSPECT:
                printComponentDescriptionAndConfigs(dto, children.toArray(new ComponentConfigurationDTO[0]), builder);
                break;
            case Converter.PART:
                break;
        }
        return builder;
    }

    CharSequence format(ComponentConfigurationDTO dto, int level) throws Exception {
        final StringBuilder builder = new StringBuilder();
        switch (level) {
            case Converter.INSPECT:
                printComponentDescriptionAndConfigs(dto.description, new ComponentConfigurationDTO[]{dto}, builder);
                break;
            case Converter.LINE:
                builder.append("Id: ").append(dto.id);
                builder.append(", ").append("State:").append(stateToString(dto.state));
                String[] pids = getStringArray(dto.properties, Constants.SERVICE_PID, null);
                if (pids != null && pids.length > 0) {
                    builder.append(", ").append("PID(s): ").append(Arrays.toString(pids));
                }
                break;
            case Converter.PART:
                break;
        }
        return builder;
    }

    void printComponentDescriptionAndConfigs(ComponentDescriptionDTO descDto, ComponentConfigurationDTO[] configs, StringBuilder builder) {
        final Map<String, String> out = new LinkedHashMap<>();

        // Component Description
        out.put("Class", descDto.implementationClass);
        out.put("Bundle", String.format("%d (%s:%s)", descDto.bundle.id, descDto.bundle.symbolicName, descDto.bundle.version));
        out.put("Enabled", Boolean.toString(descDto.defaultEnabled));
        out.put("Immediate", Boolean.toString(descDto.immediate));
        out.put("Services", arrayToString(descDto.serviceInterfaces));
        if (descDto.scope != null) {
            out.put("Scope", descDto.scope);
        }
        out.put("Config PID(s)", String.format("%s, Policy: %s", arrayToString(descDto.configurationPid), descDto.configurationPolicy));
        out.put("Base Props", printProperties(descDto.properties, INDENT_1));
        if (descDto.factory != null) {
            out.put("Factory", descDto.factory);
            try {
                ServiceReference<?>[] serviceRefs = context.getAllServiceReferences(ComponentFactory.class.getName(), String.format("(&(%s=%s)(%s=%d))", ComponentConstants.COMPONENT_NAME, descDto.name, Constants.SERVICE_BUNDLEID, descDto.bundle.id));
                if (serviceRefs != null && serviceRefs.length > 0) {
                    out.put("Factory Service", printPublishedServices(serviceRefs));
                }
            } catch (InvalidSyntaxException e) {
                // shouldn't happen
            }
        }
        printColumnsAligned(String.format("Component Description: %s", descDto.name), out, '=', builder);

        if (configs != null) for (ComponentConfigurationDTO configDto : configs) {
            out.clear();

            // Blank line separator
            builder.append("\n\n");

            // Inspect configuration DTO
            String title = String.format("Component Configuration Id: %d", configDto.id);
            out.put("State", stateToString(configDto.state));

            // Print service registration
            try {
                ServiceReference<?>[] serviceRefs = context.getAllServiceReferences(null, String.format("(%s=%d)", ComponentConstants.COMPONENT_ID, configDto.id));
                if (serviceRefs != null && serviceRefs.length > 0) {
                    out.put("Service", printPublishedServices(serviceRefs));
                }
            } catch (InvalidSyntaxException e) {
                // Shouldn't happen...
            }

            // Print Configuration Properties
            out.put("Config Props", printProperties(configDto.properties, INDENT_1));

            // Print References
            out.put("References", printServiceReferences(configDto.satisfiedReferences, configDto.unsatisfiedReferences, descDto.references));

            // Print Failure
            if (configDto.failure != null) {
                out.put("Failure", configDto.failure);
            }
            printColumnsAligned(title, out, '-', builder);
        }
    }

    String printPublishedServices(ServiceReference<?>[] serviceRefs) {
        StringBuilder sb = new StringBuilder();

        if (serviceRefs.length > 1) {
            sb.append("(total ").append(serviceRefs.length).append(')');
            sb.append('\n').append(INDENT_1);
        }

        for (ServiceReference<?> serviceRef : serviceRefs) {
            sb.append(serviceRef.getProperty(Constants.SERVICE_ID));
            sb.append(' ').append(Arrays.toString((String[]) serviceRef.getProperty(Constants.OBJECTCLASS)));
            Bundle[] consumers = serviceRef.getUsingBundles();
            if (consumers != null) for (Bundle consumer : consumers) {
                sb.append("\n").append(INDENT_2);
                sb.append(String.format("Used by bundle %d (%s:%s)", consumer.getBundleId(), consumer.getSymbolicName(), consumer.getVersion()));
            }
        }

        return sb.toString();
    }

    private String arrayToString(String[] array) {
        return array == null || array.length == 0 ? "<<none>>" : Arrays.toString(array);
    }

    static final String stateToString(int state) {
        final String string;
        switch (state) {
            case ComponentConfigurationDTO.ACTIVE:
                string = "ACTIVE";
                break;
            case ComponentConfigurationDTO.SATISFIED:
                string = "SATISFIED";
                break;
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                string = "UNSATISFIED CONFIGURATION";
                break;
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                string = "UNSATISFIED REFERENCE";
                break;
            case ComponentConfigurationDTO.FAILED_ACTIVATION:
                string = "FAILED ACTIVATION";
                break;
            default:
                string = String.format("<<UNKNOWN: %d>>", state);
        }
        return string;
    }

    static String printProperties(Map<String, ?> props, String indent) {
        StringBuilder builder = new StringBuilder();
        int size = props.size();
        builder.append('(').append(Integer.toString(size)).append(' ').append(size == 1 ? "entry" : "entries").append(')');
        if (size > 0) {
            final SortedMap<String, ?> sortedMap = new TreeMap<>(props);
            for (Map.Entry<String, ?> e : sortedMap.entrySet()) {
                builder.append('\n').append(indent);

                final Object value = e.getValue();
                final String typeName;
                final String valueStr;

                if (value == null) {
                    typeName = valueStr = "null";
                } else {
                    typeName = value.getClass().getSimpleName();
                    if (value instanceof int[])
                        valueStr = Arrays.toString((int[]) value);
                    else if (value instanceof long[])
                        valueStr = Arrays.toString((long[]) value);
                    else if (value instanceof byte[])
                        valueStr = Arrays.toString((byte[]) value);
                    else if (value instanceof short[])
                        valueStr = Arrays.toString((short[]) value);
                    else if (value instanceof byte[])
                        valueStr = Arrays.toString((byte[]) value);
                    else if (value instanceof char[])
                        valueStr = Arrays.toString((char[]) value);
                    else if (value instanceof boolean[])
                        valueStr = Arrays.toString((boolean[]) value);
                    else if (value instanceof float[])
                        valueStr = Arrays.toString((boolean[]) value);
                    else if (value instanceof double[])
                        valueStr = Arrays.toString((boolean[]) value);
                    else if (value instanceof Object[])
                        valueStr = Arrays.deepToString((Object[]) value);
                    else
                        valueStr = value.toString();
                }
                builder.append(String.format("%s<%s> = %s", e.getKey(), typeName, valueStr));
            }
        }
        return builder.toString();
    }

    String printServiceReferences(SatisfiedReferenceDTO[] satisfiedReferences, UnsatisfiedReferenceDTO[] unsatisfiedReferences, ReferenceDTO[] references) {
        StringBuilder builder = new StringBuilder();
        final Map<String, ReferenceDTO> refDtoMap = new HashMap<>();
        if (references != null) {
            for (ReferenceDTO refDto : references)
                refDtoMap.put(refDto.name, refDto);
        }
        int refCount = (satisfiedReferences != null ? satisfiedReferences.length : 0)
                + (unsatisfiedReferences != null ? unsatisfiedReferences.length : 0);
        builder.append("(total ").append(Integer.toString(refCount)).append(")");
        if (unsatisfiedReferences != null) {
            for (UnsatisfiedReferenceDTO refDto : unsatisfiedReferences)
                printServiceReference(refDtoMap.get(refDto.name), "UNSATISFIED", null, builder);
        }
        if (satisfiedReferences != null) {
            for (SatisfiedReferenceDTO refDto : satisfiedReferences)
                printServiceReference(refDtoMap.get(refDto.name), "SATISFIED", refDto.boundServices != null ? refDto.boundServices : new ServiceReferenceDTO[0], builder);
        }
        return builder.toString();
    }

    void printServiceReference(ReferenceDTO reference, String state, ServiceReferenceDTO[] bindings, StringBuilder builder) {
        StringBuilder policyWithOption = new StringBuilder().append(reference.policy);
        if (!"reluctant".equals(reference.policyOption))
            policyWithOption.append('+').append(reference.policyOption);

        builder.append(String.format("%n" + INDENT_1 + "- %s: %s %s %s %s", reference.name, reference.interfaceName, state, reference.cardinality, policyWithOption));
        builder.append(String.format("%n" + INDENT_1 + "  target=%s scope=%s", reference.target == null ? "(*)" : reference.target, reference.scope == null ? "bundle" : reference.scope));
        if (reference.collectionType != null) {
            builder.append(" collectionType=").append(reference.collectionType);
        }

        if (bindings != null) {
            Arrays.sort(bindings, serviceRefDtoComparator);
            builder.append(MessageFormat.format(" {0,choice,0#(no active bindings)|1#(1 binding):|1<({0} bindings):}", bindings.length));
            for (ServiceReferenceDTO svcDto : bindings) {
                Bundle provider = context.getBundle(svcDto.bundle);
                builder.append(String.format("%n" + INDENT_1 + "  * Bound to [%d] from bundle %d (%s:%s), props: ", svcDto.id, svcDto.bundle, provider.getSymbolicName(), provider.getVersion()));
            }
        }
    }

    static void printColumnsAligned(String title, Map<String, String> properties, char underlineChar, StringBuilder builder) {
        builder.append(title);

        // Generate the title underline
        char[] carray = new char[title.length()];
        Arrays.fill(carray, underlineChar);
        builder.append('\n');
        builder.append(carray);

        int widestKey = 0;
        for (String key : properties.keySet()) {
            widestKey = Math.max(widestKey, key.length());
        }

        for (Map.Entry<String, String> e : properties.entrySet()) {
            String key = e.getKey();
            int padLength = widestKey - key.length();
            char[] padding = new char[padLength];
            Arrays.fill(padding, ' ');

            builder.append('\n');
            builder.append(key).append(": ");
            builder.append(padding);
            builder.append(e.getValue());
        }
    }

    private static String[] getStringArray(Map<String, ?> map, String name, String[] defaultValue) throws IllegalArgumentException {
        Object o = map.get(name);
        if (o instanceof String) {
            return new String[]{(String) o};
        } else if (o instanceof String[]) {
            return (String[]) o;
        } else if (o instanceof Collection) {
            Collection<?> c = (Collection<?>) o;
            if (c.isEmpty()) {
                return new String[0];
            } else {
                String[] a = new String[c.size()];
                Iterator<?> iter = c.iterator();
                for (int i = 0; i < a.length; i++) {
                    Object elem = iter.next();
                    if (!(elem instanceof String))
                        throw new IllegalArgumentException(String.format("Collection value for field '%s' contains non-String element at index %d.", name, i));
                    a[i] = (String) elem;
                }
                return a;
            }
        } else if (o == null) {
            return defaultValue;
        } else {
            throw new IllegalArgumentException(String.format("Value for field '%s' is not a String, String-array or Collection of String. Actual type was %s.", name, o.getClass().getName()));
        }
    }

    private void safeUnregister(ServiceRegistration<?> registration) {
        try {
            if (registration != null) registration.unregister();
        } catch (Exception e) {
            // ignore
        }
    }

}
