/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.bundleplugin;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;


/**
 * Information result of the bundling process
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 */
public class BundleInfo
{
    /**
     * {@link Map} &lt; {@link String}, {@link Set} &lt; {@link Artifact} > >
     * Used to check for duplicated exports. Key is package name and value list of artifacts where it's exported.
     */
    private Map<String, Set<Artifact>> m_exportedPackages = new HashMap<String, Set<Artifact>>();


    public void addExportedPackage( String packageName, Artifact artifact )
    {
        Set<Artifact> artifacts = getExportedPackages().get( packageName );
        if ( artifacts == null )
        {
            artifacts = new HashSet<Artifact>();
            m_exportedPackages.put( packageName, artifacts );
        }
        artifacts.add( artifact );
    }


    protected Map<String, Set<Artifact>> getExportedPackages()
    {
        return m_exportedPackages;
    }


    /**
     * Get a list of packages that are exported in more than one bundle.
     * Key is package name and value list of artifacts where it's exported.
     * @return {@link Map} &lt; {@link String}, {@link Set} &lt; {@link Artifact} &gt; &gt;
     */
    public Map<String, Set<Artifact>> getDuplicatedExports()
    {
        Map<String, Set<Artifact>> duplicatedExports = new HashMap<String, Set<Artifact>>();

        for ( Iterator<Map.Entry<String, Set<Artifact>>> it = getExportedPackages().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, Set<Artifact>> entry = it.next();
            Set<Artifact> artifacts = entry.getValue();
            if ( artifacts.size() > 1 )
            {
                /* remove warnings caused by different versions of same artifact */
                Set<String> artifactKeys = new HashSet<String>();

                String packageName = entry.getKey();
                for ( Iterator<Artifact> it2 = artifacts.iterator(); it2.hasNext(); )
                {
                    Artifact artifact = it2.next();
                    artifactKeys.add( artifact.getGroupId() + "." + artifact.getArtifactId() );
                }

                if ( artifactKeys.size() > 1 )
                {
                    duplicatedExports.put( packageName, artifacts );
                }
            }
        }

        return duplicatedExports;
    }


    public void merge( BundleInfo bundleInfo )
    {
        for ( Iterator<Map.Entry<String, Set<Artifact>>> it = bundleInfo.getExportedPackages().entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, Set<Artifact>> entry = it.next();
            String packageName = entry.getKey();
            Set<Artifact> artifacts = entry.getValue();

            Set<Artifact> artifactsWithPackage = getExportedPackages().get( packageName );
            if ( artifactsWithPackage == null )
            {
                artifactsWithPackage = new HashSet<Artifact>();
                getExportedPackages().put( packageName, artifactsWithPackage );
            }
            artifactsWithPackage.addAll( artifacts );
        }
    }
}
