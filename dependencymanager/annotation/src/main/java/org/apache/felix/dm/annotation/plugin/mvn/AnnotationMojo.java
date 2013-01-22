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
package org.apache.felix.dm.annotation.plugin.mvn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.annotation.plugin.bnd.DescriptorGenerator;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.lib.osgi.Resource;

/**
 * The <code>AnnotationMojo</code>
 * generates a Dependency Manager component descriptor file based on annotations found from java classes.
 * 
 * @goal scan
 * @phase package
 * @description Build DependencyManager component descriptors from class annotations.
 * @requiresDependencyResolution compile
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationMojo extends AbstractMojo
{
    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject m_project;

    /**
     * The target extension
     * 
     * @parameter default-value="jar"
     * @required
     */
    private String m_artifactExtension;

    /**
     * If set, configures the log level.
     * 
     * @parameter alias="log"
     */
    private String m_log;

    /**
     * If set, configures if we must auto generate Import-Service/Export-Service headers.
     * 
     * @parameter alias="build-import-export-service" default-value="true"
     */
    private boolean m_buildImportExportService;

    /**
     * The maven project bas directory, used when generating metadata in maven project directory.
     * 
     * @parameter expression="${project.basedir}"
     * @required
     * @readonly
     */
    private File m_projectBaseDir;
    
    /**
     * If set, configures the output directory where generated descriptor files are generated.
     * 
     * @parameter alias="generated-output-dir"
     */
    private String m_generatedOutputDir;

    /**
     * "Import-Service" osgi header
     */
    private static final String IMPORT_SERVICE = "Import-Service";

    /**
     * "Export-Service" osgi header
     */
    private static final String EXPORT_SERVICE = "Export-Service";

    /**
     * Executes this mojo. We'll use the bnd library in order to scan classes
     * from our target bundle.
     */
    public void execute() throws MojoExecutionException
    {
        Analyzer analyzer = null;
        Jar jar = null;

        try
        {
            // Get the name of our target bundle we are parsing for annotations.
            File target = getBundleName();
            getLog().info("Generating DM component descriptors for bundle " + target);

            // Create a bnd analyzer and analyze our target bundle classes.
            analyzer = new Analyzer();
            analyzer.setJar(target);
            analyzer.analyze();

            // This helper class will parse classes using the analyzer we just created.
            DescriptorGenerator generator = new DescriptorGenerator(analyzer, new MvnLogger(getLog(), m_log));

            // Start scanning
            if (generator.execute())
            {
                // Some annotations have been parsed.
                // Add the list of generated component descriptors in our
                // special header.
                jar = analyzer.getJar();
                jar.getManifest().getMainAttributes()
                    .putValue( "DependencyManager-Component", generator.getDescriptorPaths() );

                // Add generated descriptors into the target bundle (we'll use a
                // temp file).
                Map<String, Resource> resources = generator.getDescriptors();
                for (Map.Entry<String, Resource> entry : resources.entrySet())
                {
                    addResource(entry.getKey(), entry.getValue().openInputStream());
                    jar.putResource(entry.getKey(), entry.getValue());
                }

                Resource metaType = generator.getMetaTypeResource();
                if (metaType != null)
                {
                    addResource("OSGI-INF/metatype/metatype.xml", metaType.openInputStream());
                    jar.putResource("OSGI-INF/metatype/metatype.xml", metaType);
                }

                // Possibly set the Import-Service/Export-Service header
                if (m_buildImportExportService)
                {
                    // Don't override Import-Service header, if it is found from
                    // the bnd directives.
                    if (jar.getManifest().getMainAttributes().getValue(IMPORT_SERVICE) == null)
                    {
                        buildImportExportService(jar, IMPORT_SERVICE, generator.getImportService());
                    }

                    // Don't override Export-Service header, if already defined
                    if (jar.getManifest().getMainAttributes().getValue(EXPORT_SERVICE) == null)
                    {
                        buildImportExportService(jar, EXPORT_SERVICE, generator.getExportService());
                    }
                }

                copy(jar, target);
            }
        }

        catch (MojoExecutionException e)
        {
            throw e;
        }

        catch (Throwable t)
        {
            getLog().error("Exception while scanning annotation", t);
            throw new MojoExecutionException(t.getMessage(), t.getCause());
        }

        finally
        {
            if (jar != null)
            {
                jar.close();
            }
        }
    }

    /**
     * Adds a resource file into the project base directory
     * @param key
     * @param in
     * @throws IOException
     */
    private void addResource(String key, InputStream in) throws IOException
    {
        if (m_generatedOutputDir != null) {
            File descriptorFile = new File( m_projectBaseDir + File.separator + m_generatedOutputDir, key );
            descriptorFile.getParentFile().mkdirs();
            BufferedInputStream bin = new BufferedInputStream( in );
            BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( descriptorFile ) );
            int b;
            while ( ( b = bin.read() ) != -1 )
            {
                out.write( b );
            }
            out.close();
            bin.close();
        }
    }

    private void buildImportExportService(Jar jar, String header, Set<String> services) throws IOException
    {
        getLog().info("building " + header + " header with the following services: " + services);

        if (services.size() > 0)
        {
            StringBuilder sb = new StringBuilder();
            for (String service : services)
            {
                sb.append(service);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1); // skip last comma
            jar.getManifest().getMainAttributes().putValue(header, sb.toString());
        }
    }

    /**
     * Returns the target name of this maven project.
     * 
     * @return the target name of this maven project.
     */
    private File getBundleName()
    {
        Build build = m_project.getBuild();
        return new File(build.getDirectory() + File.separator + build.getFinalName() + "."
                + m_artifactExtension);
    }

    /**
     * Copy the generated jar into our target bundle.
     * 
     * @param jar the jar with the generated component descriptors
     * @param target our target bundle
     * @throws MojoExecutionException on any errors
     * @throws Exception on any error
     */
    private void copy(Jar jar, File target) throws MojoExecutionException, Exception
    {
        File tmp = new File(getBundleName() + ".tmp");
        try
        {
            if (tmp.exists())
            {
                if (!tmp.delete())
                {
                    throw new MojoExecutionException("Could not remove " + tmp);
                }
            }
            jar.write(tmp);
            jar.close();

            if (target.exists() && !target.delete())
            {
                throw new MojoExecutionException("Could not remove " + target);
            }
            if (!tmp.renameTo(target))
            {
                throw new MojoExecutionException("Could not rename " + tmp + " to " + target);
            }
        }
        finally
        {
            jar.close();
            if (tmp.exists() && !tmp.delete())
            {
                throw new MojoExecutionException("Could not remove " + tmp);
            }
        }
    }
}
