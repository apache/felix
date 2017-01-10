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

import org.apache.felix.scr.impl.helper.SimpleLogger;
import org.osgi.framework.BundleContext;

public class DuplexReferenceMethods implements ReferenceMethods
{
    private final ReferenceMethod bind;
    private final ReferenceMethod updated;
    private final ReferenceMethod unbind;
    private final InitReferenceMethod init;

    public DuplexReferenceMethods(final List<ReferenceMethods> methods)
    {
    	final ReferenceMethod[] bindList = new ReferenceMethod[methods.size()];
    	final ReferenceMethod[] updatedList = new ReferenceMethod[methods.size()];
    	final ReferenceMethod[] unbindList = new ReferenceMethod[methods.size()];
    	int index = 0;
    	for(final ReferenceMethods m : methods)
    	{
    		bindList[index] = m.getBind();
    		updatedList[index] = m.getUpdated();
    		unbindList[index] = m.getUnbind();
    		index++;
    	}
        this.bind = new DuplexReferenceMethod(bindList);
        this.updated = new DuplexReferenceMethod(updatedList);
        this.unbind = new DuplexReferenceMethod(unbindList);
        this.init = new InitReferenceMethod()
        {
            public boolean init(final Object componentInstance, final SimpleLogger logger)
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

    public ReferenceMethod getBind()
    {
    	return this.bind;
    }

    public ReferenceMethod getUnbind()
    {
    	return this.unbind;
    }

    public ReferenceMethod getUpdated()
    {
    	return this.updated;
    }

    public InitReferenceMethod getInit()
    {
    	return this.init;
    }

    private static final class DuplexReferenceMethod implements ReferenceMethod
    {

        private final ReferenceMethod[] methods;

        public DuplexReferenceMethod(final ReferenceMethod[] methods)
        {
            this.methods = methods;
        }

        public MethodResult invoke(final Object componentInstance,
        		final BindParameters parameters,
        		final MethodResult methodCallFailureResult,
        		final SimpleLogger logger) {
        	MethodResult result = null;
        	for(final ReferenceMethod m : methods) 
        	{
        		result = m.invoke(componentInstance, parameters, methodCallFailureResult, logger);
        		if ( result == null )        			
        		{
        			break;
        		}
        	}
            return result;
        }

        public <S, T> boolean getServiceObject(
        		final BindParameters parameters,
        		final BundleContext context,
        		final SimpleLogger logger)
        {
            // only if all return true, we return true
        	boolean result = false;
        	for(final ReferenceMethod m : methods) 
        	{
        		result = m.getServiceObject(parameters, context, logger);
        		if (!result )        			
        		{
        			break;
        		}
        	}
            return result;
        }
    }
}
