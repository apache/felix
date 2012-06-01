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
package org.apache.felix.webconsole.plugins.gogo.impl;

import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The <code>SessionTerminalManager</code> manages {@link SessionTerminal}
 * instances on behalf of the {@link GogoPlugin}. The instances are stored in
 * HttpSessions and cleared when (a) the owning session is destroyed and (b)
 * when this manager is {@link #shutdown() shut down}.
 */
public class SessionTerminalManager implements HttpSessionListener {

    private final ServiceRegistration service;

    private final ServiceTracker commandProcessor;

    private final Set<SessionTerminal> sessions = new HashSet<SessionTerminal>();

    @SuppressWarnings("serial")
    public SessionTerminalManager(final BundleContext context) {
        this.commandProcessor = new ServiceTracker(context, CommandProcessor.class.getName(), null) {
            @Override
            public void removedService(ServiceReference reference, Object service) {
                cleanupSessions();
                super.removedService(reference, service);
            }
        };
        this.commandProcessor.open();

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Gogo Shell Terminal Session Reaper");
        service = context.registerService(HttpSessionListener.class.getName(), this, props);
    }

    void shutdown() {
        this.service.unregister();
        this.commandProcessor.close();
        this.cleanupSessions();
    }

    SessionTerminal getSessionTerminal(final HttpServletRequest request) throws IOException {
        final HttpSession session = request.getSession(true);
        final Object terminal = session.getAttribute("terminal");
        if (terminal instanceof SessionTerminal) {
            final SessionTerminal st = (SessionTerminal) terminal;
            if (!st.isClosed()) {
                return st;
            }
        }

        final CommandProcessor cp = (CommandProcessor) this.commandProcessor.getService();
        if (cp != null) {
            final SessionTerminal st = new SessionTerminal(cp, request.getRemoteUser());
            this.sessions.add(st);
            session.setAttribute("terminal", st);
            return st;
        }

        // no session because there is no command processor !
        return null;
    }

    public void sessionCreated(HttpSessionEvent event) {
        // don't care
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        final Object terminal = event.getSession().getAttribute("terminal");
        if (terminal instanceof SessionTerminal) {
            sessions.remove(terminal);
            ((SessionTerminal) terminal).close();
        }
    }

    void cleanupSessions() {
        for (SessionTerminal session : sessions) {
            session.close();
        }
        this.sessions.clear();
    }
}
