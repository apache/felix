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
package org.apache.felix.coordinator.impl;

import java.util.Collection;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

/**
 * The <code>CrdCommand</code> class implements the required Command Line
 * interface commands for the Coordinator Service.
 * <p>
 * This class depends on the CommandLine API support (namely the annotations)
 * which are optionally wired to this bundle. Thus this class may fail to load
 * and calling the CrdCommand.create method may throw an Error.
 * <p>
 * In addition this class implements the Converter interface and is able to
 * convert Coordination IDs and names to Coordinations and bundles to the
 * list of Coordinations for the given bundle
 */
class CrdCommand /* implements Converter */
{
    /*
     * Register as a service with the properties:
     *    osgi.command.scope = crd
     *    osgi.command.function = [ list, fail, participants, details ]
     *    osgi.command.description = scope description
     *
     *    crd list [ -f, --full ] [ <regex filter on name> ]
     *    crd fail [ -r, --reason <reason> ] [-b,--bundle <bundle>] <coordination> ...
     *    crd participants <coordination> ���
     *    crd details <coordination> ���
     *
     * A Coordinator must provide a converter to a Coordination based on the
     * following inputs:
     *    id ��� the Coordination id
     *    name ��� the Coordination name. Must be unique
     *    bundle ��� Must translate to all coordinations of a specific bundle
     */

    static ServiceRegistration create(final BundleContext context, final CoordinationMgr mgr)
    {
        CrdCommand command = new CrdCommand(mgr);

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Coordinator Service Command Implementation");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");

        // Shell command registration
        props.put("osgi.command.scope", "crd");
        props.put("osgi.command.function", new String[]
            { "list", "fail", "participants", "details" });
        props.put("osgi.command.description", "Coordinator Service Commands");

        // Coordination conversion
        props.put("", Coordination.class.getName());

        return context.registerService(Object/*Converter*/.class.getName(), command, props);
    }

    private CrdCommand(final CoordinationMgr mgr)
    {

    }


    /* @Description("The list does ....") */
    public void list(
        /* CommandSession session, */
        /* @Parameter(alias={"-f","--full"}, ifPresent=true, ifAbsent=false) */ boolean full,
        /* @Description("...") */ String filter
        ){}

    /* @Description("The fail does ....") */
    public void fail(
        /* CommandSession session, */
        /* Parameter(alias={"-r","--reason"}, ifAbsent=NOT_SET) */ String reason,
        /* Parameter(alias={"-b","--bundle"}, ifAbsent=NOT_SET) */ Bundle bundle,
        /* @Description("coordinations to fail") */ Coordination[] coordinations
        ){}
    /* @Description("The participants does ....") */
    public void participants(
        /* CommandSession session, */
        /* @Description("coordinations") */ Coordination[] coordinations
        )
    {
        for (Coordination coordination : coordinations) {
            Collection<Participant> participants = coordination.getParticipants();
            /* FormatterService.format(coordination, INSPECT, null); */
        }
    }


    /* @Description("The details does ....") */
    public void details(
        /* CommandSession session, */
        /* @Description("coordinations") */ Coordination[] coordinations
        )
    {
        for (Coordination coordination : coordinations) {
            /* FormatterService.format(coordination, INSPECT, null); */
        }
    }

    //---------- Converter service

    /* Converts int/String to Coordination
    <T> boolean canConvert(Object sourceObject, ReifiedType<T> targetType)
    {
        true if targetType is Coordination or Collection<Coordination> or
        Coordination[] and sourceObject is a Bundle, int, or String
    }
    <T> T convert(Object sourceObject, ReifiedType<T> targetType);
    */
}
