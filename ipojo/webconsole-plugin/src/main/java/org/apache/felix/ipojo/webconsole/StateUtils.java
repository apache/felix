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
package org.apache.felix.ipojo.webconsole;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.util.DependencyModel;


/**
 * Helper class dealing with instance and factory states.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class StateUtils {
    
    /**
     * Gets the number of valid instances.
     * @param archs the instance architecture list
     * @return the number of valid instances.
     */
    public static int getValidInstancesCount(List<Architecture> archs) {
        int i = 0;
        for (Architecture a : archs) { // Cannot be null, an empty list is returned.
            if (a.getInstanceDescription().getState() == ComponentInstance.VALID) {
                i ++;
            }
        }
        return i;
    }

    /**
     * Gets the number of invalid instances.
     * @param archs the instance architecture list
     * @return the number of invalid instances.
     */
    public static int getInvalidInstancesCount(List<Architecture> archs) {
        int i = 0;
        for (Architecture a : archs) {  // Cannot be null, an empty list is returned.
            if (a.getInstanceDescription().getState() == ComponentInstance.INVALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the number of valid factories.
     * @param factories the factory list
     * @return the number of valid factories.
     */
    public static int getValidFactoriesCount(List<Factory> factories) {
        int i = 0;
        for (Factory a : factories) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.VALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the number of invalid factories.
     * @param factories the factory list
     * @return the number of invalid factories.
     */
    public static int getInvalidFactoriesCount(List<Factory> factories) {
        int i = 0;
        for (Factory a : factories) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.INVALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the number of valid handlers.
     * @param handlers the handler factory list
     * @return the number of valid handlers.
     */
    public static int getValidHandlersCount(List<HandlerFactory> handlers) {
        int i = 0;
        for (Factory a : handlers) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.VALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the number of invalid handlers.
     * @param handlers the handler factory list
     * @return the number of invalid handlers.
     */
    public static int getInvalidHandlersCount(List<HandlerFactory> handlers) {
        int i = 0;
        for (Factory a : handlers) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.INVALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the instance state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    public static String getInstanceState(int state) {
        switch(state) {
            case ComponentInstance.VALID :
                return "valid";
            case ComponentInstance.INVALID :
                return "invalid";
            case ComponentInstance.DISPOSED :
                return "disposed";
            case ComponentInstance.STOPPED :
                return "stopped";
            default :
                return "unknown";
        }
    }

    /**
     * Gets the factory state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    public static String getFactoryState(int state) {
        switch(state) {
            case Factory.VALID :
                return "valid";
            case Factory.INVALID :
                return "invalid";
            default :
                return "unknown";
        }
    }
    
    /**
     * Gets the instance list created by the given factory.
     * @param archs the list of instance architectures
     * @param factory the factory name
     * @return the list containing the created instances (name)
     */
    public static List<String> getInstanceList(List<Architecture> archs, String factory) {
        List<String> list = new ArrayList<String>();
        for (Architecture arch : archs) { // Cannot be null, an empty list is returned.
            String n = arch.getInstanceDescription().getComponentDescription().getName();
            if (factory.equals(n)) {
                list.add(arch.getInstanceDescription().getName());
            }
        }
        return list;
    }
    
    /**
     * Gets the dependency state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    public static String getDependencyState(int state) {
        switch(state) {
            case DependencyModel.RESOLVED :
                return "resolved";
            case DependencyModel.UNRESOLVED :
                return "unresolved";
            case DependencyModel.BROKEN :
                return "broken";
            default :
                return "unknown (" + state + ")";
        }
    }

    /**
     * Gets the dependency binding policy as a String.
     * @param policy the policy.
     * @return the String form of the policy.
     */
    public static String getDependencyBindingPolicy(int policy) {
        switch(policy) {
            case DependencyModel.DYNAMIC_BINDING_POLICY :
                return "dynamic";
            case DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY :
                return "dynamic-priority";
            case DependencyModel.STATIC_BINDING_POLICY :
                return "static";
            default :
                return "unknown (" + policy + ")";
        }
    }

    /**
     * Gets the provided service state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    public static String getProvidedServiceState(int state) {
        switch(state) {
            case ProvidedService.REGISTERED :
                return "registered";
            case ProvidedService.UNREGISTERED :
                return "unregistered";
            default :
                return "unknown (" + state + ")";
        }
    }

}
