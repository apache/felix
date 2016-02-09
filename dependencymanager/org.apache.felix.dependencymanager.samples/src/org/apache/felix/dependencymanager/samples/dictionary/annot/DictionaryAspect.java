/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dependencymanager.samples.dictionary.annot;

import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.log.LogService;

/**
 * This aspect applies to the English DictionaryService, and allows to decorate it with some
 * custom English words, which are configurable from WebConsole.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@AspectService(ranking = 10, filter = "(lang=en)")
public class DictionaryAspect implements DictionaryService {
    /**
     * This is the service this aspect is applying to.
     */
    private volatile DictionaryService m_originalDictionary;

    /**
     * We store all configured words in a thread-safe data structure, because ConfigAdmin may
     * invoke our updated method at any time.
     */
    private CopyOnWriteArrayList<String> m_words = new CopyOnWriteArrayList<String>();

    /**
     * We'll use the OSGi log service for logging. If no log service is available, then we'll
     * use a NullObject.
     */
    @ServiceDependency(required = false)
    private LogService m_log;

    /**
     * Defines a configuration dependency for retrieving our english custom words (by default,
     * our PID is our full class name).
     */
    @ConfigurationDependency(propagate = false)
    protected void updated(DictionaryAspectConfiguration cnf) {
        if (cnf != null) {
            m_words.clear();
            for (String word : cnf.words()) {
                m_words.add(word);
            }
        }
    }

    /**
     * Our Aspect Service is starting and is about to be registered in the OSGi regsitry.
     */
    @Start
    protected void start() {
        m_log.log(LogService.LOG_INFO, "Starting aspect Dictionary with words: " + m_words
            + "; original dictionary service=" + m_originalDictionary);
    }

    /**
     * Checks if a word is found from our custom word list. if not, delegate to the decorated
     * dictionary.
     */
    public boolean checkWord(String word) {
        m_log.log(LogService.LOG_INFO, "DictionaryAspect: checking word " + word + " (original dictionary="
            + m_originalDictionary + ")");
        if (m_words.contains(word)) {
            return true;
        }
        return m_originalDictionary.checkWord(word);
    }

    public String toString() {
        return "DictionaryAspect: words=" + m_words + "; original dictionary=" + m_originalDictionary;
    }
}
