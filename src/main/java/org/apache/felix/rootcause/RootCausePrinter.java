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
package org.apache.felix.rootcause;

import java.util.Arrays;
import java.util.function.Consumer;

import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

public class RootCausePrinter {
    private Consumer<String> printCallback;
    
    public RootCausePrinter() {
        this(System.out::println);
    }
    
    public RootCausePrinter(Consumer<String> printCallback) {
        this.printCallback = printCallback;
    }
    
    public void print(DSComp desc) {
        print(desc, 0);
    }
    
    public void print(DSComp comp, int level) {
        if (comp.config == null && "require".equals(comp.desc.configurationPolicy)) {
            println(level, "Component %s missing config on pid %s", comp.desc.name, Arrays.asList(comp.desc.configurationPid));
        } else if (comp.config != null && comp.config.state == ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION){
            println(level, "Component %s unsatisifed configuration on pid %s", comp.desc.name, Arrays.asList(comp.desc.configurationPid));
        } else if (!comp.unsatisfied.isEmpty()) {
            println(level, "Component %s unsatisfied references", comp.desc.name);
        } else {
            println(level, "Component %s satisfied", comp.desc.name);
        }
        int l2 = level + 2;
        int l3 = l2 + 2;
        for (DSRef ref : comp.unsatisfied) {
            println(l2, "unsatisfied ref %s interface %s %s", ref.name, ref.iface, getFilterSt(ref.filter));
            for (DSComp cand : ref.candidates) {
                print(cand, l3);
            }
        }
    }
 
    private Object getFilterSt(String filter) {
        return filter == null ? "" : ", filter " + filter;
    }

    private void println(int level, String format, Object... args) {
        printCallback.accept(spaces(level) + String.format(format, args));
    }
    
    private String spaces(int length) {
        char[] bytes = new char[length];
        Arrays.fill(bytes, ' ');
        return new String(bytes);
    }
}
