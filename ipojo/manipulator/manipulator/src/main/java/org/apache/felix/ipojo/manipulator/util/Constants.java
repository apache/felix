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

package org.apache.felix.ipojo.manipulator.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A static class to access the constant written during packaging.
 */
public class Constants {

    public static String CONSTANTS_PATH = "META-INF/constants.properties";
    public static String MANIPULATOR_VERSION = "manipulator.version";
    public static String IPOJO_IMPORT_PACKAGES = "ipojo.import.packages";

    private static Properties m_properties;

    static {
        load();
    }

    private static void load() {
        m_properties = new Properties();
        InputStream is = Constants.class.getClassLoader().getResourceAsStream(CONSTANTS_PATH);
        try {
            m_properties.load(is);
            is.close();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load the 'constants' file");
        }
    }


    public static String getVersion() {
        return m_properties.getProperty(MANIPULATOR_VERSION);
    }

    public static String getPackageImportClause() {
        return m_properties.getProperty(IPOJO_IMPORT_PACKAGES);
    }
}
