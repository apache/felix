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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotates a class that acts as a Factory Configuration Adapter Service. For each new <code>Config Admin</code> factory configuration matching
 * the specified factoryPid, an instance of this service will be created.
 * The adapter will be registered with the specified interface, and with the specified adapter service properties.
 * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
 * (which don't start with ".") will be propagated along with the adapter service properties. <p>
 * 
 * Like in &#64;{@link ConfigurationDependency}, you can optionally specify the meta types of your
 * configurations for Web Console GUI customization (configuration heading/descriptions/default values/etc ...).
 *
 * <h3>Usage Examples</h3>
 * Here, a "Dictionary" service instance is instantiated for each existing factory configuration
 * instances matching the factory pid "DictionaryServiceFactory".
 * <blockquote>
 * <pre>
 * &#64;FactoryConfigurationAdapterService(factoryPid="DictionaryServiceFactory", updated="updated")
 * public class DictionaryImpl implements DictionaryService
 * {
 *     &#47;**
 *      * The key of our config admin dictionary language.
 *      *&#47;
 *     final static String LANG = "lang";
 *     
 *     &#47;**
 *      * The key of our config admin dictionary values.
 *      *&#47;
 *     final static String WORDS = "words";
 *     
 *     &#47;**
 *      * We store all configured words in a thread-safe data structure, because ConfigAdmin
 *      * may invoke our updated method at any time.
 *      *&#47;
 *     private CopyOnWriteArrayList&#60;String&#62; m_words = new CopyOnWriteArrayList&#60;String&#62;();
 *     
 *     &#47;**
 *      * Our Dictionary language.
 *      *&#47;
 *     private String m_lang;
 * 
 *     protected void updated(Dictionary&#60;String, ?&#62; config) {
 *         m_lang = (String) config.get(LANG);
 *         m_words.clear();
 *         String[] words = (String[]) config.get(WORDS);
 *         for (String word : words) {
 *             m_words.add(word);
 *         }
 *     }   
 *     ...
 * }
 * </pre>
 * </blockquote>
 * Here, this is the same example as above, but using meta types:
 * 
 * <blockquote>
 * <pre>
 * &#64;FactoryConfigurationAdapterService(
 *     factoryPid="DictionaryServiceFactory", 
 *     propagate=true, 
 *     updated="updated",
 *     heading="Dictionary Services",
 *     description="Declare here some Dictionary instances, allowing to instantiates some DictionaryService services for a given dictionary language",
 *     metadata={
 *         &#64;PropertyMetaData(
 *             heading="Dictionary Language",
 *             description="Declare here the language supported by this dictionary. " +
 *                 "This property will be propagated with the Dictionary Service properties.",
 *             defaults={"en"},
 *             id=DictionaryImpl.LANG,
 *             cardinality=0),
 *         &#64;PropertyMetaData(
 *             heading="Dictionary words",
 *             description="Declare here the list of words supported by this dictionary. This properties starts with a Dot and won't be propagated with Dictionary OSGi service properties.",
 *             defaults={"hello", "world"},
 *             id=DictionaryImpl.WORDS,
 *             cardinality=Integer.MAX_VALUE)
 *     }
 * )  
 * public class DictionaryImpl implements DictionaryService
 * {
 *     &#47;**
 *      * The key of our config admin dictionary language.
 *      *&#47;
 *     final static String LANG = "lang";
 *     
 *     &#47;**
 *      * The key of our config admin dictionary values.
 *      *&#47;
 *     final static String WORDS = "words";
 *     
 *     &#47;**
 *      * We store all configured words in a thread-safe data structure, because ConfigAdmin
 *      * may invoke our updated method at any time.
 *      *&#47;
 *     private CopyOnWriteArrayList&#60;String&#62; m_words = new CopyOnWriteArrayList&#60;String&#62;();
 *     
 *     &#47;**
 *      * Our Dictionary language.
 *      *&#47;
 *     private String m_lang;
 * 
 *     protected void updated(Dictionary&#60;String, ?&#62; config) {
 *         m_lang = (String) config.get(LANG);
 *         m_words.clear();
 *         String[] words = (String[]) config.get(WORDS);
 *         for (String word : words) {
 *             m_words.add(word);
 *         }
 *     }
 *     
 *     ...
 * }
 * </pre>
 * </blockquote>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface FactoryConfigurationAdapterService
{
    /**
     * The interface(s) to use when registering adapters. By default, directly implemented 
     * interfaces will be registered in the OSGi registry.
     */
    Class<?>[] provides() default {};

    /**
     * Adapter Service properties. Notice that public factory configuration is also registered in service properties,
     * (only if propagate is true). Public factory configuration properties are those which don't starts with a dot (".").
     */
    Property[] properties() default {};

    /**
     * Returns the factory pid whose configurations will instantiate the annotated service class. (By default, the pid is the 
     * service class name).
     */
    String factoryPid() default "";

    /**
     * The Update method to invoke (defaulting to "updated"), when a factory configuration is created or updated
     */
    String updated() default "updated";

    /**
     * Returns true if the configuration properties must be published along with the service. 
     * Any additional service properties specified directly are merged with these.
     * @return true if configuration must be published along with the service, false if not.
     */
    boolean propagate() default false;

    /**
     * The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service".
     * @return The label used to display the tab name where the properties are displayed.
     */
    String heading() default "";

    /**
     * A human readable description of the PID this annotation is associated with. Example: "Configuration for the PrinterService bundle".
     * @return A human readable description of the PID this annotation is associated with.
     */
    String description() default "";

    /**
     * The list of properties types used to expose properties in web console. 
     * @return The list of properties types used to expose properties in web console. 
     */
    PropertyMetaData[] metadata() default {};
    
    /**
     * Sets the static method used to create the adapter instance.
     */
    String factoryMethod() default "";
}
