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
package org.apache.felix.resolver.test.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClauseParser {

    private static final char EOF = (char) -1;

    private static char charAt(int pos, String headers, int length)
    {
        if (pos >= length)
        {
            return EOF;
        }
        return headers.charAt(pos);
    }

    private static final int CLAUSE_START = 0;
    private static final int PARAMETER_START = 1;
    private static final int KEY = 2;
    private static final int DIRECTIVE_OR_TYPEDATTRIBUTE = 4;
    private static final int ARGUMENT = 8;
    private static final int VALUE = 16;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<ParsedHeaderClause> parseStandardHeader(String header)
    {
        List<ParsedHeaderClause> clauses = new ArrayList<ParsedHeaderClause>();
        if (header == null)
        {
            return clauses;
        }
        ParsedHeaderClause clause = null;
        String key = null;
        Map targetMap = null;
        int state = CLAUSE_START;
        int currentPosition = 0;
        int startPosition = 0;
        int length = header.length();
        boolean quoted = false;
        boolean escaped = false;

        char currentChar = EOF;
        do
        {
            currentChar = charAt(currentPosition, header, length);
            switch (state)
            {
            case CLAUSE_START:
                clause = new ParsedHeaderClause();
                clauses.add(clause);
                state = PARAMETER_START;
            case PARAMETER_START:
                startPosition = currentPosition;
                state = KEY;
            case KEY:
                switch (currentChar)
                {
                case ':':
                case '=':
                    key = header.substring(startPosition, currentPosition).trim();
                    startPosition = currentPosition + 1;
                    targetMap = clause.attrs;
                    state = currentChar == ':' ? DIRECTIVE_OR_TYPEDATTRIBUTE : ARGUMENT;
                    break;
                case EOF:
                case ',':
                case ';':
                    clause.paths.add(header.substring(startPosition, currentPosition).trim());
                    state = currentChar == ',' ? CLAUSE_START : PARAMETER_START;
                    break;
                default:
                    break;
                }
                currentPosition++;
                break;
            case DIRECTIVE_OR_TYPEDATTRIBUTE:
                switch(currentChar)
                {
                case '=':
                    if (startPosition != currentPosition)
                    {
                        clause.types.put(key, header.substring(startPosition, currentPosition).trim());
                    }
                    else
                    {
                        targetMap = clause.dirs;
                    }
                    state = ARGUMENT;
                    startPosition = currentPosition + 1;
                    break;
                default:
                    break;
                }
                currentPosition++;
                break;
            case ARGUMENT:
                if (currentChar == '\"')
                {
                    quoted = true;
                    currentPosition++;
                }
                else
                {
                    quoted = false;
                }
                if (!Character.isWhitespace(currentChar)) {
                    state = VALUE;
                }
                else {
                    currentPosition++;
                }
                break;
            case VALUE:
                if (escaped)
                {
                    escaped = false;
                }
                else
                {
                    if (currentChar == '\\' )
                    {
                        escaped = true;
                    }
                    else if (quoted && currentChar == '\"')
                    {
                        quoted = false;
                    }
                    else if (!quoted)
                    {
                        String value = null;
                        switch(currentChar)
                        {
                        case EOF:
                        case ';':
                        case ',':
                            value = header.substring(startPosition, currentPosition).trim();
                            if (value.startsWith("\"") && value.endsWith("\""))
                            {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (targetMap.put(key, value) != null)
                            {
                                throw new IllegalArgumentException(
                                        "Duplicate '" + key + "' in: " + header);
                            }
                            state = currentChar == ';' ? PARAMETER_START : CLAUSE_START;
                            break;
                        default:
                            break;
                        }
                    }
                }
                currentPosition++;
                break;
            default:
                break;
            }
        } while ( currentChar != EOF);

        if (state > PARAMETER_START)
        {
            throw new IllegalArgumentException("Unable to parse header: " + header);
        }
        return clauses;
    }

    public static List<String> parseDelimitedString(String value, String delim)
    {
        return parseDelimitedString(value, delim, true);
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return a list of string or an empty list if there are none.
     **/
    public static List<String> parseDelimitedString(String value, String delim, boolean trim)
    {
        if (value == null)
        {
            value = "";
        }

        List<String> list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\'))
            {
                isEscaped = true;
                continue;
            }

            if (isEscaped)
            {
                sb.append(c);
            }
            else if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                if (trim)
                {
                    list.add(sb.toString().trim());
                }
                else
                {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if ((c == '"') && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if ((c == '"') && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }

            isEscaped = false;
        }

        if (sb.length() > 0)
        {
            if (trim)
            {
                list.add(sb.toString().trim());
            }
            else
            {
                list.add(sb.toString());
            }
        }

        return list;
    }


    public static class ParsedHeaderClause {
        public final List<String> paths = new ArrayList<String>();
        public final Map<String, String> dirs = new LinkedHashMap<String, String>();
        public final Map<String, Object> attrs = new LinkedHashMap<String, Object>();
        public final Map<String, String> types = new LinkedHashMap<String, String>();
    }

}
