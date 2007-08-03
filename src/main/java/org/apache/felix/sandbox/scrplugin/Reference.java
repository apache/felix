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
import org.apache.felix.sandbox.scrplugin.tags.JavaMethod;
import org.apache.felix.sandbox.scrplugin.tags.JavaTag;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Model: scr reference
 */
public class Reference extends AbstractDescriptorElement implements Comparable {

    private String name = "";

    private String interfaceName;

    private String cardinality;

    private String policy;

    private String target;

    private String bind = "bind";

    private String unbind = "unbind";

    Reference(Log log, JavaTag tag) {
        super(log, tag);
    }

    boolean validate(/* JavaClass javaClass */)
    throws MojoExecutionException {
        boolean valid = true;

        // validate name
        if (StringUtils.isEmpty(this.name)) {
            this.log("Reference has no name");
            valid = false;
        }

        // validate interface
        if (StringUtils.isEmpty(this.interfaceName)) {
            this.log("Missing interface name");
            valid = false;
        }

        // validate cardinality
        if (this.cardinality == null) {
            this.cardinality = "1..1";
        } else if (!"0..1".equals(this.cardinality) && !"1..1".equals(this.cardinality)
            && !"0..n".equals(this.cardinality) && !"1..n".equals(this.cardinality)) {
            this.log("Invalid Cardinality specification " + this.cardinality);
            valid = false;
        }

        // validate policy
        if (this.policy == null) {
            this.policy = "static";
        } else if (!"static".equals(this.policy) && !"dynamic".equals(this.policy)) {
            this.log("Invalid Policy specification " + this.policy);
            valid = false;
        }

        // validate bind and unbind methods
        JavaClassDescription javaClass = this.tag.getJavaClassDescription();
        if (javaClass != null) {
            this.bind = this.validateMethod(javaClass, this.bind);
            this.unbind = this.validateMethod(javaClass, this.unbind);
            valid = this.bind != null && this.unbind != null;
        } else {
            this.log("Cannot find Java class to which the reference belongs");
            valid = false;
        }

        return valid;
    }

    private String validateMethod(JavaClassDescription javaClass, String methodName)
    throws MojoExecutionException {

        JavaMethod method = this.findMethod(javaClass, methodName);

        if (method == null) {
            this.log("Missing method " + methodName + " for reference " + this.getName());
            return null;
        }

        if (method.isPublic()) {
            this.warn("Method " + method.getName() + " should be declared protected");
        } else if (!method.isProtected()) {
            this.log("Method " + method.getName() + " has wrong qualifier, public or protected required");
            return null;
        }

        return method.getName();
    }

    JavaMethod findMethod(JavaClassDescription javaClass, String methodName)
    throws MojoExecutionException {

        String[] sig = new String[]{ this.getInterfaceName() };
        String[] sig2 = new String[]{ "org.osgi.framework.ServiceReference" };

        // service interface or ServiceReference first
        String realMethodName = methodName;
        JavaMethod method = javaClass.getMethodBySignature(realMethodName, sig);
        if (method == null) {
            method = javaClass.getMethodBySignature(realMethodName, sig2);
        }

        // append reference name with service interface and ServiceReference
        if (method == null) {
            realMethodName = methodName + Character.toUpperCase(this.name.charAt(0))
            + this.name.substring(1);

            method = javaClass.getMethodBySignature(realMethodName, sig);
        }
        if (method == null) {
            method = javaClass.getMethodBySignature(realMethodName, sig2);
        }

        // append type name with service interface and ServiceReference
        if (method == null) {
            int lastDot = this.getInterfaceName().lastIndexOf('.');
            realMethodName = methodName
                + this.getInterfaceName().substring(lastDot + 1);
            method = javaClass.getMethodBySignature(realMethodName, sig);
        }
        if (method == null) {
            method = javaClass.getMethodBySignature(realMethodName, sig2);
        }

        return method;
    }

    void generate(XMLWriter xw) {
        xw.printElementStart("reference", true);
        xw.printAttribute("name", this.getName());
        xw.printAttribute("interface", this.interfaceName);
        xw.printAttribute("target", this.getTarget());
        xw.printAttribute("cardinality", this.getCardinality());
        xw.printAttribute("policy", this.getPolicy());
        xw.printAttribute("bind", this.getBind());
        xw.printAttribute("unbind", this.getUnbind());
        xw.printElementStartClose(true);
    }

    public String getBind() {
        return this.bind;
    }

    protected void setBind(String bind) {
        if (bind != null) {
            this.bind = bind;
        }
    }

    public String getCardinality() {
        return this.cardinality;
    }

    protected void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public String getName() {
        return this.name;
    }

    protected void setName(String name) {
        this.name = (name != null) ? name : "";
    }

    public String getInterfaceName() {
        return this.interfaceName;
    }

    protected void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getPolicy() {
        return this.policy;
    }

    protected void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getTarget() {
        return this.target;
    }

    protected void setTarget(String target) {
        this.target = target;
    }

    public String getUnbind() {
        return this.unbind;
    }

    protected void setUnbind(String unbind) {
        if (unbind != null) {
            this.unbind = unbind;
        }
    }

    public int compareTo(Object obj) {
        if (obj == this) {
            return 0;
        }

        Reference other = (Reference) obj;
        return this.getName().compareTo(other.getName());
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof Reference) {
            return this.getName().equals(((Reference) obj).getName());
        }

        return false;
    }

    public int hashCode() {
        return this.getName().hashCode();
    }
}
