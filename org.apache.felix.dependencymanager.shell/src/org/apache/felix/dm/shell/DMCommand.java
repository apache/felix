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
package org.apache.felix.dm.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.diagnostics.CircularDependency;
import org.apache.felix.dm.diagnostics.DependencyGraph;
import org.apache.felix.dm.diagnostics.DependencyGraph.ComponentState;
import org.apache.felix.dm.diagnostics.DependencyGraph.DependencyState;
import org.apache.felix.dm.diagnostics.MissingDependency;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Shell command for showing all services and dependencies that are managed
 * by the dependency manager.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Descriptor("Commands used to dump all existing Dependency Manager components")
public class DMCommand {
    /**
     * Bundle context used to create OSGi filters.
     */
    private final BundleContext m_context;
        
    /**
     * Comparator used to compare component declarations based on their bundle ids
     */
    private static final ComponentDeclarationComparator COMPONENT_DECLARATION_COMPARATOR = new ComponentDeclarationComparator();

    /**
     * Constant used by the wtf command, when listing missing services.
     */
    private static final String SERVICE = "service";
    
    /**
     * Constant used by the wtf command, when listing missing configurations.
     */
    private static final String CONFIGURATION = "configuration";
    
    /**
     * Constant used by the wtf command, when listing missing resource dependencies
     */
    private static final String RESOURCE = "resource";
    
    /**
     * Constant used by the wtf command, when listing missing bundle dependencies
     */
    private static final String BUNDLE = "bundle";

    /**
     * Name of a specific gogo shell variable, which may be used to configure "compact" mode.
     * Example: g! dependencymanager.compact=true
     */
    private final static String ENV_COMPACT = "dependencymanager.compact";
    
    /**
     * Name of a specific gogo shell variable, which may be used to configure an OSGi filter, normally
     * passed to the "dm services" option. It is used to display only some service providing components
     * matching the given filter. The filter can contain an "objectClass" option.
     * Example: 
     *   g! dependencymanager.services="(protocol=http)"
     *   g! dependencymanager.services="(&(objectClass=foo.Bar)(protocol=http))"
     */
    private final static String ENV_SERVICES = "dependencymanager.services";

    /**
     * Name of a specific gogo shell variable, which may be used to configure a filter on the
     * component implementation class name.
     * The value of this shell variable may contain multiple regex (space separated), and each regex can
     * be negated using "!".
     * Example: g! dependencymanager.components="foo.bar.* ga.bu.zo.*"
     */
    private final static String ENV_COMPONENTS = "dependencymanager.components";
        
    /**
     * Constructor.
     */
    public DMCommand(BundleContext context) {
        m_context = context;
    }

