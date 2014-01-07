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
package org.apache.felix.scr.integration.components.deadlock;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

public class TestComponent
{
    
    private ComponentContext cc;
    
    private ServiceReference sr;
    private boolean success1;
    private boolean success2;
    
    protected void activate(ComponentContext cc)
    {
        this.cc =cc;
    }
    
    protected void setRef(ServiceReference sr)
    {
        this.sr = sr;
    }
    
    protected void unsetRef(ServiceReference sr)
    {
        if (sr == this.sr)
        {
            this.sr = null;
        }
    }

    public void doIt()
    {
        Thread t = new Thread() 
        {

            @Override
            public void run()
            {
                Object sc = cc.locateService("Ref", sr);
                if (sc != null)
                {
                    success1 = true;
                }
            }
            
        };
        t.start();
        try
        {
            t.join();
            success2 = true;
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
       
    }
    
    public boolean isSuccess1()
    {
        return success1;
    }

    public boolean isSuccess2()
    {
        return success2;
    }


}
