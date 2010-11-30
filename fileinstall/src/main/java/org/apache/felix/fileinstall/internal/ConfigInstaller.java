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
package org.apache.felix.fileinstall.internal;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.internal.Util.Logger;
import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * ArtifactInstaller for configurations.
 * TODO: This service lifecycle should be bound to the ConfigurationAdmin service lifecycle.
 */
public class ConfigInstaller implements ArtifactInstaller, ConfigurationListener
{
    private final BundleContext context;
    private final ConfigurationAdmin configAdmin;
    private ServiceRegistration registration;

    ConfigInstaller(BundleContext context, ConfigurationAdmin configAdmin)
    {
        this.context = context;
        this.configAdmin = configAdmin;
    }

    public void init()
    {
        if (registration == null)
        {
            Properties props = new Properties();
            registration = this.context.registerService(ConfigurationListener.class.getName(), this, props);
        }
    }

    public void destroy()
    {
        if (registration != null)
        {
            registration.unregister();
            registration = null;
        }
    }

    public boolean canHandle(File artifact)
    {
        return artifact.getName().endsWith(".cfg")
            || artifact.getName().endsWith(".config");
    }

    public void install(File artifact) throws Exception
    {
        setConfig(artifact);
    }

    public void update(File artifact) throws Exception
    {
        setConfig(artifact);
    }

    public void uninstall(File artifact) throws Exception
    {
        deleteConfig(artifact);
    }

    public void configurationEvent(ConfigurationEvent configurationEvent)
    {
        // Check if writing back configurations has been disabled.
        {
            Object obj = this.context.getProperty( DirectoryWatcher.DISABLE_CONFIG_SAVE );
            if (obj instanceof String) {
                obj = new Boolean((String) obj );
            }
            if( Boolean.FALSE.equals( obj ) )
            {
                return;
            }
        }

        if (configurationEvent.getType() == ConfigurationEvent.CM_UPDATED)
        {
            try
            {
                Configuration config = getConfigurationAdmin().getConfiguration(
                                            configurationEvent.getPid(),
                                            configurationEvent.getFactoryPid());
                Dictionary dict = config.getProperties();
                String fileName = (String) dict.get( DirectoryWatcher.FILENAME );
                File file = fileName != null ? new File(fileName) : null;
                if( file != null && file.isFile()   ) {
                    if( fileName.endsWith( ".cfg" ) )
                    {
                        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties( file );
                        for( Enumeration e  = dict.keys(); e.hasMoreElements(); )
                        {
                            String key = e.nextElement().toString();
                            if( !Constants.SERVICE_PID.equals(key)
                                    && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                                    && !DirectoryWatcher.FILENAME.equals(key) )
                            {
                                String val = dict.get( key ).toString();
                                props.put( key, val );
                            }
                        }
                        props.save();
                    }
                    else if( fileName.endsWith( ".config" ) )
                    {
                        OutputStream fos = new FileOutputStream( file );
                        try
                        {
                            ConfigurationHandler.write( fos, dict );
                        }
                        finally
                        {
                            fos.close();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Util.log( context, Util.getGlobalLogLevel(context), Logger.LOG_INFO, "Unable to save configuration", e );
            }
        }
    }

    ConfigurationAdmin getConfigurationAdmin()
    {
        return configAdmin;
    }

    /**
     * Set the configuration based on the config file.
     *
     * @param f
     *            Configuration file
     * @return
     * @throws Exception
     */
    boolean setConfig(final File f) throws Exception
    {
        final Hashtable ht = new Hashtable();
        final InputStream in = new BufferedInputStream(new FileInputStream(f));
        try
        {
            if ( f.getName().endsWith( ".cfg" ) )
            {
                final Properties p = new Properties();
                in.mark(1);
                boolean isXml = in.read() == '<';
                in.reset();
                if (isXml) {
                    p.loadFromXML(in);
                } else {
                    p.load(in);
                }
                InterpolationHelper.performSubstitution((Map) p, context);
                ht.putAll(p);
            }
            else if ( f.getName().endsWith( ".config" ) )
            {
                final Dictionary config = ConfigurationHandler.read(in);
                final Enumeration i = config.keys();
                while ( i.hasMoreElements() )
                {
                    final Object key = i.nextElement();
                    ht.put(key, config.get(key));
                }
            }
        }
        finally
        {
            in.close();
        }

        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(f.getAbsolutePath(), pid[0], pid[1]);

        Hashtable old = new Hashtable(new DictionaryAsMap(config.getProperties()));
        old.remove( DirectoryWatcher.FILENAME );
        old.remove( Constants.SERVICE_PID );
        old.remove( ConfigurationAdmin.SERVICE_FACTORYPID );

        if( !ht.equals( old ) )
        {
            ht.put(DirectoryWatcher.FILENAME, f.getAbsolutePath());
            if (config.getBundleLocation() != null)
            {
                config.setBundleLocation(null);
            }
            config.update(ht);
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * Remove the configuration.
     *
     * @param f
     *            File where the configuration in whas defined.
     * @return
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception
    {
        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(f.getAbsolutePath(), pid[0], pid[1]);
        config.delete();
        return true;
    }

    String[] parsePid(String path)
    {
        String pid = path.substring(0, path.length() - 4);
        int n = pid.indexOf('-');
        if (n > 0)
        {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]
                {
                    pid, factoryPid
                };
        }
        else
        {
            return new String[]
                {
                    pid, null
                };
        }
    }

    Configuration getConfiguration(String fileName, String pid, String factoryPid)
        throws Exception
    {
        Configuration oldConfiguration = findExistingConfiguration(fileName);
        if (oldConfiguration != null)
        {
            Util.log(context, Util.getGlobalLogLevel(context),
                Logger.LOG_DEBUG, "Updating configuration from " + pid
                + (factoryPid == null ? "" : "-" + factoryPid) + ".cfg", null);
            return oldConfiguration;
        }
        else
        {
            Configuration newConfiguration;
            if (factoryPid != null)
            {
                newConfiguration = getConfigurationAdmin().createFactoryConfiguration(pid, null);
            }
            else
            {
                newConfiguration = getConfigurationAdmin().getConfiguration(pid, null);
            }
            return newConfiguration;
        }
    }

    Configuration findExistingConfiguration(String fileName) throws Exception
    {
        String filter = "(" + DirectoryWatcher.FILENAME + "=" + fileName + ")";
        Configuration[] configurations = getConfigurationAdmin().listConfigurations(filter);
        if (configurations != null && configurations.length > 0)
        {
            return configurations[0];
        }
        else
        {
            return null;
        }
    }

}
