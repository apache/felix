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

import java.util.List;

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
    /** The methods in the order they need to be called. */
    private final List<ReferenceMethods> methods;

    public DuplexReferenceMethods(final List<ReferenceMethods> methods)
    {
        this.methods = methods;
    }

    public ReferenceMethod getBind()
    {
    	final ReferenceMethod[] list = new ReferenceMethod[methods.size()];
    	int index = 0;
    	for(final ReferenceMethods m : methods)
    	{
    		list[index] = m.getBind();
    		index++;
    	}
        return new DuplexReferenceMethod(list);
    }

    public ReferenceMethod getUnbind()
    {
    	final ReferenceMethod[] list = new ReferenceMethod[methods.size()];
    	int index = 0;
    	for(final ReferenceMethods m : methods)
    	{
    		list[index] = m.getUnbind();
    		index++;
    	}
        return new DuplexReferenceMethod(list);
    }

    public ReferenceMethod getUpdated()
    {
    	final ReferenceMethod[] list = new ReferenceMethod[methods.size()];
    	int index = 0;
    	for(final ReferenceMethods m : methods)
    	{
    		list[index] = m.getUpdated();
    		index++;
    	}
        return new DuplexReferenceMethod(list);
    }

    public InitReferenceMethod getInit()
    {
        return new InitReferenceMethod()
        {

            public boolean init(Object componentInstance, SimpleLogger logger)
            {
            	boolean result = true;
            	for(final ReferenceMethods m : methods)
            	{
            		final InitReferenceMethod init = m.getInit();
            		if ( init != null )
            		{
            			result = init.init(componentInstance, logger);
            			if ( !result ) 
            			{
            				break;
            			}
            		}
            	}
                return result;
            }
        };
    }

    private static final class DuplexReferenceMethod implements ReferenceMethod
    {

        private final ReferenceMethod[] methods;

        public DuplexReferenceMethod(final ReferenceMethod[] methods)
        {
            this.methods = methods;
        }

        public MethodResult invoke(Object componentInstance,
                                   ComponentContextImpl<?> componentContext,
                                   RefPair<?, ?> refPair,
                                   MethodResult methodCallFailureResult,
                                   SimpleLogger logger) {
        	MethodResult result = null;
        	for(final ReferenceMethod m : methods) 
        	{
        		result = m.invoke(componentInstance, componentContext, refPair, methodCallFailureResult, logger);
        		if ( result == null )        			
        		{
        			break;
        		}
        	}
            return result;
        }

        public <S, T> boolean getServiceObject(ComponentContextImpl<S> key,
                RefPair<S, T> refPair, BundleContext context,
                SimpleLogger logger)
        {
            // only if all return true, we return true
        	boolean result = false;
        	for(final ReferenceMethod m : methods) 
        	{
        		result = m.getServiceObject(key, refPair, context, logger);
        		if (!result )        			
        		{
        			break;
        		}
        	}
            return result;
        }
    }
}
