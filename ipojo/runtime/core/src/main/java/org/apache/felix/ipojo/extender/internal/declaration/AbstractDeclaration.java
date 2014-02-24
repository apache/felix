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

package org.apache.felix.ipojo.extender.internal.declaration;

import org.apache.felix.ipojo.extender.Declaration;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.Status;
import org.apache.felix.ipojo.extender.internal.AbstractService;
import org.osgi.framework.BundleContext;

/**
 * Common code to all Declaration objects.
 */
public abstract class AbstractDeclaration extends AbstractService implements Declaration, DeclarationHandle, Status {

    /**
     * The message used when a declaration is bound.
     */

    public static final String DECLARATION_BOUND_MESSAGE = "Declaration bound";
    /**
     * Is the declaration bound (i.e. successfully fulfilled), or not.
     */
    private boolean m_bound = false;

    /**
     * When not bound, the reason, otherwise {@link #DECLARATION_BOUND_MESSAGE}
     */
    private String m_message;

    /**
     * If an error was thrown during the binding, the error.
     */
    private Throwable m_throwable;

    protected AbstractDeclaration(BundleContext bundleContext, Class<?> type) {
        super(bundleContext, type);
    }

    /**
     * @return whether the declaration is bound or not.
     */
    public boolean isBound() {
        return m_bound;
    }

    /**
     * @return the message. The message contains the not-bound reason.
     */
    public String getMessage() {
        return m_message;
    }

    /**
     * @return the error if something went wrong during the binding process.
     */
    public Throwable getThrowable() {
        return m_throwable;
    }

    /**
     * Gets the status of the declaration. This method returns an immutable object.
     *
     * @return the declaration status.
     */
    public Status getStatus() {
        // We return an immutable object, created on the fly.
        return new Status() {
            final boolean m_bound = AbstractDeclaration.this.m_bound;
            final String m_message = AbstractDeclaration.this.m_message;
            final Throwable m_throwable = AbstractDeclaration.this.m_throwable;

            public boolean isBound() {
                return this.m_bound;
            }

            public String getMessage() {
                return this.m_message;
            }

            public Throwable getThrowable() {
                return this.m_throwable;
            }
        };
    }

    /**
     * Binds the declaration.
     * This method just set the status, message and error.
     */
    public void bind() {
        m_bound = true;
        m_message = DECLARATION_BOUND_MESSAGE;
        m_throwable = null;
    }

    /**
     * Unbinds the declaration.
     *
     * @param message an explanation
     * @see #unbind(String, Throwable)
     */
    public void unbind(String message) {
        unbind(message, null);
    }

    /**
     * Unbinds the declaration.
     * The flag, message and error are set.
     *
     * @param message   an explanation
     * @param throwable an error
     */
    public void unbind(String message, Throwable throwable) {
        m_bound = false;
        m_message = message;
        m_throwable = throwable;
    }

    public void publish() {
        if (!isRegistered()) {
            start();
        }
    }

    public void retract() {
        if (isRegistered()) {
            stop();
        }
    }
}
