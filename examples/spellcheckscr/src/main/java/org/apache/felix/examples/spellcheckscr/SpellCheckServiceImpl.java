/*
 *   Copyright 2006-2016 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.examples.spellcheckscr;


import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.felix.examples.dictionaryservice.DictionaryService;
import org.apache.felix.examples.spellcheckservice.SpellCheckService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;


/**
 * This class re-implements the spell check service of Example 6. This service
 * implementation behaves exactly like the one in Example 6, specifically, it
 * aggregates all available dictionary services, monitors their dynamic
 * availability, and only offers the spell check service if there are dictionary
 * services available. The service implementation is greatly simplified, though,
 * by using the Service Component Runtime. Notice that there is no OSGi references in the
 * application code; instead, the annotations describe the service
 * dependencies to the Service Component Runtime, which automatically manages them and it
 * also automatically registers the spell check services as appropriate.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
public class SpellCheckServiceImpl implements SpellCheckService
{
    /**
     * List of service objects.
     *
     * This field is managed by the Service Component Runtime and updated
     * with the current set of available dictionary services.
     * At least one dictionary service is required.
     */
    @Reference(policy=ReferencePolicy.DYNAMIC, cardinality=ReferenceCardinality.AT_LEAST_ONE)
    private volatile List<DictionaryService> m_svcObjList;

    /**
     * Checks a given passage for spelling errors. A passage is any number of
     * words separated by a space and any of the following punctuation marks:
     * comma (,), period (.), exclamation mark (!), question mark (?),
     * semi-colon (;), and colon(:).
     *
     * @param passage
     *            the passage to spell check.
     * @return An array of misspelled words or null if no words are misspelled.
     */
    public String[] check( String passage )
    {
        // No misspelled words for an empty string.
        if ( ( passage == null ) || ( passage.length() == 0 ) )
        {
            return null;
        }

        List<String> errorList = new ArrayList<String>();

        // Tokenize the passage using spaces and punctionation.
        StringTokenizer st = new StringTokenizer( passage, " ,.!?;:" );

        // Put the current set of services in a local field
        // the field m_svcObjList might be modified concurrently
        final List<DictionaryService> localServices = m_svcObjList;

        // Loop through each word in the passage.
        while ( st.hasMoreTokens() )
        {
            String word = st.nextToken();
            boolean correct = false;

            // Check each available dictionary for the current word.
            for(final DictionaryService dictionary : localServices) {
                if ( dictionary.checkWord( word ) )
                {
                    correct = true;
                    break;
                }
            }

            // If the word is not correct, then add it
            // to the incorrect word list.
            if ( !correct )
            {
                errorList.add( word );
            }
        }

        // Return null if no words are incorrect.
        if ( errorList.isEmpty() )
        {
            return null;
        }

        // Return the array of incorrect words.
        return errorList.toArray( new String[errorList.size()] );
    }
}
