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

final class ClassName {

    private static final String DEFAULT_PACKAGE = "default";

    private final String qualifiedName;

    private final String packageName;

    private final String packagePath;

    private final String simpleName;

    public static ClassName get(String qualifiedName) {
        int sep = qualifiedName.lastIndexOf('.');
        String packageName;
        String packagePath;
        String simpleName;
        if (sep != -1) {
            packageName = qualifiedName.substring(0, sep);
            packagePath = packageName.replace('.', '/');
            simpleName = qualifiedName.substring(sep + 1);
        } else {
            packageName = DEFAULT_PACKAGE;
            packagePath = DEFAULT_PACKAGE;
            simpleName = qualifiedName.replace(' ', '_');
        }
        return new ClassName(qualifiedName, packageName, packagePath, simpleName);
    }

    ClassName(String qualifiedName,
              String packageName,
              String packagePath,
              String simpleName) {
        this.qualifiedName = qualifiedName;
        this.packageName = packageName;
        this.packagePath = packagePath;
        this.simpleName = simpleName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getPackagePath() {
        return packagePath;
    }

    public String getSimpleName() {
        return simpleName;
    }

}
