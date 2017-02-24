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
package org.apache.felix.utils.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very small JSON parser.
 *
 * The JSON input is parsed into an object structure in the following way:
 * <ul>
 * <li>Object names are represented as a {@link String}.
 * <li>String values are represented as a {@link String}.
 * <li>Numeric values without a decimal separator are represented as a {@link Long}.
 * <li>Numeric values with a decimal separator are represented as a {@link Double}.
 * <li>Boolean values are represented as a {@link Boolean}.
 * <li>Nested JSON objects are parsed into a {@link java.util.Map Map&lt;String, Object&gt;}.
 * <li>JSON lists are parsed into a {@link java.util.List} which may contain any of the above values.
 * </ul>
 */
public class JSONParser {
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

    public JSONParser(CharSequence json) {
        String str = json.toString();
        str = str.trim().replace('\n', ' ');
        parsed = parseObject(str);
    }

    public JSONParser(InputStream is) throws IOException {
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

        return new Pair<String, Object>(unEscapeString(matcher.group(1)), parseValue(matcher.group(2)));
    }

    private static Object parseValue(String jsonValue) {
        jsonValue = jsonValue.trim();

        switch (jsonValue.charAt(0)) {
        case '\"':
            if (!jsonValue.endsWith("\""))
                throw new IllegalArgumentException("Malformatted JSON string: " + jsonValue);

            return unEscapeString(jsonValue.substring(1, jsonValue.length() - 1));
        case '[':
            List<Object> entries = new ArrayList<Object>();
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
            if (jsonValue.contains(".")) {
                return Double.parseDouble(jsonValue);
            }
            return Long.parseLong(jsonValue);
        }
    }

    private static String unEscapeString(String s) {
        StringBuilder sb = new StringBuilder(s);

        for (int i = 0; i<sb.length(); i++) {
            if (sb.charAt(i) == '\\' && sb.length() > i+1) {
                sb.deleteCharAt(i);

                char nextChar = sb.charAt(i);
                switch (nextChar) {
                case 'b':
                    sb.setCharAt(i, '\b');
                    break;
                case 'f':
                    sb.setCharAt(i, '\f');
                    break;
                case 'n':
                    sb.setCharAt(i, '\n');
                    break;
                case 'r':
                    sb.setCharAt(i, '\r');
                    break;
                case 't':
                    sb.setCharAt(i, '\t');
                    break;
                case 'u':
                    if (sb.length() > i+4) {
                        int uc = Integer.parseInt(sb.substring(i+1, i+5), 16);
                        sb.replace(i, i+5, "" + (char) uc);
                    }
                    break;
                }
            }
        }

        return sb.toString();
    }

    private static Map<String, Object> parseObject(String jsonObject) {
        if (!(jsonObject.startsWith("{") && jsonObject.endsWith("}")))
            throw new IllegalArgumentException("Malformatted JSON object: " + jsonObject);

        Map<String, Object> values = new HashMap<String, Object>();

        jsonObject = jsonObject.substring(1, jsonObject.length() - 1).trim();
        if (jsonObject.length() == 0)
            return values;

        for (String element : parseKeyValueListRaw(jsonObject)) {
            Pair<String, Object> pair = parseKeyValue(element);
            values.put(pair.key, pair.value);
        }

        return values;
    }

    private static List<String> parseKeyValueListRaw(String jsonKeyValueList) {
        if (jsonKeyValueList.trim().length() == 0)
            return Collections.emptyList();
        jsonKeyValueList = jsonKeyValueList + ","; // append comma to simplify parsing
        List<String> elements = new ArrayList<String>();

        int i=0;
        int start=0;
        Stack<Scope> scopeStack = new Stack<Scope>();
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

    /**
     * Read an entire input stream into a byte array.
     * @param is The input stream to read.
     * @return The byte array with the contents of the input stream.
     * @throws IOException if the underlying read operation on the input stream
     * throws an error.
     */
    private static byte [] readStream(InputStream is) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[65536];

            int length = 0;
            int offset = 0;

            while ((length = is.read(bytes, offset, bytes.length - offset)) != -1) {
                offset += length;

                if (offset == bytes.length) {
                    baos.write(bytes, 0, bytes.length);
                    offset = 0;
                }
            }
            if (offset != 0) {
                baos.write(bytes, 0, offset);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    private static String readStreamAsString(InputStream is) throws IOException {
        byte [] bytes = readStream(is);
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