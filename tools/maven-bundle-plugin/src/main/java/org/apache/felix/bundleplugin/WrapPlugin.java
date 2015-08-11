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
package org.apache.felix.bundleplugin;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * Build an OSGi bundle jar for direct dependencies.
 * @deprecated The wrap goal is no longer supported and may be removed in a future release
 */
@Deprecated
@Mojo( name = "wrap", requiresDependencyResolution = ResolutionScope.TEST,
       defaultPhase = LifecyclePhase.PACKAGE)
public final class WrapPlugin extends BundleAllPlugin
{
    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().warn( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
        getLog().warn( "! The wrap goal is no longer supported and may be removed in a future release !" );
        getLog().warn( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );

        BundleInfo bundleInfo = bundleAll( getProject(), 1 );
        logDuplicatedPackages( bundleInfo );
    }
}
