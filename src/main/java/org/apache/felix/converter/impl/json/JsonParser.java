/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.converter.impl.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.converter.impl.Util;

/**
 * A very small JSON parser.
 *
 * The JSON input is parsed into an object structure in the following way:
 * <ul>
 * <li>Object names are represented as a {@link String}.
 * <li>String values are represented as a {@link String}.
 * <li>Numeric values are represented as a {@link Long} (TODO support floats).
 * <li>Boolean values are represented as a {@link Boolean}.
 * <li>Nested JSON objects are parsed into a {@link java.util.Map Map&lt;String, Object&gt;}.
 * <li>JSON lists are parsed into a {@link java.util.List} which may contain any of the above values.
 * </ul>
 */
public class JsonParser {
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^\\s*[\"](.+?)[\"]\\s*[:]\\s*(.+)$");

    private enum Scope { QUOTE, CURLY, BRACKET;
        static Scope getScope(char c) {
            switch (c) {
            case '"':
                return QUOTE;
            case '[':
            case ']':
                return BRACKET;
            case '{':
            case '}':
                return CURLY;
            default:
                return null;
            }
        }
    }

    static class Pair<K, V> {
        final K key;
        final V value;

        Pair(K k, V v) {
            key = k;
            value = v;
        }
    }

    private final Map<String, Object> parsed;

    public JsonParser(CharSequence json) {
        String str = json.toString();
        str = str.trim().replace('\n', ' ');
        parsed = parseObject(str);
    }

    public JsonParser(InputStream is) throws IOException {
        this(readStreamAsString(is));
    }

    public Map<String, Object> getParsed() {
        return parsed;
    }

    private static Pair<String, Object> parseKeyValue(String jsonKeyValue) {
        Matcher matcher = KEY_VALUE_PATTERN.matcher(jsonKeyValue);
        if (!matcher.matches() || matcher.groupCount() < 2) {
            throw new IllegalArgumentException("Malformatted JSON key-value pair: " + jsonKeyValue);
        }

        return new Pair<>(matcher.group(1), parseValue(matcher.group(2)));
    }

    private static Object parseValue(String jsonValue) {
        jsonValue = jsonValue.trim();

        switch (jsonValue.charAt(0)) {
        case '\"':
            if (!jsonValue.endsWith("\""))
                throw new IllegalArgumentException("Malformatted JSON string: " + jsonValue);

            return jsonValue.substring(1, jsonValue.length() - 1);
        case '[':
            List<Object> entries = new ArrayList<>();
            for (String v : parseListValuesRaw(jsonValue)) {
                entries.add(parseValue(v));
            }
            return entries;
        case '{':
            return parseObject(jsonValue);
        case 't':
        case 'T':
        case 'f':
        case 'F':
            return Boolean.parseBoolean(jsonValue);
        case 'n':
        case 'N':
            return null;
        default:
            return Long.parseLong(jsonValue);
        }
    }

    private static Map<String, Object> parseObject(String jsonObject) {
        if (!(jsonObject.startsWith("{") && jsonObject.endsWith("}")))
            throw new IllegalArgumentException("Malformatted JSON object: " + jsonObject);

        jsonObject = jsonObject.substring(1, jsonObject.length() - 1);
        Map<String, Object> values = new HashMap<>();
        for (String element : parseKeyValueListRaw(jsonObject)) {
            Pair<String, Object> pair = parseKeyValue(element);
            values.put(pair.key, pair.value);
        }

        return values;
    }

    private static List<String> parseKeyValueListRaw(String jsonKeyValueList) {
        jsonKeyValueList = jsonKeyValueList + ","; // append comma to simplify parsing
        List<String> elements = new ArrayList<>();

        int i=0;
        int start=0;
        Stack<Scope> scopeStack = new Stack<>();
        while (i < jsonKeyValueList.length()) {
            char curChar = jsonKeyValueList.charAt(i);
            switch (curChar) {
            case '"':
                if (i > 0 && jsonKeyValueList.charAt(i-1) == '\\') {
                    // it's escaped, ignore for now
                } else {
                    if (!scopeStack.empty() && scopeStack.peek() == Scope.QUOTE) {
                        scopeStack.pop();
                    } else {
                        scopeStack.push(Scope.QUOTE);
                    }
                }
                break;
            case '[':
            case '{':
                if ((scopeStack.empty() ? null : scopeStack.peek()) == Scope.QUOTE) {
                    // inside quotes, ignore
                } else {
                    scopeStack.push(Scope.getScope(curChar));
                }
                break;
            case ']':
            case '}':
                Scope curScope = scopeStack.empty() ? null : scopeStack.peek();
                if (curScope == Scope.QUOTE) {
                    // inside quotes, ignore
                } else {
                    Scope newScope = Scope.getScope(curChar);
                    if (curScope == newScope) {
                        scopeStack.pop();
                    } else {
                        throw new IllegalArgumentException("Unbalanced closing " +
                            curChar + " in: " + jsonKeyValueList);
                    }
                }
                break;
            case ',':
                if (scopeStack.empty()) {
                    elements.add(jsonKeyValueList.substring(start, i));
                    start = i+1;
                }
                break;
            }

            i++;
        }
        return elements;
    }

    private static List<String> parseListValuesRaw(String jsonList) {
        if (!(jsonList.startsWith("[") && jsonList.endsWith("]")))
            throw new IllegalArgumentException("Malformatted JSON list: " + jsonList);

        jsonList = jsonList.substring(1, jsonList.length() - 1);
        return parseKeyValueListRaw(jsonList);
    }

    private static String readStreamAsString(InputStream is) throws IOException {
        byte [] bytes = Util.readStream(is);
        if (bytes.length < 5)
            // need at least 5 bytes to establish the encoding
            throw new IllegalArgumentException("Malformatted JSON");

        int offset = 0;
        if ((bytes[0] == -1 && bytes[1] == -2)
            || (bytes[0] == -2 && bytes[1] == -1)) {
            // Skip UTF16/UTF32 Byte Order Mark (BOM)
            offset = 2;
        }

        /* Infer the encoding as described in section 3 of http://www.ietf.org/rfc/rfc4627.txt
         * which reads:
         *   Encoding
         *
         *   JSON text SHALL be encoded in Unicode.  The default encoding is
         *   UTF-8.
         *
         *   Since the first two characters of a JSON text will always be ASCII
         *   characters [RFC0020], it is possible to determine whether an octet
         *   stream is UTF-8, UTF-16 (BE or LE), or UTF-32 (BE or LE) by looking
         *   at the pattern of nulls in the first four octets.
         *
         *         00 00 00 xx  UTF-32BE
         *         00 xx 00 xx  UTF-16BE
         *         xx 00 00 00  UTF-32LE
         *         xx 00 xx 00  UTF-16LE
         *         xx xx xx xx  UTF-8
         */
        String encoding;
        if (bytes[offset + 2] == 0) {
            if (bytes[offset + 1] != 0) {
                encoding = "UTF-16";
            } else {
                encoding = "UTF-32";
            }
        } else if (bytes[offset + 1] == 0) {
            encoding = "UTF-16";
        } else {
            encoding = "UTF-8";
        }
        return new String(bytes, encoding);
    }
}