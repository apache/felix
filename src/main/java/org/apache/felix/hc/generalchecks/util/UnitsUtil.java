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
package org.apache.felix.hc.generalchecks.util;

public class UnitsUtil {

    private static final String SIZES[] = { "kB", "MB", "GB", "TB" };

    public static String formatBytes(double size) {
        double step = 1024, current = step;

        int i;
        for (i = 0; i < SIZES.length - 1; ++i) {
            if (size < current * step) {
                break;
            }
            current *= step;
        }

        String unit = SIZES[i];
        double value = size / current;
        String retVal = String.format("%.1f", value) + unit;
        return retVal;
    }
    
}
