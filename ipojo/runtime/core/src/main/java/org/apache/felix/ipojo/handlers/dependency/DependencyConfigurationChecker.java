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

package org.apache.felix.ipojo.handlers.dependency;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.ServiceReference;

import java.util.*;

/**
 * Utility class checking the configuration of a dependency.
 */
public class DependencyConfigurationChecker {

    public static void ensure(Dependency dependency, Element metadata, PojoMetadata manipulation) throws
            ConfigurationException {
        ensureThatAtLeastOneInjectionIsSpecified(dependency);
        ensureThatTheFieldIsInComponentClass(dependency, manipulation);
        ensureThatTheConstructorParameterIsCoherent(dependency, manipulation);
        ensureThatCallbacksAreCoherent(dependency, manipulation);
        deduceAggregationFromTheInjectionPoints(dependency, manipulation);
        deduceTheServiceSpecification(dependency, manipulation);
        checkTheServiceUnavailableAction(dependency, metadata);
        checkTheConsistencyOfTheFromAttribute(dependency, metadata);
        disableProxyForInconsistentTypes(dependency);
    }

    /**
     * Disables the proxy settings for types that does not support it: Vector, Array, and non interface specification.
     * If the dependency is disabled, check that we are no constructor injection.
     * @param dependency the dependency
     */
    private static void disableProxyForInconsistentTypes(Dependency dependency) throws ConfigurationException {
        if (! dependency.getSpecification().isInterface()
                || dependency.isAggregate()  && dependency.getAggregateType() == AggregateDependencyInjectionType.ARRAY
                || dependency.isAggregate()  && dependency.getAggregateType() == AggregateDependencyInjectionType.VECTOR) {
            dependency.setProxy(false);
            if (dependency.getConstructorParameterIndex() != -1) {
                throw new ConfigurationException("The dependency " + DependencyHandler.getDependencyIdentifier
                        (dependency) + " has an inconsistent configuration. - reason: the service specification " +
                        "or container do not support proxy, which is required for constructor injection");
            }
            dependency.getHandler().info("Proxy disabled for " + DependencyHandler.getDependencyIdentifier
                    (dependency) + " - the service specification or container do not support proxy");
        }
    }

    /**
     * Checks that the dependency callbacks are consistent:
     * <ul>
     *     <li>have a supported 'type'</li>
     *     <li>have a supported signature</li>
     * </ul>
     * If the method is not in the component class, a message is logged, as this verification cannot be used.
     * If the method is found in the manipulation metadata, the callback parameters are set.
     * @param dependency the dependency
     * @param manipulation the manipulation
     * @throws ConfigurationException if the methods do not obey to the previously mentioned rules.
     */
    private static void ensureThatCallbacksAreCoherent(Dependency dependency, PojoMetadata manipulation) throws
            ConfigurationException {
        DependencyCallback[] callbacks = dependency.getCallbacks();
        if (callbacks != null) {
            for (DependencyCallback callback : callbacks) {
                MethodMetadata metadata = manipulation.getMethod(callback.getMethodName());
                if (metadata == null) {
                    dependency.getHandler().debug("A dependency callback " + callback.getMethodName() + " of " +
                            DependencyHandler.getDependencyIdentifier(dependency) + " does not " +
                            "exist in the implementation class, will try the parent classes");
                } else {
                    String[] parameters = metadata.getMethodArguments();
                   switch(parameters.length) {
                       case 0 : // Just a notification method.
                            callback.setArgument(parameters);
                            break;
                       case 1 :
                           // Can be the service object, service reference or properties
                           callback.setArgument(parameters);
                           break;
                       case 2 :
                           // Constraints on the second argument, must be a service reference, a dictionary or a map
                           if (!ServiceReference.class.getName().equals(parameters[1])
                               && ! Dictionary.class.getName().equals(parameters[1])
                               && ! Map.class.getName().equals(parameters[1])) {
                                throw new ConfigurationException("The method " + callback.getMethodName() + " of " +
                                        DependencyHandler.getDependencyIdentifier(dependency) + " is not a valid " +
                                        "dependency callback - reason: the second argument (" + parameters[1] +
                                        ")  must be a service reference, a dictionary or a map.");
                                }
                           callback.setArgument(parameters);
                           break;
                       default:
                           // Invalid signature.
                           throw new ConfigurationException("The method " + callback.getMethodName() + " of " +
                                   DependencyHandler.getDependencyIdentifier(dependency) + " is not a valid " +
                                   "dependency callback - reason: the signature is invalid");
                    }
                }
            }
        }
    }

