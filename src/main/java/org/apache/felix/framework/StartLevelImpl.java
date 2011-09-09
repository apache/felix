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
package org.apache.felix.framework;


import org.osgi.framework.Bundle;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.startlevel.StartLevel;

/**
 * StartLevel service implementation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
**/
public class StartLevelImpl implements StartLevel
{
    private final Felix m_felix;

    StartLevelImpl(Felix felix)
    {
        m_felix = felix;
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getStartLevel()
    **/
    public int getStartLevel()
    {
        return m_felix.adapt(FrameworkStartLevel.class).getStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setStartLevel(int)
    **/
    public void setStartLevel(int startlevel)
    {
        m_felix.adapt(FrameworkStartLevel.class).setStartLevel(startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getBundleStartLevel(org.osgi.framework.Bundle)
    **/
    public int getBundleStartLevel(Bundle bundle)
    {
        return bundle.adapt(BundleStartLevel.class).getStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setBundleStartLevel(org.osgi.framework.Bundle, int)
    **/
    public void setBundleStartLevel(Bundle bundle, int startlevel)
    {
        bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#getInitialBundleStartLevel()
    **/
    public int getInitialBundleStartLevel()
    {
        return m_felix.adapt(FrameworkStartLevel.class).getInitialBundleStartLevel();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#setInitialBundleStartLevel(int)
    **/
    public void setInitialBundleStartLevel(int startlevel)
    {
        m_felix.adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(startlevel);
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#isBundlePersistentlyStarted(org.osgi.framework.Bundle)
    **/
    public boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        return bundle.adapt(BundleStartLevel.class).isPersistentlyStarted();
    }

    /* (non-Javadoc)
     * @see org.osgi.service.startlevel.StartLevel#isBundleActivationPolicyUsed(org.osgi.framework.Bundle)
    **/
	public boolean isBundleActivationPolicyUsed(Bundle bundle)
    {
        return bundle.adapt(BundleStartLevel.class).isActivationPolicyUsed();
    }
}