    /**
     * Dependency Manager "dm" command. We use gogo annotations, in order to automate documentation,
     * and also to automatically manage optional flags/options and parameters ordering.
     * 
     * @param session the gogo command session, used to get some variables declared in the shell
     *        This parameter is automatically passed by the gogo runtime.
     * @param nodeps false means that dependencies are not displayed
     * @param compact true means informations are displayed in a compact format. This parameter can also be 
     *        set using the "dependencymanager.compact" gogo shell variable.
     * @param notavail only unregistered components / unavailable dependencies are displayed 
     * @param stats true means some statistics are displayed
     * @param services an osgi filter used to filter on some given osgi service properties.  This parameter can also be 
     *        set using the "dependencymanager.services" gogo shell variable.
     * @param components a regular expression to match either component implementation class names.  This parameter can also be 
     *        set using the "dependencymanager.components" gogo shell variable.
     * @param componentIds only components matching one of the specified components ids are displayed
     * @param bundleIds a list of bundle ids or symbolic names, used to filter on some given bundles
     */
    @Descriptor("List dependency manager components")
    public void dm(
            CommandSession session,

            @Descriptor("Hides component dependencies") 
            @Parameter(names = {"nodeps", "nd"}, presentValue = "true", absentValue = "false") 
            boolean nodeps,

            @Descriptor("Displays components using a compact form") 
            @Parameter(names = {"compact", "cp"}, presentValue = "true", absentValue = "") 
            String compact,
            
            @Descriptor("Only displays unavailable components") 
            @Parameter(names = {"notavail", "na"}, presentValue = "true", absentValue = "false") 
            boolean notavail,

            @Descriptor("Detects where are the root failures") 
            @Parameter(names = {"wtf"}, presentValue = "true", absentValue = "false") 
            boolean wtf,

            @Descriptor("Displays components statistics") 
            @Parameter(names = {"stats", "stat", "st"}, presentValue = "true", absentValue = "false") 
            boolean stats,

            @Descriptor("<OSGi filter used to filter some service properties>") 
            @Parameter(names = {"services", "s"}, absentValue = "") 
            String services,

            @Descriptor("<Regex(s) used to filter on component implementation class names (comma separated), can be negated using \"!\" prefix>") 
            @Parameter(names = {"components", "c"}, absentValue = "") 
            String components,
            
            @Descriptor("<List of component identifiers to display (comma separated)>") 
            @Parameter(names = {"componentIds", "cid", "ci"}, absentValue = "") 
            String componentIds,

            @Descriptor("<List of bundle ids or bundle symbolic names to display (comma separated)>") 
            @Parameter(names = {"bundleIds", "bid", "bi", "b"}, absentValue = "") 
            String bundleIds,
            
            @Descriptor("<Max number of top components to display (0=all)> This command displays components callbacks (init/start) times>") 
            @Parameter(names = {"top"}, absentValue = "-1") 
            int top) throws Throwable
        {
        
        boolean comp = Boolean.parseBoolean(getParam(session, ENV_COMPACT, compact));
        services = getParam(session, ENV_SERVICES, services);
        String[] componentsRegex = getParams(session, ENV_COMPONENTS, components);
        ArrayList<String> bids = new ArrayList<String>(); // list of bundle ids or bundle symbolic names
        ArrayList<Long> cids = new ArrayList<Long>(); // list of component ids
        
        // Parse and check componentIds option
        StringTokenizer tok = new StringTokenizer(componentIds, ", ");
        while (tok.hasMoreTokens()) {
            try {
                cids.add(Long.parseLong(tok.nextToken()));
            } catch (NumberFormatException e) {
                System.out.println("Invalid value for componentIds option");
                return;
            }
        }

        // Parse services filter
        Filter servicesFilter = null;
        try {
            if (services != null) {
                servicesFilter = m_context.createFilter(services);
            }
        } catch (InvalidSyntaxException e) {
            System.out.println("Invalid services OSGi filter: " + services);
            e.printStackTrace(System.err);
            return;
        }

        // Parse and check bundleIds option
        tok = new StringTokenizer(bundleIds, ", ");
        while (tok.hasMoreTokens()) {
            bids.add(tok.nextToken());
        }
        
        if (top != -1) {
            showTopComponents(top);
            return;
        }
        
        if (wtf) {
            wtf();
            return;
        }
        
        DependencyGraph graph = null;
        if(notavail) {
        	graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED, DependencyState.ALL_UNAVAILABLE);
        } else {
        	graph = DependencyGraph.getGraph(ComponentState.ALL, DependencyState.ALL);
        }
        
        List<ComponentDeclaration> allComponents = graph.getAllComponents();
        Collections.sort(allComponents, COMPONENT_DECLARATION_COMPARATOR);
        long numberOfComponents = 0;
        long numberOfDependencies = 0;
        long lastBundleId = -1;
        
