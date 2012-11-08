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
package org.apache.felix.servicediagnostics.sample

import aQute.bnd.annotation.component._

/**
 * This class is a basic SCR based demonstration
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(immediate=true, provide=Array(classOf[TestDS])) class TestDS 

//  DM1 -> DS1 -> DM2 -> Unavail
@Component(provide=Array(classOf[DS1])) 
class DS1 
{
    @Reference def bind(s:DM2) = {}
}

// DMP(0,1) -> DSP(0) -> DMP(2) -> Unavail
@Component(provide=Array(classOf[DSP]), properties=Array("p=0")) 
class DSP
{
    @Reference(target="(p=2)") def bind(s:DMP) = {}
}

// DML1 -> DSL1 -> DML2 -> DML1
@Component(provide=Array(classOf[DSL1]), properties=Array("p=0","q=1")) 
class DSL1 
{
    @Reference(target="(p=2)") def bind(s:DML2) = {}
}

// DSL2 -(opt)-> DML3 --> DSL3 -> DSL2
@Component(provide=Array(classOf[DSL2]), properties=Array("p=1","q=2")) 
class DSL2 
{
    @Reference(target="(p=3)", optional=true, dynamic=true) def bind(s:DML3) = {}
}
@Component(provide=Array(classOf[DSL3]), properties=Array("p=0","q=3")) 
class DSL3 
{
    @Reference(target="(q=2)") def bind(s:DSL2) = {}
}

