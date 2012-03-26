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
package org.apache.felix.example.extenderbased.host.launch;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.Constants;

/**
 * Util class for creating the framework configuration
 */
final class ConfigUtil
{

    /**
     * Creates a configuration for the framework. Therefore this method attempts to create
     * a temporary cache dir. If creation of the cache dir is successful, it will be added
     * to the configuration.
     *
     * @return
     */
    public static Map<String, String> createConfig()
    {
        final File cachedir = createCacheDir();

        Map<String, String> configMap = new HashMap<String, String>();
        // Tells the framework to export the extension package, making it accessible
        // for the other shape bundels
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
            "org.apache.felix.example.extenderbased.host.extension; version=1.0.0");

        // if we could create a cache dir, we use it. Otherwise the platform default will be used
        if (cachedir != null)
        {
            configMap.put(Constants.FRAMEWORK_STORAGE, cachedir.getAbsolutePath());
        }

        return configMap;
    }

    /**
     * Tries to create a temporay cache dir. If creation of the cache dir is successful,
     * it will be returned. If creation fails, null will be returned.
     *
     * @return a {@code File} object representing the cache dir
     */
    private static File createCacheDir()
    {
        final File cachedir;
        try
        {
            cachedir = File.createTempFile("felix.example.extenderbased", null);
            cachedir.delete();
            createShutdownHook(cachedir);
            return cachedir;
        }
        catch (IOException e)
        {
            // temp dir creation failed, return null
            return null;
        }
    }

    /**
     * Adds a shutdown hook to the runtime, that will make sure, that the cache dir will
     * be deleted after the application has been terminated.
     */
    private static void createShutdownHook(final File cachedir)
    {
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                deleteFileOrDir(cachedir);
            }
        });
    }


    /**
     * Utility method used to delete the profile directory when run as
     * a stand-alone application.
     * @param file The file to recursively delete.
    **/
    private static void deleteFileOrDir(File file)
    {
        if (file.isDirectory())
        {
            File[] childs = file.listFiles();
            for (File child : childs)
            {
                deleteFileOrDir(child);
            }
        }
        file.delete();
    }
}