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
package org.apache.felix.dm.lambda.samples.dictionary;

import static java.lang.System.out;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_FUNCTION;
import static org.apache.felix.service.command.CommandProcessor.COMMAND_SCOPE;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Activator extends DependencyManagerActivator {
    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
    	out.println("type \"log info\" to see the logs emitted by this test.");

        // Create the factory configuration for our DictionaryImpl service. An instance of the DictionaryImpl is created for each
    	// factory configuration that you add from webconsole (using the DictionaryConfiguration factory pid).
        factoryPidAdapter(adapter -> adapter
            .impl(DictionaryImpl.class)
            .provides(DictionaryService.class)
            .propagate()
            .update(DictionaryConfiguration.class, DictionaryImpl::updated)
            .withSvc(LogService.class, true));
                            
        // Create the Dictionary Aspect that decorates any registered Dictionary service. For each Dictionary, an instance of the 
        // DictionaryAspect service is created).
        aspect(DictionaryService.class, aspect -> aspect
            .impl(DictionaryAspect.class)
            .filter("(lang=en)").rank(10)
            .withCnf(conf -> conf.update(DictionaryAspectConfiguration.class, DictionaryAspect::addWords))
            .withSvc(LogService.class, true));
                    
        // Create the SpellChecker component. It depends on all available DictionaryService instances, possibly
        // decorated by some DictionaryAspects.
        component(comp -> comp
            .impl(SpellChecker.class)
            .provides(SpellChecker.class, COMMAND_SCOPE, "dictionary", COMMAND_FUNCTION, new String[] {"spellcheck"}) 
            .withSvc(true, LogService.class, DictionaryService.class));
    }
}
