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
package org.apache.felix.configurator.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.configurator.impl.logger.SystemLogger;
import org.apache.felix.configurator.impl.model.BundleState;
import org.apache.felix.configurator.impl.model.Config;
import org.apache.felix.configurator.impl.model.ConfigList;
import org.apache.felix.configurator.impl.model.ConfigPolicy;
import org.apache.felix.configurator.impl.model.ConfigState;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.apache.felix.configurator.impl.model.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * The main class of the configurator.
 *
 */
public class Configurator {

    private static final String PROP_INITIAL = "configurator.initial";

    private static final String PROP_DIRECTORY = "configurator.binaries";

    private final BundleContext bundleContext;

    private final ConfigurationAdmin configAdmin;

    private final State state;

    private final Set<String> activeEnvironments;

    private final org.osgi.util.tracker.BundleTracker<Bundle> tracker;

    private volatile boolean active = true;

    private volatile Object coordinator;

    /**
     * Create a new configurator and start it
     * @param bc The bundle context
     * @param ca The configuration admin
     */
    public Configurator(final BundleContext bc, final ConfigurationAdmin ca) {
        this.bundleContext = bc;
        this.configAdmin = ca;
        this.activeEnvironments = Util.getActiveEnvironments(bc);
        this.state = State.createOrReadState(bundleContext);
        this.state.changeEnvironments(this.activeEnvironments);
        this.tracker = new org.osgi.util.tracker.BundleTracker<Bundle>(this.bundleContext,
                Bundle.INSTALLED|Bundle.ACTIVE|Bundle.RESOLVED|Bundle.STARTING,

                new BundleTrackerCustomizer<Bundle>() {

                    @Override
                    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
                        if ( active ) {
                            synchronized ( this ) {
                                processAddBundle(bundle);
                                process();
                            }
                        }
                        return bundle;
                    }

                    @Override
                    public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
                        this.addingBundle(bundle, event);
                    }

                    @Override
                    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
                        if ( active ) {
                            try {
                                synchronized ( this ) {
                                    processRemoveBundle(bundle.getBundleId());
                                    process();
                                }
                            } catch ( final IllegalStateException ise) {
                                SystemLogger.error("Error processing bundle " + bundle.getBundleId() + " - " + bundle.getSymbolicName(), ise);
                            }
                        }
                    }

        });
    }

    /**
     * Shut down the configurator
     */
    public void shutdown() {
        this.active = false;
        this.tracker.close();
    }

    /**
     * Start the configurator.
     */
    public void start() {
        // get the directory for storing binaries
        String dirPath = this.bundleContext.getProperty(PROP_DIRECTORY);
        if ( dirPath != null ) {
            final File dir = new File(dirPath);
            if ( dir.exists() && dir.isDirectory() ) {
                Util.binDirectory = dir;
            } else if ( dir.exists() ) {
                SystemLogger.error("Directory property is pointing at a file not a dir: " + dirPath + ". Using default path.");
            } else {
                try {
                    if ( dir.mkdirs() ) {
                        Util.binDirectory = dir;
                    }
                } catch ( final SecurityException se ) {
                    // ignore
                }
                if ( Util.binDirectory == null ) {
                    SystemLogger.error("Unable to create a directory at: " + dirPath + ". Using default path.");
                }
            }
        }
        if ( Util.binDirectory == null ) {
            Util.binDirectory = this.bundleContext.getDataFile("binaries" + File.separatorChar + ".check");
            Util.binDirectory = Util.binDirectory.getParentFile();
            Util.binDirectory.mkdirs();
        }

        // before we start the tracker we process all available bundles and initial configuration
        final String initial = this.bundleContext.getProperty(PROP_INITIAL);
        if ( initial == null ) {
            this.processRemoveBundle(-1);
        } else {
            // JSON or URLs ?
            final Set<String> hashes = new HashSet<>();
            final Map<String, String> files = new TreeMap<>();

            if ( !initial.trim().startsWith("{") ) {
                // URLs
                final String[] urls = initial.trim().split(",");
                for(final String urlString : urls) {
                    URL url = null;
                    try {
                        url = new URL(urlString);
                    } catch (final MalformedURLException e) {
                    }
                    if ( url != null ) {
                        final String contents = Util.getResource(urlString, url);
                        if ( contents != null ) {
                            files.put(urlString, contents);
                            hashes.add(Util.getSHA256(contents.trim()));
                        }
                    }
                }
            } else {
                // JSON
                hashes.add(Util.getSHA256(initial.trim()));
                files.put(PROP_INITIAL, initial);
            }
            if ( state.getInitialHashes() != null && state.getInitialHashes().equals(hashes)) {
                if ( state.environmentsChanged() ) {
                    state.checkEnvironments(-1);
                }
            } else {
                if ( state.getInitialHashes() != null ) {
                    processRemoveBundle(-1);
                }
                final List<ConfigurationFile> allFiles = new ArrayList<>();
                for(final Map.Entry<String, String> entry : files.entrySet()) {
                    final ConfigurationFile file = org.apache.felix.configurator.impl.json.JSONUtil.readJSON(null, entry.getKey(), null, -1, entry.getValue());
                    if ( file != null ) {
                        allFiles.add(file);
                    }
                }
                final BundleState bState = new BundleState();
                bState.addFiles(allFiles);
                for(final String pid : bState.getPids()) {
                    state.addAll(pid, bState.getConfigurations(pid));
                }
                state.setInitialHashes(hashes);
            }

        }

        final Bundle[] bundles = this.bundleContext.getBundles();
        final Set<Long> ids = new HashSet<>();
        for(final Bundle b : bundles) {
            ids.add(b.getBundleId());
            processAddBundle(b);
        }
        for(final long id : state.getKnownBundleIds()) {
            if ( !ids.contains(id) ) {
                processRemoveBundle(id);
            }
        }
        this.process();
        this.tracker.open();
    }

    public void processAddBundle(final Bundle bundle) {
        try {
            final long bundleId = bundle.getBundleId();
            final long bundleLastModified = bundle.getLastModified();
            final Long lastModified = state.getLastModified(bundleId);
            if ( lastModified != null && lastModified == bundleLastModified ) {
                if ( state.environmentsChanged() ) {
                    state.checkEnvironments(bundleId);
                }
                // no changes, nothing to do
                return;
            }
            if ( lastModified != null ) {
                processRemoveBundle(bundleId);
            }
            final Set<String> paths = Util.isConfigurerBundle(bundle);
            if ( paths != null ) {
                final BundleState config = org.apache.felix.configurator.impl.json.JSONUtil.readConfigurationsFromBundle(bundle, paths);
                for(final String pid : config.getPids()) {
                    state.addAll(pid, config.getConfigurations(pid));
                }
            }
            state.setLastModified(bundleId, bundleLastModified);
        } catch ( final IllegalStateException ise) {
            SystemLogger.error("Error processing bundle " + bundle.getBundleId() + " - " + bundle.getSymbolicName(), ise);
        }
    }

    public void processRemoveBundle(final long bundleId) {
        state.removeLastModified(bundleId);
        for(final String pid : state.getPids()) {
            final ConfigList configList = state.getConfigurations(pid);
            configList.uninstall(bundleId);
        }
    }

    /**
     * Set or unset the coordinator service
     * @param coordinator The coordinator service or {@code null}
     */
    public void setCoordinator(final Object coordinator) {
        this.coordinator = coordinator;
    }

    /**
     * Process the state to activate/deactivate configurations
     */
    public void process() {
        final Object localCoordinator = this.coordinator;
        Object coordination = null;
        if ( localCoordinator != null ) {
            coordination = CoordinatorUtil.getOrStartCoordination(localCoordinator);
        }
        try {
            for(final String pid : state.getPids()) {
                final ConfigList configList = state.getConfigurations(pid);

                if ( configList.hasChanges() ) {
                    process(configList);
                    State.writeState(this.bundleContext, state);
                }
            }
        } finally {
            if ( coordination != null ) {
                CoordinatorUtil.endCoordination(coordination);
            }
        }
    }

    /**
     * Process changes to a pid.
     * @param configList The config list
     */
    public void process(final ConfigList configList) {
        Config toActivate = null;
        Config toDeactivate = null;

        for(final Config cfg : configList) {
            final boolean canBeActive = cfg.isActive(activeEnvironments);

            switch ( cfg.getState() ) {
                case INSTALL     : // activate if first found
                                   if ( canBeActive && toActivate == null ) {
                                       toActivate = cfg;
                                   }
                                   break;

                case IGNORED     : // same as installed
                case INSTALLED   : // check if we have to uninstall
                                   if ( canBeActive ) {
                                       if ( toActivate == null ) {
                                           toActivate = cfg;
                                       } else {
                                           cfg.setState(ConfigState.INSTALL);
                                       }
                                   } else {
                                       if ( toDeactivate == null ) { // this should always be null
                                           cfg.setState(ConfigState.UNINSTALL);
                                           toDeactivate = cfg;
                                       } else {
                                           cfg.setState(ConfigState.UNINSTALLED);
                                       }
                                   }
                                   break;

                case UNINSTALL   : // deactivate if first found (we should only find one anyway)
                                   if ( toDeactivate == null ) {
                                       toDeactivate = cfg;
                                   }
                                   break;

                case UNINSTALLED : // nothing to do
                                   break;
            }

        }
        // if there is a configuration to activate, we can directly activate it
        // without deactivating (reduced the changes of the configuration from two
        // to one)
        if ( toActivate != null && toActivate.getState() == ConfigState.INSTALL ) {
            activate(configList, toActivate);
        }
        if ( toActivate == null && toDeactivate != null ) {
            deactivate(configList, toDeactivate);
        }

        // remove all uninstall(ed) configurations
        final Iterator<Config> iter = configList.iterator();
        boolean foundInstalled = false;
        while ( iter.hasNext() ) {
            final Config cfg = iter.next();
            if ( cfg.getState() == ConfigState.UNINSTALL || cfg.getState() == ConfigState.UNINSTALLED ) {
                if ( cfg.getFiles() != null ) {
                    for(final File f : cfg.getFiles()) {
                        f.delete();
                    }
                }
                iter.remove();
            } else if ( cfg.getState() == ConfigState.INSTALLED ) {
                if ( foundInstalled ) {
                    cfg.setState(ConfigState.INSTALL);
                } else {
                    foundInstalled = true;
                }
            }
        }

        // mark as processed
        configList.setHasChanges(false);
    }

    /**
     * Try to activate a configuration
     * Check policy and change count
     * @param cfg The configuration
     */
    public void activate(final ConfigList configList, final Config cfg) {
        boolean ignore = false;
        try {
            // get existing configuration - if any
            boolean update = false;
            Configuration configuration = ConfigUtil.getOrCreateConfiguration(this.configAdmin, cfg.getPid(), false);
            if ( configuration == null ) {
                // new configuration
                configuration = ConfigUtil.getOrCreateConfiguration(this.configAdmin, cfg.getPid(), true);
                update = true;
            } else {
                if ( cfg.getPolicy() == ConfigPolicy.FORCE ) {
                    update = true;
                } else {
                    if ( configList.getLastInstalled() == null
                         || configList.getChangeCount() != configuration.getChangeCount() ) {
                        ignore = true;
                    } else {
                       update = true;
                    }
                }
            }

            if ( update ) {
                configuration.updateIfDifferent(cfg.getProperties());
                cfg.setState(ConfigState.INSTALLED);
                configList.setChangeCount(configuration.getChangeCount());
                configList.setLastInstalled(cfg);
            }
        } catch (final InvalidSyntaxException | IOException e) {
            SystemLogger.error("Unable to update configuration " + cfg.getPid() + " : " + e.getMessage(), e);
            ignore = true;
        }
        if ( ignore ) {
            cfg.setState(ConfigState.IGNORED);
            configList.setChangeCount(-1);
            configList.setLastInstalled(null);
        }
    }

    /**
     * Try to deactivate a configuration
     * Check policy and change count
     * @param cfg The configuration
     */
    public void deactivate(final ConfigList configList, final Config cfg) {
        try {
            final Configuration c = ConfigUtil.getOrCreateConfiguration(this.configAdmin, cfg.getPid(), false);
            if ( c != null ) {
                if ( cfg.getPolicy() == ConfigPolicy.FORCE
                     || configList.getChangeCount() == c.getChangeCount() ) {
                    c.delete();
                }
            }
        } catch (final InvalidSyntaxException | IOException e) {
            SystemLogger.error("Unable to remove configuration " + cfg.getPid() + " : " + e.getMessage(), e);
        }
        cfg.setState(ConfigState.UNINSTALLED);
        configList.setChangeCount(-1);
        configList.setLastInstalled(null);
    }
}
