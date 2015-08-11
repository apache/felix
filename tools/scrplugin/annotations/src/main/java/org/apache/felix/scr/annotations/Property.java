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
package org.apache.felix.scr.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The <code>Property</code> annotation defines properties which are made
 * available to the component through the ComponentContext.getProperties()
 * method. These annotations are not strictly required but may be used by
 * components to defined initial configuration. Additionally properties may be
 * set here to identify the component if it is registered as a service, for
 * example the service.description and service.vendor properties.
 * <p>
 * This tag is used to declare &lt;property&gt; elements of the component
 * declaration. See section 112.4.5, Properties and Property Elements, in the
 * OSGi Service Platform Service Compendium Specification for more information.
 */
@Target( { ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface Property {

    /**
     * The name of the property
     */
    String name() default "";

    /**
     * The label to display in a form to configure this property. This name may
     * be localized by prepending a % sign to the name. Default value:
     * %&lt;name&gt;.name
     */
    String label() default "";

    /**
     * A descriptive text to provide the client in a form to configure this
     * property. This name may be localized by prepending a % sign to the name.
     * Default value: %&lt;name&gt;.description
     */
    String description() default "";

    /**
     * The value(s) of the property. If the property type is not String, parsing
     * of the value is done using the valueOf(String) method of the class
     * defined by the property type.
     * This attribute should not be used in combination with any of the other
     * value attributes.
     */
    String[] value() default {};

    /**
     * The class value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     * @since 1.5
     */
    Class<?>[] classValue() default {};

    /**
     * The long value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    long[] longValue() default {};

    /**
     * The double value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes.
     */
    double[] doubleValue() default {};

    /**
     * The float value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    float[] floatValue() default {};

    /**
     * The int value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    int[] intValue() default {};

    /**
     * The byte value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    byte[] byteValue() default {};

    /**
     * The char value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    char[] charValue() default {};

    /**
     * The bool value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    boolean[] boolValue() default {};

    /**
     * The short value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     */
    short[] shortValue() default {};

    /**
     * The password value(s) of the property.
     * This attribute should not be used in combination with any of the other
     * value attributes or the type attribute.
     * @since 1.8.0
     */
    String[] passwordValue() default {};

    /**
     * Defines the cardinality of the property and its collection type. If the
     * cardinality is negative, the property is expected to be stored in a
     * {@link java.util.Vector} (primitive types such as boolean are boxed in
     * the Wrapper class), if the cardinality is positive, the property is
     * stored in an array (primitve types are unboxed, that is Boolean type
     * values are stored in boolean[]). The actual value defines the maximum
     * number of elements in the vector or array. If the cardinality is
     * zero, the property is a scalar value. If the defined value of the
     * property is set in the value attribute, the cardinality defaults to 0
     * (zero for scalar value). If the property is defined in one or more
     * properties starting with values, the cardinality defaults to
     * an unbounded array.
     */
    int cardinality() default 0;

    /**
     * This defines if the property is unbounded. The property can either be
     * an unbounded array or vector. If this attribute is set to any other
     * value than {@link PropertyUnbounded#DEFAULT},
     * this overrides the {@link #cardinality()}.
     * @since 1.4
     */
    PropertyUnbounded unbounded() default PropertyUnbounded.DEFAULT;

    /**
     * Boolean flag defining whether a metatype descriptor entry should be
     * generated for this property or not. By default a metatype descriptor
     * entry, i.e. an <code>AD</code> element, is generated. If a property
     * should not be available for display in a configuration user interface,
     * this parameter should be set to <code>true</code>. For properties names
     * <code>service.ranking</code> the <code>AD</code> element is not created
     * by default, which can be overwritten by stating
     * <code>propertyPrivate=false</code>. For the predefined properties
     * <code>service.pid</code>, <code>service.description</code>,
     * <code>service.id</code>, <code>service.vendor</code>,
     * <code>service.bundlelocation</code> and <code>service.factoryPid</code>
     * an <code>AD</code> element will never be created and the
     * <code>propertyPrivate</code> attribute has no effect.
     */
    boolean propertyPrivate() default false;

    /**
     * Some properties may only be set to a set of possible values. To support
     * user interfaces which provide a selection list of values or a list of
     * checkboxes the option values and labels may be defined as parameters to
     * the {@link Property} annotation. All parameters in the form of name-value
     * pairs are used to build the list of available value options. The
     * parameter name is used as the value while the parameter value is used as
     * the label in the user interface. This label may be prepended with a %
     * sign to localize the string.
     * <p>
     * The options are written to the metatype.xml file as Option elements
     * inside the AD element defining the property. The name of the parameter
     * will be used for the Option.value attribute while the value of the
     * parameter defines the Option.label attribute.
     */
    PropertyOption[] options() default {};

}
