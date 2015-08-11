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
package org.apache.felix.scrplugin.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ComponentContainerUtil {

    public static class ComponentContainerContainer {
        public List<ComponentContainer> components;
        public String className;
    }

    /**
     * Split the list of components into separate lists depending
     * on the configuration.
     */
    public static List<ComponentContainerContainer> split(final List<ComponentContainer> components) {
        final List<ComponentContainerContainer> result = new ArrayList<ComponentContainerContainer>();

        while ( !components.isEmpty() ) {
            // get the first component
            final List<ComponentContainer> innerList = new ArrayList<ComponentContainer>();
            final ComponentContainer component = components.remove(0);
            innerList.add(component);
            final int pos = component.getClassDescription().getDescribedClass().getName().indexOf('$');
            final String baseClassName;
            if ( pos == -1 ) {
                baseClassName = component.getClassDescription().getDescribedClass().getName();
            } else {
                baseClassName = component.getClassDescription().getDescribedClass().getName().substring(0, pos);
            }
            final String baseClassPrefix = baseClassName + '$';

            // check for inner classes
            final Iterator<ComponentContainer> i = components.iterator();
            while ( i.hasNext() ) {
                final ComponentContainer cc = i.next();
                if ( cc.getClassDescription().getDescribedClass().getName().startsWith(baseClassPrefix) ) {
                    innerList.add(cc);
                    i.remove();
                }
            }

            final ComponentContainerContainer ccc = new ComponentContainerContainer();
            ccc.components = innerList;
            ccc.className = baseClassName;
            result.add(ccc);
        }

        return result;
    }
}
