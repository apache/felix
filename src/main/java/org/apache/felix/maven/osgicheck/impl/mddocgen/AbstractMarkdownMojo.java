/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.maven.osgicheck.impl.mddocgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

abstract class AbstractMarkdownMojo extends AbstractMojo {

    @Parameter(defaultValue = "${user.name}", required = true, readonly = true)
    private String currentUser;

    @Parameter(defaultValue = "${project.description}", required = true, readonly = true)
    private String projectDescription;

    @Parameter(defaultValue = "Copyright &copy; ${project.inceptionYear} - %tY [${project.organization.name}](${project.organization.url}). All Rights Reserved.%n", required = true, readonly = true)
    private String projectCopyrights;

    @Parameter
    private Set<String> excludes;

    private Map<String, List<ClassName>> index;

    protected abstract String getReadmeTitle();

    protected abstract String getIncludes();

    protected abstract File getSourceDir();

    protected abstract File getTargetDir();

    protected abstract void handle(Collection<File> found);

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (!getSourceDir().exists()) {
            throw new MojoFailureException(getSourceDir() + " does not exist, please check MOJO configuration.");
        }

        if (!getTargetDir().exists()) {
            getTargetDir().mkdirs();
        }

        Collection<File> found = null;
        try {
            found = FileUtils.getFiles(getSourceDir(), getIncludes(), null);
        } catch (IOException e) {
            throw new MojoExecutionException("An error occurred while scanning directory '"
                                             + getSourceDir()
                                             + "', please check if it exists and the current user '"
                                             + currentUser
                                             + "' has enough permissions for reading");
        }

        if (found == null || found.isEmpty()) {
            getLog().warn("No " + getIncludes() + " file found in " + getSourceDir());
            return;
        }

        index = new TreeMap<>();

        handle(found);

        writeIndex();
    }

    protected final boolean isExcluded(String name) {
        if (excludes.contains(name)) {
            getLog().debug(name + " is in the exclude list, it won't be processed");
            return true;
        }
        return false;
    }

    protected final void doIndex(String key, ClassName value) {
        // TODO replace with Map#computeIfAbsent once switched to java 8
        List<ClassName> values = index.get(key);
        if (values == null) {
            values = new ArrayList<>();
            index.put(key, values);
        }
        values.add(value);
    }

    private void writeIndex() throws MojoExecutionException {
        PrintWriter writer = null;
        try {
            writer = newPrintWriter(new File(getTargetDir(), "README.md"));

            writer.println(getReadmeTitle());
            writer.println("=======================");
            writer.println();
            writer.println(projectDescription);
            writer.println();
            writer.printf(projectCopyrights, new Date());
            writer.println();

            // write the ToC first

            writer.println("# Table of contents");
            writer.println();

            for (String key : index.keySet()) {
                writer.printf(" * [%1$2s](#%1$2s)%n", key);
            }
            writer.println();

            // write sections

            for (Entry<String, List<ClassName>> entry : index.entrySet()) {
                writer.printf("# %1$2s <a id=\"%1$2s\"></a>%n", entry.getKey());
                writer.println();

                for (ClassName className : entry.getValue()) {
                    writer.printf(" * [%s](./%s/%s.md)%n",
                                  className.getQualifiedName(),
                                  className.getPackagePath(),
                                  className.getSimpleName());
                }
                writer.println();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("An error occurred while rendering index in README.md", e);
        } finally {
            IOUtil.close(writer);
        }
    }

    protected static PrintWriter newPrintWriter(File target) throws IOException {
        return new PrintWriter(new BufferedWriter(new FileWriter(target)));
    }

}
