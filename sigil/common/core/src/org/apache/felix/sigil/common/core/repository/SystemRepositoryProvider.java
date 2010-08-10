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

package org.apache.felix.sigil.common.core.repository;

import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.util.Properties;

import org.apache.felix.sigil.common.config.BldUtil;
import org.apache.felix.sigil.common.repository.IBundleRepository;
import org.apache.felix.sigil.common.repository.IRepositoryProvider;
import org.apache.felix.sigil.common.repository.RepositoryException;

public class SystemRepositoryProvider implements IRepositoryProvider
{

    public IBundleRepository createRepository(String id, Properties properties)
        throws RepositoryException
    {
        String fw = properties.getProperty("framework");
        File frameworkPath = fw == null ? null : new File(fw);
        String extraPkgs = properties.getProperty("packages");
        String profile = properties.getProperty("profile");

        try
        {
            String pkgs = readSystemPackages(profile);
            
            if ( extraPkgs != null ) {
                pkgs += "," + extraPkgs;
            }
            
            return new SystemRepository(id, frameworkPath, pkgs);
        }
        catch (IOException e)
        {
            throw new RepositoryException("Failed to load profile", e);
        }
    }
    
    public static String readSystemPackages(String name) throws IOException {
        Properties p = readProperties();
        
        String key = systemPackagesKey(name);
        
        return BldUtil.expand(p.getProperty(key), p);
    }

    public static Properties readProperties() throws IOException
    {

        String profilePath = "profiles/jvm-packages.properties";
        InputStream in = SystemRepositoryProvider.class.getClassLoader().getResourceAsStream(
            profilePath);

        if (in == null)
            throw new IllegalStateException("Missing jvm packages: " + profilePath);

        Properties p = new Properties();
        
        p.load(in);

        return p;
    }
    
    private static String systemPackagesKey(String name) {
        if (name == null)
        {
            String version = System.getProperty("java.specification.version");
            String[] split = version.split("\\.");
            String prefix = ("6".compareTo(split[1]) <= 0) ? "JavaSE-" : "J2SE-";
            name = prefix + version;
        }
        
        return name + ".system";
    }

}
