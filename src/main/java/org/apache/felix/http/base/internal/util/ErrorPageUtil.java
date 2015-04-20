/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.util;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class ErrorPageUtil
{
    private static final String CLIENT_ERROR = "4xx";
    private static final String SERVER_ERROR = "5xx";
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\d{3}");

    private static final List<Integer> CLIENT_ERROR_CODES = hundredOf(400);
    private static final List<Integer> SERVER_ERROR_CODES = hundredOf(500);

    private static List<Integer> hundredOf(int start)
    {
        List<Integer> result = new ArrayList<Integer>();
        for (int i = start; i < start + 100; i++)
        {
            result.add(i);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<Integer> parseErrorCodes(String string)
    {
        if (CLIENT_ERROR.equalsIgnoreCase(string))
        {
            return CLIENT_ERROR_CODES;
        }
        else if (SERVER_ERROR.equalsIgnoreCase(string))
        {
            return SERVER_ERROR_CODES;
        }
        else if (ERROR_CODE_PATTERN.matcher(string).matches())
        {
            return asList(Integer.parseInt(string));
        }
        return null;
    }
}
