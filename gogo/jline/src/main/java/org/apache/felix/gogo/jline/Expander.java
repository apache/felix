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
package org.apache.felix.gogo.jline;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.service.command.CommandSession;
import org.jline.reader.impl.DefaultExpander;

public class Expander extends DefaultExpander {

    private final CommandSession session;

    public Expander(CommandSession session) {
        this.session = session;
    }

    @Override
    public String expandVar(String word) {
        try {
            Object o = org.apache.felix.gogo.runtime.Expander.expand(
                    word,
                    new Closure((CommandSessionImpl) session, null, null));
            if (o instanceof Collection) {
                return ((Collection<Object>) o).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(" "));
            }
            else if (o != null) {
                return o.toString();
            }
        } catch (Exception e) {
            // ignore
        }
        return word;
    }

}
