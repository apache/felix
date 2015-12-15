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

import java.io.*;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.apache.felix.fileinstall.ArtifactListener;
import org.apache.felix.fileinstall.internal.Util.Logger;
import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.felix.utils.properties.InterpolationHelper;
import org.osgi.framework.*;
import org.osgi.service.cm.*;

/**
 * ArtifactInstaller for configurations.
 * TODO: This service lifecycle should be bound to the ConfigurationAdmin service lifecycle.
 */
public class ConfigInstaller implements ArtifactInstaller, ConfigurationListener
{
    private final BundleContext context;
    private final ConfigurationAdmin configAdmin;
    private final FileInstall fileInstall;
    private ServiceRegistration registration;

    ConfigInstaller(BundleContext context, ConfigurationAdmin configAdmin, FileInstall fileInstall)
    {
        this.context = context;
        this.configAdmin = configAdmin;
        this.fileInstall = fileInstall;
    }

    public void init()
    {
        registration = this.context.registerService(
                new String[] {
                    ConfigurationListener.class.getName(),
                    ArtifactListener.class.getName(),
                    ArtifactInstaller.class.getName()
                },
                this, null);
    }

    public void destroy()
    {
        registration.unregister();
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

    public void configurationEvent(final ConfigurationEvent configurationEvent)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.doPrivileged(
                    new PrivilegedAction<Void>()
                    {
                        public Void run()
                        {
                            doConfigurationEvent(configurationEvent);
                            return null;
                        }
                    }
            );
        }
        else
        {
            doConfigurationEvent(configurationEvent);
        }
    }

    public void doConfigurationEvent(ConfigurationEvent configurationEvent)
    {
        // Check if writing back configurations has been disabled.
        {
            if (!shouldSaveConfig())
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
                File file = fileName != null ? fromConfigKey(fileName) : null;
                if( file != null && file.isFile()   ) {
                    if( fileName.endsWith( ".cfg" ) )
                    {
                        org.apache.felix.utils.properties.Properties props = new org.apache.felix.utils.properties.Properties( file, context );
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
                        Properties props = new Properties();
                        for( Enumeration e  = dict.keys(); e.hasMoreElements(); )
                        {
                            String key = e.nextElement().toString();
                            if( !Constants.SERVICE_PID.equals(key)
                                    && !ConfigurationAdmin.SERVICE_FACTORYPID.equals(key)
                                    && !DirectoryWatcher.FILENAME.equals(key) )
                            {
                                props.put( key, dict.get( key ) );
                            }
                        }
                        try
                        {
                            ConfigurationHandler.write( fos, props );
                        }
                        finally
                        {
                            fos.close();
                        }
                    }
                    // we're just writing out what's already loaded into ConfigAdmin, so
                    // update file checksum since lastModified gets updated when writing
                    fileInstall.updateChecksum(file);
                }
            }
            catch (Exception e)
            {
                Util.log( context, Logger.LOG_INFO, "Unable to save configuration", e );
            }
        }
    }

    boolean shouldSaveConfig()
    {
        String str = this.context.getProperty( DirectoryWatcher.ENABLE_CONFIG_SAVE );
        if (str == null)
        {
            str = this.context.getProperty( DirectoryWatcher.DISABLE_CONFIG_SAVE );
        }
        if (str != null)
        {
            return Boolean.valueOf(str);
        }
        return true;
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
     * @return <code>true</code> if the configuration has been updated
     * @throws Exception
     */
    boolean setConfig(final File f) throws Exception
    {
        final Hashtable<String, Object> ht = new Hashtable<String, Object>();
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
                Map<String, String> strMap = new HashMap<String, String>();
                for (Object k : p.keySet()) {
                    strMap.put(k.toString(), p.getProperty(k.toString()));
                }
                InterpolationHelper.performSubstitution(strMap, context);
                ht.putAll(strMap);
            }
            else if ( f.getName().endsWith( ".config" ) )
            {
                final Dictionary config = ConfigurationHandler.read(in);
                final Enumeration i = config.keys();
                while ( i.hasMoreElements() )
                {
                    final Object key = i.nextElement();
                    ht.put(key.toString(), config.get(key));
                }
            }
        }
        finally
        {
            in.close();
        }

        String pid[] = parsePid(f.getName());
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1]);

        Dictionary<String, Object> props = config.getProperties();
        Hashtable<String, Object> old = props != null ? new Hashtable<String, Object>(new DictionaryAsMap<String, Object>(props)) : null;
        if (old != null) {
        	old.remove( DirectoryWatcher.FILENAME );
        	old.remove( Constants.SERVICE_PID );
        	old.remove( ConfigurationAdmin.SERVICE_FACTORYPID );
        }

        if( !ht.equals( old ) )
        {
            ht.put(DirectoryWatcher.FILENAME, toConfigKey(f));
            if (old == null) {
                Util.log(context, Logger.LOG_INFO, "Creating configuration from " + pid[0]
                        + (pid[1] == null ? "" : "-" + pid[1]) + ".cfg", null);
            } else {
                Util.log(context, Logger.LOG_INFO, "Updating configuration from " + pid[0]
                        + (pid[1] == null ? "" : "-" + pid[1]) + ".cfg", null);
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
     *            File where the configuration in was defined.
     * @return <code>true</code>
     * @throws Exception
     */
    boolean deleteConfig(File f) throws Exception
    {
        String pid[] = parsePid(f.getName());
        Util.log(context, Logger.LOG_INFO, "Deleting configuration from " + pid[0]
                + (pid[1] == null ? "" : "-" + pid[1]) + ".cfg", null);
        Configuration config = getConfiguration(toConfigKey(f), pid[0], pid[1]);
        config.delete();
        return true;
    }

    String toConfigKey(File f) {
        return f.getAbsoluteFile().toURI().toString();
    }

    File fromConfigKey(String key) {
        return new File(URI.create(key));
    }

    String[] parsePid(String path)
    {
        String pid = path.substring(0, path.lastIndexOf('.'));
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
        String filter = "(" + DirectoryWatcher.FILENAME + "=" + escapeFilterValue(fileName) + ")";
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

    private String escapeFilterValue(String s) {
        return s.replaceAll("[(]", "\\\\(").
                replaceAll("[)]", "\\\\)").
                replaceAll("[=]", "\\\\=").
                replaceAll("[\\*]", "\\\\*");
    }

}
