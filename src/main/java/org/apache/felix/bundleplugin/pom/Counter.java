package org.apache.felix.bundleplugin.pom;

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

/**
 * Separate class for counter.
 */
public class Counter
{

    // --------------------------/
    // - Class/Member Variables -/
    // --------------------------/

    /**
     * Field currentIndex
     */
    private int currentIndex = 0;

    /**
     * Field level
     */
    private int level;

    // ----------------/
    // - Constructors -/
    // ----------------/

    public Counter( int depthLevel )
    {
        level = depthLevel;
    } // -- org.apache.maven.model.io.jdom.Counter(int)

    // -----------/
    // - Methods -/
    // -----------/

    /**
     * Method getCurrentIndex
     */
    public int getCurrentIndex()
    {
        return currentIndex;
    } // -- int getCurrentIndex()

    /**
     * Method getDepth
     */
    public int getDepth()
    {
        return level;
    } // -- int getDepth()

    /**
     * Method increaseCount
     */
    public void increaseCount()
    {
        currentIndex = currentIndex + 1;
    } // -- void increaseCount()

}
