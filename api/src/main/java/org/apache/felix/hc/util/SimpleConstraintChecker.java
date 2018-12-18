/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.util;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;

/** Simple check of numeric values against expressions like &lt; N, &gt; N, between two values etc. See the SimpleConstraintCheckerTest for
 * examples. */
public class SimpleConstraintChecker {

    public static final String CONTAINS = "contains";

    /** Check value against expression and report to result */
    public void check(Object inputValue, String constraint, ResultLog resultLog) {

        final String stringValue = inputValue == null ? "" : inputValue.toString();

        if (constraint == null || constraint.trim().length() == 0) {
            throw new IllegalArgumentException("Empty constraint, cannot evaluate");
        }

        final String[] parts = constraint.split(" ");
        boolean matches = false;
        try {
            if (constraint.startsWith(">") && parts.length == 2) {
                final int value = Integer.valueOf(stringValue).intValue();
                matches = value > Integer.valueOf(parts[1]);

            } else if (constraint.startsWith("<") && parts.length == 2) {
                final int value = Integer.valueOf(stringValue).intValue();
                matches = value < Integer.valueOf(parts[1]);

            } else if (parts.length == 4 && "between".equalsIgnoreCase(parts[0]) && "and".equalsIgnoreCase(parts[2])) {
                final int value = Integer.valueOf(stringValue).intValue();
                final int lowerBound = Integer.valueOf(parts[1]);
                final int upperBound = Integer.valueOf(parts[3]);
                matches = value > lowerBound && value < upperBound;

            } else if (parts.length > 1 && CONTAINS.equalsIgnoreCase(parts[0])) {
                final String pattern = constraint.substring(CONTAINS.length()).trim();
                matches = stringValue.contains(pattern);

            } else {
                matches = constraint.equals(stringValue);
            }
        } catch (NumberFormatException nfe) {
            resultLog.add(new ResultLog.Entry(
                    Result.Status.WARN,
                    "Invalid numeric value [" + inputValue + "] while evaluating " + constraint));
        }

        if (matches) {
            resultLog.add(new ResultLog.Entry(
                    "Value [" + inputValue + "] matches constraint [" + constraint + "]", true));
        } else {
            resultLog.add(new ResultLog.Entry(
                    Result.Status.WARN,
                    "Value [" + inputValue + "] does not match constraint [" + constraint + "]"));
        }
    }
}