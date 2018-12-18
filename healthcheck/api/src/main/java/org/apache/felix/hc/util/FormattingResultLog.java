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

import java.text.NumberFormat;
import java.util.Locale;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.osgi.annotation.versioning.ProviderType;
import org.slf4j.helpers.MessageFormatter;

/** Utility that provides a logging-like facade on a ResultLog */
@ProviderType
public class FormattingResultLog extends ResultLog {

    private ResultLog.Entry createEntry(Result.Status status, String format, Object... args) {
        return new ResultLog.Entry(status, MessageFormatter.arrayFormat(format, args).getMessage());
    }
    private ResultLog.Entry createEntry(boolean isDebug, String format, Object... args) {
        return new ResultLog.Entry(MessageFormatter.arrayFormat(format, args).getMessage(), isDebug);
    }

    public void debug(String format, Object... args) {
        add(createEntry(true, format, args));
    }

    public void info(String format, Object... args) {
        add(createEntry(false, format, args));
    }

    public void warn(String format, Object... args) {
        add(createEntry(Result.Status.WARN, format, args));
    }

    public void critical(String format, Object... args) {
        add(createEntry(Result.Status.CRITICAL, format, args));
    }

    public void healthCheckError(String format, Object... args) {
        add(createEntry(Result.Status.HEALTH_CHECK_ERROR, format, args));
    }

    /** Utility method to return any magnitude of milliseconds in a human readable format using the appropriate time unit (ms, sec, min)
     * depending on the magnitude of the input.
     * 
     * @param millis milliseconds
     * @return a string with a number and a unit */
    public static String msHumanReadable(final long millis) {

        double number = millis;
        final String[] units = new String[] { "ms", "sec", "min", "h", "days" };
        final double[] divisors = new double[] { 1000, 60, 60, 24 };

        int magnitude = 0;
        do {
            double currentDivisor = divisors[Math.min(magnitude, divisors.length - 1)];
            if (number < currentDivisor) {
                break;
            }
            number /= currentDivisor;
            magnitude++;
        } while (magnitude < units.length - 1);
        NumberFormat format = NumberFormat.getNumberInstance(Locale.UK);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(1);
        String result = format.format(number) + units[magnitude];
        return result;
    }
}