    /**
     * Checks whether the constructor parameter injection is suitable. this check verified that the constructor has
     * enough parameter.
     * @param dependency the dependency
     * @param manipulation the manipulation metadata
     * @throws ConfigurationException if the constructor is not suitable
     */
    private static void ensureThatTheConstructorParameterIsCoherent(Dependency dependency,
                                                                    PojoMetadata manipulation) throws
            ConfigurationException {
        if (dependency.getConstructorParameterIndex() != -1) {
            MethodMetadata[] constructors = manipulation.getConstructors();
            if (constructors == null  || constructors.length == 0) {
                throw new ConfigurationException("The constructor parameter attribute of " + DependencyHandler
                        .getDependencyIdentifier(dependency) + " is inconsistent - reason: there is no constructor in" +
                        " the component class (" + dependency.getHandler().getInstanceManager().getClassName() + ")");
            }

            //TODO Consider only the first constructor. This is a limitation we should think about,
            // how to determine which constructor to use. Only one constructor should have annotations,
            // it could be use as hint.
            MethodMetadata constructor = constructors[0];
            if (! (constructor.getMethodArguments().length > dependency.getConstructorParameterIndex())) {
                throw new ConfigurationException("The constructor parameter attribute of " + DependencyHandler
                        .getDependencyIdentifier(dependency) + " is inconsistent - reason: the constructor with the " +
                        "signature " + Arrays.toString(constructor.getMethodArguments()) + " has not enough " +
                        "parameters");
            }

        }
    }

    /**
     * Checks that the field used to inject the dependency is in the component class. If the dependency has no field,
     * this method does nothing.
     * @param dependency the dependency
     * @param manipulation the manipulation metadata
     * @throws ConfigurationException if the field used to inject the given dependency is not in the component class.
     */
    private static void ensureThatTheFieldIsInComponentClass(Dependency dependency, PojoMetadata manipulation) throws ConfigurationException {
        if (dependency.getField() != null) {
            FieldMetadata field = manipulation.getField(dependency.getField());
            if (field == null) {
                throw new ConfigurationException("Incorrect field injection for " + DependencyHandler
                        .getDependencyIdentifier(dependency) + " - reason: the field " + dependency.getField() + " is" +
                        " not in the component class (" + dependency.getHandler().getInstanceManager().getClassName()
                      + ")");
            }
        }
    }

