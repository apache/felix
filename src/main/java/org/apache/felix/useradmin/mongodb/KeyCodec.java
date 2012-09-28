/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.mongodb;


/**
 * Provides a small utility class for encoding/decoding keys used directly in 
 * MongoDB.
 * <p>
 * MongoDB does not allow keys to have '.' and '$' (amongst others?) in their 
 * name, so we need to ensure that those characters are somehow encoded.
 * </p>
 * 
 * @see http://www.mongodb.org/display/DOCS/Legal+Key+Names
 * @see http://www.yoyobrain.com/flashcards/show/261984
 */
final class KeyCodec {
    
    private static final String ENCODED_NULL = "%00";
    private static final String ENCODED_UNDERSCORE = "%5F";
    private static final String ENCODED_DOT = "%2E";
    private static final String ENCODED_DOLLAR = "%24"; 
    
    /**
     * Encodes a given key by replacing all '.' and '$' with their URL encoded variants.
     * 
     * @param input the input to encode, may be <code>null</code>.
     * @return the encoded input, can be <code>null</code>.
     */
    public static String encode(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == 0) {
                sb.append(ENCODED_NULL);
            } else if (c == '_') {
                sb.append(ENCODED_UNDERSCORE);
            } else if (c == '.') {
                sb.append(ENCODED_DOT);
            } else if (c == '$') {
                sb.append(ENCODED_DOLLAR);
            } else if (c == '%') {
                // escape all '%' as well...
                sb.append("%%");
            } else {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Decodes a given key by replacing all URL encoded '.' and '$' entities with their real characters.
     * 
     * @param input the input to decode, may be <code>null</code>.
     * @return the decoded input, can be <code>null</code>.
     */
    public static String decode(String input) {
        if (input == null) {
            return null;
        }
        
        boolean percentSeen = false;
        int length = input.length();
        char oldC = input.charAt(0);
        
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 1; i < length; i++) {
            char c = input.charAt(i);
            if (oldC == '%') {
                if (c == '%') {
                    // Escaped percent...
                    sb.append("%");
                    c = 0; // erase percent...
                    percentSeen = false;
                } else {
                    percentSeen = true;
                }
            } else if (c == '%') {
                percentSeen = true;
            } else if (percentSeen) {
                if (oldC == '0' && c == '0') {
                    // Encoded null-character
                    sb.append((char) 0);
                    percentSeen = false;
                } else if (oldC == '2' && c == '4') {
                    // Encoded dollar...
                    sb.append("$");
                    percentSeen = false;
                } else if (oldC == '2' && c == 'E') {
                    // Encoded dot...
                    sb.append(".");
                    percentSeen = false;
                } else if (oldC == '5' && c == 'F') {
                    // Encoded underscore...
                    sb.append("_");
                    percentSeen = false;
                } else {
                    // Unknown encoded entity...
                    sb.append("%").append(oldC).append(c);
                    percentSeen = false;
                }
            } else {
                if (i == 1) {
                    sb.append(oldC);
                }
                sb.append(c);
            }

            if (percentSeen && (i == length - 1)) {
                // At the end; incomplete entity found...
                if (oldC != '%' && c != '%') {
                    sb.append("%").append(oldC).append(c);
                } else if (oldC == '%' && c != '%') {
                    sb.append("%").append(c);
                } else if (c == '%') {
                    sb.append("%");
                }
            }

            oldC = c;
        }

        return sb.toString();
    }
}
