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

package org.apache.felix.sigil.eclipse.internal.model.repository;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryType;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

/**
 * @author dave
 *
 */
public class ExtensionUtils
{
    public static List<IRepositoryType> loadRepositoryTypes()
    {
        List<IRepositoryType> repositories = new ArrayList<IRepositoryType>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();

        IExtensionPoint p = registry.getExtensionPoint(SigilCore.REPOSITORY_PROVIDER_EXTENSION_POINT_ID);

        for (IExtension e : p.getExtensions())
        {
            for (IConfigurationElement c : e.getConfigurationElements())
            {
                repositories.add(toRepositoryType(e, c));
            }
        }

        return repositories;
    }
    
    /**
     * @param ext 
     * @param conf
     * @return
     */
    public static IRepositoryType toRepositoryType(IExtension ext, IConfigurationElement conf)
    {
        String id = conf.getAttribute("id");
        String type = conf.getAttribute("type");
        boolean dynamic = Boolean.valueOf(conf.getAttribute("dynamic"));
        String icon = conf.getAttribute("icon");
        String provider = conf.getAttribute("alias");                
        Image image = (icon == null || icon.trim().length() == 0) ? null
            : loadImage(ext, icon);
        return new RepositoryType(id, provider, type, dynamic, image);
    }
    
    @SuppressWarnings("unchecked")
    private static Image loadImage(IExtension ext, String icon)
    {
        int i = icon.lastIndexOf("/");
        String path = i == -1 ? "/" : icon.substring(0, i);
        String name = i == -1 ? icon : icon.substring(i + 1);

        Bundle b = Platform.getBundle(ext.getContributor().getName());

        Enumeration<URL> en = b.findEntries(path, name, false);
        Image image = null;

        if (en.hasMoreElements())
        {
            try
            {
                image = SigilCore.loadImage(en.nextElement());
            }
            catch (IOException e)
            {
                SigilCore.error("Failed to load image", e);
            }
        }
        else
        {
            SigilCore.error("No such image " + icon + " in bundle " + b.getSymbolicName());
        }

        return image;
    }
    
}
