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
package org.apache.felix.dependencymanager.samples.dictionary.annot;

import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

/**
 * A Dictionary Service. This service uses a FactoryConfigurationAdapterService annotation, 
 * allowing to instantiate this service from webconsole. This annotation will actually register
 * a ManagedServiceFactory in the registry. The Configuration metatype informations is described using the
 * bnd metatype information (see the DictionaryConfiguration interface).
 * 
 * You must configure at least one Dictionary from web console, since the SpellCheck won't start if no Dictionary
 * Service is available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@FactoryConfigurationAdapterService(configType = DictionaryConfiguration.class, propagate = true, updated = "updated")
public class DictionaryImpl implements DictionaryService {
    /**
     * We store all configured words in a thread-safe data structure, because ConfigAdmin
     * may invoke our updated method at any time.
     */
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();

    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll use a NullObject.
     */
    @ServiceDependency(required = false)
    private LogService m_log;

    /**
     * Our Dictionary language.
     */
    private String m_lang;

    /**
     * Our service will be initialized from ConfigAdmin.
     * @param config The configuration where we'll lookup our words list (key=".words").
     */
    protected void updated(DictionaryConfiguration cnf) {
        m_lang = cnf.lang();
        m_words.clear();
        for (String word : cnf.words()) {
            m_words.add(word);
        }
    }

    /**
     * A new Dictionary Service is starting (because a new factory configuration has been created
     * from webconsole).
     */
    @Start
    protected void start() {
        m_log.log(LogService.LOG_INFO, "Starting Dictionary Service with language: " + m_lang);
    }

    /**
     * Check if a word exists if the list of words we have been configured from ConfigAdmin/WebConsole.
     */
    public boolean checkWord(String word) {
        return m_words.contains(word);
    }

    @Override
    public String toString() {
        return "Dictionary: language=" + m_lang + ", words=" + m_words;
    }
}