    /**
     * Determines if the dependency is aggregate from the field or constructor parameter used to inject the dependency.
     * If the dependency just uses methods, this method does nothing. This method also check that dependencies set to
     * aggregate have a valid injection type.
     * @param dependency the dependency
     * @param manipulation the manipulation metadata
     * @throws ConfigurationException if the type of the field or constructor parameter used to inject the dependency
     * is not suitable for aggregate dependencies.
     */
    private static void deduceAggregationFromTheInjectionPoints(Dependency dependency, PojoMetadata manipulation) throws ConfigurationException {
        if (dependency.getField() != null) {
            FieldMetadata field = manipulation.getField(dependency.getField());
            String type = field.getFieldType();
            if (type.endsWith("[]")) {
                dependency.setAggregateType(AggregateDependencyInjectionType.ARRAY);
            } else if (Collection.class.getName().equals(type)  || List.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.LIST);
            } else if (Set.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.SET);
            } else if (Vector.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.VECTOR);
            } else if (dependency.isAggregate()) {
                // Something wrong. The dependency has a field that is not suitable for aggregate dependencies
                throw new ConfigurationException("The dependency " + DependencyHandler.getDependencyIdentifier
                        (dependency) + " cannot be an aggregate dependency - reason: the type " + field.getFieldType
                        () + " of the field " + field.getFieldName() + " is not suitable for aggregate " +
                        "dependencies. Compatible types are array, vector, list, set and collection.");
            }
        }
        if (dependency.getConstructorParameterIndex() != -1) {
            String type = manipulation.getConstructors()[0].getMethodArguments()[dependency
                    .getConstructorParameterIndex()];
            if (type.endsWith("[]")) {
                dependency.setAggregateType(AggregateDependencyInjectionType.ARRAY);
            } else if (Collection.class.getName().equals(type)  || List.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.LIST);
            } else if (Set.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.SET);
            } else if (Vector.class.getName().equals(type)) {
                dependency.setAggregateType(AggregateDependencyInjectionType.VECTOR);
            } else if (dependency.isAggregate()) {
                // Something wrong. The dependency has a field that is not suitable for aggregate dependencies
                throw new ConfigurationException("The dependency " + DependencyHandler.getDependencyIdentifier
                        (dependency) + " cannot be an aggregate dependency - reason: the type " + type
                        + " of the constructor parameter " + dependency.getConstructorParameterIndex() + " is not suitable for aggregate " +
                        "dependencies. Compatible types are array, vector, list, set and collection.");
            }
        }
        //TODO We may not cover some cases such as inconsistency between the constructor and the field. However this
        // should be very rare.

    }

    /**
     * Checks that the dependency has at least one injection point.
     * @param dependency the dependency
     * @throws ConfigurationException if the dependency has no injection point
     */
    private static void ensureThatAtLeastOneInjectionIsSpecified(Dependency dependency) throws ConfigurationException {
        if (dependency.getField() == null
                && (dependency.getCallbacks() == null  || dependency.getCallbacks().length == 0)
                && dependency.getConstructorParameterIndex() == -1) {
            throw new ConfigurationException("The dependency " + DependencyHandler.getDependencyIdentifier
                    (dependency) + " is invalid - reason: no injection specified, at least a field, " +
                    "a method or a constructor parameter index must be set");
        }
    }

    /**
     * Checks that the `from` attribute is used consistently:
     * <ul>
     * <li>Rule 1 : it cannot be used on aggregate dependency</li>
     * <li>Rule 2 : it cannot be used in combination with the `comparator` attribute</li>
     * <li>Rule 3 : it cannot be used in combination with the `dynamic-priority` binding policy</li>
     * </ul>
     *
     * @param dependency the dependency
     * @param metadata the dependency metadata
     * @throws ConfigurationException if the `from` attribute is used inconsistently.
     */
    private static void checkTheConsistencyOfTheFromAttribute(Dependency dependency,
                                                              Element metadata) throws ConfigurationException {
        // Check if we have a from attribute.
        if (metadata.getAttribute("from") != null) {
            final String message = "The `from` attribute is not usable in " + DependencyHandler
                    .getDependencyIdentifier(dependency) + " - reason: ";
            // Rule 1
            if (dependency.isAggregate()) {
                throw new ConfigurationException(message + "the dependency is " +
                        "aggregate");
            }
            // Rule 2
            String comparator = metadata.getAttribute("comparator");
            if (comparator != null) {
                throw new ConfigurationException(message + "the dependency uses a comparator");
            }
            // Rule 3
            if (dependency.getBindingPolicy() == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY) {
                throw new ConfigurationException(message + "the dependency uses the dynamic-priority " +
                        "binding policy");
            }
        }
    }

    /**
     * Checks that service unavailable actions are consistent.
     * <ul>
     * <li>Rule 1: Nullable, Exception, Default-Implementation... can only be used for scalar optional dependency</li>
     * <li>Rule 2: Only one can be used</li>
     * <li>Rule 3: Timeout can only be used on optional dependency</li>
     * </ul>
     *
     * @param dependency the dependency
     * @throws ConfigurationException if the dependency used inconsistent attributes
     */
    private static void checkTheServiceUnavailableAction(Dependency dependency,
                                                         Element metadata) throws ConfigurationException {
        if (metadata.containsAttribute("nullable") || dependency.getDefaultImplementation() != null  || dependency
                .getException() != null) {
            // Rule 1:
            String message = "The `nullable`, `default-implementation` and `exception` attributes are not " +
                    "usable in " + DependencyHandler.getDependencyIdentifier(dependency) + " - reason: ";
            if (dependency.isAggregate()) {
                throw  new ConfigurationException(message + "the dependency is aggregate");
            }
            if (! dependency.isOptional()) {
                throw  new ConfigurationException(message + "the dependency is mandatory");
            }

            // At this point, we know that the dependency is scalar and optional, and at least one attribute is set

            // Rule 2:
            message = "Inconsistent use of the `nullable`, `default-implementation` and `exception` attributes are " +
                    "not usable in " + DependencyHandler.getDependencyIdentifier(dependency) + " - reason: ";
            if (metadata.containsAttribute("nullable")  && dependency.getDefaultImplementation() != null) {
                throw new ConfigurationException(message + "`nullable` and `default-implementation` cannot be " +
                        "combined");
            }
            if (metadata.containsAttribute("nullable") && dependency.getException() != null) {
                throw new ConfigurationException(message + "`nullable` and `exception` cannot be combined");
            }
            if (dependency.getDefaultImplementation() != null  && dependency.getException() != null) {
                throw new ConfigurationException(message + "`exception` and `default-implementation` cannot be " +
                        "combined");
            }
        }

        // Rule 3:
        if (dependency.getTimeout() != 0  && ! dependency.isOptional()) {
            throw new ConfigurationException("The `timeout` attribute is not usable in " + DependencyHandler
                    .getDependencyIdentifier(dependency) + " - reason: the dependency is not optional");
        }




    }

    /**
     * Tries to determine the service specification to inject in the dependency.
     * If the specification is already checked by the dependency, just checks the consistency.
     *
     * @param dependency   the dependency
     * @param manipulation the manipulation metadata
     * @throws ConfigurationException if the specification cannot be deduced, or when the set specification is not
     *                                consistent.
     */
    private static void deduceTheServiceSpecification(Dependency dependency, PojoMetadata manipulation) throws
            ConfigurationException {

        // Deduction algorithm
        String fieldType = null;
        String callbackType = null;
        String constructorType = null;
        // First check the field
        if (dependency.getField() != null) {
           fieldType = extractSpecificationFromField(dependency.getField(), manipulation);
        }
        if (dependency.getCallbacks() != null  && dependency.getCallbacks().length != 0) {
            callbackType = extractSpecificationFromMethods(dependency, dependency.getCallbacks(), manipulation);
        }
        if (dependency.getConstructorParameterIndex() != -1) {
            constructorType = extractSpecificationFromConstructor(dependency.getConstructorParameterIndex(),
                    manipulation);
        }

        if (dependency.getSpecification() == null
                && fieldType == null  && callbackType == null  && constructorType == null) {
            throw new ConfigurationException("The deduction of the service specification for " + DependencyHandler
                    .getDependencyIdentifier(dependency) + " has failed - reason: when neither the field, " +
                    "methods and constructor parameter have provided the service specification, " +
                    "the `specification` attribute must be set");

        }

        // The Dependency.setSpecification method check whether the specification coming from the different sources
        // are consistent.
        if (fieldType != null) {
            setSpecification(dependency, fieldType);
        }
        if (callbackType != null) {
            setSpecification(dependency, callbackType);
        }
        if (constructorType != null) {
            setSpecification(dependency, constructorType);
        }
    }

    private static String extractSpecificationFromMethods(Dependency dependency, DependencyCallback[] callbacks,
                                                          PojoMetadata manipulation) throws ConfigurationException {
        String type = null;
        for (DependencyCallback callback : callbacks) {
            MethodMetadata metadata = manipulation.getMethod(callback.getMethodName());
            if (metadata != null) {
                String[] parameters = metadata.getMethodArguments();
                if (parameters.length == 1  || parameters.length == 2) {
                    if (! ServiceReference.class.getName().equals(parameters[0])
                        && ! Dictionary.class.getName().equals(parameters[0])
                        && ! Map.class.getName().equals(parameters[0])) {
                        if (type == null) {
                            type = parameters[0];
                        } else {
                            if (! type.equals(parameters[0])) {
                                throw new ConfigurationException("The callbacks of " + DependencyHandler
                                        .getDependencyIdentifier(dependency) + " have inconsistent parameters");
                            }
                        }
                    }
                }
            }
        }
        return type;
    }

    private static String extractSpecificationFromConstructor(int index, PojoMetadata manipulation) {
        // We can write the following instructions as everything was previously checked.
        String type = manipulation.getConstructors()[0].getMethodArguments()[index];
        if (type.endsWith("[]")) {
            return type.substring(0, type.length() - 2);
        }
        if (AggregateDependencyInjectionType.AGGREGATE_TYPES.contains(type)) {
            return null; // It's an aggregate
        }
        return type;
    }

    /**
     * Extracts the service specification from the field.
     * When this method is called, we know that the field is containing in the component class.
     * @param field the field
     * @param manipulation the manipulation metadata
     * @return the service specification, or {@code null} if is cannot be extracted.
     */
    private static String extractSpecificationFromField(String field, PojoMetadata manipulation) {
        FieldMetadata metadata = manipulation.getField(field);
        if (metadata.getFieldType().endsWith("[]")) {
            return metadata.getFieldType().substring(0, metadata.getFieldType().length() - 2);
        }
        if (AggregateDependencyInjectionType.AGGREGATE_TYPES.contains(metadata.getFieldType())) {
            return null; // It's an aggregate
        }
        return metadata.getFieldType();
    }

    /**
     * Sets the dependency specification. If the dependency has already a specification set,
     * throw an error if the current specification and the given one are not equal.
     * @param dep the dependency
     * @param className the service specification
     * @throws ConfigurationException if the given specification is not loadable or if the dependency has already a
     * specification set that is not the given one.
     */
    private static void setSpecification(Dependency dep, String className) throws ConfigurationException {
        if (dep.getSpecification() != null  && ! dep.getSpecification().getName().equals(className)) {
            throw new ConfigurationException("Inconsistent service specification for " + DependencyHandler
                    .getDependencyIdentifier(dep) + " - reason: mismatch between the current specification (" + dep
                    .getSpecification().getName() + ") and the discovered specification (" + className + ")");
        } else if (dep.getSpecification() == null) {
            // Set the specification
            try {
                dep.setSpecification(dep.getBundleContext().getBundle().loadClass(className));
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Cannot set the service specification of " + DependencyHandler
                        .getDependencyIdentifier(dep) + " - reason: the class " + className + " cannot be loaded from" +
                        " the bundle " + dep.getBundleContext().getBundle().getBundleId(), e);
            }
        }
    }

}
