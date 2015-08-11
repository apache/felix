package org.apache.felix.bundleplugin;

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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Fixed version of class that uses stable set ordering for reliable testing.
 */
@SuppressWarnings( { "rawtypes", "unchecked" } )
class ArtifactStubFactory extends org.apache.maven.plugin.testing.ArtifactStubFactory
{

    public ArtifactStubFactory( File workingDir, boolean createFiles )
    {
        super( workingDir, createFiles );
    }

    @Override
    public Set getClassifiedArtifacts() throws IOException
    {
        Set set = new LinkedHashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "jar", "one" ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", "two" ) );
        set.add( createArtifact( "g", "c", "1.0", Artifact.SCOPE_COMPILE, "jar", "three" ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "jar", "four" ) );
        return set;
    }

    @Override
    public Set getScopedArtifacts() throws IOException
    {
        Set set = new LinkedHashSet();
        set.add( createArtifact( "g", "compile", "1.0", Artifact.SCOPE_COMPILE ) );
        set.add( createArtifact( "g", "provided", "1.0", Artifact.SCOPE_PROVIDED ) );
        set.add( createArtifact( "g", "test", "1.0", Artifact.SCOPE_TEST ) );
        set.add( createArtifact( "g", "runtime", "1.0", Artifact.SCOPE_RUNTIME ) );
        set.add( createArtifact( "g", "system", "1.0", Artifact.SCOPE_SYSTEM ) );
        return set;
    }

    @Override
    public Set getTypedArtifacts() throws IOException
    {
        Set set = new LinkedHashSet();
        set.add( createArtifact( "g", "a", "1.0", Artifact.SCOPE_COMPILE, "war", null ) );
        set.add( createArtifact( "g", "b", "1.0", Artifact.SCOPE_COMPILE, "jar", null ) );
        set.add( createArtifact( "g", "c", "1.0", Artifact.SCOPE_COMPILE, "sources", null ) );
        set.add( createArtifact( "g", "d", "1.0", Artifact.SCOPE_COMPILE, "zip", null ) );
        set.add( createArtifact( "g", "e", "1.0", Artifact.SCOPE_COMPILE, "rar", null ) );
        return set;
    }

}
