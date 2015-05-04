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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class containings pattern matching helper methods.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Patterns
{
    // Pattern used to check if a method is void and does not take any params
    public final static Pattern VOID = Pattern.compile("\\(\\)V");

    // Pattern used to check if a method returns an array of Objects
    public final static Pattern COMPOSITION = Pattern.compile("\\(\\)\\[Ljava/lang/Object;");

    // Pattern used to parse service type from "bind(Component, ServiceReference, Service)" signature
    public final static Pattern BIND_CLASS1 = Pattern.compile("\\((Lorg/apache/felix/dm/Component;)(Lorg/osgi/framework/ServiceReference;)L([^;]+);\\)V");
    
    // Pattern used to parse service type from "bind(Component, Service)" signature
    public final static Pattern BIND_CLASS2 = Pattern.compile("\\((Lorg/apache/felix/dm/Component;)L([^;]+);\\)V");

    // Pattern used to parse service type from "bind(Component, Map, Service)" signature
    public final static Pattern BIND_CLASS3 = Pattern.compile("\\((Lorg/apache/felix/dm/Component;)(Ljava/util/Map;)L([^;]+);\\)V");

    // Pattern used to parse service type from "bind(ServiceReference, Service)" signature
    public final static Pattern BIND_CLASS4 = Pattern.compile("\\((Lorg/osgi/framework/ServiceReference;)L([^;]+);\\)V");

    // Pattern used to parse service type from "bind(Service)" signature
    public final static Pattern BIND_CLASS5 = Pattern.compile("\\(L([^;]+);\\)V");

    // Pattern used to parse service type from "bind(Service, Map)" signature
    public final static Pattern BIND_CLASS6 = Pattern.compile("\\(L([^;]+);(Ljava/util/Map;)\\)V");

    // Pattern used to parse service type from "bind(Map, Service)" signature
    public final static Pattern BIND_CLASS7 = Pattern.compile("\\((Ljava/util/Map;)L([^;]+);\\)V");

    // Pattern used to parse service type from "bind(Service, Dictionary)" signature
    public final static Pattern BIND_CLASS8 = Pattern.compile("\\(L([^;]+);(Ljava/util/Dictionary;)\\)V");

    // Pattern used to parse service type from "bind(Dictionary, Service)" signature
    public final static Pattern BIND_CLASS9 = Pattern.compile("\\((Ljava/util/Dictionary;)L([^;]+);\\)V");

    // Pattern used to parse classes from class descriptors;
    public final static Pattern CLASS = Pattern.compile("L([^;]+);");
    
    // Pattern used to parse the field on which a Publisher annotation may be applied on
    public final static Pattern RUNNABLE = Pattern.compile("Ljava/lang/Runnable;");
    
    // Pattern used to parse a field whose type is BundleContext
    public final static Pattern BUNDLE_CONTEXT = Pattern.compile("Lorg/osgi/framework/BundleContext;");

    // Pattern used to parse a field whose type is DependencyManager
    public final static Pattern DEPENDENCY_MANAGER = Pattern.compile("Lorg.apache.felix.dm.DependencyManager;");
    
    // Pattern used to parse a field whose type is Component
    public final static Pattern COMPONENT = Pattern.compile("Lorg.apache.felix.dm.Component;");

    /**
     * Parses a class.
     * @param clazz the class to be parsed (the package is "/" separated).
     * @param pattern the pattern used to match the class.
     * @param group the pattern group index where the class can be retrieved.
     * @return the parsed class.
     */
    public static String parseClass(String clazz, Pattern pattern, int group)
    {
    	return parseClass(clazz, pattern, group, true);
    }
    
    /**
     * Parses a class.
     * @param clazz the class to be parsed (the package is "/" separated).
     * @param pattern the pattern used to match the class.
     * @param group the pattern group index where the class can be retrieved.
     * @param throwException true if an Exception must be thrown in case the clazz does not match the pattern.
     * @return the parsed class.
     */
    public static String parseClass(String clazz, Pattern pattern, int group, boolean throwException)
    {
        Matcher matcher = pattern.matcher(clazz);
        if (matcher.matches())
        {
            return matcher.group(group).replace("/", ".");
        }
        else if (throwException)
        {
            throw new IllegalArgumentException("Invalid class descriptor: " + clazz);
        } else {
        	return null;
        }
    }

    /**
     * Checks if a method descriptor matches a given pattern. 
     * @param the method whose signature descriptor is checked
     * @param pattern the pattern used to check the method signature descriptor
     * @throws IllegalArgumentException if the method signature descriptor does not match the given pattern.
     */
    public static void parseMethod(String method, String descriptor, Pattern pattern)
    {
        Matcher matcher = pattern.matcher(descriptor);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException("Invalid method " + method + ", wrong signature: "
                + descriptor);
        }
    }
    
    /**
     * Checks if a field descriptor matches a given pattern.
     * @param field the field whose type descriptor is checked
     * @param descriptor the field descriptor to be checked
     * @param pattern the pattern to use
     * @throws IllegalArgumentException if the method signature descriptor does not match the given pattern.
     */
    public static void parseField(String field, String descriptor, Pattern pattern) {
        Matcher matcher = pattern.matcher(descriptor);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException("Invalid field " + field + ", wrong signature: "
                + descriptor);
        }
    }
}
