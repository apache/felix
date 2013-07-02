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
package spell.checker;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import spell.services.DictionaryService;
import spell.services.SpellChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

@Component
@Provides
@Instantiate
public class SpellCheck implements SpellChecker {

    @Requires // This is a service dependency.
    private DictionaryService dictionary;

    /**
     * Implements SpellChecker.check(). Checks the given passage for misspelled words.
     *
     * @param passage the passage to spell check.
     * @return An array of misspelled words or null if no words are misspelled.
     */
    public String[] check(String passage) {
        // No misspelled words for an empty string.
        if ((passage == null) || (passage.length() == 0)) {
            return null;
        }

        List<String> errorList = new ArrayList<String>();

        // Tokenize the passage using spaces and punctuation.
        StringTokenizer st = new StringTokenizer(passage, " ,.!?;:");

        // Loop through each word in the passage.
        while (st.hasMoreTokens()) {
            String word = st.nextToken();

            // Check the current word.
            if (!dictionary.checkWord(word)) {
                // If the word is not correct, then add it
                // to the incorrect word list.
                errorList.add(word);
            }
        }

        // Return null if no words are incorrect.
        if (errorList.size() == 0) {
            return null;
        }

        // Return the array of incorrect words.
        System.out.println("Wrong words:" + errorList);
        return errorList.toArray(new String[errorList.size()]);
    }
}
