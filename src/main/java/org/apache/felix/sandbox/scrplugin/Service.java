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
package org.apache.felix.sandbox.scrplugin;

import org.apache.felix.sandbox.scrplugin.tags.JavaClassDescription;
import org.apache.felix.sandbox.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * The <code>Service</code>
 *
 */
public class Service extends AbstractDescriptorElement implements Comparable {

    private String interfaceName = "";

    /**
     * @param log
     * @param tag
     */
    public Service(Log log, JavaTag tag) {
        super(log, tag);
    }

    void generate(XMLWriter xmlWriter) {
        xmlWriter.printElementStart("provide", true);
        xmlWriter.printAttribute("interface", this.getInterfaceName());
        xmlWriter.printElementStartClose(true);
    }

    boolean validate() throws MojoExecutionException {
        JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass == null) {
            this.log("Must be declared in a Java class");
            return false;
        }

        if ( javaClass.isA(this.getInterfaceName()) ) {
            return true;
        }

        // interface not implemented
        this.log("Class must implement provided interface " + this.getInterfaceName());
        return false;
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    void setInterfaceName(String interfaceName) {
        this.interfaceName = (interfaceName != null) ? interfaceName : "";
    }

    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }

        Service other = (Service) obj;
        return this.getInterfaceName().compareTo(other.getInterfaceName());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Service) {
            return this.getInterfaceName().equals(((Service) obj).getInterfaceName());
        }

        return false;
    }

    public int hashCode() {
        return this.getInterfaceName().hashCode();
    }
}
