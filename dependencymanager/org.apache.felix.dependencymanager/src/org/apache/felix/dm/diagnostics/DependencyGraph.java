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
package org.apache.felix.dm.diagnostics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

/**
 * The dependency graph is a view of all components managed by the dependency manager 
 * and of their dependencies. Using this API you can get the dependencies of a given component,
 * the components providing a given service, the circular dependencies that might exist. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class DependencyGraph {

	/**
	 * Use this to specify which components the dependency graph should contain 
	 */
	public enum ComponentState {
		ALL, 
		UNREGISTERED
	};

	/**
	 * Use this to specify which dependencies the graph should contain
	 */
	public enum DependencyState {
		ALL,
		ALL_UNAVAILABLE,
		REQUIRED_UNAVAILABLE
	};
	
	private static final String SERVICE = "service";

	private Map<ComponentDeclaration, DependencyGraphNode> m_componentToNode = new HashMap<>();
	private Map<ComponentDependencyDeclaration, DependencyGraphNode> m_dependencyToNode = new HashMap<>();
	private List<List<DependencyGraphNode>> m_circularDependencies = new ArrayList<>();
	private Map<DependencyGraphNode, DependencyGraphNode> m_parent = new HashMap<>();
	
	private ComponentState m_componentState = ComponentState.ALL;
	private DependencyState m_dependencyState = DependencyState.ALL; 
	
	private DependencyGraph(ComponentState componentState, DependencyState dependencyState) {
		
		m_componentState = componentState;
		m_dependencyState = dependencyState;
		
		buildComponentNodes();
		buildDependecyNodesAndEdges();
		
	}
	
	private void buildComponentNodes() {
		List<DependencyManager> dependencyManagers = DependencyManager.getDependencyManagers();
		for(DependencyManager dm : dependencyManagers) {
			List<Component> components = dm.getComponents();
			for(Component c : components) {
				ComponentDeclaration cd = c.getComponentDeclaration();
				if(componentMustBeAddedToGraph(cd)) {
					m_componentToNode.put(cd, new ComponentNode(cd));
				}
			}
		}
	}
	
	private boolean componentMustBeAddedToGraph(ComponentDeclaration cd) {
		if(m_componentState == ComponentState.ALL) {
			return true;
		} else if(m_componentState == ComponentState.UNREGISTERED) {
			return cd.getState() == ComponentDeclaration.STATE_UNREGISTERED;
		}
		return false;
	}

	private void buildDependecyNodesAndEdges() {
		
		for(DependencyGraphNode node : m_componentToNode.values()) {
			ComponentNode componentNode = (ComponentNode)node;
			ComponentDependencyDeclaration[] dependencyDeclarations = componentNode.getComponentDeclaration().getComponentDependencies();
			
			for(ComponentDependencyDeclaration cdd : dependencyDeclarations) {
				if(dependencyMustBeAddedToGraph(cdd)) {
					DependencyNode dependencyNode = new DependencyNode(cdd);
					m_dependencyToNode.put(cdd, dependencyNode);
					
					// add edges from the component node to newly created dependency node
					componentNode.addSuccessor(dependencyNode);
					
					// add edges from the newly created dependency node to the components 
					// providing those dependencies (only applicable to service dependencies)
					List<ComponentNode> providerComponents = getProviderComponents(dependencyNode);
					for(ComponentNode p : providerComponents) {
						dependencyNode.addSuccessor(p);
					}
				}
			}
		}
	}

	private List<ComponentNode> getProviderComponents(DependencyNode dependencyNode) {
		List<ComponentNode> result = new ArrayList<>();

		ComponentDependencyDeclaration cdd = dependencyNode.getDependencyDeclaration();
		if(!SERVICE.equals(cdd.getType())) {
			return result;
		}
		
		for(DependencyGraphNode n : m_componentToNode.values()) {
			ComponentNode componentNode = (ComponentNode)n;
			if(componentProvidesDependency(componentNode, dependencyNode)) {
				result.add(componentNode);
			}
		}
		
		return result;
	}

	private boolean componentProvidesDependency(ComponentNode componentNode, DependencyNode dependencyNode) {
		ComponentDeclaration cd = componentNode.getComponentDeclaration();
		
		String dependencyName = dependencyNode.getDependencyDeclaration().getName();
		String simpleName = getSimpleName(dependencyName);
		Properties properties = parseProperties(dependencyName);

        String componentName = cd.getName();
        int cuttOff = componentName.indexOf("(");
        if (cuttOff != -1) {
            componentName = componentName.substring(0, cuttOff).trim();
        }
        for (String serviceName : componentName.split(",")) {
            if (simpleName.equals(serviceName.trim()) && doPropertiesMatch(properties, parseProperties(cd.getName()))) {
                return true;
            }
        }
		return false;
	}

	private boolean dependencyMustBeAddedToGraph(ComponentDependencyDeclaration cdd) {
		if(m_dependencyState == DependencyState.ALL) {
			return true;
		} else if(m_dependencyState == DependencyState.ALL_UNAVAILABLE) {
			return 
					(cdd.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED) ||
					(cdd.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_OPTIONAL);
		} else if(m_dependencyState == DependencyState.REQUIRED_UNAVAILABLE) {
			return cdd.getState() == ComponentDependencyDeclaration.STATE_UNAVAILABLE_REQUIRED;
			
		}
		return false;
	}	

	/**
	 * Build the dependency graph. It will contain all the components managed by the dependency manager, provided
	 * that those components are in the given state, and all dependencies of their dependencies, provided that the
	 * dependencies are in the given state.
	 * 
	 * <p>This implementation currently only builds a graph of unregistered components and 
	 * required unavailable dependencies.
	 * 
	 * @param componentState Include only the components in this state
	 * @param dependencyState Include only the dependencies in this state
	 * @return
	 */
	public static DependencyGraph getGraph(ComponentState componentState, DependencyState dependencyState) {
		return new DependencyGraph(componentState, dependencyState);
	}
	
	/**
	 * Returns the list of components in the graph
	 * @return the list of components in the graph
	 */
	public List<ComponentDeclaration> getAllComponents() {
		return new ArrayList<ComponentDeclaration>(m_componentToNode.keySet());
	}
	
	/**
	 * Returns a list all dependencies in the graph
	 * @return the list of all dependencies in the graph 
	 */
	public List<ComponentDependencyDeclaration> getAllDependencies() {
		return new ArrayList<ComponentDependencyDeclaration>(m_dependencyToNode.keySet());

	}
	
	/**
	 * For a given component declaration, it returns a list of its dependencies in the state
	 * specified when the graph was built.
	 * @param componentDeclaration
	 * @return the list of dependencies or null if the component declaration is not in the graph
	 */
	public List<ComponentDependencyDeclaration> getDependecies(ComponentDeclaration componentDeclaration) {
		List<ComponentDependencyDeclaration> result = new ArrayList<>();
		
		DependencyGraphNode node = m_componentToNode.get(componentDeclaration);
		if(node == null) {
			return null;
		}

		for(DependencyGraphNode s : node.getSuccessors()) {
			result.add( ((DependencyNode)s).getDependencyDeclaration() );
		}
		
		return result;
	}
	
	/**
	 * Returns the list of components that provide the given dependency. This only returns the components
	 * managed by the dependency manager that are in the state specified when the graph was built. The 
	 * dependency can only be a service dependency.
	 * 
	 * @param dependency 
	 * @return the list of components providing this dependency or null if the dependency declaration is 
	 * 		   not in the graph
	 */
	public List<ComponentDeclaration> getProviders(ComponentDependencyDeclaration dependency) {
		List<ComponentDeclaration> result = new ArrayList<>();
		
		DependencyGraphNode node = m_dependencyToNode.get(dependency);
		if(node == null) {
			return null;
		}
		
		for(DependencyGraphNode s : node.getSuccessors()) {
			result.add(((ComponentNode)s).getComponentDeclaration());
		}
		
		return result;
	}
	
	/**
	 * Returns the list of circular dependencies in the graph
	 * @return the list of circular dependencies
	 */
	public List<CircularDependency> getCircularDependencies() {
    	List<CircularDependency> result = new ArrayList<CircularDependency>();
		
		for(DependencyGraphNode n : m_componentToNode.values()) {
			if(n.isUndiscovered()) {
				depthFirstSearch(n);
			}
		}		
		
		for(List<DependencyGraphNode> cycle : m_circularDependencies) {
			CircularDependency circularDependency = new CircularDependency();
			for(DependencyGraphNode n : cycle) {
				if(n instanceof ComponentNode) {
					circularDependency.addComponent(((ComponentNode) n).getComponentDeclaration());
				}
			}
			
			result.add(circularDependency);
		}
		
		return result;
	}
	
	private void depthFirstSearch(DependencyGraphNode n) {

		n.setState(DependencyGraphNode.DependencyGraphNodeState.DISCOVERED);
		for(DependencyGraphNode s : n.getSuccessors()) {
			if(s.isUndiscovered()) {
				m_parent.put(s, n);
				depthFirstSearch(s);
			} else if(s.isDiscovered()) {
				addCycle(n, s);
			}
		}
		n.setState(DependencyGraphNode.DependencyGraphNodeState.PROCESSED);
	}
	
    private void addCycle(DependencyGraphNode n, DependencyGraphNode s) {
		List<DependencyGraphNode> cycle = new ArrayList<>();
		Stack<DependencyGraphNode> stack = new Stack<>();
		
		stack.push(s);
		for(DependencyGraphNode p = n; p != s; p = m_parent.get(p)) {
			stack.push(p);
		}
		stack.push(s);
		
		while(!stack.isEmpty()) {
			cycle.add(stack.pop());
		}
		m_circularDependencies.add(cycle);
	}

	/**
     * Returns all the missing dependencies of a given type.
     * @param type The type of the dependencies to be returned. This can be either one of the types 
     * 			   known by the DependencyManager (service, bundle, configuration, resource), 
     * 			   a user defined type or null, in which case all missing dependencies must be returned.
     * 			   
     * @return The missing dependencies of the given type or all the missing dependencies.
     */
	public List<MissingDependency> getMissingDependencies(String type) {
		
		List<MissingDependency> result = new ArrayList<>();

		// get all dependency nodes that have no out-going edges
		List<DependencyNode> missingDependencies = new ArrayList<>();
		for(DependencyGraphNode node : m_dependencyToNode.values()) {
			DependencyNode dependencyNode = (DependencyNode)node;
			if(!dependencyNode.isUnavailable()) {
				continue;
			}
			
			if( (type != null) && (!dependencyNode.getDependencyDeclaration().getType().equals(type)) ) {
				continue;
			}
			if (dependencyNode.getSuccessors().isEmpty()) {
				missingDependencies.add(dependencyNode);
			}
		}
		
		for(DependencyNode node : missingDependencies) {
			for(DependencyGraphNode p : node.getPredecessors()) {
				ComponentNode componentNode = (ComponentNode)p;
				Bundle bundle = componentNode.getComponentDeclaration().getBundleContext().getBundle();
				MissingDependency missingDependency = new MissingDependency(
						node.getDependencyDeclaration().getName(), 
						node.getDependencyDeclaration().getType(), 
						bundle.getSymbolicName());
				result.add(missingDependency);
			}
		}
		return result;
	}
    
    private String getSimpleName(String name) {
        int cuttOff = name.indexOf("(");
        if (cuttOff != -1) {
            return name.substring(0, cuttOff).trim();
        }
        return name.trim();
    }
    
    private Properties parseProperties(String name) {
        Properties result = new Properties();
        int cuttOff = name.indexOf("(");
        if (cuttOff != -1) {
            String propsText = name.substring(cuttOff + 1, name.indexOf(")"));
            String[] split = propsText.split(",");
            for (String prop : split) {
                String[] kv = prop.split("=");
                if (kv.length == 2) {
                    result.put(kv[0], kv[1]);
                }
            }
        }
        return result;
    }
    
    private boolean doPropertiesMatch(Properties need, Properties provide) {
        for (Entry<Object, Object> entry : need.entrySet()) {
            Object prop = provide.get(entry.getKey());
            if (prop == null || !prop.equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

}
