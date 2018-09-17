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
package org.apache.felix.maven.osgicheck.impl.checks;

import static org.objectweb.asm.ClassReader.SKIP_CODE;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.maven.osgicheck.impl.Check;
import org.apache.felix.maven.osgicheck.impl.CheckContext;
import org.apache.felix.maven.osgicheck.impl.featureutil.ManifestUtil;
import org.apache.felix.maven.osgicheck.impl.featureutil.PackageInfo;
import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/**
 * The following checks are performed:
 * <ul>
 *   <li>If a package is exported, the classes must be marked with either ConsumerType or ProviderType
 * </ul>
 */
public class ConsumerProviderTypeCheck implements Check {

    @Override
    public String getName() {
        return "exportannotation";
    }

    private boolean include(final File f) {
        return f.isFile() && f.getName().endsWith(".class");
    }

    private boolean exclude(final File f) {
        return f.getName().equals("package-info.class");
    }

    @Override
    public void check(final CheckContext ctx) throws MojoExecutionException {
        ctx.getLog().info("Checking exported packages for provider/consumer type");
        final List<PackageInfo> exp = ManifestUtil.extractExportedPackages(ctx.getManifest());
        for(final PackageInfo p : exp) {
            final File packageDir = new File(ctx.getRootDir(), p.getName().replace(".", File.separator));
            if ( !packageDir.exists() ) {
                ctx.reportError("Package directory " + packageDir + " does not exist!");
            } else {
                ctx.getLog().info("Examining package " + p.getName() + "...");
                for(final File f : packageDir.listFiles()) {
                    if ( include(f) && !exclude(f) ) {
                        final String className = f.getName().substring(0, f.getName().length() - 6);
                        processClass(ctx, f, className);
                    }
                }
            }
        }
    }

    /**
     * Scan a single class.
     */
    private void processClass(final CheckContext ctx, final File classFile, final String className) {
        ctx.getLog().debug("Processing class " + className);
        try (final InputStream input = new FileInputStream(classFile)) {
            // get the class file for ASM
            final ClassReader classReader = new ClassReader(input);
            final ClassNode classNode = new ClassNode();

            classReader.accept(classNode, SKIP_CODE | SKIP_DEBUG | SKIP_FRAMES);

            // create descriptions
            final List<String> annotations = extractAnnotation(classNode);
            for(final String s : annotations) {
                ctx.getLog().debug("Found annotation " + s );
            }

            int count = 0;
            if ( annotations.contains("org.osgi.annotation.versioning.ProviderType") ) {
                count++;
            }
            if ( annotations.contains("org.osgi.annotation.versioning.ConsumerType") ) {
                count++;
            }
            if ( count == 0 ) {
                ctx.reportError("Class " + className + " does neither declare ConsumerType nor ProducerType");
            } else if ( count == 2 ) {
                ctx.reportError("Class " + className + " declares ConsumerType and ProducerType");
            }
        } catch (final IllegalArgumentException | IOException ioe) {
            ctx.reportError("Unable to scan annotations " + ioe.getMessage());
        }
    }

    /**
     * Extract annotations
     */
    private final List<String> extractAnnotation(final ClassNode classNode) {
        final List<String> descriptions = new ArrayList<>();
        for(final AnnotationNode n : getAllAnnotations(classNode.invisibleAnnotations)) {
            this.parseAnnotation(descriptions, n);
        }
        for(final AnnotationNode n : getAllAnnotations(classNode.visibleAnnotations)) {
            this.parseAnnotation(descriptions, n);
        }

        return descriptions;
    }

    @SuppressWarnings("unchecked")
    private List<AnnotationNode> getAllAnnotations(@SuppressWarnings("rawtypes") final List annotationList) {
        final List<AnnotationNode> resultList = new ArrayList<>();
        if ( annotationList != null ) {
            resultList.addAll(annotationList);
        }
        return resultList;
    }

    /**
     * Parse annotation
     */
    private void parseAnnotation(final List<String> descriptions, final AnnotationNode annotation) {
        // desc has the format 'L' + className.replace('.', '/') + ';'
        final String name = annotation.desc.substring(1, annotation.desc.length() - 1).replace('/', '.');

        descriptions.add(name);
    }
}
