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
package org.apache.sling.console.web.internal;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;

/**
 * The <code>BundleListRender</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.reference name="installerService" interface="org.apache.sling.assembly.installer.InstallerService"
 * @scr.reference name="startLevel" interface="org.osgi.service.startlevel.StartLevel"
 * @scr.service
 */
public class AssemblyListRender extends BundleListRender {

    public static final String NAME = "assemblyList";
    public static final String LABEL = "Assemblies";

    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getName()
     */
    public String getName() {
        return NAME;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Render#getLabel()
     */
    public String getLabel() {
        return LABEL;
    }

    protected Bundle[] getBundles() {
        Bundle[] bundles = this.getBundleContext().getBundles();
        List assList = new ArrayList();
        for (int i=0; i < bundles.length; i++) {
            if (bundles[i].getHeaders().get("Assembly-Bundles") != null) {
                assList.add(bundles[i]);
            }
        }
        return (Bundle[]) assList.toArray(new Bundle[assList.size()]);
    }
}
