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
package org.apache.felix.scr.impl.metadata;

public enum DSVersion
{
    DSnone(-1),
    DS10(0),
    DS11(1),
    DS11Felix(2),
    DS12(3),
    DS12Felix(4),
    DS13(5);
    
    private final int version;
    
    DSVersion(int version) 
    {
        this.version = version;
    }
    
    public boolean isDS10()
    {
        return version >=DS10.version;
    }

    public boolean isDS11()
    {
        return version >=DS11.version;
    }

    public boolean isDS12()
    {
        return version >=DS12.version;
    }

    public boolean isDS13()
    {
        return version >=DS13.version;
    }

}