        for(ComponentDeclaration cd : allComponents) {
        	Bundle bundle = cd.getBundleContext().getBundle();
        	if(!matchBundle(bundle, bids)) {
        		continue;
        	}
        	
        	Component component = (Component)cd;
        	String name = cd.getName();
        	if (!mayDisplay(component, servicesFilter, componentsRegex, cids)) {
                continue;
            }
        	
        	numberOfComponents++;
    		long bundleId = bundle.getBundleId();
    		if(lastBundleId != bundleId) {
    			lastBundleId = bundleId;
    			if (comp) {
                    System.out.println("[" + bundleId + "] " + compactName(bundle.getSymbolicName()));
                } else {
                    System.out.println("[" + bundleId + "] " + bundle.getSymbolicName());
                }
    		}
    		if (comp) {
                System.out.print(" [" + cd.getId() + "] " + compactName(name) + " "
                        + compactState(ComponentDeclaration.STATE_NAMES[cd.getState()]));
            } else {
                System.out.println(" [" + cd.getId() + "] " + name + " "
                        + ComponentDeclaration.STATE_NAMES[cd.getState()]);
            }
    		
    		if(!nodeps) {
    			List<ComponentDependencyDeclaration> dependencies = graph.getDependecies(cd);
    			if(!dependencies.isEmpty()) {
    				numberOfDependencies += dependencies.size();
    				if (comp) {
                        System.out.print('(');
                    }
    				for(int j = 0; j < dependencies.size(); j ++) {
        				ComponentDependencyDeclaration dep = dependencies.get(j);
        				
        				String depName = dep.getName();
                        String depType = dep.getType();
                        int depState = dep.getState();

                        if (comp) {
                            if (j > 0) {
                                System.out.print(' ');
                            }
                            System.out.print(compactName(depName) + " " + compactState(depType) + " "
                                    + compactState(ComponentDependencyDeclaration.STATE_NAMES[depState]));
                        } else {
                            System.out.println("    " + depName + " " + depType + " "
                                    + ComponentDependencyDeclaration.STATE_NAMES[depState]);
                        }

        			}
                    if (comp) {
                        System.out.print(')');
                    }
    			}
    		}
    		if (comp) {
                System.out.println();
            }
        }
        
