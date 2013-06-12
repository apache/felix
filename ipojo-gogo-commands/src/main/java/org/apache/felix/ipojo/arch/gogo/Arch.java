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
package org.apache.felix.ipojo.arch.gogo;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.service.command.Descriptor;

/**
 * iPOJO Arch command giving information about the current
 * system architecture. This is a Gogo command.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(public_factory = false, immediate = true)
@Instantiate
@Provides(specifications = Arch.class)
public class Arch {
    
    /**
     * Defines the command scope (ipojo).
     */
    @ServiceProperty(name = "osgi.command.scope", value = "ipojo")
    String m_scope;
    
    /**
     * Defines the functions (commands). 
     */
    @ServiceProperty(name = "osgi.command.function", value = "{}")
    String[] m_function = new String[] {
        "instances",
        "instance",
        "factory",
        "component",
        "factories",
        "components",
        "handlers",
        "extensions"
    };
    
    /**
     * Instance architecture services.
     */
    @Requires(optional = true)
    private Architecture[] m_archs;
    
    /**
     * Factory services.
     */
    @Requires(optional = true)
    private Factory[] m_factories;
    
    /**
     * Handler Factory services.
     */
    @Requires(optional = true)
    private HandlerFactory[] m_handlers;

    /**
     * The instance declaration services.
     */
    @Requires(optional = true)
    private InstanceDeclaration[] m_instances;

    /**
     * The type declaration services.
     */
    @Requires(optional = true)
    private TypeDeclaration[] m_types;

    /**
     * The extension declaration services.
     */
    @Requires(optional = true)
    private ExtensionDeclaration[] m_extensions;

    /**
     * Displays iPOJO instances.
     */
    @Descriptor("Display iPOJO instances")
    public void instances() {
        StringBuilder buffer = new StringBuilder();
        for (Architecture m_arch : m_archs) {
            InstanceDescription instance = m_arch.getInstanceDescription();
            if (instance.getState() == ComponentInstance.VALID) {
                buffer.append(format("Instance %s -> valid%n", instance.getName()));
            }
            if (instance.getState() == ComponentInstance.INVALID) {
                buffer.append(format("Instance %s -> invalid%n", instance.getName()));
            }
            if (instance.getState() == ComponentInstance.STOPPED) {
                buffer.append(format("Instance %s -> stopped%n", instance.getName()));
            }
        }

        for (InstanceDeclaration instance : m_instances) {
            // Only print unbound instances (others already printed above)
            if (!instance.getStatus().isBound()) {
                buffer.append(format("Instance %s of type %s is not bound.%n",
                        name(instance.getConfiguration()),
                        instance.getConfiguration().get("component")));
                buffer.append(format("  Reason: %s", instance.getStatus().getMessage()));
                buffer.append("\n");
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No instances \n");
        }
        
        System.out.println(buffer.toString());   
    }

    private String name(Dictionary<String, Object> configuration) {
        String name = (String) configuration.get("instance.name");
        if (name == null) {
            name = "unnamed";
        }
        return name;
    }

    /**
     * Displays the architecture of a specific instance.
     * @param instance the instance name
     */
    @Descriptor("Display the architecture of a specific instance")
    public void instance(@Descriptor("target instance name") String instance) {

        StringBuilder sb = new StringBuilder();

        for (Architecture m_arch : m_archs) {
            InstanceDescription id = m_arch.getInstanceDescription();
            if (id.getName().equalsIgnoreCase(instance)) {
                sb.append(id.getDescription());
                sb.append('\n');
            }
        }

        for (InstanceDeclaration instanceDeclaration : m_instances) {
            if (!instanceDeclaration.getStatus().isBound()) {
                if (instance.equals(name(instanceDeclaration.getConfiguration()))) {
                    sb.append(format("InstanceDeclaration %s not bound to its factory%n", instance));
                    sb.append(format(" type: %s%n", instanceDeclaration.getComponentName()));
                    sb.append(format(" reason: %s%n", instanceDeclaration.getStatus().getMessage()));
                    Throwable throwable = instanceDeclaration.getStatus().getThrowable();
                    if (throwable != null) {
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        throwable.printStackTrace(new PrintStream(os));
                        sb.append(" throwable: ");
                        sb.append(os.toString());
                    }
                }
            }
        }

        if (sb.length() == 0) {
            System.err.printf("Instance named '%s' not found", instance);
        } else {
            System.out.print(sb);
        }

    }

    /**
     * Displays the information about a specific factory.
     * Note that factory name are not unique, so all matching
     * factories are displayed.
     * @param name the factory name
     */
    @Descriptor("Display the information about a specific factory / component")
    public void component(@Descriptor("target factory name") String name) {
        factory(name);
    }
    
    /**
     * Displays the information about a specific factory.
     * Note that factory name are not unique, so all matching
     * factories are displayed.
     * @param name the factory name
     */
    @Descriptor("Display the information about a specific factory")
    public void factory(@Descriptor("target factory name") String name) {

        List<Factory> factories = new ArrayList<Factory>();
        List<TypeDeclaration> types = new ArrayList<TypeDeclaration>();

        // Looking for public factories
        for (Factory factory : m_factories) {
            if (factory.getName().equalsIgnoreCase(name)) {
                factories.add(factory);
            }
        }

        // Looking for all unbound or private bound types
        for (TypeDeclaration type : m_types) {
            if (name.equalsIgnoreCase(type.getComponentName())) {
                // (Public + Unbound) or private types have no exported factories
                if (!type.isPublic() || (!type.getStatus().isBound() && type.isPublic())) {
                    types.add(type);
                }
            }
        }

        if (factories.isEmpty() && types.isEmpty()) {
            System.err.println("Factory " + name + " not found");
            return;
        }

        // Display found factories and types
        for (Factory factory : factories) {
            System.out.println(factory.getComponentDescription());
        }
        for (TypeDeclaration type : types) {
            if (!type.getStatus().isBound()) {
                // Unbound: maybe private or public type
                System.out.printf("Factory %s is not bound%n", type.getComponentName());
                System.out.printf("  reason: %s%n", type.getStatus().getMessage());
                Throwable throwable = type.getStatus().getThrowable();
                if (throwable != null) {
                    System.out.print("  throwable: ");
                    throwable.printStackTrace(System.out);
                }
            } else {
                // Bound, this is only a private factory
                System.out.printf("Factory %s is bound - Private%n", type.getComponentName());
            }
        }

    }

    /**
     * Displays the list of public iPOJO factories.
     */
    @Descriptor("Display iPOJO factories / components")
    public void components() {
        factories();
    }
    
    /**
     * Displays the list of public iPOJO factories.
     */
    @Descriptor("Display iPOJO factories")
    public void factories() {
        StringBuilder buffer = new StringBuilder();
        for (Factory m_factory : m_factories) {
            if (m_factory.getMissingHandlers().size() == 0) {
                buffer.append(format("Factory %s (VALID)%n", m_factory.getName()));
            } else {
                buffer.append(format("Factory %s (INVALID: %s)%n",
                        m_factory.getName(),
                        m_factory.getMissingHandlers()));
            }
        }

        for (TypeDeclaration type : m_types) {
            if (!type.isPublic()) {
                // Private factories: always display them
                // Cannot display much more than presence/absence since the TypeDeclaration API does not
                // give access to the underlying Factory or description (if valid)
                if (type.getStatus().isBound()) {
                    buffer.append(format("Factory %s (UNKNOWN) - Private%n", type.getComponentName()));
                } else {
                    // Unbound type means that required extension is not available
                    // We'll say that the factory is INVALID even if in reality it's not even instantiated
                    buffer.append(format("Factory %s (INVALID) - Private%n", type.getComponentName()));
                    buffer.append(format("  -> %s", type.getStatus().getMessage()));
                }
            } else {
                if (!type.getStatus().isBound()) {
                    buffer.append(format("Factory %s is not bound%n", type.getComponentName()));
                    buffer.append(format("  -> %s%n", type.getStatus().getMessage()));
                }
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No factories \n");
        }
        
        System.out.println(buffer.toString());
    }

    /**
     * Displays the list of available handlers.
     */
    @Descriptor("Display iPOJO handlers")
    public void handlers() {
        PrintStream out = System.out;
        for (HandlerFactory m_handler : m_handlers) {
            String name = m_handler.getHandlerName();
            if ("composite".equals(m_handler.getType())) {
                name = name + " [composite]";
            }
            if (m_handler.getMissingHandlers().size() == 0) {
                out.println("Handler " + name + " (VALID)");
            } else {
                out.println("Handler " + name + " (INVALID : " + m_handler.getMissingHandlers() + ")");
            }
        }

        for (TypeDeclaration type : m_types) {
            if (!type.getStatus().isBound()) {
                out.println("HandlerFactory " + type.getComponentName() + " is not bound");
                out.println("  -> " + type.getStatus().getMessage());
            }
        }
    }

    /**
     * Displays the list of available extensions.
     */
    @Descriptor("Display iPOJO extensions")
    public void extensions() {
        PrintStream out = System.out;
        out.println("Available extensions:");
        for (ExtensionDeclaration extension : m_extensions) {
            out.println("  * " + extension.getExtensionName());
        }
    }

}
