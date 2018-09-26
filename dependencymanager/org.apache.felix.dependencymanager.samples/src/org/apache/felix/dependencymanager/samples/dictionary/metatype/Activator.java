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
package org.apache.felix.dependencymanager.samples.dictionary.metatype;

import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyActivatorBase {
    @Override
    public void init(BundleContext context, DependencyManager dm) throws Exception {
        // Create the factory configuration for our DictionaryImpl service.
        // We also provide metatype info in order to be able to configure our factory configuration
        // from config admin.
                
        Component factory = createFactoryComponent()
            .setFactoryPid(DictionaryConfiguration.class)
            .setPropagate(true)
            .setConfigType(DictionaryConfiguration.class)                
            .setHeading("Spell Checker Dictionary")
            .setDesc("Declare here some Dictionary instances, allowing to instantiates some DictionaryService services for a given dictionary language")
            .add(dm.createPropertyMetaData().setId("lang").setHeading("language").setDefaults("en").setDescription("Enter the dictionary language"))
            .add(dm.createPropertyMetaData().setId("words").setHeading("words database").setDescription("Enter the dictionary words (comma separated)"))
            .setInterface(DictionaryService.class.getName(), null)
            .setImplementation(DictionaryImpl.class)
            .add(createServiceDependency().setService(LogService.class)); // Null Object
            
        dm.add(factory);
        
        // Create the Dictionary Aspect
        Component aspect = createAspectComponent()
        	.setAspect(DictionaryService.class, "(lang=en)", 10)
            .setImplementation(DictionaryAspect.class)
            .add(createServiceDependency().setService(LogService.class)) // NullObject
            .add(createConfigurationDependency()
                .setPid(DictionaryAspectConfiguration.class.getName()).setCallback("updated", DictionaryConfiguration.class)
                .setHeading("Spell Checker Aspect Dictionary")
                .setDescription("Enter english words (comma separated)")
                .add(dm.createPropertyMetaData().setId("words").setHeading("Dictionary aspect words").setDescription("Enter enlish words")));
        dm.add(aspect);
        
        // Create the SpellChecker component
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, "dictionary");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "spellcheck" });
        dm.add(createComponent()
            .setImplementation(SpellChecker.class)
            .setInterface(SpellChecker.class.getName(), props)
            .add(createServiceDependency().setService(DictionaryService.class).setRequired(true))
            .add(createServiceDependency().setService(LogService.class))); // NullObject
    }
}
