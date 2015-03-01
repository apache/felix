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
package org.apache.felix.dependencymanager.samples.dictionary.api;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.felix.service.command.Descriptor;
import org.osgi.service.log.LogService;

/**
 * Felix "spellcheck" Gogo Shell Command. This command allows to check if some given words are valid or not.
 * This command will be activated only if (at least) one DictionaryService has been injected.
 * To create a Dictionary Service, you have to go the the web console and add a configuration in the 
 * "Dictionary Configuration" factory pid.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SpellChecker {
    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll use a NullObject.
     */
    private volatile LogService m_log;

    /**
     * We'll store all Dictionaries in a concurrent list, in order to avoid method synchronization.
     * (Auto-Injected from Activator, at any time).
     */
    private final Iterable<DictionaryService> m_dictionaries = new ConcurrentLinkedQueue<>();

    /**
     * Lifecycle method callback, used to check if our service has been activated.
     */
    protected void start() {
        m_log.log(LogService.LOG_WARNING, "Spell Checker started");
    }

    /**
     * Lifecycle method callback, used to check if our service has been activated.
     */
    protected void stop() {
        m_log.log(LogService.LOG_WARNING, "Spell Checker stopped");
    }

    // --- Gogo Shell command

    @Descriptor("checks if word is found from an available dictionary")
    public void spellcheck(@Descriptor("the word to check") String word) {
        m_log.log(LogService.LOG_INFO, "Checking spelling of word \"" + word + "\" using the following dictionaries: "
            + m_dictionaries);

        for (DictionaryService dictionary : m_dictionaries) {
            if (dictionary.checkWord(word)) {
                System.out.println("word " + word + " is correct");
                return;
            }
        }
        System.err.println("word " + word + " is incorrect");
    }
}
