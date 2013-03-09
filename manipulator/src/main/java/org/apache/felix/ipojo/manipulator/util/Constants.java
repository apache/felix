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
