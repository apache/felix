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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Verifies OSGi bundle metadata contains valid entries.
 *
 * Supported checks in the current version:
 * <ul>
 * <li>All packages declared in the <a href="http://bnd.bndtools.org/heads/export_package.html">Export-Package<a/> header are really included in the bundle.</li>
 * </ul>
 */
@Mojo(
    name = "verify",
    threadSafe = true,
    defaultPhase = LifecyclePhase.VERIFY
)
public final class VerifyBundlePlugin
    extends AbstractMojo
{

    private static final String EXPORT_PACKAGE = "Export-Package";

    private Pattern skipDirs = Pattern.compile( "(META|OSGI)-INF(.*)" );

    @Component
    private MavenProject project;

    /**
     * Flag to easily skip execution.
     */
    @Parameter( property = "skip", defaultValue = "false" )
    protected boolean skip;

    /**
     * Whether to fail on errors.
     */
    @Parameter( property = "failOnError", defaultValue = "true" )
    protected boolean failOnError;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( skip )
        {
            getLog().info( "Skipping Verify execution" );
            return;
        }

        Set<String> packagesNotFound = checkPackages();

        if ( !packagesNotFound.isEmpty() )
        {
            Formatter formatter = new Formatter();
            formatter.format( "Current bundle %s exports packages that do not exist:%n",
                              project.getArtifact().getFile() );
            for ( String packageNotFound : packagesNotFound )
            {
                formatter.format( " * %s%n", packageNotFound );
            }
            formatter.format( "Please review the <Export-Package> instruction in the `configuration/instructions` element of the `maven-bundle-plugin`%n" );
            formatter.format( "For more details, see http://bnd.bndtools.org/heads/export_package.html" );
            String message = formatter.toString();
            formatter.close();

            if ( failOnError )
            {
                throw new MojoFailureException( message );
            }
            else
            {
                getLog().warn( message );
            }
        }
    }

    private Set<String> checkPackages()
        throws MojoExecutionException
    {
        Set<String> packagesNotFound = new TreeSet<String>();

        File bundle = project.getArtifact().getFile();
        JarInputStream input = null;

        try
        {
            input = new JarInputStream( new FileInputStream( bundle ) );
            Manifest manifest = input.getManifest();
            Attributes mainAttributes = manifest.getMainAttributes();
            String exportPackage = mainAttributes.getValue( EXPORT_PACKAGE );

            if ( exportPackage == null || exportPackage.isEmpty() )
            {
                getLog().warn( "Bundle manifest file does not contain valid 'Export-Package' OSGi entry, it will be ignored" );
                return packagesNotFound;
            }

            // use a technique similar to the Sieve of Eratosthenes:
            // create a set with all exported packages
            Clause[] clauses = Parser.parseHeader( exportPackage );
            for ( Clause clause : clauses )
            {
                packagesNotFound.add( clause.getName() );
            }

            // then, for each package found in the bundle, drop it from the set
            JarEntry jarEntry = null;
            while ( ( jarEntry = input.getNextJarEntry() ) != null )
            {
                String entryName = jarEntry.getName();
                if ( jarEntry.isDirectory() && !skipDirs.matcher( entryName ).matches() )
                {
                    if ( File.separatorChar == entryName.charAt( entryName.length() - 1 ) )
                    {
                        entryName = entryName.substring( 0, entryName.length() - 1 );
                    }

                    String currentPackage = entryName.replace( File.separatorChar, '.' );
                    packagesNotFound.remove( currentPackage );
                }
            }

            // if there is a package not found in the set, it is a misconfigured package
            return packagesNotFound;
        }
        catch ( IOException ioe )
        {
            throw new MojoExecutionException( "An error occurred while reading manifest file " + bundle, ioe );
        }
        finally
        {
            if ( input != null )
            {
                try
                {
                    input.close();
                }
                catch ( IOException e )
                {
                    // close it quietly
                }
            }
        }
    }

}