        if(stats) {
        	System.out.println("Statistics:");
        	System.out.println(" - Dependency managers: " + DependencyManager.getDependencyManagers().size());
        	System.out.println(" - Components: " + numberOfComponents);
            if (!nodeps) {
                System.out.println(" - Dependencies: " + numberOfDependencies);
            }
        }

        }

    /**
     * Displays components callbacks (init/start/stop/destroy) elapsed time.
     * The components are sorted (the most time consuming components are displayed first).
     * @param max the max number of components to display (0 means all components)
     */
    private void showTopComponents(int max) {
        List<Component> components = new ArrayList<>();
        for (DependencyManager manager : DependencyManager.getDependencyManagers()) {
           components.addAll(manager.getComponents()); 
        }
        Collections.sort(components, new Comparator<Component>() {
            @Override
            public int compare(Component c1, Component c2) {
                Map<String, Long> c1Times = c1.getComponentDeclaration().getCallbacksTime();
                Map<String, Long> c2Times = c2.getComponentDeclaration().getCallbacksTime();
                Long c1Start = c1Times.get("start");
                Long c2Start = c2Times.get("start");
                if (c1Start != null) {
                    if (c2Start != null) {
                        return c1Start > c2Start ? 1 : -1;
                    } else {
                        return 1;
                    }
                } else {
                    if (c2Start != null) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
        });
        
        Collections.reverse(components);

        System.out.printf("%-100s %10s %10s%n%n", "Top components (sorted by start duration time)", "[init time]", "[start time]");
        
        if (components.size() > 0) {
            System.out.println();
            
            max = max == 0 ? components.size() : Math.min(components.size(), max);
            for (int i = 0 ; i < components.size()  && i < max; i++) {
                ComponentDeclaration decl = components.get(i).getComponentDeclaration();
                System.out.printf("%-100s %10d %10d%n", decl.getClassName(),
                    decl.getCallbacksTime().get("init"), decl.getCallbacksTime().get("start"));
            }
        }
    }

    private boolean matchBundle(Bundle bundle, List<String> ids) {
        if (ids.size() == 0) {
            return true;
        }
        
        for (int i = 0; i < ids.size(); i ++) {
            String id = ids.get(i);
            try {
                Long longId = Long.valueOf(id);
                if (longId == bundle.getBundleId()) {
                    return true;
                }                
            } catch (NumberFormatException e) {
                // must match symbolic name
                if (id.equals(bundle.getSymbolicName())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Returns the value of a command arg parameter, or from the gogo shell if the parameter is not passed to
     * the command.
     */
    private String getParam(CommandSession session, String param, String value) {
        if (value != null && value.length() > 0) {
            return value;
        }
        Object shellParamValue = session.get(param);
        return shellParamValue != null ? shellParamValue.toString() : null;
    }

    /**
     * Returns the value of a command arg parameter, or from the gogo shell if the parameter is not passed to
     * the command. The parameter value is meant to be a list of values separated by a blank or a comma. 
     * The values are split and returned as an array.
     */
    private String[] getParams(CommandSession session, String name, String value) {
        String values = null;
        if (value == null || value.length() == 0) {
            value = (String) session.get(name);
            if (value != null) {
                values = value;
            }
        } else {
            values = value;
        }
        if (values == null) {
            return new String[0];
        }      
        return values.trim().split(", ");
    }

    /**
     * Checks if a component can be displayed. We make a logical OR between the three following conditions:
     * 
     *  - the component service properties are matching a given service filter ("services" option)
     *  - the component implementation class name is matching some regex ("components" option)
     *  - the component declaration name is matching some regex ("names" option)
     *  
     *  If some component ids are provided, then the component must also match one of them.
     */
    private boolean mayDisplay(Component component, Filter servicesFilter, String[] components, List<Long> componentIds) {   
        // Check component id
        if (componentIds.size() > 0) {
            long componentId = ((ComponentDeclaration) component).getId();
            if (componentIds.indexOf(componentId) == -1) {
                return false;
            }
        }
        
        if (servicesFilter == null && components.length == 0) {
            return true;
        }     
        
        // Check component service properties
        boolean servicesMatches = servicesMatches(component, servicesFilter);        

        // Check components regexs, which may match component implementation class name
        boolean componentsMatches = componentMatches(((ComponentDeclaration) component).getClassName(), components);

        // Logical OR between service properties match and component service/impl match.
        return servicesMatches || componentsMatches;   
    }

    /**
     * Checks if a given filter is matching some service properties possibly provided by a component
     */
    private boolean servicesMatches(Component component, Filter servicesFilter) {
        boolean match = false;
        if (servicesFilter != null) {
            String[] services = ((ComponentDeclaration) component).getServices();
            if (services != null) {
                Dictionary<String, Object> properties = component.getServiceProperties();
                if (properties == null) {
                    properties = new Hashtable<String, Object>();
                }
                if (properties.get(Constants.OBJECTCLASS) == null) {
                    properties.put(Constants.OBJECTCLASS, services);
                }
                match = servicesFilter.match(properties);
            }
        }
        return match;
    }

    /**
     * Checks if the component implementation class name (or some possible provided services) are matching
     * some regular expressions.
     */
    private boolean componentMatches(String description, String[] names) {
        for (int i = 0; i < names.length; i ++) {
            String name = names[i];
            boolean not = false;
            if (name.startsWith("!")) {
                name = name.substring(1);
                not = true;
            }
            boolean match = false;

            if (description.matches(name)) {
                match = true;
            }
                       
            if (not) {
                match = !match;
            }
            
            if (match) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Compact names that look like state strings. State strings consist of
     * one or more words. Each word will be shortened to the first letter,
     * all letters concatenated and uppercased.
     */
    private String compactState(String input) {
        StringBuffer output = new StringBuffer();
        StringTokenizer st = new StringTokenizer(input);
        while (st.hasMoreTokens()) {
            output.append(st.nextToken().toUpperCase().charAt(0));
        }
        return output.toString();
    }

    /**
     * Compacts names that look like fully qualified class names. All packages
     * will be shortened to the first letter, except for the last one. So
     * something like "org.apache.felix.MyClass" will become "o.a.f.MyClass".
     */
    private String compactName(String input) {
        StringBuffer output = new StringBuffer();
        int lastIndex = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '.' :
                    output.append(input.charAt(lastIndex));
                    output.append('.');
                    lastIndex = i + 1;
                    break;
                case ' ' :
                case ',' :
                    if (lastIndex < i) {
                        output.append(input.substring(lastIndex, i));
                    }
                    output.append(c);
                    lastIndex = i + 1;
                    break;
                default:
            }
        }
        if (lastIndex < input.length()) {
            output.append(input.substring(lastIndex));
        }
        return output.toString();
    }

    public void wtf() {
    	
    	DependencyGraph graph = DependencyGraph.getGraph(ComponentState.UNREGISTERED, DependencyState.REQUIRED_UNAVAILABLE);
    	List<ComponentDeclaration> unregisteredComponents = graph.getAllComponents();
    	
    	if(unregisteredComponents.isEmpty()) {
    		System.out.println("No unregistered components found");
    	} else {
    		String message = unregisteredComponents.size() + " unregistered components found";
			System.out.println(message);
            System.out.println("----------------------------------------------------".substring(0, message.length()));
    	}
    	
    	listResolvedBundles();
    	listInstalledBundles();
    	
    	List<CircularDependency> circularDependencies = graph.getCircularDependencies();
    	if(!circularDependencies.isEmpty()) {
    		System.out.println("Circular dependencies:");
    		printCircularDependencies(circularDependencies);
    	}
    	
    	List<MissingDependency> missingConfigDependencies = graph.getMissingDependencies(CONFIGURATION);
    	if(!missingConfigDependencies.isEmpty()) {
    		System.out.println("The following configuration(s) are missing: ");
    		printMissingDependencies(missingConfigDependencies);
    	}
    	
    	List<MissingDependency> missingServiceDependencies = graph.getMissingDependencies(SERVICE);
    	if(!missingServiceDependencies.isEmpty()) {
    		System.out.println("The following service(s) are missing: ");
    		printMissingDependencies(missingServiceDependencies);
    	}

    	
    	List<MissingDependency> missingResourceDependencies = graph.getMissingDependencies(RESOURCE);
    	if(!missingResourceDependencies.isEmpty()) {
    		System.out.println("The following resource(s) are missing: ");
    		printMissingDependencies(missingResourceDependencies);
    	}
    	
    	List<MissingDependency> missingBundleDependencies = graph.getMissingDependencies(BUNDLE);
    	if(!missingBundleDependencies.isEmpty()) {
    		System.out.println("The following bundle(s) are missing: ");
    		printMissingDependencies(missingBundleDependencies);
    	}
    }

	private void printCircularDependencies(List<CircularDependency> circularDependencies) {
		for(CircularDependency c : circularDependencies) {
			System.out.print(" *");
			for(ComponentDeclaration cd : c.getComponents()) {
				System.out.print(" -> " + cd.getName());
			}
			System.out.println();
		}
	}

	private void printMissingDependencies(List<MissingDependency> missingConfigDependencies) {
		for(MissingDependency m : missingConfigDependencies) {
			System.out.println(" * " + m.getName() + " for bundle " + m.getBundleName());
		}
	}

    private void listResolvedBundles() {
        boolean areResolved = false;
        for (Bundle b : m_context.getBundles()) {
            if (b.getState() == Bundle.RESOLVED && !isFragment(b)) {
                areResolved = true;
                break;
            }
        }
        if (areResolved) {
            System.out.println("Please note that the following bundles are in the RESOLVED state:");
            for (Bundle b : m_context.getBundles()) {
                if (b.getState() == Bundle.RESOLVED && !isFragment(b)) {
                    System.out.println(" * [" + b.getBundleId() + "] " + b.getSymbolicName());
                }
            }
        }
    }
    
    private void listInstalledBundles() {
        boolean areResolved = false;
        for (Bundle b : m_context.getBundles()) {
            if (b.getState() == Bundle.INSTALLED) {
                areResolved = true;
                break;
            }
        }
        if (areResolved) {
            System.out.println("Please note that the following bundles are in the INSTALLED state:");
            for (Bundle b : m_context.getBundles()) {
                if (b.getState() == Bundle.INSTALLED) {
                    System.out.println(" * [" + b.getBundleId() + "] " + b.getSymbolicName());
                }
            }
        }
    }

    private boolean isFragment(Bundle b) {
        @SuppressWarnings("unchecked")
        Dictionary<String, String> headers = b.getHeaders();
        return headers.get("Fragment-Host") != null;
    }
    
    public static class ComponentDeclarationComparator implements Comparator<ComponentDeclaration> {
		@Override
		public int compare(ComponentDeclaration cd1, ComponentDeclaration cd2) {
			long id1 = cd1.getBundleContext().getBundle().getBundleId();
			long id2 = cd2.getBundleContext().getBundle().getBundleId();
			if(id1 == id2) {
				// sort by component id
				long cid1 = cd1.getId();
				long cid2 = cd2.getId();
				return cid1 > cid2 ? 1 : -1;
			} 
			return id1 > id2 ? 1 : -1;
		}
    }
}
