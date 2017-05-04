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
package org.apache.felix.gogo.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Freely adapted from Spring's AntPathMatcher.
 * We don't use the file system's glob PathMatcher
 * because it can't detect directories which can't be
 * a start of a match.
 */
public class GlobPathMatcher {

    /** Default path separator: "/" */
    public static final String DEFAULT_PATH_SEPARATOR = "/";

    private String pattern;
    private String pathSeparator;
    private boolean caseSensitive;

    private String[] pattDirs;
    private Pattern[] pattPats;

    /**
     * Create a new instance with the {@link #DEFAULT_PATH_SEPARATOR}.
     */
    public GlobPathMatcher(String pattern) {
        this(pattern, DEFAULT_PATH_SEPARATOR, true);
    }

    /**
     * A convenient, alternative constructor to use with a custom path separator.
     * @param pathSeparator the path separator to use, must not be {@code null}.
     */
    public GlobPathMatcher(String pattern, String pathSeparator, boolean caseSensitive) {
        Objects.requireNonNull(pathSeparator, "'pathSeparator' is required");
        this.pattern = pattern;
        this.pathSeparator = pathSeparator;
        this.caseSensitive = caseSensitive;
        this.pattDirs = tokenizePath(pattern);
        this.pattPats = new Pattern[pattDirs.length];
        for (int i = 0; i < pattDirs.length; i++) {
            pattPats[i] = createMatcherPattern(pattDirs[i]);
        }
    }


    /**
     * Actually match the given {@code path} against the given {@code pattern}.
     * @param path the path String to test
     * @param fullMatch whether a full pattern match is required (else a pattern match
     * as far as the given base path goes is sufficient)
     * @return {@code true} if the supplied {@code path} matched, {@code false} if it didn't
     */
    public boolean matches(String path, boolean fullMatch) {
        if (path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        }

        String[] pathDirs = tokenizePath(path);

        int pattIdxStart = 0;
        int pattIdxEnd = pattDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;

        // Match all elements up to the first **
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxStart];
            if ("**".equals(pattDir)) {
                break;
            }
            if (!matchStrings(pattIdxStart, pathDirs[pathIdxStart])) {
                return false;
            }
            pattIdxStart++;
            pathIdxStart++;
        }

        if (pathIdxStart > pathIdxEnd) {
            // Path is exhausted, only match if rest of pattern is * or **'s
            if (pattIdxStart > pattIdxEnd) {
                return (pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator));
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }
        else if (pattIdxStart > pattIdxEnd) {
            // String not exhausted, but pattern is. Failure.
            return false;
        }
        else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
            // Path start definitely matches due to "**" part in pattern.
            return true;
        }

        // up to last '**'
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String pattDir = pattDirs[pattIdxEnd];
            if (pattDir.equals("**")) {
                break;
            }
            if (!matchStrings(pattIdxEnd, pathDirs[pathIdxEnd])) {
                return false;
            }
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            // String is exhausted
            for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }

        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                // '**/**' situation, so skip one
                pattIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;

            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(pattIdxStart + j + 1, subStr)) {
                        continue strLoop;
                    }
                }
                foundIdx = pathIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }

        for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
            if (!pattDirs[i].equals("**")) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tokenize the given path String into parts, based on this matcher's settings.
     * @param path the path to tokenize
     * @return the tokenized path parts
     */
    private String[] tokenizePath(String path) {
        StringTokenizer st = new StringTokenizer(path, pathSeparator);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }

    private boolean matchStrings(int pattIdx, String str) {
        return pattPats[pattIdx].matcher(str).matches();
    }

    private Pattern createMatcherPattern(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append("\\\\");
                                break;
                            default:
                                sb.append('\\');
                                break;
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    sb.append(inClass == 0 ? ".*" : "*");
                    break;
                case '?':
                    sb.append(inClass == 0 ? '.' : '?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^')) {
                        sb.append('\\');
                    }
                    sb.append(ch);
                    break;
                case '!':
                    sb.append(firstIndexInClass == i ? '^' : '!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    sb.append(inGroup > 0 ? '|' : ',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return (caseSensitive ? Pattern.compile(sb.toString()) :
                Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE));
    }


}