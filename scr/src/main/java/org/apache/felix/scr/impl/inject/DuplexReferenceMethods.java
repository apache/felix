/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.scr.impl.inject;

import org.apache.felix.scr.impl.helper.InitReferenceMethod;
import org.apache.felix.scr.impl.helper.MethodResult;
import org.apache.felix.scr.impl.helper.ReferenceMethod;
import org.apache.felix.scr.impl.helper.ReferenceMethods;
import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.apache.felix.scr.impl.manager.ComponentContextImpl;
import org.apache.felix.scr.impl.manager.RefPair;
import org.osgi.framework.BundleContext;

public class DuplexReferenceMethods implements ReferenceMethods
{
    /** First is field methods. */
    private final ReferenceMethods first;

    /** Second is method methods. */
    private final ReferenceMethods second;

    public DuplexReferenceMethods(final ReferenceMethods first, final ReferenceMethods second)
    {
        this.first = first;
        this.second = second;
    }

    public ReferenceMethod getBind()
    {
        return new DuplexReferenceMethod(first.getBind(), second.getBind());
    }

    public ReferenceMethod getUnbind()
    {
        return new DuplexReferenceMethod(first.getUnbind(), second.getUnbind());
    }

    public ReferenceMethod getUpdated()
    {
        return new DuplexReferenceMethod(first.getUpdated(), second.getUpdated());
    }

    public InitReferenceMethod getInit()
    {
        return new InitReferenceMethod()
        {

            public boolean init(Object componentInstance, SimpleLogger logger)
            {
                final InitReferenceMethod i1 = first.getInit();
                if ( i1 != null )
                {
                    if ( !i1.init(componentInstance, logger))
                    {
                        return false;
                    }
                }
                final InitReferenceMethod i2 = second.getInit();
                if ( i2 != null )
                {
                    if ( !i2.init(componentInstance, logger))
                    {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private static final class DuplexReferenceMethod implements ReferenceMethod
    {

        private final ReferenceMethod first;

        private final ReferenceMethod second;

        public DuplexReferenceMethod(final ReferenceMethod first, final ReferenceMethod second)
        {
            this.first = first;
            this.second = second;
        }

        public MethodResult invoke(Object componentInstance,
                                   ComponentContextImpl<?> componentContext,
                                   RefPair<?, ?> refPair,
                                   MethodResult methodCallFailureResult,
                                   SimpleLogger logger) {
            if ( first.invoke(componentInstance, componentContext, refPair, methodCallFailureResult, logger) != null )
            {
                return second.invoke(componentInstance, componentContext, refPair, methodCallFailureResult, logger);
            }
            return null;
        }

        public <S, T> boolean getServiceObject(ComponentContextImpl<S> key,
                RefPair<S, T> refPair, BundleContext context,
                SimpleLogger logger)
        {
            // only if both return true, we return true
            boolean result = first.getServiceObject(key, refPair, context, logger);
            if ( result )
            {
                result = second.getServiceObject(key, refPair, context, logger);
            }
            return result;
        }

    }
}
