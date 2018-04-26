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
package org.apache.felix.scr.impl;

import org.apache.felix.scr.info.ScrInfo;
import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import java.io.PrintWriter;

public class ComponentCommandsScrInfo implements ScrInfo {

    private final ComponentCommands commands;
    private final BundleContext context;

    ComponentCommandsScrInfo(ComponentCommands commands, BundleContext context) {
        this.commands = commands;
        this.context = context;
    }

    @Override
    public void list(String bundleIdentifier, PrintWriter out) {
        long bundleId;

        try {
            bundleId = Long.parseLong(bundleIdentifier);
        } catch (NumberFormatException e) {
            // might be a BSN
            Bundle bundle = findBundle(bundleIdentifier);
            if (bundle == null) throw new IllegalArgumentException("Cannot find bundle with ID: " + bundleIdentifier);
            bundleId = bundle.getBundleId();
        }

        final CharSequence formatted;
        try {
            ComponentDescriptionDTO[] dtos = commands.list(bundleId);
            if (dtos != null) {
                formatted = commands.format(dtos, 1);
            } else {
                formatted = "No components found for bundle  " + bundleId;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing or formatting SCR runtime information", e);
        }
        out.println(formatted);
    }

    @Override
    public void info(String componentId, PrintWriter out) {
        Object infoObj;
        try {
            long configId = Long.parseLong(componentId);
            infoObj = commands.info(configId);
        } catch (NumberFormatException e) {
            infoObj = commands.info(componentId);
        }

        CharSequence formatted;
        if (infoObj != null) {
            try {
                formatted = commands.format(infoObj, Converter.INSPECT);
            } catch (Exception e) {
                throw new RuntimeException("Error formatting SCR runtime information", e);
            }
        } else {
            formatted = "No component found with ID " + componentId;
        }
        out.println(formatted);
    }

    @Override
    public void config(PrintWriter out) {
        out.println(commands.config());
    }

    private Bundle findBundle(String bsn) {
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(bsn)) return b;
        }
        return null;
    }
}
