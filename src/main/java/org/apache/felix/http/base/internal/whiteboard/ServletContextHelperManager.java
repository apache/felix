/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.http.base.internal.runtime.ContextInfo;

public final class ServletContextHelperManager
{
    private final Map<String, List<ContextHolder>> nameMap = new HashMap<String, List<ContextHolder>>();

    /**
     * Create a new servlet context helper manager
     * and the default context
     */
    public ServletContextHelperManager()
    {
        // create default context
        final ContextInfo info = new ContextInfo();
        this.addContextHelper(info);
    }

    public void close()
    {
        // TODO Auto-generated method stub

    }

    private void activate(final ContextHolder holder)
    {
        // TODO
    }

    private void deactivate(final ContextHolder holder)
    {
     // TODO
    }

    /**
     * Add a servlet context helper.
     */
    public void addContextHelper(final ContextInfo info)
    {
        final ContextHolder holder = new ContextHolder(info);
        synchronized ( this.nameMap )
        {
            List<ContextHolder> holderList = this.nameMap.get(info.getName());
            if ( holderList == null )
            {
                holderList = new ArrayList<ContextHolder>();
                this.nameMap.put(info.getName(), holderList);
            }
            holderList.add(holder);
            Collections.sort(holderList);
            // check for activate/deactivate
            if ( holderList.get(0) == holder )
            {
                // check for deactivate
                if ( holderList.size() > 1 )
                {
                    this.deactivate(holderList.get(1));
                }
                this.activate(holder);
            }
        }
    }

    /**
     * Remove a servlet context helper
     */
    public void removeContextHelper(final ContextInfo info)
    {
        synchronized ( this.nameMap )
        {
            final List<ContextHolder> holderList = this.nameMap.get(info.getName());
            if ( holderList != null )
            {
                final Iterator<ContextHolder> i = holderList.iterator();
                boolean first = true;
                boolean activateNext = false;
                while ( i.hasNext() )
                {
                    final ContextHolder holder = i.next();
                    if ( holder.getInfo().compareTo(info) == 0 )
                    {
                        i.remove();
                        // check for deactivate
                        if ( first )
                        {
                            this.deactivate(holder);
                            activateNext = true;
                        }
                        break;
                    }
                    first = false;
                }
                if ( holderList.isEmpty() )
                {
                    this.nameMap.remove(info.getName());
                }
                else if ( activateNext )
                {
                    this.activate(holderList.get(0));
                }
            }
        }
    }

    private final static class ContextHolder implements Comparable<ContextHolder>
    {
        private final ContextInfo info;

        public ContextHolder(final ContextInfo info)
        {
            this.info = info;
        }

        public ContextInfo getInfo()
        {
            return this.info;
        }

        @Override
        public int compareTo(final ContextHolder o)
        {
            return this.info.compareTo(o.info);
        }
    }

}
