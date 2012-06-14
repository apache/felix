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
package org.apache.felix.scrplugin.om;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.SpecVersion;
import org.apache.felix.scrplugin.annotations.ScannedAnnotation;
import org.apache.felix.scrplugin.description.ReferenceCardinality;
import org.apache.felix.scrplugin.description.ReferencePolicy;
import org.apache.felix.scrplugin.description.ReferenceStrategy;
import org.apache.felix.scrplugin.helper.StringUtils;

/**
 * <code>Reference.java</code>...
 *
 */
public class Reference extends AbstractObject {

    protected String name;
    protected String interfacename;
    protected String target;
    protected ReferenceCardinality cardinality;
    protected ReferencePolicy policy;
    protected String bind;
    protected String unbind;
    protected String updated;

    /** @since 1.0.9 */
    protected ReferenceStrategy strategy;

    private Field field;

    /**
     * Constructor from java source.
     */
    public Reference(final ScannedAnnotation annotation, final String sourceLocation) {
        super(annotation, sourceLocation);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Field getField() {
        return this.field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public String getInterfacename() {
        return this.interfacename;
    }

    public void setInterfacename(String interfacename) {
        this.interfacename = interfacename;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public ReferenceCardinality getCardinality() {
        return this.cardinality;
    }

    public void setCardinality(ReferenceCardinality cardinality) {
        this.cardinality = cardinality;
    }

    public ReferencePolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(ReferencePolicy policy) {
        this.policy = policy;
    }

    public String getBind() {
        return this.bind;
    }

    public void setBind(String bind) {
        this.bind = bind;
    }

    public String getUnbind() {
        return this.unbind;
    }

    public void setUnbind(String unbind) {
        this.unbind = unbind;
    }

    public String getUpdated() {
        return this.updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    /** @since 1.0.9 */
    public ReferenceStrategy getStrategy() {
        return strategy;
    }

    /** @since 1.0.9 */
    public void setStrategy(ReferenceStrategy strategy) {
        this.strategy = strategy;
    }

    /** @since 1.0.9 */
    public boolean isLookupStrategy() {
        return this.getStrategy() == ReferenceStrategy.LOOKUP;
    }

    /**
     * Validate the property.
     * If errors occur a message is added to the issues list,
     * warnings can be added to the warnings list.
     */
    public void validate(final Context context, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final int currentIssueCount = context.getIssueLog().getNumberOfErrors();

        // validate name
        if (StringUtils.isEmpty(this.name)) {
            if (context.getSpecVersion().ordinal() < SpecVersion.VERSION_1_1.ordinal() ) {
                this.logError(context.getIssueLog(), "Reference has no name");
            }
        }

        // validate interface
        if (StringUtils.isEmpty(this.interfacename)) {
            this.logError(context.getIssueLog(), "Missing interface name");
        }
        try {
            context.getProject().getClassLoader().loadClass(this.interfacename);
        } catch (final ClassNotFoundException e) {
            this.logError(context.getIssueLog(), "Interface class can't be loaded: " + this.interfacename);
        }

        // validate cardinality
        if (this.cardinality == null) {
            this.cardinality = ReferenceCardinality.MANDATORY_UNARY;
        }

        // validate policy
        if (this.policy == null) {
            this.policy = ReferencePolicy.STATIC;
        }

        // validate strategy
        if (this.strategy == null) {
            this.strategy = ReferenceStrategy.EVENT;
        }

        // validate bind and unbind methods
        if (!isLookupStrategy()) {
            String bindName = this.bind;
            String unbindName = this.unbind;

            final boolean canGenerate = context.getOptions().isGenerateAccessors() &&
                            !this.isLookupStrategy() && this.getField() != null
                            && (this.getCardinality() == ReferenceCardinality.OPTIONAL_UNARY || this.getCardinality() == ReferenceCardinality.MANDATORY_UNARY);
            if (bindName == null && !canGenerate ) {
                bindName = "bind";
            }
            if (unbindName == null && !canGenerate ) {
                unbindName = "unbind";
            }

            if ( bindName != null ) {
                bindName = this.validateMethod(context, bindName, componentIsAbstract);
            } else {
                bindName = "bind" + Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
            }
            if ( unbindName != null ) {
                unbindName = this.validateMethod(context, unbindName, componentIsAbstract);
            } else {
                unbindName = "unbind" + Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1);
            }

            if (context.getIssueLog().getNumberOfErrors() == currentIssueCount) {
                this.bind = bindName;
                this.unbind = unbindName;
            }
        } else {
            this.bind = null;
            this.unbind = null;
        }

        // validate updated method
        if (this.updated != null) {
            if (context.getSpecVersion().ordinal() < SpecVersion.VERSION_1_1_FELIX.ordinal()) {
                this.logError(context.getIssueLog(), "Updated method declaration requires version "
                                + SpecVersion.VERSION_1_1_FELIX.getName() + ", " + SpecVersion.VERSION_1_2 + " or newer");
            }
        }

    }

    private String validateMethod(final Context ctx, final String methodName, final boolean componentIsAbstract)
    throws SCRDescriptorException {
        final Method method = this.findMethod(ctx, methodName);
        if (method == null) {
            if (!componentIsAbstract) {
                this.logError(ctx.getIssueLog(),
                                "Missing method " + methodName + " for reference "
                                                + (this.getName() == null ? "" : this.getName()));
            }
            return null;
        }

        // method needs to be protected for 1.0
        if (ctx.getSpecVersion() == SpecVersion.VERSION_1_0) {
            if (Modifier.isPublic(method.getModifiers())) {
                this.logWarn(ctx.getIssueLog(), "Method " + method.getName() + " should be declared protected");
            } else if (!Modifier.isProtected(method.getModifiers())) {
                this.logError(ctx.getIssueLog(), "Method " + method.getName() + " has wrong qualifier, public or protected required");
                return null;
            }
        }
        return method.getName();
    }

    private static final String TYPE_SERVICE_REFERENCE = "org.osgi.framework.ServiceReference";

    private Method getMethod(final Context ctx, final String name, final Class<?>[] sig) {
        try {
            return ctx.getClassDescription().getDescribedClass().getDeclaredMethod(name, sig);
        } catch (final SecurityException e) {
            // ignore
        } catch (final NoSuchMethodException e) {
            // ignore
        }
        return null;
    }

    public Method findMethod(final Context ctx, final String methodName)
    throws SCRDescriptorException {
        try {
            final Class<?>[] sig = new Class<?>[] { ctx.getProject().getClassLoader().loadClass(TYPE_SERVICE_REFERENCE) };
            final Class<?>[] sig2 = new Class<?>[] {ctx.getProject().getClassLoader().loadClass(this.interfacename) };
            final Class<?>[] sig3 = new Class<?>[] { ctx.getProject().getClassLoader().loadClass(this.interfacename), Map.class };

            // service interface or ServiceReference first
            String realMethodName = methodName;
            Method method = getMethod(ctx, realMethodName, sig);
            if (method == null) {
                method = getMethod(ctx, realMethodName, sig2);
                if (method == null && ctx.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(ctx, realMethodName, sig3);
                }
            }

            // append reference name with service interface and ServiceReference
            if (method == null) {
                final String info;
                if (StringUtils.isEmpty(this.name)) {
                    final String interfaceName = this.getInterfacename();
                    final int pos = interfaceName.lastIndexOf('.');
                    info = interfaceName.substring(pos + 1);
                } else {
                    info = this.name;
                }
                realMethodName = methodName + Character.toUpperCase(info.charAt(0)) + info.substring(1);

                method = getMethod(ctx, realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(ctx, realMethodName, sig2);
                if (method == null && ctx.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(ctx, realMethodName, sig3);
                }
            }

            // append type name with service interface and ServiceReference
            if (method == null) {
                int lastDot = this.getInterfacename().lastIndexOf('.');
                realMethodName = methodName + this.getInterfacename().substring(lastDot + 1);
                method = getMethod(ctx, realMethodName, sig);
            }
            if (method == null) {
                method = getMethod(ctx, realMethodName, sig2);
                if (method == null && ctx.getSpecVersion().ordinal() >= SpecVersion.VERSION_1_1.ordinal() ) {
                    method = getMethod(ctx, realMethodName, sig3);
                }
            }

            return method;
        } catch (final ClassNotFoundException cnfe) {
            throw new SCRDescriptorException("Unable to load class!", cnfe);
        }
    }

}
