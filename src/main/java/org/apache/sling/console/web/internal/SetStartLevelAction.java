/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.console.web.internal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.console.web.Action;
import org.osgi.service.startlevel.StartLevel;

/**
 * The <code>GCAction</code> TODO
 *
 * @scr.component metatype="false"
 * @scr.service
 */
public class SetStartLevelAction implements Action {
    
    public static final String NAME = "setStartLevel";
    public static final String LABEL = "Set Start Level";

    /** @scr.reference */
    private StartLevel startLevel;
    
    public String getName() {
        return NAME;
    }
    
    public String getLabel() {
        return LABEL;
    }
    
    /* (non-Javadoc)
     * @see org.apache.sling.manager.web.internal.Action#performAction(javax.servlet.http.HttpServletRequest)
     */
    public boolean performAction(HttpServletRequest request, HttpServletResponse response) {
        
        int bundleSL = getParameterInt(request, "bundleStartLevel");
        if (bundleSL > 0 && bundleSL != startLevel.getInitialBundleStartLevel()) {
            startLevel.setInitialBundleStartLevel(bundleSL);
        }

        int systemSL = getParameterInt(request, "systemStartLevel");
        if (systemSL > 0 && systemSL != startLevel.getStartLevel()) {
            startLevel.setStartLevel(systemSL);
        }
        
        return true;
    }

    private int getParameterInt(HttpServletRequest request, String name) {
        try {
            return Integer.parseInt(request.getParameter(name));
        } catch (NumberFormatException nfe) {
            // don't care
        }
        
        return -1;
    }
    
    //--------- SCR -----------------------------------------------------------
    
    protected void bindStartLevel(StartLevel startLevel) {
        this.startLevel = startLevel;
    }

    protected void unbindStartLevel(StartLevel startLevel) {
        this.startLevel = null;
    }
}
