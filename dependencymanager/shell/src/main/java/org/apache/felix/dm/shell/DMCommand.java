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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Shell command for showing all services and dependencies that are managed
 * by the dependency manager.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DMCommand {
    private final BundleContext m_context;
    private static final DependencyManagerSorter SORTER = new DependencyManagerSorter();

    public DMCommand(BundleContext context) {
        m_context = context;
    }

    public void execute(String line, PrintStream out, PrintStream err) {
        boolean nodeps = false;
        boolean notavail = false;
        boolean compact = false;
        boolean stats = false;
        ArrayList ids = new ArrayList();

        // parse the command line
        StringTokenizer st = new StringTokenizer(line);
        if (st.hasMoreTokens()) {
            st.nextToken();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if ("nodeps".equals(token)) {
                    nodeps = true;
                }
                else if ("notavail".equals(token)) {
                    notavail = true;
                }
                else if ("compact".equals(token)) {
                    compact = true;
                }
                else if ("stats".equals(token)) {
                    stats = true;
                }
                else {
                    try {
                        ids.add(Long.valueOf(token));
                    }
                    catch (NumberFormatException e) {
                        // if it's not a number, we abort with an error
                        err.println("Argument " + token + " is not a valid number.");
                        return;
                    }
                }
            }
            
            // lookup all dependency manager service components
            List managers = DependencyManager.getDependencyManagers();
            Collections.sort(managers, SORTER);
            Iterator iterator = managers.iterator();
            long numberOfComponents = 0;
            long numberOfDependencies = 0;
            long lastBundleId = -1;
            while (iterator.hasNext()) {
                DependencyManager manager = (DependencyManager) iterator.next();
                List components = manager.getComponents();
                Iterator componentIterator = components.iterator();
                while (componentIterator.hasNext()) {
                    Component component = (Component) componentIterator.next();
                    ComponentDeclaration sc = (ComponentDeclaration) component;
                    String name = sc.getName();
                    int state = sc.getState();
                    Bundle bundle = sc.getBundleContext().getBundle();
                    long bundleId = bundle.getBundleId();
                    if (ids.size() == 0 || ids.contains(Long.valueOf(bundleId))) {
                        if (!notavail || (notavail && sc.getState() == ComponentDeclaration.STATE_UNREGISTERED)) {
                            numberOfComponents++;
                            if (lastBundleId != bundleId) {
                                lastBundleId = bundleId;
                                if (compact) {
                                    out.println("[" + bundleId + "] " + compactName(bundle.getSymbolicName()));
                                }
                                else {
                                    out.println("[" + bundleId + "] " + bundle.getSymbolicName());
                                }
                            }
                            if (compact) {
                                out.print(" " + compactName(name) + " " + compactState(ComponentDeclaration.STATE_NAMES[state]));
                            }
                            else {
                                out.println("  " + name + " " + ComponentDeclaration.STATE_NAMES[state]);
                            }
                            if (!nodeps) {
                                ComponentDependencyDeclaration[] dependencies = sc.getComponentDependencies();
                                numberOfDependencies += dependencies.length;
                                if (dependencies != null && dependencies.length > 0) {
                                    if (compact) {
                                        out.print('(');
                                    }
                                    for (int j = 0; j < dependencies.length; j++) {
                                        ComponentDependencyDeclaration dep = dependencies[j];
                                        String depName = dep.getName();
                                        String depType = dep.getType();
                                        int depState = dep.getState();
                                        if (compact) {
                                            if (j > 0) {
                                                out.print(' ');
                                            }
                                            out.print(compactName(depName) + " " + compactState(depType) + " " + compactState(ComponentDependencyDeclaration.STATE_NAMES[depState]));
                                        }
                                        else {
                                            out.println("    " + depName + " " + depType + " " + ComponentDependencyDeclaration.STATE_NAMES[depState]);
                                        }
                                    }
                                    if (compact) {
                                        out.print(')');
                                    }
                                }
                            }
                            if (compact) {
                                out.println();
                            }
                        }
                    }
                }
            }
            if (stats) {
                out.println("Statistics:");
                out.println(" - Dependency managers: " + managers.size());
                out.println(" - Components: " + numberOfComponents);
                if (!nodeps) {
                    out.println(" - Dependencies: " + numberOfDependencies);
                }
            }
        }
        else {
            err.println("Invalid command line: " + line);
        }
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
                case '.':
                    output.append(input.charAt(lastIndex));
                    output.append('.');
                    lastIndex = i + 1;
                    break;
                case ' ':
                case ',':
                    if (lastIndex < i) {
                        output.append(input.substring(lastIndex, i));
                    }
                    output.append(c);
                    lastIndex = i + 1;
                    break;
            }
        }
        if (lastIndex < input.length()) {
            output.append(input.substring(lastIndex));
        }
        return output.toString();
    }

    public String getName() {
        return "dm";
    }

    public String getShortDescription() {
        return "list dependency manager component diagnostics.";
    }

    public String getUsage() {
        return "dm [nodeps] [notavail] [compact] [stats] [<bundleid> ...]";
    }
    
    public static class DependencyManagerSorter implements Comparator {
        public int compare(Object o1, Object o2) {
            DependencyManager dm1 = (DependencyManager) o1;
            DependencyManager dm2 = (DependencyManager) o2;
            long id1 = dm1.getBundleContext().getBundle().getBundleId();
            long id2 = dm2.getBundleContext().getBundle().getBundleId();
            return id1 > id2 ? 1 : -1;
        }
    }
}
