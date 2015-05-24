/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;

import org.apache.felix.http.base.internal.handler.ListenerHandler;
import org.apache.felix.http.base.internal.runtime.ListenerInfo;
import org.apache.felix.http.base.internal.runtime.dto.ListenerDTOBuilder;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;

public class ListenerMap<T> {

    private volatile List<ListenerRegistrationStatus<T>> handlers = Collections.emptyList();

    /**
     * The status object keeps track of the registration status of a listener
     * The status objects are sorted by result first, followed by ranking. The active
     * listeners ( result == -1) are first, followed by the inactive ones. This sorting
     * allows to only traverse over the active ones and also avoids any sorting
     * as the listeners are processed in the correct order already.
     */
    private static final class ListenerRegistrationStatus<T> implements Comparable<ListenerRegistrationStatus<T>>
    {
        private final int result;
        private final ListenerHandler<T> handler;

        public ListenerRegistrationStatus(@Nonnull final ListenerHandler<T> handler, final int result)
        {
            this.handler = handler;
            this.result = result;
        }

        public int getResult()
        {
            return this.result;
        }

        public @Nonnull ListenerHandler<T> getHandler()
        {
            return this.handler;
        }

        @Override
        public int compareTo(final ListenerRegistrationStatus<T> o)
        {
            int result = this.result - o.result;
            if ( result == 0 )
            {
                result = this.handler.compareTo(o.handler);
            }
            return result;
        }
    }

    public void cleanup()
    {
        this.handlers = Collections.emptyList();
    }

    public synchronized void add(final ListenerHandler<T> handler)
    {
        final int reason = handler.init();
        final ListenerRegistrationStatus<T> status = new ListenerRegistrationStatus<T>(handler, reason);

        final List<ListenerRegistrationStatus<T>> newList = new ArrayList<ListenerMap.ListenerRegistrationStatus<T>>(this.handlers);
        newList.add(status);
        Collections.sort(newList);
        this.handlers = newList;
    }

    public synchronized void remove(final ListenerInfo<T> info)
    {
        final List<ListenerRegistrationStatus<T>> newList = new ArrayList<ListenerMap.ListenerRegistrationStatus<T>>(this.handlers);
        final Iterator<ListenerRegistrationStatus<T>> i = newList.iterator();
        while ( i.hasNext() )
        {
            final ListenerRegistrationStatus<T> status = i.next();
            if ( status.getHandler().getListenerInfo().equals(info) )
            {
                if ( status.getResult() == - 1 )
                {
                    status.getHandler().destroy();
                }
                i.remove();
                this.handlers = newList;
                break;
            }
        }
    }

    public ListenerHandler<T> getListenerHandler(@Nonnull final ListenerInfo<T> info)
    {
        final List<ListenerRegistrationStatus<T>> list = this.handlers;
        for(final ListenerRegistrationStatus<T> status : list)
        {
            if ( status.getHandler().getListenerInfo().equals(info) )
            {
                return status.getHandler();
            }
        }
        return null;
    }

    public Iterable<ListenerHandler<T>> getActiveHandlers()
    {
        final List<ListenerRegistrationStatus<T>> list = this.handlers;
        final Iterator<ListenerRegistrationStatus<T>> iter = list.iterator();
        final Iterator<ListenerHandler<T>> newIter = new Iterator<ListenerHandler<T>>()
        {

            private ListenerHandler<T> next;

            {
                peek();
            }

            private void peek()
            {
                next = null;
                if ( iter.hasNext() )
                {
                    final ListenerRegistrationStatus<T> status = iter.next();
                    if ( status.getResult() == -1 )
                    {
                        next = status.getHandler();
                    }
                }
            }

            @Override
            public boolean hasNext()
            {
                return this.next != null;
            }

            @Override
            public ListenerHandler<T> next()
            {
                if ( this.next == null )
                {
                    throw new NoSuchElementException();
                }
                final ListenerHandler<T> result = this.next;
                peek();
                return result;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
        return new Iterable<ListenerHandler<T>>()
        {

            @Override
            public Iterator<ListenerHandler<T>> iterator()
            {
                return newIter;
            }
        };
    }

    public Iterable<T> getActiveListeners()
    {
        final Iterator<ListenerHandler<T>> iter = this.getActiveHandlers().iterator();
        final Iterator<T> newIter = new Iterator<T>()
        {

            private T next;

            {
                peek();
            }

            private void peek()
            {
                next = null;
                while ( next == null && iter.hasNext() )
                {
                    final ListenerHandler<T> handler = iter.next();
                    next = handler.getListener();
                }
            }

            @Override
            public boolean hasNext()
            {
                return this.next != null;
            }

            @Override
            public T next()
            {
                if ( this.next == null )
                {
                    throw new NoSuchElementException();
                }
                final T result = this.next;
                peek();
                return result;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
        return new Iterable<T>()
        {

            @Override
            public Iterator<T> iterator()
            {
                return newIter;
            }
        };
    }

    public void getRuntimeInfo(final List<ListenerDTO> listenerDTOs, final List<FailedListenerDTO> failedListenerDTOs)
    {
        final int length = listenerDTOs.size();
        final int failedLength = failedListenerDTOs.size();
        final List<ListenerRegistrationStatus<T>> list = this.handlers;
        for (final ListenerRegistrationStatus<T> status : list)
        {
            // search for DTO with same service id and failureCode
            if ( status.getResult() == -1 )
            {
                int index = 0;
                boolean found = false;
                final Iterator<ListenerDTO> i = listenerDTOs.iterator();
                while ( !found && index < length )
                {
                    final ListenerDTO dto = i.next();
                    if ( dto.serviceId == status.getHandler().getListenerInfo().getServiceId() )
                    {
                        found = true;
                        final String[] types = new String[dto.types.length + 1];
                        System.arraycopy(dto.types, 0, types, 0, dto.types.length);
                        types[dto.types.length] = status.getHandler().getListenerInfo().getListenerName();
                        dto.types = types;
                    }
                    index++;
                }
                if ( !found )
                {
                    listenerDTOs.add(ListenerDTOBuilder.build(status.getHandler(), status.getResult()));
                }
            }
            else
            {
                int index = 0;
                boolean found = false;
                final Iterator<FailedListenerDTO> i = failedListenerDTOs.iterator();
                while ( !found && index < failedLength )
                {
                    final FailedListenerDTO dto = i.next();
                    if ( dto.serviceId == status.getHandler().getListenerInfo().getServiceId()
                         && dto.failureReason == status.getResult() )
                    {
                        found = true;
                        final String[] types = new String[dto.types.length + 1];
                        System.arraycopy(dto.types, 0, types, 0, dto.types.length);
                        types[dto.types.length] = status.getHandler().getListenerInfo().getListenerName();
                        dto.types = types;
                    }
                    index++;
                }
                if ( !found )
                {
                    failedListenerDTOs.add((FailedListenerDTO)ListenerDTOBuilder.build(status.getHandler(), status.getResult()));
                }
            }
        }
    }
}
