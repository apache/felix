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

import java.util.List;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * This interface describes the configuration for our DictionaryImpl component. We are using the bnd metatype
 * annotations, allowing to configure our Dictionary Services from web console.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@OCD(name="Spell Checker Dictionary",
     factory = true, 
     description = "Declare here some Dictionary instances, allowing to instantiates some DictionaryService services for a given dictionary language")
public interface DictionaryConfiguration {
    @AD(description = "Describes the dictionary language", deflt = "en")
    String lang();

    @AD(description = "Declare here the list of words supported by this dictionary.")
    List<String> words();
}
