/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.apache.felix.scrplugin.ant;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.felix.scrplugin.JavaClassDescriptorManager;
import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.SCRDescriptorFailureException;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;


public class AntJavaClassDescriptorManager extends JavaClassDescriptorManager
{

    private final FileSet sourceFiles;

    private final String classesDirectory;

    private final Path classPath;


    public AntJavaClassDescriptorManager( Log log, ClassLoader classLoader, FileSet sourceFiles, File classesDirectory,
        Path classPath, String[] annotationTagProviders, boolean parseJavadocs, boolean processAnnotations )
        throws SCRDescriptorFailureException
    {
        super( log, classLoader, annotationTagProviders, parseJavadocs, processAnnotations );
        this.sourceFiles = sourceFiles;
        this.classesDirectory = classesDirectory.getAbsolutePath();
        this.classPath = classPath;
    }


    @Override
    protected Iterator<File> getSourceFiles()
    {
        @SuppressWarnings("unchecked")
        final Iterator<Resource> resources = sourceFiles.iterator();
        return new Iterator<File>()
        {
            File next = seek();


            public boolean hasNext()
            {
                return next != null;
            }


            public File next()
            {
                if ( !hasNext() )
                {
                    throw new NoSuchElementException();
                }
                File result = next;
                next = seek();
                return result;
            }


            public void remove()
            {
                throw new UnsupportedOperationException( "remove" );
            }


            private File seek()
            {
                while ( resources.hasNext() )
                {
                    Resource r = resources.next();
                    if ( r instanceof FileResource )
                    {
                        return ( ( FileResource ) r ).getFile();
                    }
                }
                return null;
            }
        };
    }


    @Override
    protected List<File> getDependencies()
    {
        ArrayList<File> files = new ArrayList<File>();
        for ( String entry : classPath.list() )
        {
            File file = new File( entry );
            if ( file.isFile() )
            {
                files.add( file );
            }
        }
        return files;
    }


    @Override
    public String getOutputDirectory()
    {
        return classesDirectory;
    }

}
