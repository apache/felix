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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.security.AccessControlException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.ServiceRegistry.ServiceRegistryCallbacks;
import org.apache.felix.framework.cache.BundleArchive;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.ext.SecurityProvider;
import org.apache.felix.framework.util.EventDispatcher;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.ListenerInfo;
import org.apache.felix.framework.util.MapToDictionary;
import org.apache.felix.framework.util.SecureAction;
import org.apache.felix.framework.util.ShrinkableCollection;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.ThreadGate;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.NativeLibraryClause;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.resolver.ResolutionException;

public class Felix extends BundleImpl implements Framework
{
    // The secure action used to do privileged calls
    static final SecureAction m_secureAction = new SecureAction();

    // The extension manager to handle extension bundles
    private final ExtensionManager m_extensionManager;

    // Framework wiring object.
    private final FrameworkWiringImpl m_fwkWiring;
    private final FrameworkStartLevelImpl m_fwkStartLevel;

    // Logging related member variables.
    private final Logger m_logger;
    // Immutable config properties.
    private final Map<String, Object> m_configMap;
    // Mutable configuration properties passed into constructor.
    private final Map<String, Object> m_configMutableMap;

    // Resolver and resolver state.
    private final StatefulResolver m_resolver;

    // Lock object used to determine if an individual bundle
    // lock or the global lock can be acquired.
    private final Object[] m_bundleLock = new Object[0];
    // Keeps track of threads wanting to acquire the global lock.
    private final List<Thread> m_globalLockWaitersList = new ArrayList<Thread>();
    // The thread currently holding the global lock.
    private Thread m_globalLockThread = null;
    // How many times the global lock was acquired by the thread holding
    // the global lock; if this value is zero, then it means the global
    // lock is free.
    private int m_globalLockCount = 0;

    // Maps a bundle location to a bundle location;
    // used to reserve a location when installing a bundle.
    private final Map<String, String> m_installRequestMap = new HashMap<String, String>();
    // This lock must be acquired to modify m_installRequestMap;
    // to help avoid deadlock this lock as priority 1 and should
    // be acquired before locks with lower priority.
    private final Object[] m_installRequestLock_Priority1 = new Object[0];

    // Contains two maps, one mapping a String bundle location to a bundle
    // and the other mapping a Long bundle identifier to a bundle.
    // CONCURRENCY: Access guarded by the global lock for writes,
    // but no lock for reads since it is copy on write.
    private volatile Map[] m_installedBundles;
    private static final int LOCATION_MAP_IDX = 0;
    private static final int IDENTIFIER_MAP_IDX = 1;
    // An array of uninstalled bundles before a refresh occurs.
    // CONCURRENCY: Access guarded by the global lock for writes,
    // but no lock for reads since it is copy on write.
    private volatile List<BundleImpl> m_uninstalledBundles;
    // Object to keep track of dependencies among bundle revisions.
    private final BundleRevisionDependencies m_dependencies =
        new BundleRevisionDependencies();

    // Framework's active start level.
    private volatile int m_activeStartLevel = FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;
    // Framework's target start level.
    // Normally the target start will equal the active start level, except
    // when the start level is changing, in which case the target start level
    // will report the new start level while the active start level will report
    // the old start level. Once the start level change is complete, the two
    // will become equal again.
    private volatile int m_targetStartLevel = FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;
    // Keep track of bundles currently being processed by start level thread.
    private final SortedSet<StartLevelTuple> m_startLevelBundles =
        new TreeSet<StartLevelTuple>();

    // Local bundle cache.
    private BundleCache m_cache = null;

    // System bundle activator list.
    List m_activatorList = null;

    // Next available bundle identifier.
    private long m_nextId = 1L;
    private final Object m_nextIdLock = new Object[0];

    // Service registry.
    private final ServiceRegistry m_registry;

    // List of event listeners.
    private final EventDispatcher m_dispatcher;

    // Reusable bundle URL stream handler.
    private final URLStreamHandler m_bundleStreamHandler;

    // Boot package delegation.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Shutdown gate.
    private volatile ThreadGate m_shutdownGate = null;

    // Security Manager created by the framework
    private SecurityManager m_securityManager = null;

    // Do we need to consult the default java security policy if no security provider is present?
    private volatile boolean m_securityDefaultPolicy;

    /**
     * <p>
     * This constructor creates a framework instance with a specified <tt>Map</tt>
     * of configuration properties. Configuration properties are used internally
     * by the framework to alter its default behavior. The configuration properties
     * should have a <tt>String</tt> key and an <tt>Object</tt> value. The passed
     * in <tt>Map</tt> is copied by the framework and all keys are converted to
     * <tt>String</tt>s.
     * </p>
     * <p>
     * Configuration properties are generally the sole means to configure the
     * framework's default behavior; the framework does not typically refer to
     * any system properties for configuration information. If a <tt>Map</tt> is
     * supplied to this method for configuration properties, then the framework
     * will consult the <tt>Map</tt> instance for any and all configuration
     * properties. It is possible to specify a <tt>null</tt> for the configuration
     * property map, in which case the framework will use its default behavior
     * in all cases.
     * </p>
     * <p>
     * The following configuration properties can be specified (properties starting
     * with "<tt>felix</tt>" are specific to Felix, while those starting with
     * "<tt>org.osgi</tt>" are standard OSGi properties):
     * </p>
     * <ul>
     *   <li><tt>org.osgi.framework.storage</tt> - Sets the directory to use as
     *       the bundle cache; by default bundle cache directory is
     *       <tt>felix-cache</tt> in the current working directory. The value
     *       should be a valid directory name. The directory name can be either absolute
     *       or relative. Relative directory names are relative to the current working
     *       directory. The specified directory will be created if it does
     *       not exist.
     *   </li>
     *   <li><tt>org.osgi.framework.storage.clean</tt> - Determines whether the
     *       bundle cache is flushed. The value can either be "<tt>none</tt>"
     *       or "<tt>onFirstInit</tt>", where "<tt>none</tt>" does not flush
     *       the bundle cache and "<tt>onFirstInit</tt>" flushes the bundle
     *       cache when the framework instance is first initialized. The default
     *       value is "<tt>none</tt>".
     *   </li>
     *   <li><tt>felix.cache.rootdir</tt> - Sets the root directory to use to
     *       calculate the bundle cache directory for relative directory names. If
     *       <tt>org.osgi.framework.storage</tt> is set to a relative name, by
     *       default it is relative to the current working directory. If this
     *       property is set, then it will be calculated as being relative to
     *       the specified root directory.
     *   </li>
     *   <li><tt>felix.cache.filelimit</tt> - The integer value of this string
     *       sets an upper limit on how many files the cache will open. The default
     *       value is zero, which means there is no limit.
     *   </li>
     *   <li><tt>felix.cache.locking</tt> - Enables or disables bundle cache locking,
     *       which is used to prevent concurrent access to the bundle cache. This is
     *       enabled by default, but on older/smaller JVMs file channel locking is
     *       not available; set this property to <tt>false</tt> to disable it.
     *   </li>
     *   <li><tt>felix.cache.bufsize</tt> - Sets the buffer size to be used by
     *       the cache; the default value is 4096. The integer value of this
     *       string provides control over the size of the internal buffer of the
     *       disk cache for performance reasons.
     *   </li>
     *   <li><tt>org.osgi.framework.system.packages</tt> - Specifies a
     *       comma-delimited list of packages that should be exported via the
     *       System Bundle from the parent class loader. The framework will set
     *       this to a reasonable default. If the value is specified, it
     *       replaces any default value.
     *   </li>
     *   <li><tt>org.osgi.framework.system.packages.extra</tt> - Specifies a
     *       comma-delimited list of packages that should be exported via the
     *       System Bundle from the parent class loader in addition to the
     *       packages in <tt>org.osgi.framework.system.packages</tt>. The default
     *       value is empty. If a value is specified, it is appended to the list
     *       of default or specified packages in
     *       <tt>org.osgi.framework.system.packages</tt>.
     *   </li>
     *   <li><tt>org.osgi.framework.bootdelegation</tt> - Specifies a
     *       comma-delimited list of packages that should be made implicitly
     *       available to all bundles from the parent class loader. It is
     *       recommended not to use this property since it breaks modularity.
     *       The default value is empty.
     *   </li>
     *   <li><tt>felix.systembundle.activators</tt> - A <tt>List</tt> of
     *       <tt>BundleActivator</tt> instances that are started/stopped when
     *       the System Bundle is started/stopped. The specified instances will
     *       receive the System Bundle's <tt>BundleContext</tt> when invoked.
     *   </li>
     *   <li><tt>felix.log.logger</tt> - An instance of <tt>Logger</tt> that the
     *       framework uses as its default logger.
     *   </li>
     *   <li><tt>felix.log.level</tt> - An integer value indicating the degree
     *       of logging reported by the framework; the higher the value the more
     *       logging is reported. If zero ('0') is specified, then logging is
     *       turned off completely. The log levels match those specified in the
     *       OSGi Log Service (i.e., 1 = error, 2 = warning, 3 = information,
     *       and 4 = debug). The default value is 1.
     *   </li>
     *   <li><tt>org.osgi.framework.startlevel.beginning</tt> - The initial
     *       start level of the framework once it starts execution; the default
     *       value is 1.
     *   </li>
     *   <li><tt>felix.startlevel.bundle</tt> - The default start level for
     *       newly installed bundles; the default value is 1.
     *   </li>
     *   <li><tt>felix.service.urlhandlers</tt> - Flag to indicate whether
     *       to activate the URL Handlers service for the framework instance;
     *       the default value is "<tt>true</tt>". Activating the URL Handlers
     *       service will result in the <tt>URL.setURLStreamHandlerFactory()</tt>
     *       and <tt>URLConnection.setContentHandlerFactory()</tt> being called.
     *   </li>
     *   <li><tt>felix.fragment.validation</tt> - Determines if installing
     *       unsupported fragment bundles throws an exception or logs a warning.
     *       Possible values are "<tt>exception</tt>" or "<tt>warning</tt>". The
     *       default value is "<tt>exception</tt>".
     *   </li>
     *   <li><tt>felix.security.defaultpolicy</tt> - Flag to indicate whether
     *       to consult the default java securtiy policy if no security extension
     *       is present. The default value is "<tt>false</tt>".
     *   </li>
     * </ul>
     * <p>
     * The <a href="Main.html"><tt>Main</tt></a> class implements some
     * functionality for default property file handling, which makes it
     * possible to specify configuration properties and framework properties
     * in files that are automatically loaded when starting the framework. If you
     * plan to create your own framework instance, you may be
     * able to take advantage of the features it provides; refer to its
     * class documentation for more information.
     * </p>
     * <p>
     * The framework is not actually started until the <tt>start()</tt> method
     * is called.
     * </p>
     *
     * @param configMap A map for obtaining configuration properties,
     *        may be <tt>null</tt>.
    **/
    public Felix(Map configMap)
    {
        super();
        // Copy the configuration properties; convert keys to strings.
        m_configMutableMap = new StringMap();
        if (configMap != null)
        {
            for (Iterator i = configMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                m_configMutableMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        m_configMap = createUnmodifiableMap(m_configMutableMap);

        // Create logger with appropriate log level. Even though the
        // logger needs the system bundle's context for tracking log
        // services, it is created now because it is needed before
        // the system bundle is activated. The system bundle's context
        // will be set in the init() method after the system bundle
        // is activated.
        if (m_configMutableMap.get(FelixConstants.LOG_LOGGER_PROP) != null)
        {
            m_logger = (Logger) m_configMutableMap.get(FelixConstants.LOG_LOGGER_PROP);
        }
        else
        {
            m_logger = new Logger();
        }
        try
        {
            m_logger.setLogLevel(
                Integer.parseInt(
                    (String) m_configMutableMap.get(FelixConstants.LOG_LEVEL_PROP)));
        }
        catch (NumberFormatException ex)
        {
            // Ignore and just use the default logging level.
        }

        // Initialize framework properties.
        initializeFrameworkProperties();

        // Read the boot delegation property and parse it.
        String s = (m_configMap == null)
            ? null
            : (String) m_configMap.get(Constants.FRAMEWORK_BOOTDELEGATION);
        s = (s == null) ? "java.*" : s + ",java.*";
        StringTokenizer st = new StringTokenizer(s, " ,");
        m_bootPkgs = new String[st.countTokens()];
        m_bootPkgWildcards = new boolean[m_bootPkgs.length];
        for (int i = 0; i < m_bootPkgs.length; i++)
        {
            s = st.nextToken();
            if (s.equals("*") || s.endsWith(".*"))
            {
                m_bootPkgWildcards[i] = true;
                s = s.substring(0, s.length() - 1);
            }
            m_bootPkgs[i] = s;
        }

        //Initialize Native Library Aliases
        NativeLibraryClause.initializeNativeAliases(m_configMap);

        // Read the security default policy property
        m_securityDefaultPolicy = "true".equals(getProperty(FelixConstants.SECURITY_DEFAULT_POLICY));

        // Create default bundle stream handler.
        m_bundleStreamHandler = new URLHandlersBundleStreamHandler(this);

        // Create service registry.
        m_registry = new ServiceRegistry(m_logger, new ServiceRegistryCallbacks() {
            @Override
            public void serviceChanged(ServiceEvent event, Dictionary oldProps)
            {
                fireServiceEvent(event, oldProps);
            }
        });

        // Create a resolver and its state.
        m_resolver = new StatefulResolver(this, m_registry);

        // Create the extension manager, which we will use as the
        // revision for the system bundle.
        m_extensionManager = new ExtensionManager(m_logger, m_configMap, this);
        try
        {
            addRevision(m_extensionManager.getRevision());
        }
        catch (Exception ex)
        {
            // This should not throw an exception, but if so, lets convert it to
            // a runtime exception.
            throw new RuntimeException(ex.getMessage());
        }

        // Create event dispatcher.
        m_dispatcher = new EventDispatcher(m_logger, m_registry);

        // Create framework wiring object.
        m_fwkWiring = new FrameworkWiringImpl(this, m_registry);
        // Create framework start level object.
        m_fwkStartLevel = new FrameworkStartLevelImpl(this, m_registry);
    }

    Logger getLogger()
    {
        return m_logger;
    }

    Map getConfig()
    {
        return m_configMap;
    }

    StatefulResolver getResolver()
    {
        return m_resolver;
    }

    BundleRevisionDependencies getDependencies()
    {
        return m_dependencies;
    }

    URLStreamHandler getBundleStreamHandler()
    {
        return m_bundleStreamHandler;
    }

    String[] getBootPackages()
    {
        return m_bootPkgs;
    }

    boolean[] getBootPackageWildcards()
    {
        return m_bootPkgWildcards;
    }

    private Map createUnmodifiableMap(Map mutableMap)
    {
        Map result = Collections.unmodifiableMap(mutableMap);

        // Work around a bug in certain version of J9 where a call to
        // Collections.unmodifiableMap().keySet().iterator() throws
        // a NoClassDefFoundError. We try to detect this and return
        // the given mutableMap instead.
        try
        {
            result.keySet().iterator();
        }
        catch (NoClassDefFoundError ex)
        {
            return mutableMap;
        }

        return result;
    }

    // This overrides BundleImpl.close() which avoids removing the
    // system bundle module from the resolver state.
    @Override
    void close()
    {
    }

    // This overrides the default behavior of BundleImpl.getFramework()
    // to return "this", since the system bundle is the framework.
    @Override
    Felix getFramework()
    {
        return this;
    }

    @Override
    public <A> A adapt(Class<A> type)
    {
        checkAdapt(type);
        if ((type == Framework.class)
            || (type == Felix.class))
        {
            return (A) this;
        }
        else if ((type == FrameworkWiring.class)
            || (type == FrameworkWiringImpl.class))
        {
            return (A) m_fwkWiring;
        }
        else if ((type == FrameworkStartLevel.class)
            || (type == FrameworkStartLevelImpl.class))
        {
            return (A) m_fwkStartLevel;
        }
        return super.adapt(type);
    }

    @Override
    public long getBundleId()
    {
        return 0;
    }

    @Override
    public long getLastModified()
    {
        return 0;
    }

    @Override
    void setLastModified(long l)
    {
        // Ignore.
    }

    @Override
    String _getLocation()
    {
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    @Override
    public int getPersistentState()
    {
        return Bundle.ACTIVE;
    }

    @Override
    public void setPersistentStateInactive()
    {
        // Ignore.
    }

    @Override
    public void setPersistentStateActive()
    {
        // Ignore.
    }

    @Override
    public void setPersistentStateUninstalled()
    {
        // Ignore.
    }

    /**
     * Overrides standard <tt>BundleImpl.getStartLevel()</tt> behavior to
     * always return zero for the system bundle.
     * @param defaultLevel This parameter is ignored by the system bundle.
     * @return Always returns zero.
    **/
    @Override
    int getStartLevel(int defaultLevel)
    {
        return 0;
    }

    /**
     * Overrides standard <tt>BundleImpl.setStartLevel()</tt> behavior to
     * always throw an exception since the system bundle's start level cannot
     * be changed.
     * @param level This parameter is ignored by the system bundle.
     * @throws IllegalArgumentException Always throws exception since system
     *         bundle's start level cannot be changed.
    **/
    @Override
    void setStartLevel(int level)
    {
        throw new IllegalArgumentException("Cannot set the system bundle's start level.");
    }

    @Override
    public boolean hasPermission(Object obj)
    {
        return true;
    }


    @Override
    public void init() throws BundleException
    {
        init((FrameworkListener[]) null);
    }
    /**
     * @see org.osgi.framework.launch.Framework#init(org.osgi.framework.FrameworkListener[])
     */
    @Override
    public void init(final FrameworkListener... listeners) throws BundleException
    {
        // The system bundle can only be initialized if it currently isn't started.
        acquireBundleLock(this,
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        try
        {
            if ((getState() == Bundle.INSTALLED) || (getState() == Bundle.RESOLVED))
            {
                String security = (String) m_configMap.get(Constants.FRAMEWORK_SECURITY);
                if (security != null)
                {
                    if (System.getSecurityManager() != null)
                    {
                        throw new SecurityException("SecurityManager already installed");
                    }
                    security = security.trim();
                    if (Constants.FRAMEWORK_SECURITY_OSGI.equalsIgnoreCase(security) || (security.length() == 0))
                    {
                        System.setSecurityManager(m_securityManager = new SecurityManager());
                    }
                    else
                    {
                        try
                        {
                            System.setSecurityManager(m_securityManager =
                                (SecurityManager) Class.forName(security).newInstance());
                        }
                        catch (Throwable t)
                        {
                            SecurityException se =
                                new SecurityException(
                                    "Unable to install custom SecurityManager: " + security);
                            se.initCause(t);
                            throw se;
                        }
                    }
                }

                // Generate a framework UUID.
                // Spec says we get a new UUID for each invocation of init().
                m_configMutableMap.put(
                    FelixConstants.FRAMEWORK_UUID,
                    Util.randomUUID());

                // Get any system bundle activators.
                m_activatorList = (List) m_configMutableMap.get(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP);
                m_activatorList = (m_activatorList == null) ? new ArrayList() : new ArrayList(m_activatorList);

                // Initialize event dispatcher.
                m_dispatcher.startDispatching();

                // Create the bundle cache, if necessary, so that we can reload any
                // installed bundles.
                m_cache = (BundleCache)
                    m_configMutableMap.get(FelixConstants.FRAMEWORK_BUNDLECACHE_IMPL);
                if (m_cache == null)
                {
                       try
                       {
                           m_cache = new BundleCache(m_logger, m_configMap);
                       }
                       catch (Exception ex)
                       {
                           m_logger.log(Logger.LOG_ERROR, "Error creating bundle cache.", ex);
                           throw new BundleException("Error creating bundle cache.", ex);
                       }
                }

                // If this is the first time init is called, check to see if
                // we need to flush the bundle cache.
                if (getState() == Bundle.INSTALLED)
                {
                    String clean = (String) m_configMap.get(Constants.FRAMEWORK_STORAGE_CLEAN);
                    if ((clean != null)
                        && clean.equalsIgnoreCase(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT))
                    {
                        try
                        {
                            m_cache.delete();
                        }
                        catch (Exception ex)
                        {
                            throw new BundleException("Unable to flush bundle cache.", ex);
                        }
                    }
                }

                // Initialize installed bundle data structures.
                Map[] maps = new Map[] {
                    new HashMap<String, BundleImpl>(1),
                    new TreeMap<Long, BundleImpl>()
                };
                m_uninstalledBundles = new ArrayList<BundleImpl>(0);

                // Add the system bundle to the set of installed bundles.
                maps[LOCATION_MAP_IDX].put(_getLocation(), this);
                maps[IDENTIFIER_MAP_IDX].put(new Long(0), this);
                m_installedBundles = maps;

                // Manually resolve the system bundle, which will cause its
                // state to be set to RESOLVED.
                try
                {
                    m_resolver.resolve(
                        Collections.singleton(adapt(BundleRevision.class)),
                        Collections.EMPTY_SET);
                }
                catch (ResolutionException ex)
                {
                    // This should never happen.
                    throw new BundleException(
                        "Unresolved constraint in System Bundle:"
                        + ex.getUnresolvedRequirements());
                }

                // Reload the cached bundles before creating and starting the
                // system bundle, since we want all cached bundles to be reloaded
                // when we activate the system bundle and any subsequent system
                // bundle activators passed into the framework constructor.
                BundleArchive[] archives = null;

                // First get cached bundle identifiers.
                try
                {
                    archives = m_cache.getArchives();
                }
                catch (Exception ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Unable to list saved bundles.", ex);
                    archives = null;
                }

                // Create system bundle activator and bundle context so we can activate it.
                setActivator(new SystemBundleActivator());
                setBundleContext(new BundleContextImpl(m_logger, this, this));

                // Now load all cached bundles.
                for (int i = 0; (archives != null) && (i < archives.length); i++)
                {
                    try
                    {
                        // Keep track of the max bundle ID currently in use since we
                        // will need to use this as our next bundle ID value if the
                        // persisted value cannot be read.
                        m_nextId = Math.max(m_nextId, archives[i].getId() + 1);

                        // It is possible that a bundle in the cache was previously
                        // uninstalled, but not completely deleted (perhaps because
                        // of a crash or a locked file), so if we see an archive
                        // with an UNINSTALLED persistent state, then try to remove
                        // it now.
                        if (archives[i].getPersistentState() == Bundle.UNINSTALLED)
                        {
                            archives[i].closeAndDelete();
                        }
                        // Otherwise re-install the cached bundle.
                        else
                        {
                            // Install the cached bundle.
                            reloadBundle(archives[i]);
                        }
                    }
                    catch (Exception ex)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, this, ex);
                        try
                        {
                            m_logger.log(
                                Logger.LOG_ERROR,
                                "Unable to re-install " + archives[i].getLocation(),
                                ex);
                        }
                        catch (Exception ex2)
                        {
                            m_logger.log(
                                Logger.LOG_ERROR,
                                "Unable to re-install cached bundle.",
                                ex);
                        }
                        // TODO: FRAMEWORK - Perhaps we should remove the cached bundle?
                    }
                }

                // Now that we have loaded all cached bundles and have determined the
                // max bundle ID of cached bundles, we need to try to load the next
                // bundle ID from persistent storage. In case of failure, we should
                // keep the max value.
                m_nextId = Math.max(m_nextId, loadNextId());

                // The framework is now in its startup sequence.
                setBundleStateAndNotify(this, Bundle.STARTING);

                // Now it is possible for threads to wait for the framework to stop,
                // so create a gate for that purpose.
                m_shutdownGate = new ThreadGate();

                // add framework listeners
                if ( listeners != null )
                {
                    for(final FrameworkListener fl : listeners)
                    {
                        addFrameworkListener(this, fl);
                    }
                }

                // Start services
                m_resolver.start();
                m_fwkWiring.start();
                m_fwkStartLevel.start();

                try
                {
                    Felix.m_secureAction.startActivator(
                        getActivator(), _getBundleContext());
                }
                catch (Throwable ex)
                {
                    m_dispatcher.stopDispatching();
                    m_logger.log(Logger.LOG_ERROR, "Unable to start system bundle.", ex);
                    throw new RuntimeException("Unable to start system bundle.");
                }

                // We have to check with the security provider (if there is one).
                // This is to avoid having bundles in the cache that have been tampered with
                SecurityProvider sp = getFramework().getSecurityProvider();
                if ((sp != null) && (System.getSecurityManager() != null))
                {
                    boolean locked = acquireGlobalLock();
                    if (!locked)
                    {
                        throw new BundleException(
                            "Unable to acquire the global lock to check the bundle.");
                    }
                    try
                    {
                        for (Object bundle : m_installedBundles[IDENTIFIER_MAP_IDX].values())
                        {
                            try
                            {
                                if (bundle != this)
                                {
                                    setBundleProtectionDomain(((BundleImpl) bundle).adapt(BundleRevisionImpl.class));
                                }
                            }
                            catch (Exception ex)
                            {
                                ((BundleImpl) bundle).close();
                                maps = new Map[] {
                                    new HashMap<String, BundleImpl>(m_installedBundles[LOCATION_MAP_IDX]),
                                    new TreeMap<Long, BundleImpl>(m_installedBundles[IDENTIFIER_MAP_IDX])
                                };
                                maps[LOCATION_MAP_IDX].remove(((BundleImpl) bundle)._getLocation());
                                maps[IDENTIFIER_MAP_IDX].remove(new Long(((BundleImpl) bundle).getBundleId()));
                                m_installedBundles = maps;

                                m_logger.log(
                                    Logger.LOG_ERROR,
                                    "Bundle in cache doesn't pass security check anymore.",
                                    ex);
                            }
                        }
                    }
                    finally
                    {
                        // Always release the global lock.
                        releaseGlobalLock();
                    }
                }

                m_extensionManager.startPendingExtensionBundles(Felix.this);
                m_fwkWiring.refreshBundles(null);

                // Clear the cache of classes coming from the system bundle.
                // This is only used for Felix.getBundle(Class clazz) to speed
                // up class lookup for the system bundle.
                synchronized (m_systemBundleClassCache)
                {
                    m_systemBundleClassCache.clear();
                }
            }
        }
        finally
        {
            releaseBundleLock(this);

            if ( listeners != null )
            {
                for(final FrameworkListener fl : listeners)
                {
                    removeFrameworkListener(this, fl);
                }
            }
        }
    }

    void setBundleProtectionDomain(BundleRevisionImpl revisionImpl) throws Exception
    {
        Object certificates = null;
        SecurityProvider sp = getFramework().getSecurityProvider();
        if ((sp != null) && (System.getSecurityManager() != null))
        {
            BundleImpl bundleImpl = revisionImpl.getBundle();
            sp.checkBundle(bundleImpl);
            Map signers = (Map) sp.getSignerMatcher(bundleImpl, Bundle.SIGNERS_TRUSTED);
            certificates = signers.keySet().toArray(new java.security.cert.Certificate[signers.size()]);
        }
        revisionImpl.setProtectionDomain(
            new BundleProtectionDomain(revisionImpl, certificates));
    }

    /**
     * This method starts the framework instance, which will transition the
     * framework from start level 0 to its active start level as specified in
     * its configuration properties (1 by default). If the <tt>init()</tt> was
     * not explicitly invoked before calling this method, then it will be
     * implicitly invoked before starting the framework.
     *
     * @throws org.osgi.framework.BundleException if any error occurs.
    **/
    @Override
    public void start() throws BundleException
    {
        int startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;

        acquireBundleLock(this,
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        try
        {
            // Initialize if necessary.
            if ((getState() == Bundle.INSTALLED) || (getState() == Bundle.RESOLVED))
            {
                init();
            }

            // If the current state is STARTING, then the system bundle can be started.
            if (getState() == Bundle.STARTING)
            {
                // Get the framework's default start level.
                String s = (String) m_configMap.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
                if (s != null)
                {
                    try
                    {
                        startLevel = Integer.parseInt(s);
                    }
                    catch (NumberFormatException ex)
                    {
                        startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
                    }
                }

                m_fwkStartLevel.setStartLevelAndWait(startLevel);

                // The framework is now running.
                setBundleStateAndNotify(this, Bundle.ACTIVE);
            }
        }
        finally
        {
            releaseBundleLock(this);
        }

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, this);

        // Send a framework event to indicate the framework has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, this, null);
    }

    @Override
    public void start(int options) throws BundleException
    {
        start();
    }

    /**
     * This method asynchronously shuts down the framework, it must be called at the
     * end of a session in order to shutdown all active bundles.
    **/
    @Override
    public void stop() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        if ((getState() & (Bundle.INSTALLED | Bundle.RESOLVED)) == 0)
        {
            // Spec says stop() on SystemBundle should return immediately and
            // shutdown framework on another thread.
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    try
                    {
                        stopBundle(Felix.this, true);
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Exception trying to stop framework.",
                            ex);
                    }
                }
            }, "FelixShutdown").start();
        }
    }

    @Override
    public void stop(int options) throws BundleException
    {
        stop();
    }

    /**
     * This method will cause the calling thread to block until the framework
     * shuts down.
     * @param timeout A timeout value.
     * @throws java.lang.InterruptedException If the thread was interrupted.
    **/
    @Override
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException
    {
        // Throw exception if timeout is negative.
        if (timeout < 0)
        {
            throw new IllegalArgumentException("Timeout cannot be negative.");
        }

        // If there is a gate, wait on it; otherwise, return immediately.
        // Grab a copy of the gate, since it is volatile.
        ThreadGate gate = m_shutdownGate;
        boolean open = false;
        if (gate != null)
        {
            open = gate.await(timeout);
        }

        FrameworkEvent event;
        if (open && (gate.getMessage() != null))
        {
            event = (FrameworkEvent) gate.getMessage();
        }
        else if (!open && (gate != null))
        {
            event = new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
        }
        else
        {
            event = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
        }
        return event;
    }

    @Override
    public void uninstall() throws BundleException
    {
        throw new BundleException("Cannot uninstall the system bundle.");
    }

    @Override
    public void update() throws BundleException
    {
        update(null);
    }

    @Override
    public void update(InputStream is) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        // Spec says to close input stream first.
        try
        {
            if (is != null) is.close();
        }
        catch (IOException ex)
        {
            m_logger.log(Logger.LOG_WARNING, "Exception closing input stream.", ex);
        }

        // Then to stop and restart the framework on a separate thread.
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    // First acquire the system bundle lock to verify the state.
                    acquireBundleLock(Felix.this, Bundle.STARTING | Bundle.ACTIVE);
                    // Set the reason for the shutdown.
                    m_shutdownGate.setMessage(
                        new FrameworkEvent(FrameworkEvent.STOPPED_UPDATE, Felix.this, null));
                    // Record the state and stop the system bundle.
                    int oldState = Felix.this.getState();
                    try
                    {
                        stop();
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Exception stopping framework.", ex);
                    }
                    finally
                    {
                        releaseBundleLock(Felix.this);
                    }

                    // Make sure the framework is stopped.
                    try
                    {
                        waitForStop(0);
                    }
                    catch (InterruptedException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Did not wait for framework to stop.", ex);
                    }

                    // Depending on the old state, restart the framework.
                    try
                    {
                        switch (oldState)
                        {
                            case Bundle.STARTING:
                                init();
                                break;
                            case Bundle.ACTIVE:
                                start();
                                break;
                        }
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Exception restarting framework.", ex);
                    }
                }
                catch (Exception ex)
                {
                    m_logger.log(Logger.LOG_WARNING, "Cannot update an inactive framework.");
                }
            }
        }).start();
    }

    @Override
    public String toString()
    {
        return getSymbolicName() + " [" + getBundleId() +"]";
    }

    /**
     * Returns the active start level of the framework; this method
     * implements functionality for the Start Level service.
     * @return The active start level of the framework.
    **/
    int getActiveStartLevel()
    {
        return m_activeStartLevel;
    }

    /**
     * Implements the functionality of the <tt>setStartLevel()</tt>
     * method for the StartLevel service, but does not do the security or
     * parameter check. The security and parameter check are done in the
     * StartLevel service implementation because this method is called on
     * a separate thread and the caller's thread would already be gone if
     * we did the checks in this method. This method should not be called
     * directly.
     * @param requestedLevel The new start level of the framework.
    **/
    void setActiveStartLevel(int requestedLevel, FrameworkListener[] listeners)
    {
        Bundle[] bundles;

        // Record the target start level immediately and use this for
        // comparisons for starting/stopping bundles to avoid race
        // conditions to restart stopped bundles.
        m_targetStartLevel = requestedLevel;

        // Do nothing if the requested start level is the same as the
        // active start level.
        if (m_targetStartLevel != m_activeStartLevel)
        {
            // Synchronization for changing the start level is rather loose.
            // The framework's active start level is volatile, so no lock is
            // needed to access it. No locks are held while processing the
            // currently installed bundles for starting/stopping based on the new
            // active start level. The only locking that occurs is for individual
            // bundles when startBundle()/stopBundle() is called, but this locking
            // is done in the respective method.
            //
            // This approach does mean that it is possible for a for individual
            // bundle states to change during this operation. If a bundle's start
            // level changes, then it is possible for it to be processed out of
            // order. Uninstalled bundles are just logged and ignored.
            //
            // Calls to this method are only made by the start level thread, which
            // serializes framework start level changes. Thus, it is not possible
            // for two requests to change the framework's start level to interfere
            // with each other.

            // Acquire global lock.
            boolean locked = acquireGlobalLock();
            if (!locked)
            {
                // If the calling thread holds bundle locks, then we might not
                // be able to get the global lock.
                throw new IllegalStateException(
                    "Unable to acquire global lock to create bundle snapshot.");
            }

            boolean bundlesRemaining;
            try
            {
                synchronized (m_startLevelBundles)
                {
                    // Get a sorted snapshot of all installed bundles
                    // to be processed during the start level change.
                    // We also snapshot the start level here, since it
                    // may change and we don't want to consider any
                    // changes since they will be queued for the start
                    // level thread.
                    bundles = getBundles();
                    for (Bundle b : bundles)
                    {
                        m_startLevelBundles.add(
                            new StartLevelTuple(
                                (BundleImpl) b,
                                ((BundleImpl) b).getStartLevel(
                                    getInitialBundleStartLevel())));
                    }
                    bundlesRemaining = !m_startLevelBundles.isEmpty();
                }
            }
            finally
            {
                releaseGlobalLock();
            }

            // Determine if we are lowering or raising the
            // active start level.
            boolean isLowering = (m_targetStartLevel < m_activeStartLevel);
            // Determine the range of start levels to process.
            int low = (isLowering) ? m_targetStartLevel + 1 : m_activeStartLevel + 1;
            int high = (isLowering) ? m_activeStartLevel : m_targetStartLevel;
            m_activeStartLevel = (isLowering) ? high : low;

            // Process bundles and stop or start them accordingly.
            while (bundlesRemaining)
            {
                StartLevelTuple tuple;

                // Remove our tuple to be processed while holding the queue lock
                // and update the active start level accordingly, which allows
                // us to determine in startBundle() if concurrent requests to
                // start a bundle should be handled synchronously or just added
                // to the queue and handled asynchronously.
                synchronized (m_startLevelBundles)
                {
                    if (isLowering)
                    {
                        tuple = m_startLevelBundles.last();
                    }
                    else
                    {
                        tuple = m_startLevelBundles.first();
                    }

                    if ((tuple.m_level >= low) && (tuple.m_level <= high))
                    {
                        m_activeStartLevel = tuple.m_level;
                    }
                }

                // Ignore the system bundle, since its start() and
                // stop() methods get called explicitly in Felix.start()
                // and Felix.stop(), respectively.
                if (tuple.m_bundle.getBundleId() != 0)
                {
                    // Lock the current bundle.
                    try
                    {
                        acquireBundleLock(tuple.m_bundle,
                            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE
                            | Bundle.STARTING | Bundle.STOPPING);
                    }
                    catch (IllegalStateException ex)
                    {
                        // Ignore if the bundle has been uninstalled.
                        if (tuple.m_bundle.getState() != Bundle.UNINSTALLED)
                        {
                            fireFrameworkEvent(FrameworkEvent.ERROR, tuple.m_bundle, ex);
                            m_logger.log(tuple.m_bundle,
                                Logger.LOG_ERROR,
                                "Error locking " + tuple.m_bundle._getLocation(), ex);
                        }
                        else
                        {
                            synchronized (m_startLevelBundles)
                            {
                                m_startLevelBundles.remove(tuple);
                                bundlesRemaining = !m_startLevelBundles.isEmpty();
                            }
                        }
                        continue;
                    }

                    try
                    {
                        // Start the bundle if necessary.
                        // Note that we only attempt to start the bundle if
                        // its start level is equal to the active start level,
                        // which means we assume lower bundles are in the state
                        // they should be in (i.e., we won't attempt to restart
                        // them if they previously failed to start).
                        if (!isLowering
                            && (((tuple.m_bundle.getPersistentState() == Bundle.ACTIVE)
                                || (tuple.m_bundle.getPersistentState() == Bundle.STARTING))
                                && (tuple.m_level == m_activeStartLevel)))
                        {
                            try
                            {
// TODO: LAZY - Not sure if this is the best way...
                                int options = Bundle.START_TRANSIENT;
                                options = (tuple.m_bundle.getPersistentState() == Bundle.STARTING)
                                    ? options | Bundle.START_ACTIVATION_POLICY
                                    : options;
                                startBundle(tuple.m_bundle, options);
                            }
                            catch (Throwable th)
                            {
                                fireFrameworkEvent(FrameworkEvent.ERROR, tuple.m_bundle, th);
                                m_logger.log(tuple.m_bundle,
                                    Logger.LOG_ERROR,
                                    "Error starting " + tuple.m_bundle._getLocation(), th);
                            }
                        }
                        // Stop the bundle if necessary.
                        else if (isLowering
                            && (((tuple.m_bundle.getState() == Bundle.ACTIVE)
                                || (tuple.m_bundle.getState() == Bundle.STARTING))
                                && (tuple.m_level == m_activeStartLevel)))
                        {
                            try
                            {
                                stopBundle(tuple.m_bundle, false);
                            }
                            catch (Throwable th)
                            {
                                fireFrameworkEvent(FrameworkEvent.ERROR, tuple.m_bundle, th);
                                m_logger.log(tuple.m_bundle,
                                    Logger.LOG_ERROR,
                                    "Error stopping " + tuple.m_bundle._getLocation(), th);
                            }
                        }
                    }
                    finally
                    {
                        // Always release bundle lock.
                        releaseBundleLock(tuple.m_bundle);
                    }
                }

                synchronized (m_startLevelBundles)
                {
                    m_startLevelBundles.remove(tuple);
                    bundlesRemaining = !m_startLevelBundles.isEmpty();
                }
            }

            m_activeStartLevel = m_targetStartLevel;
        }

        if (getState() == Bundle.ACTIVE)
        {
            fireFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, this, null);

            if (listeners != null)
            {
                FrameworkEvent event = new FrameworkEvent(
                    FrameworkEvent.STARTLEVEL_CHANGED, this, null);
                for (FrameworkListener l : listeners)
                {
                    try
                    {
                        l.frameworkEvent(event);
                    }
                    catch (Throwable th)
                    {
                        m_logger.log(Logger.LOG_ERROR,
                            "Framework listener delivery error.", th);
                    }
                }
            }
        }
    }

    /**
     * Returns the start level into which newly installed bundles will
     * be placed by default; this method implements functionality for
     * the Start Level service.
     * @return The default start level for newly installed bundles.
    **/
    int getInitialBundleStartLevel()
    {
        String s = (String) m_configMap.get(FelixConstants.BUNDLE_STARTLEVEL_PROP);

        if (s != null)
        {
            try
            {
                int i = Integer.parseInt(s);
                return (i > 0) ? i : FelixConstants.BUNDLE_DEFAULT_STARTLEVEL;
            }
            catch (NumberFormatException ex)
            {
                // Ignore and return the default value.
            }
        }
        return FelixConstants.BUNDLE_DEFAULT_STARTLEVEL;
    }

    /**
     * Sets the default start level into which newly installed bundles
     * will be placed; this method implements functionality for the Start
     * Level service.
     * @param startLevel The new default start level for newly installed
     *        bundles.
     * @throws java.lang.IllegalArgumentException If the specified start
     *         level is not greater than zero.
     * @throws java.security.SecurityException If the caller does not
     *         have <tt>AdminPermission</tt>.
    **/
    void setInitialBundleStartLevel(int startLevel)
    {
        if (startLevel <= 0)
        {
            throw new IllegalArgumentException(
                "Initial start level must be greater than zero.");
        }

        m_configMutableMap.put(
            FelixConstants.BUNDLE_STARTLEVEL_PROP, Integer.toString(startLevel));
    }

    /**
     * Returns the start level for the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle The bundle to examine.
     * @return The start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    int getBundleStartLevel(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((BundleImpl) bundle).getStartLevel(getInitialBundleStartLevel());
    }

    /**
     * Sets the start level of the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle The bundle whose start level is to be modified.
     * @param startLevel The new start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle is the system bundle or if the bundle has been
     *          uninstalled.
     * @throws java.security.SecurityException If the caller does not
     *          have <tt>AdminPermission</tt>.
    **/
    void setBundleStartLevel(Bundle bundle, int startLevel)
    {
        // Acquire bundle lock.
        BundleImpl impl = (BundleImpl) bundle;
        try
        {
            acquireBundleLock(impl,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE
                | Bundle.STARTING | Bundle.STOPPING);
        }
        catch (IllegalStateException ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, impl, ex);
            m_logger.log(impl,
                Logger.LOG_ERROR,
                "Error locking " + impl._getLocation(), ex);
            return;
        }

        Throwable rethrow = null;

        try
        {
            if (startLevel >= 1)
            {
                // NOTE: The start level was persistently recorded inside
                // the start level impl because the spec requires it to be
                // done synchronously.

                try
                {
                    // Start the bundle if necessary.
                    // We don't need to be concerned about the target
                    // start level here, since this method is only executed
                    // by the start level thread, so no start level change
                    // can be occurring concurrently.
                    if (((impl.getPersistentState() == Bundle.ACTIVE)
                        || (impl.getPersistentState() == Bundle.STARTING))
                        && (startLevel <= m_activeStartLevel))
                    {
// TODO: LAZY - Not sure if this is the best way...
                        int options = Bundle.START_TRANSIENT;
                        options = (impl.getPersistentState() == Bundle.STARTING)
                            ? options | Bundle.START_ACTIVATION_POLICY
                            : options;
                        startBundle(impl, options);
                    }
                    // Stop the bundle if necessary.
                    // We don't need to be concerned about the target
                    // start level here, since this method is only executed
                    // by the start level thread, so no start level change
                    // can be occurring concurrently.
                    else if (((impl.getState() == Bundle.ACTIVE)
                        || (impl.getState() == Bundle.STARTING))
                        && (startLevel > m_activeStartLevel))
                    {
                        stopBundle(impl, false);
                    }
                }
                catch (Throwable th)
                {
                    rethrow = th;
                    m_logger.log(
                        impl,
                        Logger.LOG_ERROR, "Error starting/stopping bundle.", th);
                }
            }
            else
            {
                m_logger.log(
                    impl,
                    Logger.LOG_WARNING, "Bundle start level must be greater than zero.");
            }
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(impl);
        }

        if (rethrow != null)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, rethrow);
        }
    }

    /**
     * Returns whether a bundle is persistently started; this is an
     * method implementation for the Start Level service.
     * @param bundle The bundle to examine.
     * @return <tt>true</tt> if the bundle is marked as persistently
     *          started, <tt>false</tt> otherwise.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return (((BundleImpl) bundle).getPersistentState() == Bundle.ACTIVE)
            || (((BundleImpl) bundle).getPersistentState() == Bundle.STARTING);
    }

    /**
     * Returns whether the bundle is using its declared activation policy;
     * this is an method implementation for the Start Level service.
     * @param bundle The bundle to examine.
     * @return <tt>true</tt> if the bundle is using its declared activation
     *         policy, <tt>false</tt> otherwise.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    boolean isBundleActivationPolicyUsed(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((BundleImpl) bundle).isDeclaredActivationPolicyUsed();
    }

    //
    // Implementation of Bundle interface methods.
    //

    /**
     * Get bundle headers and resolve any localized strings from resource bundles.
     * @param bundle
     * @param locale
     * @return localized bundle headers dictionary.
    **/
    Dictionary getBundleHeaders(BundleImpl bundle, String locale)
    {
        return new MapToDictionary(bundle.getCurrentLocalizedHeader(locale));
    }

    /**
     * Implementation for Bundle.getResource().
    **/
    URL getBundleResource(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (Util.isFragment(bundle.adapt(BundleRevision.class)))
        {
            return null;
        }
        else if (bundle.getState() == Bundle.INSTALLED)
        {
            try
            {
                resolveBundleRevision(bundle.adapt(BundleRevision.class));
            }
            catch (Exception ex)
            {
                // Ignore.
            }
        }

        // If the bundle revision isn't resolved, then just search
        // locally, otherwise delegate.
        if (bundle.adapt(BundleRevision.class).getWiring() == null)
        {
            return ((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                .getResourceLocal(name);
        }
        else
        {
            return ((BundleWiringImpl) bundle.adapt(BundleRevision.class).getWiring())
                .getResourceByDelegation(name);
        }
    }

    /**
     * Implementation for Bundle.getResources().
    **/
    Enumeration getBundleResources(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (Util.isFragment(bundle.adapt(BundleRevision.class)))
        {
            return null;
        }
        else if (bundle.getState() == Bundle.INSTALLED)
        {
            try
            {
                resolveBundleRevision(bundle.adapt(BundleRevision.class));
            }
            catch (Exception ex)
            {
                // Ignore.
            }
        }

        if (bundle.adapt(BundleRevision.class).getWiring() == null)
        {
            return ((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                .getResourcesLocal(name);
        }
        else
        {
            return ((BundleWiringImpl) bundle.adapt(BundleRevision.class).getWiring())
                .getResourcesByDelegation(name);
        }
    }

    /**
     * Implementation for Bundle.getEntry().
    **/
    URL getBundleEntry(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        URL url = ((BundleRevisionImpl) bundle.adapt(BundleRevision.class)).getEntry(name);

        // Some JAR files do not contain directory entries, so if
        // the entry wasn't found and is a directory, scan the entries
        // to see if we should synthesize an entry for it.
        if ((url == null) && name.endsWith("/") && !name.equals("/"))
        {
            // Use the entry filter enumeration to search the bundle content
            // recursively for matching entries and return URLs to them.
            Enumeration enumeration =
                new EntryFilterEnumeration(
                    bundle.adapt(BundleRevision.class), false, name, "*", true, true);
            // If the enumeration has elements, then that means we need
            // to synthesize the directory entry.
            if (enumeration.hasMoreElements())
            {
                URL entryURL = (URL) enumeration.nextElement();
                try
                {
                    url = new URL(entryURL, ((name.charAt(0) == '/') ? name : "/" + name));
                }
                catch (MalformedURLException ex)
                {
                    url = null;
                }
            }
        }
        return url;
    }

    /**
     * Implementation for Bundle.getEntryPaths().
    **/
    Enumeration getBundleEntryPaths(BundleImpl bundle, String path)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Get the entry enumeration from the revision content and
        // create a wrapper enumeration to filter it.
        Enumeration enumeration =
            new EntryFilterEnumeration(
                bundle.adapt(BundleRevision.class), false, path, "*", false, false);

        // Return the enumeration if it has elements.
        return (!enumeration.hasMoreElements()) ? null : enumeration;
    }

    /**
     * Implementation for Bundle.findEntries().
    **/
    Enumeration findBundleEntries(
            BundleImpl bundle, String path, String filePattern, boolean recurse)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (!Util.isFragment(bundle.adapt(BundleRevision.class)) && bundle.getState() == Bundle.INSTALLED)
        {
            try
            {
                resolveBundleRevision(bundle.adapt(BundleRevision.class));
            }
            catch (Exception ex)
            {
                // Ignore.
            }
        }
        return findBundleEntries(
                bundle.adapt(BundleRevision.class), path, filePattern, recurse);
    }

    /**
     * Implementation for BundleWiring.findEntries().
     **/
    Enumeration findBundleEntries(
        BundleRevision revision, String path, String filePattern, boolean recurse)
    {
        // Get the entry enumeration from the revision content and
        // create a wrapper enumeration to filter it.
        Enumeration enumeration =
            new EntryFilterEnumeration(revision, true, path, filePattern, recurse, true);

        // Return the enumeration if it has elements.
        return (!enumeration.hasMoreElements()) ? null : enumeration;
    }

    ServiceReference[] getBundleRegisteredServices(BundleImpl bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Filter list of registered service references.
        ServiceReference[] refs = m_registry.getRegisteredServices(bundle);

        return refs;
    }

    ServiceReference[] getBundleServicesInUse(Bundle bundle)
    {
        // Filter list of "in use" service references.
        ServiceReference[] refs = m_registry.getServicesInUse(bundle);

        return refs;
    }

    boolean bundleHasPermission(BundleImpl bundle, Object obj)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        if (System.getSecurityManager() != null)
        {
            try
            {
                return (obj instanceof java.security.Permission)
                    ? impliesBundlePermission(
                    (BundleProtectionDomain)
                    bundle.getProtectionDomain(),
                    (java.security.Permission) obj, true)
                    : false;
            }
            catch (Exception ex)
            {
                m_logger.log(
                    bundle,
                    Logger.LOG_WARNING,
                    "Exception while evaluating the permission.",
                    ex);
                return false;
            }
        }

        return true;
    }

    /**
     * Implementation for Bundle.loadClass().
    **/
    Class loadBundleClass(BundleImpl bundle, String name) throws ClassNotFoundException
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("Bundle is uninstalled");
        }
        else if (Util.isFragment(bundle.adapt(BundleRevision.class)))
        {
            throw new ClassNotFoundException("Fragments cannot load classes.");
        }
        else if (bundle.getState() == Bundle.INSTALLED)
        {
            try
            {
                resolveBundleRevision(bundle.adapt(BundleRevision.class));
            }
            catch (BundleException ex)
            {
                // The spec says we must fire a framework error.
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                // Then throw a class not found exception.
                throw new ClassNotFoundException(name, ex);
            }
        }

        // We do not call getClassLoader().loadClass() for arrays because
        // it does not correctly handle array types, which is necessary in
        // cases like deserialization using a wrapper class loader.
        if ((name != null) && (name.length() > 0) && (name.charAt(0) == '['))
        {
            return Class.forName(name, false,
                ((BundleWiringImpl) bundle.adapt(BundleWiring.class)).getClassLoader());
        }

        return ((BundleWiringImpl)
            bundle.adapt(BundleWiring.class)).getClassLoader().loadClass(name);
    }

    /**
     * Implementation for Bundle.start().
    **/
    void startBundle(BundleImpl bundle, int options) throws BundleException
    {
        // CONCURRENCY NOTE:
        // We will first acquire the bundle lock for the specific bundle
        // as long as the bundle is INSTALLED, RESOLVED, or ACTIVE. If this
        // bundle is not yet resolved, then it will be resolved too. In
        // that case, the global lock will be acquired to make sure no
        // bundles can be installed or uninstalled during the resolve.

        int eventType;
        boolean isTransient = (options & Bundle.START_TRANSIENT) != 0;

        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot start an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be started, since it is either starting or stopping.");
            }
        }

        // Record whether the bundle is using its declared activation policy.
        boolean wasDeferred = bundle.isDeclaredActivationPolicyUsed()
            && (((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                .getDeclaredActivationPolicy() == BundleRevisionImpl.LAZY_ACTIVATION);
        bundle.setDeclaredActivationPolicyUsed(
            (options & Bundle.START_ACTIVATION_POLICY) != 0);

        BundleException rethrow = null;
        try
        {
            // The spec doesn't say whether it is possible to start an extension
            // We just do nothing
            if (bundle.isExtension())
            {
                return;
            }

            // As per the OSGi spec, fragment bundles can not be started and must
            // throw a BundleException when there is an attempt to start one.
            if (Util.isFragment(bundle.adapt(BundleRevision.class)))
            {
                throw new BundleException("Fragment bundles can not be started.");
            }

            // Set and save the bundle's persistent state to active
            // if we are supposed to record state change.
            if (!isTransient)
            {
                if ((options & Bundle.START_ACTIVATION_POLICY) != 0)
                {
                    bundle.setPersistentStateStarting();
                }
                else
                {
                    bundle.setPersistentStateActive();
                }
            }

            // Check to see if the bundle's start level is greater than the
            // the framework's target start level and if so just return. For
            // transient starts we compare their start level to the current
            // active start level, since we never process transient starts
            // asynchronously (i.e., they are either satisfied and process
            // synchronously here or they throw an exception).
            int bundleLevel = bundle.getStartLevel(getInitialBundleStartLevel());
            if (isTransient && (bundleLevel > m_activeStartLevel))
            {
                // Throw an exception for transient starts.
                throw new BundleException(
                    "Cannot start bundle " + bundle + " because its start level is "
                    + bundleLevel
                    + ", which is greater than the framework's start level of "
                    + m_activeStartLevel + ".", BundleException.START_TRANSIENT_ERROR);
            }
            else if (bundleLevel > m_targetStartLevel)
            {
                // Ignore persistent starts that are not satisfied.
                return;
            }

            // Check to see if there is a start level change in progress and if
            // so queue this bundle to the start level bundle queue for the start
            // level thread and return, except for transient starts which are
            // queued but processed synchronously.
            // Note: Don't queue starts from the start level thread, otherwise
            // we'd never get anything started.
            if (!Thread.currentThread().getName().equals(FrameworkStartLevelImpl.THREAD_NAME))
            {
                synchronized (m_startLevelBundles)
                {
                    // Since we have the start level queue lock, we know the
                    // active start level cannot change. For transient starts
                    // we now need to double check and make sure we can still
                    // start them since technically the active start level could
                    // have changed.
                    if (isTransient && (bundleLevel > m_activeStartLevel))
                    {
                        throw new BundleException(
                            "Cannot start bundle " + bundle + " because its start level is "
                            + bundleLevel
                            + ", which is greater than the framework's start level of "
                            + m_activeStartLevel + ".", BundleException.START_TRANSIENT_ERROR);
                    }

                    // If the start level bundle queue is not empty, then we know
                    // there is a start level operation ongoing. We know that the
                    // bundle being started is satisfied by the target start level
                    // otherwise we wouldn't be here. In most cases we simply want
                    // to queue the bundle; however, if the bundle level is lower
                    // than the current active start level, then we want to handle
                    // it synchronously. This is because the start level thread
                    // ignores bundles that it should have already processed.
                    // So queue the bundle if its bundle start level is greater
                    // or equal to the active start level and then simply return
                    // since it will be processed asynchronously.
                    if (!m_startLevelBundles.isEmpty()
                        && (bundleLevel >= m_activeStartLevel))
                    {
                        // Only add the bundle to the start level bundles
                        // being process if it is not already there.
                        boolean found = false;
                        for (StartLevelTuple tuple : m_startLevelBundles)
                        {
                            if (tuple.m_bundle == bundle)
                            {
                                found = true;
                            }
                        }

                        if (!found)
                        {
                            m_startLevelBundles.add(new StartLevelTuple(bundle, bundleLevel));
                        }

                        // Note that although we queued the transiently started
                        // bundle, we don't return here because we handle all
                        // transient bundles synchronously. The reason why we
                        // queue it anyway is for the case where the start level
                        // is lowering, since the transiently started bundle may
                        // have already been processed by the start level thread
                        // and we will start it briefly here synchronously, but
                        // we want the start level thread to process it again
                        // so it gets another chance to stop it. This is not an
                        // issue when the start level is raising because the
                        // start level thread ignores non-persistently started
                        // bundles or if it is also persistently started it will
                        // be a no-op.
                        if (!isTransient)
                        {
                            return;
                        }
                    }
                }
            }

            switch (bundle.getState())
            {
                case Bundle.UNINSTALLED:
                    throw new IllegalStateException("Cannot start an uninstalled bundle.");
                case Bundle.STARTING:
                    if (!wasDeferred)
                    {
                        throw new BundleException(
                            "Bundle " + bundle
                            + " cannot be started, since it is starting.");
                    }
                    break;
                case Bundle.STOPPING:
                    throw new BundleException(
                        "Bundle " + bundle
                        + " cannot be started, since it is stopping.");
                case Bundle.ACTIVE:
                    return;
                case Bundle.INSTALLED:
                    resolveBundleRevision(bundle.adapt(BundleRevision.class));
                    // No break.
                case Bundle.RESOLVED:
                    // Set the bundle's context.
                    bundle.setBundleContext(new BundleContextImpl(m_logger, this, bundle));
                    // At this point, no matter if the bundle's activation policy is
                    // eager or deferred, we need to set the bundle's state to STARTING.
                    // We don't fire a BundleEvent here for this state change, since
                    // STARTING events are only fired if we are invoking the activator,
                    // which we may not do if activation is deferred.
                    setBundleStateAndNotify(bundle, Bundle.STARTING);
                    break;
            }

            // If the bundle's activation policy is eager or activation has already
            // been triggered, then activate the bundle immediately.
            if (!bundle.isDeclaredActivationPolicyUsed()
                || (((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                    .getDeclaredActivationPolicy() != BundleRevisionImpl.LAZY_ACTIVATION)
                || ((BundleClassLoader) bundle.adapt(BundleWiring.class).getClassLoader())
                    .isActivationTriggered())
            {
                // Record the event type for the final event and activate.
                eventType = BundleEvent.STARTED;
                // Note that the STARTING event is thrown in the activateBundle() method.
                try
                {
                    activateBundle(bundle, false);
                }
                catch (BundleException ex)
                {
                    rethrow = ex;
                }
            }
            // Otherwise, defer bundle activation.
            else
            {
                // Record the event type for the final event.
                eventType = BundleEvent.LAZY_ACTIVATION;
            }

            // We still need to fire the STARTED event, but we will do
            // it later so we can release the bundle lock.
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STARTED
        // or LAZY_ACTIVATION event here without holding the lock. Otherwise,
        // fire STOPPED and rethrow exception.
        if (rethrow == null)
        {
            fireBundleEvent(eventType, bundle);
        }
        else
        {
            fireBundleEvent(BundleEvent.STOPPED, bundle);
            throw rethrow;
        }
    }

    void activateBundle(BundleImpl bundle, boolean fireEvent) throws BundleException
    {
        // CONCURRENCY NOTE:
        // We will first acquire the bundle lock for the specific bundle
        // as long as the bundle is STARTING or ACTIVE, which is necessary
        // because we may change the bundle state.

        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.STARTING | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            throw new IllegalStateException(
                "Activation only occurs for bundles in STARTING state.");
        }

        try
        {
            // If the bundle is already active or its start level is not met,
            // simply return. Generally, the bundle start level should always
            // be less than or equal to the active start level since the bundle
            // must be in the STARTING state to activate it. One potential corner
            // case is if the bundle is being lazily activated at the same time
            // there is a start level change going on to lower the start level.
            // In that case, we test here and avoid activating the bundle since
            // it will be stopped by the start level thread.
            if ((bundle.getState() == Bundle.ACTIVE) ||
                (bundle.getStartLevel(getInitialBundleStartLevel()) > m_targetStartLevel))
            {
                return;
            }

            // Fire STARTING event to signify call to bundle activator.
            fireBundleEvent(BundleEvent.STARTING, bundle);

            try
            {
                // Set the bundle's activator.
                bundle.setActivator(createBundleActivator(bundle));

                // Activate the bundle if it has an activator.
                if (bundle.getActivator() != null)
                {
                    m_secureAction.startActivator(
                        bundle.getActivator(), bundle._getBundleContext());
                }

                setBundleStateAndNotify(bundle, Bundle.ACTIVE);

                // We still need to fire the STARTED event, but we will do
                // it later so we can release the bundle lock.
            }
            catch (Throwable th)
            {
                // Spec says we must fire STOPPING event.
                fireBundleEvent(BundleEvent.STOPPING, bundle);

                // If there was an error starting the bundle,
                // then reset its state to RESOLVED.
                setBundleStateAndNotify(bundle, Bundle.RESOLVED);

                // Clean up the bundle activator
                bundle.setActivator(null);

                // Clean up the bundle context.
                // We invalidate this first to make sure it cannot be used
                // after stopping the activator.
                BundleContextImpl bci = (BundleContextImpl) bundle._getBundleContext();
                bci.invalidate();
                bundle.setBundleContext(null);

                // Unregister any services offered by this bundle.
                m_registry.unregisterServices(bundle);

                // Release any services being used by this bundle.
                m_registry.ungetServices(bundle);

                // Remove any listeners registered by this bundle.
                m_dispatcher.removeListeners(bci);

                // The spec says to expect BundleException or
                // SecurityException, so rethrow these exceptions.
                if (th instanceof BundleException)
                {
                    throw (BundleException) th;
                }
                else if ((System.getSecurityManager() != null) &&
                    (th instanceof java.security.PrivilegedActionException))
                {
                    th = ((java.security.PrivilegedActionException) th).getException();
                }

                // Rethrow all other exceptions as a BundleException.
                throw new BundleException(
                    "Activator start error in bundle " + bundle + ".",
                    BundleException.ACTIVATOR_ERROR, th);
            }
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STARTED
        // event here without holding the lock if specified.
        // TODO: LAZY - It would be nice to figure out how to do this without
        //       duplicating code; this method is called from two different
        //       places -- one fires the event itself the other one needs it.
        if (fireEvent)
        {
            fireBundleEvent(BundleEvent.STARTED, bundle);
        }
    }

    void updateBundle(BundleImpl bundle, InputStream is)
        throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot update an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be update, since it is either starting or stopping.");
            }
        }

        // We must release the lock and close the input stream, so do both
        // in a finally block.
        try
        {
            // Variable to indicate whether bundle is active or not.
            Throwable rethrow = null;

            final int oldState = bundle.getState();

            // First get the update-URL from our header.
            String updateLocation = (String)
                ((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                    .getHeaders().get(Constants.BUNDLE_UPDATELOCATION);

            // If no update location specified, use original location.
            if (updateLocation == null)
            {
                updateLocation = bundle._getLocation();
            }

            // Stop the bundle if it is active, but do not change its
            // persistent state.
            if (oldState == Bundle.ACTIVE)
            {
                stopBundle(bundle, false);
            }

            try
            {
                // Revising the bundle creates a new revision, which modifies
                // the global state, so we need to acquire the global lock
                // before revising.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    throw new BundleException(
                        "Cannot acquire global lock to update the bundle.");
                }
                try
                {
                    // Try to revise.
                    boolean wasExtension = bundle.isExtension();
                    bundle.revise(updateLocation, is);

                    // Verify bundle revision.
                    try
                    {
                        Object sm = System.getSecurityManager();

                        if (sm != null)
                        {
                            ((SecurityManager) sm).checkPermission(
                                new AdminPermission(bundle, AdminPermission.LIFECYCLE));
                        }

                        // If this is an update from a normal to an extension bundle
                        // then attach the extension
                        if (!wasExtension && bundle.isExtension())
                        {
                            m_extensionManager.addExtensionBundle(this, bundle);
// TODO: REFACTOR - Perhaps we could move this into extension manager.
                            m_resolver.addRevision(m_extensionManager.getRevision());
// TODO: REFACTOR - Not clear why this is here. We should look at all of these steps more closely.
                            setBundleStateAndNotify(bundle, Bundle.RESOLVED);
                        }
                        else if (wasExtension)
                        {
                            setBundleStateAndNotify(bundle, Bundle.INSTALLED);
                        }
                    }
                    catch (Throwable ex)
                    {
                        try
                        {
                            bundle.rollbackRevise();
                        }
                        catch (Exception busted)
                        {
                            m_logger.log(
                                bundle, Logger.LOG_ERROR, "Unable to rollback.", busted);
                        }

                        throw ex;
                    }
                }
                finally
                {
                    // Always release the global lock.
                    releaseGlobalLock();
                }
            }
            catch (Throwable ex)
            {
                m_logger.log(
                    bundle, Logger.LOG_ERROR, "Unable to update the bundle.", ex);
                rethrow = ex;
            }

            // Set new state, mark as needing a refresh, and fire updated event
            // if successful.
            if (rethrow == null)
            {
                bundle.setLastModified(System.currentTimeMillis());

                if (!bundle.isExtension())
                {
                    setBundleStateAndNotify(bundle, Bundle.INSTALLED);
                }
                else
                {
                    m_extensionManager.startExtensionBundle(this, bundle);
                }

                fireBundleEvent(BundleEvent.UNRESOLVED, bundle);

                fireBundleEvent(BundleEvent.UPDATED, bundle);

                // Acquire global lock to check if we should auto-refresh.
                boolean locked = acquireGlobalLock();
                // If we did not get the global lock, then do not try to
                // auto-refresh.
                if (locked)
                {
                    try
                    {
                        if (!m_dependencies.hasDependents(bundle)
                            && !bundle.isExtension())
                        {
                            try
                            {
                                List<Bundle> list = new ArrayList<Bundle>(1);
                                list.add(bundle);
                                refreshPackages(list, null);
                            }
                            catch (Exception ex)
                            {
                                m_logger.log(bundle,
                                    Logger.LOG_ERROR,
                                    "Unable to immediately purge the bundle revisions.", ex);
                            }
                        }
                    }
                    finally
                    {
                        // Always release the global lock.
                        releaseGlobalLock();
                    }
                }
            }

            // If the old state was active, but the new revision is a fragment,
            // then mark the persistent state to inactive.
            if ((oldState == Bundle.ACTIVE)
                && Util.isFragment(bundle.adapt(BundleRevision.class)))
            {
                bundle.setPersistentStateInactive();
                m_logger.log(bundle, Logger.LOG_WARNING,
                    "Previously active bundle was updated to a fragment, resetting state to inactive: "
                    + bundle);
            }
            // Otherwise, restart the bundle if it was previously active,
            // but do not change its persistent state.
            else if (oldState == Bundle.ACTIVE)
            {
                startBundle(bundle, Bundle.START_TRANSIENT);
            }

            // If update failed, rethrow exception.
            if (rethrow != null)
            {
                if (rethrow instanceof AccessControlException)
                {
                    throw (AccessControlException) rethrow;
                }
                else if (rethrow instanceof BundleException)
                {
                    throw (BundleException) rethrow;
                }
                else
                {
                    throw new BundleException(
                        "Update of bundle " + bundle + " failed.", rethrow);
                }
            }
        }
        finally
        {
            // Close the input stream.
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
                m_logger.log(bundle, Logger.LOG_ERROR, "Unable to close input stream.", ex);
            }

            // Release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    void stopBundle(BundleImpl bundle, boolean record)
        throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot stop an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be stopped since it is already stopping.");
            }
        }

        try
        {
            Throwable rethrow = null;

            // Set the bundle's persistent state to inactive if necessary.
            if (record)
            {
                bundle.setPersistentStateInactive();
            }

            // If the bundle is not persistently started, then we
            // need to reset the activation policy flag, since it
            // does not persist across persistent stops or transient
            // stops.
            if (!isBundlePersistentlyStarted(bundle))
            {
                bundle.setDeclaredActivationPolicyUsed(false);
            }

            // As per the OSGi spec, fragment bundles can not be stopped and must
            // throw a BundleException when there is an attempt to stop one.
            if (Util.isFragment(bundle.adapt(BundleRevision.class)))
            {
                throw new BundleException("Fragment bundles can not be stopped: " + bundle);
            }

            boolean wasActive = false;
            switch (bundle.getState())
            {
                case Bundle.UNINSTALLED:
                    throw new IllegalStateException("Cannot stop an uninstalled bundle.");
                case Bundle.STARTING:
                    if (bundle.isDeclaredActivationPolicyUsed()
                        && ((BundleRevisionImpl) bundle.adapt(BundleRevision.class))
                            .getDeclaredActivationPolicy() != BundleRevisionImpl.LAZY_ACTIVATION)
                    {
                        throw new BundleException(
                            "Stopping a starting or stopping bundle is currently not supported.");
                    }
                    break;
                case Bundle.STOPPING:
                    throw new BundleException(
                        "Stopping a starting or stopping bundle is currently not supported.");
                case Bundle.INSTALLED:
                case Bundle.RESOLVED:
                    return;
                case Bundle.ACTIVE:
                    wasActive = true;
                    break;
            }

            // At this point, no matter if the bundle's activation policy is
            // eager or deferred, we need to set the bundle's state to STOPPING
            // and fire the STOPPING event.
            setBundleStateAndNotify(bundle, Bundle.STOPPING);
            fireBundleEvent(BundleEvent.STOPPING, bundle);

            // If the bundle was active, then invoke the activator stop() method
            // or if we are stopping the system bundle.
            if ((wasActive) || (bundle.getBundleId() == 0))
            {
                try
                {
                    if (bundle.getActivator() != null)
                    {
                        m_secureAction.stopActivator(bundle.getActivator(), bundle._getBundleContext());
                    }
                }
                catch (Throwable th)
                {
                    m_logger.log(bundle, Logger.LOG_ERROR, "Error stopping bundle.", th);
                    rethrow = th;
                }
            }

            // Do not clean up after the system bundle since it will
            // clean up after itself.
            if (bundle.getBundleId() != 0)
            {
                // Clean up the bundle activator.
                bundle.setActivator(null);

                // Clean up the bundle context.
                // We invalidate this first to make sure it cannot be used
                // after stopping the activator.
                BundleContextImpl bci = (BundleContextImpl) bundle._getBundleContext();
                bci.invalidate();
                bundle.setBundleContext(null);

                // Unregister any services offered by this bundle.
                m_registry.unregisterServices(bundle);

                // Release any services being used by this bundle.
                m_registry.ungetServices(bundle);

                // The spec says that we must remove all event
                // listeners for a bundle when it is stopped.
                m_dispatcher.removeListeners(bci);

                setBundleStateAndNotify(bundle, Bundle.RESOLVED);

                // We still need to fire the STOPPED event, but we will do
                // it later so we can release the bundle lock.
            }

            // Throw activator error if there was one.
            if (rethrow != null)
            {
                // The spec says to expect BundleException or
                // SecurityException, so rethrow these exceptions.
                if (rethrow instanceof BundleException)
                {
                    throw (BundleException) rethrow;
                }
                else if ((System.getSecurityManager() != null) &&
                    (rethrow instanceof java.security.PrivilegedActionException))
                {
                    rethrow = ((java.security.PrivilegedActionException) rethrow).getException();
                }

                // Rethrow all other exceptions as a BundleException.
                throw new BundleException(
                    "Activator stop error in bundle " + bundle + ".", rethrow);
            }
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STOPPED event
        // here without holding the lock.
        fireBundleEvent(BundleEvent.STOPPED, bundle);
    }

    void uninstallBundle(BundleImpl bundle) throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot uninstall an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be uninstalled since it is stopping.");
            }
        }

        try
        {
            // The spec says that uninstall should always succeed, so
            // catch an exception here if stop() doesn't succeed and
            // rethrow it at the end.
            if (!bundle.isExtension() && (bundle.getState() == Bundle.ACTIVE))
            {
                try
                {
                    stopBundle(bundle, true);
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                }
            }

            // Remove the bundle from the installed map.
            BundleImpl target = null;
            // Acquire global lock.
            boolean locked = acquireGlobalLock();
            if (!locked)
            {
                // If the calling thread holds bundle locks, then we might not
                // be able to get the global lock.
                throw new IllegalStateException(
                    "Unable to acquire global lock to remove bundle.");
            }
            try
            {
                // Use a copy-on-write approach to remove the bundle
                // from the installed maps.
                Map[] maps = new Map[] {
                    new HashMap<String, BundleImpl>(m_installedBundles[LOCATION_MAP_IDX]),
                    new TreeMap<Long, BundleImpl>(m_installedBundles[IDENTIFIER_MAP_IDX])
                };
                target = (BundleImpl) maps[LOCATION_MAP_IDX].remove(bundle._getLocation());
                if (target != null)
                {
                    maps[IDENTIFIER_MAP_IDX].remove(new Long(target.getBundleId()));
                    m_installedBundles = maps;

                    // Set the bundle's persistent state to uninstalled.
                    bundle.setPersistentStateUninstalled();

                    // Put bundle in uninstalled bundle array.
                    rememberUninstalledBundle(bundle);
                }
            }
            finally
            {
                releaseGlobalLock();
            }

            if (target == null)
            {
                m_logger.log(bundle,
                    Logger.LOG_ERROR, "Unable to remove bundle from installed map!");
            }

            setBundleStateAndNotify(bundle, Bundle.INSTALLED);

            // Unfortunately, fire UNRESOLVED event while holding the lock,
            // since we still need to change the bundle state.
            fireBundleEvent(BundleEvent.UNRESOLVED, bundle);

            // Set state to uninstalled.
            setBundleStateAndNotify(bundle, Bundle.UNINSTALLED);
            bundle.setLastModified(System.currentTimeMillis());
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }

        // Fire UNINSTALLED event without holding the lock.
        fireBundleEvent(BundleEvent.UNINSTALLED, bundle);

        // Acquire global lock to check if we should auto-refresh.
        boolean locked = acquireGlobalLock();
        if (locked)
        {
            // Populate a set of refresh candidates. This also includes any bundles that this bundle
            // is wired to but have previously been uninstalled.
            List<Bundle> refreshCandidates = new ArrayList<Bundle>();
            refreshCandidates.add(bundle); // Add this bundle first, so that it gets refreshed first
            BundleRevisions bundleRevisions = bundle.adapt(BundleRevisions.class);
            if (bundleRevisions != null)
            {
                for (BundleRevision br : bundleRevisions.getRevisions())
                {
                    BundleWiring bw = br.getWiring();
                    if (bw != null)
                    {
                        for (BundleWire wire : bw.getRequiredWires(null))
                        {
                            Bundle b = wire.getProvider().getBundle();
                            if (Bundle.UNINSTALLED == b.getState() && !refreshCandidates.contains(b))
                                refreshCandidates.add(b);
                        }
                    }
                }
            }

            try
            {
                for (Bundle b : refreshCandidates)
                {
                    // If the bundle is not used by anyone, then garbage
                    // collect it now.
                    if (!m_dependencies.hasDependents(b))
                    {
                        try
                        {
                            List<Bundle> list = Collections.singletonList(b);
                            refreshPackages(list, null);
                        }
                        catch (Exception ex)
                        {
                            m_logger.log(b,
                                Logger.LOG_ERROR,
                                "Unable to immediately garbage collect the bundle.", ex);
                        }
                    }
                }
            }
            finally
            {
                // Always release the global lock.
                releaseGlobalLock();
            }
        }
    }

    //
    // Implementation of BundleContext interface methods.
    //

    /**
     * Implementation for BundleContext.getProperty(). Returns
     * environment property associated with the framework.
     *
     * @param key The name of the property to retrieve.
     * @return The value of the specified property or null.
    **/
    String getProperty(String key)
    {
        // First, check the config properties.
        String val = (String) m_configMap.get(key);
        // If not found, then try the system properties.
        return (val == null) ? System.getProperty(key) : val;
    }

    private Bundle reloadBundle(BundleArchive ba)
        throws BundleException
    {
        BundleImpl bundle = null;

        // Try to purge old revisions before installing;
        // this is done just in case a "refresh" didn't
        // occur last session...this would only be due to
        // an error or system crash.
        try
        {
            if (ba.isRemovalPending())
            {
                ba.purge();
            }
        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "Could not purge bundle.", ex);
        }

        try
        {
            // Acquire the global lock to create the bundle,
            // since this impacts the global state.
            boolean locked = acquireGlobalLock();
            if (!locked)
            {
                throw new BundleException(
                    "Unable to acquire the global lock to install the bundle.");
            }
            try
            {
                bundle = new BundleImpl(this, null, ba);

                // Extensions are handled as a special case.
                if (bundle.isExtension())
                {
                    m_extensionManager.addExtensionBundle(this, bundle);
                    m_resolver.addRevision(m_extensionManager.getRevision());
                }

                // Use a copy-on-write approach to add the bundle
                // to the installed maps.
                Map[] maps = new Map[] {
                    new HashMap<String, BundleImpl>(m_installedBundles[LOCATION_MAP_IDX]),
                    new TreeMap<Long, BundleImpl>(m_installedBundles[IDENTIFIER_MAP_IDX])
                };
                maps[LOCATION_MAP_IDX].put(bundle._getLocation(), bundle);
                maps[IDENTIFIER_MAP_IDX].put(new Long(bundle.getBundleId()), bundle);
                m_installedBundles = maps;
            }
            finally
            {
                // Always release the global lock.
                releaseGlobalLock();
            }
        }
        catch (Throwable ex)
        {
            if (ex instanceof BundleException)
            {
                throw (BundleException) ex;
            }
            else if (ex instanceof AccessControlException)
            {
                throw (AccessControlException) ex;
            }
            else
            {
                throw new BundleException("Could not create bundle object.", ex);
            }
        }

        if (bundle.isExtension())
        {
            m_extensionManager.startExtensionBundle(this, bundle);
        }

        return bundle;
    }

    Bundle installBundle(
        Bundle origin, String location, InputStream is)
        throws BundleException
    {
        BundleArchive ba = null;
        BundleImpl existing, bundle = null;

        // Acquire an install lock.
        acquireInstallLock(location);

        try
        {
            // Check to see if the framework is still running;
            if ((getState() == Bundle.STOPPING) ||
                (getState() == Bundle.UNINSTALLED))
            {
                throw new BundleException("The framework has been shutdown.");
            }

            // If bundle location is already installed, then
            // return it as required by the OSGi specification.
            existing = (BundleImpl) getBundle(location);
            if (existing == null)
            {
                // First generate an identifier for it.
                long id = getNextId();

                try
                {
                    // Add the bundle to the cache.
                    ba = m_cache.create(id, getInitialBundleStartLevel(), location, is);
                }
                catch (Exception ex)
                {
                    throw new BundleException(
                        "Unable to cache bundle: " + location, ex);
                }
                finally
                {
                    try
                    {
                        if (is != null) is.close();
                    }
                    catch (IOException ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to close input stream.", ex);
                    }
                }

                try
                {
                    // Acquire the global lock to create the bundle,
                    // since this impacts the global state.
                    boolean locked = acquireGlobalLock();
                    if (!locked)
                    {
                        throw new BundleException(
                            "Unable to acquire the global lock to install the bundle.");
                    }
                    try
                    {
                        bundle = new BundleImpl(this, origin, ba);
                    }
                    finally
                    {
                        // Always release the global lock.
                        releaseGlobalLock();
                    }

                    if (!bundle.isExtension())
                    {
                        Object sm = System.getSecurityManager();
                        if (sm != null)
                        {
                            ((SecurityManager) sm).checkPermission(
                                new AdminPermission(bundle, AdminPermission.LIFECYCLE));
                        }
                    }
                    else
                    {
                        m_extensionManager.addExtensionBundle(this, bundle);
                        m_resolver.addRevision(m_extensionManager.getRevision());
                    }
                }
                catch (Throwable ex)
                {
                    // Remove bundle from the cache.
                    try
                    {
                        if (bundle != null)
                        {
                            bundle.closeAndDelete();
                        }
                        else if (ba != null)
                        {
                            ba.closeAndDelete();
                        }
                    }
                    catch (Exception ex1)
                    {
                        m_logger.log(bundle,
                            Logger.LOG_ERROR,
                            "Could not remove from cache.", ex1);
                    }
                    if (ex instanceof BundleException)
                    {
                        throw (BundleException) ex;
                    }
                    else if (ex instanceof AccessControlException)
                    {
                        throw (AccessControlException) ex;
                    }
                    else
                    {
                        throw new BundleException("Could not create bundle object.", ex);
                    }
                }

                // Acquire global lock.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    // If the calling thread holds bundle locks, then we might not
                    // be able to get the global lock.
                    throw new IllegalStateException(
                        "Unable to acquire global lock to add bundle.");
                }
                try
                {
                    // Use a copy-on-write approach to add the bundle
                    // to the installed maps.
                    Map[] maps = new Map[] {
                        new HashMap<String, BundleImpl>(m_installedBundles[LOCATION_MAP_IDX]),
                        new TreeMap<Long, BundleImpl>(m_installedBundles[IDENTIFIER_MAP_IDX])
                    };
                    maps[LOCATION_MAP_IDX].put(location, bundle);
                    maps[IDENTIFIER_MAP_IDX].put(new Long(bundle.getBundleId()), bundle);
                    m_installedBundles = maps;
                }
                finally
                {
                    releaseGlobalLock();
                }

                if (bundle.isExtension())
                {
                    m_extensionManager.startExtensionBundle(this, bundle);
                }
            }
        }
        finally
        {
            // Always release install lock.
            releaseInstallLock(location);

            // Always try to close the input stream.
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                m_logger.log(bundle,
                    Logger.LOG_ERROR,
                    "Unable to close input stream.", ex);
                // Not much else we can do.
            }
        }

        if (existing != null)
        {
            Set<ServiceReference<org.osgi.framework.hooks.bundle.FindHook>> hooks =
                    getHookRegistry().getHooks(org.osgi.framework.hooks.bundle.FindHook.class);
            if (!hooks.isEmpty())
            {
                Collection<Bundle> bundles = new ArrayList<Bundle>(1);
                bundles.add(existing);
                bundles = new ShrinkableCollection<Bundle>(bundles);
                for (ServiceReference<org.osgi.framework.hooks.bundle.FindHook> hook : hooks)
                {
                    org.osgi.framework.hooks.bundle.FindHook fh = getService(this, hook, false);
                    if (fh != null)
                    {
                        try
                        {
                            m_secureAction.invokeBundleFindHook(
                                fh, origin.getBundleContext(), bundles);
                        }
                        catch (Throwable th)
                        {
                            m_logger.doLog(
                                hook.getBundle(),
                                hook,
                                Logger.LOG_WARNING,
                                "Problem invoking bundle hook.",
                                th);
                        }
                    }
                }

                if (origin != this)
                {
                    // If the origin was something else than the system bundle, reject this action if
                    // the bundle has been removed by the hooks. However, if it is the system bundle,
                    // the install action should always succeed, regardless of whether the hooks are
                    // trying to prevent it.
                    if (bundles.isEmpty())
                    {
                        throw new BundleException(
                            "Bundle installation rejected by hook.",
                            BundleException.REJECTED_BY_HOOK);
                    }
                }
            }
        }
        else
        {
            // Fire bundle event.
            fireBundleEvent(BundleEvent.INSTALLED, bundle, origin);
        }

        // Return new bundle.
        return (existing != null) ? existing : bundle;
    }

    /**
     * Retrieves a bundle from its location.
     *
     * @param location The location of the bundle to retrieve.
     * @return The bundle associated with the location or null if there
     *         is no bundle associated with the location.
    **/
    Bundle getBundle(String location)
    {
        return (Bundle) m_installedBundles[LOCATION_MAP_IDX].get(location);
    }

    /**
     * Implementation for BundleContext.getBundle(). Retrieves a
     * bundle from its identifier.
     *
     * @param id The identifier of the bundle to retrieve.
     * @return The bundle associated with the identifier or null if there
     *         is no bundle associated with the identifier.
    **/
    Bundle getBundle(BundleContext bc, long id)
    {
        BundleImpl bundle = (BundleImpl)
            m_installedBundles[IDENTIFIER_MAP_IDX].get(new Long(id));
        if (bundle != null)
        {
            List<BundleImpl> uninstalledBundles = m_uninstalledBundles;
            for (int i = 0;
                (bundle == null)
                    && (uninstalledBundles != null)
                    && (i < uninstalledBundles.size());
                i++)
            {
                if (uninstalledBundles.get(i).getBundleId() == id)
                {
                    bundle = uninstalledBundles.get(i);
                }
            }
        }

        Set<ServiceReference<org.osgi.framework.hooks.bundle.FindHook>> hooks =
                getHookRegistry().getHooks(org.osgi.framework.hooks.bundle.FindHook.class);
        if (!hooks.isEmpty() && (bundle != null))
        {
            Collection<Bundle> bundles = new ArrayList<Bundle>(1);
            bundles.add(bundle);
            bundles = new ShrinkableCollection<Bundle>(bundles);
            for (ServiceReference<org.osgi.framework.hooks.bundle.FindHook> hook : hooks)
            {
                org.osgi.framework.hooks.bundle.FindHook fh = getService(this, hook, false);
                if (fh != null)
                {
                    try
                    {
                        m_secureAction.invokeBundleFindHook(fh, bc, bundles);
                    }
                    catch (Throwable th)
                    {
                        m_logger.doLog(
                            hook.getBundle(),
                            hook,
                            Logger.LOG_WARNING,
                            "Problem invoking bundle hook.",
                            th);
                    }
                }
            }

            if (bc.getBundle() != this)
            {
                // If the requesting bundle is not the system bundle, return the filtered result
                bundle = (bundles.isEmpty()) ? null : bundle;
            }
        }
        return bundle;
    }

    /**
     * Retrieves a bundle by its identifier and avoids bundles hooks.
     *
     * @return The bundle associated with the identifier or null.
    **/
    Bundle getBundle(long id)
    {
        BundleImpl bundle = (BundleImpl)
            m_installedBundles[IDENTIFIER_MAP_IDX].get(new Long(id));
        if (bundle != null)
        {
            return bundle;
        }

        List<BundleImpl> uninstalledBundles = m_uninstalledBundles;
        for (int i = 0;
            (uninstalledBundles != null) && (i < uninstalledBundles.size());
            i++)
        {
            if (uninstalledBundles.get(i).getBundleId() == id)
            {
                return uninstalledBundles.get(i);
            }
        }

        return null;
    }

    /**
     * Implementation for BundleContext.getBundles(). Retrieves
     * all installed bundles.
     *
     * @return An array containing all installed bundles or an empty
     *        array if there are no installed bundles.
     **/
    Bundle[] getBundles(BundleContext bc)
    {
        Collection<Bundle> bundles = m_installedBundles[IDENTIFIER_MAP_IDX].values();
        if ( !bundles.isEmpty() )
        {
            Set<ServiceReference<org.osgi.framework.hooks.bundle.FindHook>> hooks =
                    getHookRegistry().getHooks(org.osgi.framework.hooks.bundle.FindHook.class);
            if (!hooks.isEmpty())
            {
                Collection<Bundle> shrunkBundles = new ShrinkableCollection<Bundle>(new ArrayList<Bundle>(bundles));
                for (ServiceReference<org.osgi.framework.hooks.bundle.FindHook> hook : hooks)
                {
                    org.osgi.framework.hooks.bundle.FindHook fh = getService(this, hook, false);
                    if (fh != null)
                    {
                        try
                        {
                            m_secureAction.invokeBundleFindHook(fh, bc, shrunkBundles);
                        }
                        catch (Throwable th)
                        {
                            m_logger.doLog(
                                hook.getBundle(),
                                hook,
                                Logger.LOG_WARNING,
                                "Problem invoking bundle hook.",
                                th);
                        }
                    }
                }
                if ( bc.getBundle() != this ) {
                    // If the requesting bundle is something other than the system bundle, return the shrunk
                    // collection of bundles. If it *is* the system bundle, it should receive the unfiltered bundles.
                    bundles = shrunkBundles;
                }
            }
        }

        return bundles.toArray(new Bundle[bundles.size()]);
    }

    /**
     * Retrieves all installed bundles and avoids bundles hooks.
     *
     * @return An array containing all installed bundles or null if
     *         there are no installed bundles.
    **/
    Bundle[] getBundles()
    {
        Collection<Bundle> bundles = m_installedBundles[IDENTIFIER_MAP_IDX].values();
        return bundles.toArray(new Bundle[bundles.size()]);
    }

    void addBundleListener(BundleImpl bundle, BundleListener l)
    {
        m_dispatcher.addListener(
            bundle._getBundleContext(), BundleListener.class, l, null);
    }

    void removeBundleListener(BundleImpl bundle, BundleListener l)
    {
        m_dispatcher.removeListener(
            bundle._getBundleContext(), BundleListener.class, l);
    }

    /**
     * Implementation for BundleContext.addServiceListener().
     * Adds service listener to the listener list so that is
     * can listen for <code>ServiceEvent</code>s.
     *
     * @param bundle The bundle that registered the listener.
     * @param l The service listener to add to the listener list.
     * @param f The filter for the listener; may be null.
    **/
    void addServiceListener(BundleImpl bundle, ServiceListener l, String f)
        throws InvalidSyntaxException
    {
        Filter oldFilter;
        Filter newFilter = (f == null) ? null : FrameworkUtil.createFilter(f);

        oldFilter = m_dispatcher.addListener(
            bundle._getBundleContext(), ServiceListener.class, l, newFilter);

        // Invoke ListenerHook.removed() if filter updated.
        Set<ServiceReference<org.osgi.framework.hooks.service.ListenerHook>> listenerHooks =
                getHookRegistry().getHooks(org.osgi.framework.hooks.service.ListenerHook.class);
        if (oldFilter != null)
        {
            final Collection removed = Collections.singleton(
                new ListenerInfo(bundle, bundle._getBundleContext(),
                    ServiceListener.class, l, oldFilter, null, true));
            for (ServiceReference<org.osgi.framework.hooks.service.ListenerHook> sr : listenerHooks)
            {
                org.osgi.framework.hooks.service.ListenerHook lh = getService(this, sr, false);
                if (lh != null)
                {
                    try
                    {
                        m_secureAction.invokeServiceListenerHookRemoved(lh, removed);
                    }
                    catch (Throwable th)
                    {
                        m_logger.log(sr, Logger.LOG_WARNING,
                            "Problem invoking service registry hook", th);
                    }
                    finally
                    {
                        m_registry.ungetService(this, sr, null);
                    }
                }
            }
        }

        // Invoke the ListenerHook.added() on all hooks.
        final Collection added = Collections.singleton(
            new ListenerInfo(bundle, bundle._getBundleContext(),
                ServiceListener.class, l, newFilter, null, false));
        for (ServiceReference<org.osgi.framework.hooks.service.ListenerHook> sr : listenerHooks)
        {
            org.osgi.framework.hooks.service.ListenerHook lh = getService(this, sr, false);
            if (lh != null)
            {
                try
                {
                    m_secureAction.invokeServiceListenerHookAdded(lh, added);
                }
                catch (Throwable th)
                {
                    m_logger.log(sr, Logger.LOG_WARNING,
                        "Problem invoking service registry hook", th);
                }
                finally
                {
                    m_registry.ungetService(this, sr, null);
                }
            }
        }
    }

    /**
     * Implementation for BundleContext.removeServiceListener().
     * Removes service listeners from the listener list.
     *
     * @param bundle The context bundle of the listener
     * @param l The service listener to remove from the listener list.
    **/
    void removeServiceListener(BundleImpl bundle, ServiceListener l)
    {
        org.osgi.framework.hooks.service.ListenerHook.ListenerInfo listener =
            m_dispatcher.removeListener(
                bundle._getBundleContext(), ServiceListener.class, l);

        if (listener != null)
        {
            // Invoke the ListenerHook.removed() on all hooks.
            Set<ServiceReference<org.osgi.framework.hooks.service.ListenerHook>> listenerHooks =
                    getHookRegistry().getHooks(org.osgi.framework.hooks.service.ListenerHook.class);
            Collection removed = Collections.singleton(listener);
            for (ServiceReference<org.osgi.framework.hooks.service.ListenerHook> sr : listenerHooks)
            {
                org.osgi.framework.hooks.service.ListenerHook lh = getService(this, sr, false);
                if (lh != null)
                {
                    try
                    {
                        m_secureAction.invokeServiceListenerHookRemoved(lh, removed);
                    }
                    catch (Throwable th)
                    {
                        m_logger.log(sr, Logger.LOG_WARNING,
                            "Problem invoking service registry hook", th);
                    }
                    finally
                    {
                        m_registry.ungetService(this, sr, null);
                    }
                }
            }
        }
    }

    void addFrameworkListener(BundleImpl bundle, FrameworkListener l)
    {
        m_dispatcher.addListener(
            bundle._getBundleContext(), FrameworkListener.class, l, null);
    }

    void removeFrameworkListener(BundleImpl bundle, FrameworkListener l)
    {
        m_dispatcher.removeListener(
            bundle._getBundleContext(), FrameworkListener.class, l);
    }

    /**
     * Implementation for BundleContext.registerService(). Registers
     * a service for the specified bundle bundle.
     *
     * @param classNames A string array containing the names of the classes
     *                under which the new service is available.
     * @param svcObj The service object or <code>ServiceFactory</code>.
     * @param dict A dictionary of properties that further describe the
     *             service or null.
     * @return A <code>ServiceRegistration</code> object or null.
    **/
    ServiceRegistration registerService(
        BundleContextImpl context, String[] classNames, Object svcObj, Dictionary dict)
    {
        if (classNames == null)
        {
            throw new NullPointerException("Service class names cannot be null.");
        }
        else if (svcObj == null)
        {
            throw new IllegalArgumentException("Service object cannot be null.");
        }

        ServiceRegistration reg = null;

        // Check to make sure that the service object is
        // an instance of all service classes; ignore if
        // service object is a service factory.
        if (!(svcObj instanceof ServiceFactory))
        {
            for (int i = 0; i < classNames.length; i++)
            {
                Class clazz = Util.loadClassUsingClass(svcObj.getClass(), classNames[i], m_secureAction);
                if (clazz == null)
                {
                    throw new IllegalArgumentException(
                        "Cannot cast service: " + classNames[i]);
                }
                else if (!clazz.isAssignableFrom(svcObj.getClass()))
                {
                    throw new IllegalArgumentException(
                        "Service object is not an instance of \""
                        + classNames[i] + "\".");
                }
            }
        }

        reg = m_registry.registerService(context.getBundle(), classNames, svcObj, dict);

        // Check to see if this a listener hook; if so, then we need
        // to invoke the callback with all existing service listeners.
        if (HookRegistry.isHook(
            classNames, org.osgi.framework.hooks.service.ListenerHook.class, svcObj))
        {
            org.osgi.framework.hooks.service.ListenerHook lh =
                (org.osgi.framework.hooks.service.ListenerHook)
                    getService(this, reg.getReference(), false);
            if (lh != null)
            {
                try
                {
                    m_secureAction.invokeServiceListenerHookAdded(
                        lh, m_dispatcher.getAllServiceListeners());
                }
                catch (Throwable th)
                {
                    m_logger.log(reg.getReference(), Logger.LOG_WARNING,
                        "Problem invoking service registry hook", th);
                }
                finally
                {
                    this.ungetService(this, reg.getReference(), null);
                }
            }
        }

        this.fireServiceEvent(new ServiceEvent(ServiceEvent.REGISTERED, reg.getReference()), null);

        return reg;
    }

    /**
     * Retrieves an array of {@link ServiceReference} objects based on calling bundle,
     * service class name, and filter expression.  Optionally checks for isAssignable to
     * make sure that the service can be cast to the
     * @param bundle Calling Bundle
     * @param className Service Classname or <code>null</code> for all
     * @param expr Filter Criteria or <code>null</code>
     * @return Array of ServiceReference objects that meet the criteria
     * @throws InvalidSyntaxException
     */
    ServiceReference[] getServiceReferences(
        final BundleImpl bundle, final String className,
        final String expr, final boolean checkAssignable)
        throws InvalidSyntaxException
    {
        // Define filter if expression is not null.
        SimpleFilter filter = null;
        if (expr != null)
        {
            try
            {
                filter = SimpleFilter.parse(expr);
            }
            catch (Exception ex)
            {
                throw new InvalidSyntaxException(ex.getMessage(), expr);
            }
        }

        // Ask the service registry for all matching service references.
        final Collection refList = m_registry.getServiceReferences(className, filter);

        // Filter on assignable references
        if (checkAssignable)
        {
            for (Iterator refIter = refList.iterator(); refIter.hasNext();)
            {
                // Get the current service reference.
                ServiceReference ref = (ServiceReference) refIter.next();

                // Now check for castability.
                if (!Util.isServiceAssignable(bundle, ref))
                {
                    refIter.remove();
                }
            }
        }

        // If the requesting bundle is the system bundle, ignore the effects of the findhooks
        Collection resRefList = (bundle == this ? new ArrayList(refList) : refList);

        // activate findhooks
        Set<ServiceReference<org.osgi.framework.hooks.service.FindHook>> findHooks =
                getHookRegistry().getHooks(org.osgi.framework.hooks.service.FindHook.class);
        for (ServiceReference<org.osgi.framework.hooks.service.FindHook> sr : findHooks)
        {
            org.osgi.framework.hooks.service.FindHook fh = getService(this, sr, false);
            if (fh != null)
            {
                try
                {
                    m_secureAction.invokeServiceFindHook(
                        fh,
                        bundle._getBundleContext(),
                        className,
                        expr,
                        !checkAssignable,
                        new ShrinkableCollection(refList));
                }
                catch (Throwable th)
                {
                    m_logger.log(sr, Logger.LOG_WARNING,
                        "Problem invoking service registry hook", th);
                }
                finally
                {
                    m_registry.ungetService(this, sr, null);
                }
            }
        }

        // We return resRefList which is normally the same as refList and therefore any modifications
        // to refList are also visible to resRefList. However in the case of the system bundle being
        // the requestor, resRefList is a copy of the original list before the hooks were invoked.
        if (resRefList.size() > 0)
        {
            return (ServiceReference[]) resRefList.toArray(new ServiceReference[resRefList.size()]);
        }

        return null;
    }

    /**
     * Retrieves Array of {@link ServiceReference} objects based on calling bundle, service class name,
     * optional filter expression, and optionally filters further on the version.
     * If running under a {@link SecurityManager}, checks that the calling bundle has permissions to
     * see the service references and removes references that aren't.
     * @param bundle Calling Bundle
     * @param className Service Classname or <code>null</code> for all
     * @param expr Filter Criteria or <code>null</code>
     * @param checkAssignable <code>true</code> to check for isAssignable, <code>false</code> to return all versions
     * @return Array of ServiceReference objects that meet the criteria
     * @throws InvalidSyntaxException
     */
    ServiceReference[] getAllowedServiceReferences(
        BundleImpl bundle, String className, String expr, boolean checkAssignable)
        throws InvalidSyntaxException
    {
        ServiceReference[] refs = getServiceReferences(bundle, className, expr, checkAssignable);

        Object sm = System.getSecurityManager();

        if ((sm == null) || (refs == null))
        {
            return refs;
        }

        List result = new ArrayList();

        for (int i = 0; i < refs.length; i++)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new ServicePermission(refs[i], ServicePermission.GET));
                result.add(refs[i]);
            }
            catch (Exception ex)
            {
                // Ignore, since we are just testing permission.
            }
        }

        if (result.isEmpty())
        {
            return null;
        }

        return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);

    }

    <S> S getService(Bundle bundle, ServiceReference<S> ref, boolean isServiceObjetcs)
    {
        try
        {
            return m_registry.getService(bundle, ref, isServiceObjetcs);
        }
        catch (ServiceException ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
        }

        return null;
    }

    boolean ungetService(Bundle bundle, ServiceReference ref, Object srvObj)
    {
        return m_registry.ungetService(bundle, ref, srvObj);
    }

    File getDataFile(BundleImpl bundle, String s)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("Bundle has been uninstalled");
        }
        else if (Util.isFragment(adapt(BundleRevision.class)))
        {
            return null;
        }
        try
        {
            if (bundle == this)
            {
                return m_cache.getSystemBundleDataFile(s);
            }

            return bundle.getArchive().getDataFile(s);
        }
        catch (Exception ex)
        {
            m_logger.log(bundle, Logger.LOG_ERROR, ex.getMessage());
            return null;
        }
    }

    //
    // Hook service management methods.
    //

    HookRegistry getHookRegistry()
    {
        return m_registry.getHookRegistry();
    }

    //
    // PackageAdmin related methods.
    //

    private final Map<Class, Boolean> m_systemBundleClassCache =
        new WeakHashMap<Class, Boolean>();

    /**
     * This method returns the bundle associated with the specified class if
     * the class was loaded from a bundle from this framework instance. If the
     * class was not loaded from a bundle or was loaded by a bundle in another
     * framework instance, then <tt>null</tt> is returned.
     *
     * @param clazz the class for which to find its associated bundle.
     * @return the bundle associated with the specified class or <tt>null</tt>
     *         if the class was not loaded by a bundle or its associated
     *         bundle belongs to a different framework instance.
    **/
    Bundle getBundle(Class clazz)
    {
        // If the class comes from bundle class loader, then return
        // associated bundle if it is from this framework instance.
        if (clazz.getClassLoader() instanceof BundleReference)
        {
            // Only return the bundle if it is from this framework.
            BundleReference br = (BundleReference) clazz.getClassLoader();
            return ((br.getBundle() instanceof BundleImpl)
                && (((BundleImpl) br.getBundle()).getFramework() == this))
                    ? br.getBundle() : null;
        }
        // Otherwise check if the class conceptually comes from the
        // system bundle. Ignore implicit boot delegation.
        if (!clazz.getName().startsWith("java."))
        {
            Boolean fromSystemBundle;
            synchronized (m_systemBundleClassCache)
            {
                fromSystemBundle = m_systemBundleClassCache.get(clazz);
            }
            if (fromSystemBundle == null)
            {
                Class sbClass = null;
                try
                {
                    sbClass = ((BundleWiringImpl) m_extensionManager
                        .getRevision().getWiring()).getClassByDelegation(clazz.getName());
                }
                catch (ClassNotFoundException ex)
                {
                    // Ignore, treat as false.
                }
                synchronized (m_systemBundleClassCache)
                {
                    if (sbClass == clazz)
                    {
                        fromSystemBundle = Boolean.TRUE;
                    }
                    else
                    {
                        fromSystemBundle = Boolean.FALSE;
                    }
                    m_systemBundleClassCache.put(clazz, fromSystemBundle);
                }
            }
            return fromSystemBundle.booleanValue() ? this : null;
        }
        return null;
    }

    /**
     * Returns the exported packages associated with the specified
     * package name. This is used by the PackageAdmin service
     * implementation.
     *
     * @param pkgName The name of the exported package to find.
     * @return The exported package or null if no matching package was found.
    **/
    ExportedPackage[] getExportedPackages(String pkgName)
    {
        // First, get all exporters of the package.
        Map<String, Object> attrs = Collections.singletonMap(
            BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            null,
            BundleRevision.PACKAGE_NAMESPACE,
            Collections.EMPTY_MAP,
            attrs);
        List<BundleCapability> exports = m_resolver.findProviders(req, false);

        // We only want resolved capabilities.
        for (Iterator<BundleCapability> it = exports.iterator(); it.hasNext(); )
        {
            if (it.next().getRevision().getWiring() == null)
            {
                it.remove();
            }
        }

        if (exports != null)
        {
            List pkgs = new ArrayList();

            for (Iterator<BundleCapability> it = exports.iterator(); it.hasNext(); )
            {
                // Get the bundle associated with the current exporting revision.
                Bundle bundle = it.next().getRevision().getBundle();

                // We need to find the version of the exported package, but this
                // is tricky since there may be multiple versions of the package
                // offered by a given bundle, since multiple revisions of the
                // bundle JAR file may exist if the bundle was updated without
                // refreshing the framework. In this case, each revision of the
                // bundle JAR file is represented as a revision ordered from
                // newest to oldest. We assume that the first revision found to
                // be exporting the package is the provider of the package,
                // which makes sense since it must have been resolved first.
                List<BundleRevision> originRevisions =
                    bundle.adapt(BundleRevisions.class).getRevisions();
                for (int i = originRevisions.size() - 1; i >= 0; i--)
                {
                    BundleRevision originBr = originRevisions.get(i);
                    List<BundleRevision> revisions = Collections.singletonList(originBr);

                    if ((originBr.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0)
                    {
                        // If it's a fragment, find the revisions of the attached
                        // bundle(s) and work with those instead. Note that fragments
                        // can be attached to multiple hosts...
                        revisions = new ArrayList<BundleRevision>();

                        for (BundleWire bw : originBr.getWiring().getRequiredWires(HostNamespace.HOST_NAMESPACE))
                        {
                            revisions.add(bw.getProviderWiring().getRevision());
                        }
                    }

                    for (BundleRevision br : revisions)
                    {
                        List<BundleCapability> caps = (br.getWiring() == null)
                            ? br.getDeclaredCapabilities(null)
                            : br.getWiring().getCapabilities(null);
                        for (BundleCapability cap : caps)
                        {
                            if (cap.getNamespace().equals(req.getNamespace())
                                && CapabilitySet.matches(cap, req.getFilter()))
                            {
                                pkgs.add(
                                    new ExportedPackageImpl(
                                        this, (BundleImpl) br.getBundle(), br, cap));
                            }
                        }
                    }
                }
            }

            return (pkgs.isEmpty())
                ? null
                : (ExportedPackage[]) pkgs.toArray(new ExportedPackage[pkgs.size()]);
        }

        return null;
    }

    /**
     * Returns an array of all actively exported packages from the specified
     * bundle or if the specified bundle is <tt>null</tt> an array
     * containing all actively exported packages by all bundles.
     *
     * @param b The bundle whose exported packages are to be retrieved
     *        or <tt>null</tt> if the exported packages of all bundles are
     *        to be retrieved.
     * @return An array of exported packages.
    **/
    ExportedPackage[] getExportedPackages(Bundle b)
    {
        List list = new ArrayList();

        // If a bundle is specified, then return its
        // exported packages.
        if (b != null)
        {
            BundleImpl bundle = (BundleImpl) b;
            getExportedPackages(bundle, list);
        }
        // Otherwise return all exported packages.
        else
        {
            // To create a list of all exported packages, we must look
            // in the installed and uninstalled sets of bundles.
            boolean locked = acquireGlobalLock();
            if (!locked)
            {
                // If the calling thread holds bundle locks, then we might not
                // be able to get the global lock.
                throw new IllegalStateException(
                    "Unable to acquire global lock to retrieve exported packages.");
            }
            try
            {
                // First get exported packages from uninstalled bundles.
                for (int bundleIdx = 0;
                    (m_uninstalledBundles != null) && (bundleIdx < m_uninstalledBundles.size());
                    bundleIdx++)
                {
                    BundleImpl bundle = m_uninstalledBundles.get(bundleIdx);
                    getExportedPackages(bundle, list);
                }

                // Now get exported packages from installed bundles.
                Bundle[] bundles = getBundles();
                for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
                {
                    BundleImpl bundle = (BundleImpl) bundles[bundleIdx];
                    getExportedPackages(bundle, list);
                }
            }
            finally
            {
                releaseGlobalLock();
            }
        }

        return (list.isEmpty())
            ? null
            : (ExportedPackage[]) list.toArray(new ExportedPackage[list.size()]);
    }

    /**
     * Adds any current active exported packages from the specified bundle
     * to the passed in list.
     * @param bundle The bundle from which to retrieve exported packages.
     * @param list The list to which the exported packages are added
    **/
    private void getExportedPackages(Bundle bundle, List list)
    {
        // Since a bundle may have many revisions associated with it,
        // one for each revision in the cache, search each revision
        // to get all exports.
        for (BundleRevision br : bundle.adapt(BundleRevisions.class).getRevisions())
        {
            List<BundleCapability> caps = (br.getWiring() == null)
                ? br.getDeclaredCapabilities(null)
                : br.getWiring().getCapabilities(null);
            if ((caps != null) && (caps.size() > 0))
            {
                for (BundleCapability cap : caps)
                {
                    if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        String pkgName = (String)
                            cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
                        list.add(new ExportedPackageImpl(
                            this, (BundleImpl) bundle, br, cap));
                    }
                }
            }
        }
    }

    // Needed by ExportedPackageImpl.
    Set<Bundle> getImportingBundles(BundleImpl exporter, BundleCapability cap)
    {
        return m_dependencies.getImportingBundles(exporter, cap);
    }

    // Needed by RequiredBundleImpl.
    Set<Bundle> getRequiringBundles(BundleImpl bundle)
    {
        return m_dependencies.getRequiringBundles(bundle);
    }

    boolean resolveBundles(Collection<Bundle> targets)
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                "Unable to acquire global lock to perform resolve.",
                null);
            return false;
        }

        try
        {
            // Remember original targets.
            Collection<Bundle> originalTargets = targets;

            // Determine set of bundles to be resolved, which is either the
            // specified bundles or all bundles if null.
            if (targets == null)
            {
                // Add all bundles to the list.
                targets = m_installedBundles[LOCATION_MAP_IDX].values();
            }

            // Now resolve each target bundle.
            boolean result = true;

            // If there are targets, then resolve each one.
            if (!targets.isEmpty())
            {
                // Get bundle revisions for bundles in INSTALLED state.
                Set<BundleRevision> revisions =
                    new HashSet<BundleRevision>(targets.size());
                for (Bundle b : targets)
                {
                    if (b.getState() != Bundle.UNINSTALLED)
                    {
                        revisions.add(b.adapt(BundleRevision.class));
                    }
                }
                // If we had to filter any of the original targets, then
                // the return result will be false regardless.
                if ((originalTargets != null) && (originalTargets.size() != revisions.size()))
                {
                    result = false;
                }
                try
                {
                    m_resolver.resolve(Collections.EMPTY_SET, revisions);
                    if (result)
                    {
                        for (BundleRevision br : revisions)
                        {
                            if (br.getWiring() == null)
                            {
                                result = false;
                                break;
                            }
                        }
                    }
                }
                catch (ResolutionException ex)
                {
                    result = false;
                }
                catch (BundleException ex)
                {
                    result = false;
                }
            }

            return result;
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }
    }

    private void resolveBundleRevision(BundleRevision revision) throws BundleException
    {
        try
        {
            m_resolver.resolve(Collections.singleton(revision), Collections.EMPTY_SET);
        }
        catch (ResolutionException ex)
        {
            throw new BundleException(ex.getMessage() +
                " Unresolved requirements: " + ex.getUnresolvedRequirements(),
                BundleException.RESOLVE_ERROR);
        }
    }

    void refreshPackages(Collection<Bundle> targets, FrameworkListener[] listeners)
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the thread calling holds bundle locks, then we might not
            // be able to get the global lock. However, in practice this
            // should not happen since the calls to this method have either
            // already acquired the global lock or it is PackageAdmin which
            // doesn't hold bundle locks.
            throw new IllegalStateException(
                "Unable to acquire global lock for refresh.");
        }

        // Determine set of bundles to refresh, which is all transitive
        // dependencies of specified set or all transitive dependencies
        // of all bundles if null is specified.
        Collection<Bundle> newTargets = targets;
        if (newTargets == null)
        {
            List<Bundle> list = new ArrayList<Bundle>();

            // First add all uninstalled bundles.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.size());
                i++)
            {
                list.add(m_uninstalledBundles.get(i));
            }

            // Then add all updated bundles.
            Iterator iter = m_installedBundles[LOCATION_MAP_IDX].values().iterator();
            while (iter.hasNext())
            {
                BundleImpl bundle = (BundleImpl) iter.next();
                if (bundle.isRemovalPending())
                {
                    list.add(bundle);
                }
            }

            if (!list.isEmpty())
            {
                newTargets = list;
            }
        }

        // If there are targets, then find all dependencies for each one.
        Set<Bundle> bundles = null;
        if (newTargets != null)
        {
            // Create map of bundles that import the packages
            // from the target bundles.
            bundles = new HashSet<Bundle>();
            for (Bundle target : newTargets)
            {
                // If anyone passes in a null bundle, then just
                // ignore it.
                if (target != null)
                {
                    // Add the current target bundle to the map of
                    // bundles to be refreshed.
                    bundles.add(target);
                    // Add all importing bundles to map.
                    populateDependentGraph((BundleImpl) target, bundles);
                }
            }
        }

        // Now refresh each bundle.
        try
        {
            boolean restart = false;

            Bundle systemBundle = this;

            // We need to restart the framework if either an extension bundle is
            // refreshed or the system bundle is refreshed and any extension bundle
            // has been updated or uninstalled.
            if (bundles != null)
            {
                for (Bundle b : bundles)
                {
                    if ((systemBundle == b) || ((BundleImpl) b).isExtension())
                    {
                        restart = true;
                        break;
                    }
                }

                // If we need to restart the framework, then no reason to
                // do a refresh.
                if (!restart)
                {
                    // Now we actually need to refresh the affected bundles.
                    // At this point the collection contains every bundle that has
                    // been updated and/or removed as well as all bundles that import
                    // packages from these bundles.

                    // Create refresh helpers for each bundle.
                    List<RefreshHelper> helpers = new ArrayList<RefreshHelper>(bundles.size());
                    for (Bundle b : bundles)
                    {
                        // Remove any targeted bundles from the uninstalled bundles
                        // array, since they will be removed from the system after
                        // the refresh.
                        // TODO: FRAMEWORK - Is this correct?
                        forgetUninstalledBundle((BundleImpl) b);

                        // Create refresh helper for bundle.
                        helpers.add(new RefreshHelper(b));
                    }

                    // Stop all refreshing bundles.
                    for (RefreshHelper helper : helpers)
                    {
                        if (helper != null)
                        {
                            helper.stop();
                        }
                    }

                    // Refresh or remove all refreshing bundles first.
                    for (RefreshHelper helper : helpers)
                    {
                        if (helper != null)
                        {
                            helper.refreshOrRemove();
                        }
                    }

                    // Restart all refreshed bundles that were previously running.
                    for (RefreshHelper helper : helpers)
                    {
                        if (helper != null)
                        {
                            helper.restart();
                        }
                    }
                }
                else
                {
                    try
                    {
                        update();
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(Logger.LOG_ERROR, "Framework restart error.", ex);
                    }
                }
            }
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }

        fireFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, this, null);

        if (listeners != null)
        {
            FrameworkEvent event = new FrameworkEvent(
                FrameworkEvent.PACKAGES_REFRESHED, this, null);
            for (FrameworkListener l : listeners)
            {
                try
                {
                    l.frameworkEvent(event);
                }
                catch (Throwable th)
                {
                    m_logger.log(Logger.LOG_ERROR,
                        "Framework listener delivery error.", th);
                }
            }
        }
    }

    Collection<Bundle> getDependencyClosure(Collection<Bundle> targets)
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the thread calling holds bundle locks, then we might not
            // be able to get the global lock. However, in practice this
            // should not happen since the calls to this method have either
            // already acquired the global lock or it is PackageAdmin which
            // doesn't hold bundle locks.
            throw new IllegalStateException(
                "Unable to acquire global lock for refresh.");
        }

        try
        {
            // If there are targets, then find all dependencies for each one.
            Set<Bundle> bundles = Collections.EMPTY_SET;
            if (targets != null)
            {
                // Create map of bundles that import the packages
                // from the target bundles.
                bundles = new HashSet<Bundle>();
                for (Bundle target : targets)
                {
                    // Add the current target bundle to the map of
                    // bundles to be refreshed.
                    bundles.add(target);
                    // Add all importing bundles to map.
                    populateDependentGraph((BundleImpl) target, bundles);
                }
            }
            return bundles;
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }
    }

    // Calls to this method must have the global lock.
    private void populateDependentGraph(BundleImpl exporter, Set<Bundle> set)
    {
        // Get all dependent bundles of this bundle.
        Set<Bundle> dependents = m_dependencies.getDependentBundles(exporter);

        if (dependents != null)
        {
            for (Bundle b : dependents)
            {
                // Avoid cycles if the bundle is already in set.
                if (!set.contains(b))
                {
                    // Add each dependent bundle to set.
                    set.add(b);
                    // Now recurse into each bundle to get its dependents.
                    populateDependentGraph((BundleImpl) b, set);
                }
            }
        }
    }

    Collection<Bundle> getRemovalPendingBundles()
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the thread calling holds bundle locks, then we might not
            // be able to get the global lock. However, in practice this
            // should not happen since the calls to this method have either
            // already acquired the global lock or it is PackageAdmin which
            // doesn't hold bundle locks.
            throw new IllegalStateException(
                "Unable to acquire global lock for refresh.");
        }

        try
        {
            List<Bundle> bundles = new ArrayList<Bundle>();
            if (m_uninstalledBundles != null)
            {
                for (Bundle b : m_uninstalledBundles)
                {
                    bundles.add(b);
                }
            }
            for (Bundle b : getBundles())
            {
                if (((BundleImpl) b).isRemovalPending())
                {
                    bundles.add(b);
                }
            }
            return bundles;
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }
    }

    //
    // Miscellaneous private methods.
    //

    private volatile SecurityProvider m_securityProvider;

    SecurityProvider getSecurityProvider()
    {
        return m_securityProvider;
    }

    void setSecurityProvider(SecurityProvider securityProvider)
    {
        m_securityProvider = securityProvider;
    }

    Object getSignerMatcher(BundleImpl bundle, int signersType)
    {
        if ((bundle != this) && (m_securityProvider != null))
        {
            return m_securityProvider.getSignerMatcher(bundle, signersType);
        }
        return new HashMap();
    }

    boolean impliesBundlePermission(BundleProtectionDomain bundleProtectionDomain, Permission permission, boolean direct)
    {
        if (direct && permission instanceof PackagePermission)
        {
            if (bundleProtectionDomain.impliesWoven(permission))
            {
                return true;
            }
        }
        if (m_securityProvider != null)
        {
            return m_securityProvider.hasBundlePermission(bundleProtectionDomain, permission, direct);
        }
        else
        {
            Bundle source = bundleProtectionDomain.getBundle();

            return (m_securityDefaultPolicy && (source == null || source.getBundleId() != 0)) ?
                bundleProtectionDomain.superImplies(permission) : true;
        }
    }

    private BundleActivator createBundleActivator(Bundle impl)
        throws Exception
    {
        // CONCURRENCY NOTE:
        // This method is called indirectly from startBundle() (via _startBundle()),
        // which has the bundle lock, so there is no need to do any locking here.

        // Get the activator class from the header map.
        BundleActivator activator = null;
        Map headerMap = ((BundleRevisionImpl) impl.adapt(BundleRevision.class)).getHeaders();
        String className = (String) headerMap.get(Constants.BUNDLE_ACTIVATOR);
        // Try to instantiate activator class if present.
        if (className != null)
        {
            className = className.trim();
            Class clazz;
            try
            {
                clazz = ((BundleWiringImpl)
                    impl.adapt(BundleRevision.class).getWiring()).getClassByDelegation(className);
            }
            catch (ClassNotFoundException ex)
            {
                throw new BundleException("Not found: " + className, ex);
            }
            activator = (BundleActivator) clazz.newInstance();
        }

        return activator;
    }

    private void refreshBundle(BundleImpl bundle) throws Exception
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.INSTALLED | Bundle.RESOLVED);
        }
        catch (IllegalStateException ex)
        {
            throw new BundleException(
                "Bundle state has changed unexpectedly during refresh.");
        }

        try
        {
            // See if we need to fire UNRESOLVED event.
            boolean fire = (bundle.getState() != Bundle.INSTALLED);
            // Remove dependencies.
            m_dependencies.removeDependencies(bundle);
            // Reset the bundle object.
            bundle.refresh();
            // Fire UNRESOLVED event if necessary
            // and notify state change..
            if (fire)
            {
                setBundleStateAndNotify(bundle, Bundle.INSTALLED);
                fireBundleEvent(BundleEvent.UNRESOLVED, bundle);
            }
        }
        catch (Exception ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
        }
        finally
        {
            // Always release the bundle lock.
            releaseBundleLock(bundle);
        }
    }

    //
    // Event-related methods.
    //

    /**
     * Fires bundle events.
    **/
    void fireFrameworkEvent(
        int type, Bundle bundle, Throwable throwable)
    {
        m_dispatcher.fireFrameworkEvent(new FrameworkEvent(type, bundle, throwable));
    }

    /**
     * Fires bundle events.
     *
     * @param type The type of bundle event to fire.
     * @param bundle The bundle associated with the event.
    **/
    void fireBundleEvent(int type, Bundle bundle)
    {
        m_dispatcher.fireBundleEvent(new BundleEvent(type, bundle), this);
    }

    void fireBundleEvent(int type, Bundle bundle, Bundle origin)
    {
        m_dispatcher.fireBundleEvent(new BundleEvent(type, bundle, origin), this);
    }

    /**
     * Fires service events.
     *
     * @param event The service event to fire.
     * @param reg The service registration associated with the service object.
    **/
    private void fireServiceEvent(ServiceEvent event, Dictionary oldProps)
    {
        m_dispatcher.fireServiceEvent(event, oldProps, this);
    }

    //
    // Property related methods.
    //

    private void initializeFrameworkProperties()
    {
        // Standard OSGi properties.
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_VERSION,
            FelixConstants.FRAMEWORK_VERSION_VALUE);
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_VENDOR,
            FelixConstants.FRAMEWORK_VENDOR_VALUE);
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_LANGUAGE,
            System.getProperty("user.language"));
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_OS_VERSION,
            System.getProperty("os.version"));
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_EXTENSION,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_FRAGMENT,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
            "false");

        String s = null;
        s = NativeLibraryClause.normalizeOSName(System.getProperty("os.name"));
        m_configMutableMap.put(FelixConstants.FRAMEWORK_OS_NAME, s);
        s = NativeLibraryClause.normalizeProcessor(System.getProperty("os.arch"));
        m_configMutableMap.put(FelixConstants.FRAMEWORK_PROCESSOR, s);
        m_configMutableMap.put(
            FelixConstants.FELIX_VERSION_PROPERTY, getFrameworkVersion());

        Properties defaultProperties = Util.loadDefaultProperties(m_logger);
        // Set supported execution environments to default value,
        // if not explicitly configured.
        loadFromDefaultIfNotDefined(defaultProperties, Constants.FRAMEWORK_EXECUTIONENVIRONMENT);

        // Set supported native capabilities to default value,
        // if not explicitly configured.
        loadPrefixFromDefaultIfNotDefined(m_configMutableMap, defaultProperties, FelixConstants.NATIVE_OS_NAME_ALIAS_PREFIX);
        loadPrefixFromDefaultIfNotDefined(m_configMutableMap, defaultProperties, FelixConstants.NATIVE_PROC_NAME_ALIAS_PREFIX);
    }

    private void loadFromDefaultIfNotDefined(Properties defaultProperties, String propertyName)
    {
        String s;
        if (!getConfig().containsKey(propertyName))
        {
            s = Util.getDefaultProperty(m_logger, propertyName);
            if (s != null)
            {
                m_configMutableMap.put(propertyName, s);
            }
        }
    }

    private void loadPrefixFromDefaultIfNotDefined(Map configMap, Properties defaultProperties, String prefix)
    {
        Map<String, String> defaultPropsWithPrefix = Util.getDefaultPropertiesWithPrefix(defaultProperties, prefix);

        for(String currentDefaultProperty: defaultPropsWithPrefix.keySet())
        {
            if(!configMap.containsKey(currentDefaultProperty))
            {
                configMap.put(currentDefaultProperty, defaultPropsWithPrefix.get(currentDefaultProperty));
            }
        }
    }

    /**
     * Read the framework version from the property file.
     * @return the framework version as a string.
    **/
    private static String getFrameworkVersion()
    {
        // The framework version property.
        Properties props = new Properties();
        InputStream in = Felix.class.getResourceAsStream("Felix.properties");
        if (in != null)
        {
            try
            {
                props.load(in);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                try
                {
                    in.close();
                }
                catch (IOException ex)
                {
                    // Not much we can do.
                }
            }
        }

        // Maven uses a '-' to separate the version qualifier,
        // while OSGi uses a '.', so we need to convert to a '.'
        StringBuffer sb =
            new StringBuffer(
                props.getProperty(
                    FelixConstants.FELIX_VERSION_PROPERTY, "0.0.0"));
        String toRet = cleanMavenVersion(sb);
        if (toRet.indexOf("${pom") >= 0)
        {
            return "0.0.0";
        }
        else
        {
            return toRet;
        }
    }

    /**
     * The main purpose of this method is to turn a.b.c-SNAPSHOT into a.b.c.SNAPSHOT
     * it can also deal with a.b-SNAPSHOT and turns it into a.b.0.SNAPSHOT and
     * will leave the dash in a.b.c.something-else, as it's valid in that last example.
     * In short this method attempts to map a Maven version to an OSGi version as well
     * as possible.
     * @param sb The version to be cleaned
     * @return The cleaned version
     */
    private static String cleanMavenVersion(StringBuffer sb)
    {
        int dots = 0;
        for (int i = 0; i < sb.length(); i++)
        {
            switch (sb.charAt(i))
            {
                case '.':
                    dots++;
                    break;
                case '-':
                    if (dots < 3)
                    {
                        sb.setCharAt(i, '.');
                        for (int j = dots; j < 2; j++)
                        {
                            sb.insert(i, ".0");
                        }
                    }
                    break;
            }
        }
        return sb.toString();
    }

    //
    // Private utility methods.
    //

    /**
     * Generated the next valid bundle identifier.
    **/
    private long loadNextId()
    {
        synchronized (m_nextIdLock)
        {
            // Read persisted next bundle identifier.
            InputStream is = null;
            BufferedReader br = null;
            try
            {
                File file = m_cache.getSystemBundleDataFile("bundle.id");
                is = m_secureAction.getFileInputStream(file);
                br = new BufferedReader(new InputStreamReader(is));
                return Long.parseLong(br.readLine());
            }
            catch (FileNotFoundException ex)
            {
                // Ignore this case because we assume that this is the
                // initial startup of the framework and therefore the
                // file does not exist yet.
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Unable to initialize next bundle identifier from persistent storage.",
                    ex);
            }
            finally
            {
                try
                {
                    if (br != null) br.close();
                    if (is != null) is.close();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Unable to close next bundle identifier file.",
                        ex);
                }
            }
        }

        return -1;
    }

    private long getNextId()
    {
        synchronized (m_nextIdLock)
        {
            // Save the current id.
            long id = m_nextId;

            // Increment the next id.
            m_nextId++;

            // Write the bundle state.
            OutputStream os = null;
            BufferedWriter bw = null;
            try
            {
                File file = m_cache.getSystemBundleDataFile("bundle.id");
                os = m_secureAction.getFileOutputStream(file);
                bw = new BufferedWriter(new OutputStreamWriter(os));
                String s = Long.toString(m_nextId);
                bw.write(s, 0, s.length());
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Unable to save next bundle identifier to persistent storage.",
                    ex);
            }
            finally
            {
                try
                {
                    if (bw != null) bw.close();
                    if (os != null) os.close();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Unable to close next bundle identifier file.",
                        ex);
                }
            }

            return id;
        }
    }

    //
    // Miscellaneous inner classes.
    //

    class SystemBundleActivator implements BundleActivator
    {
        @Override
        public void start(BundleContext context) throws Exception
        {
            // Add the bundle activator for the url handler service.
            m_activatorList.add(0, new URLHandlersActivator(m_configMap, Felix.this));

            // Start all activators.
            for (Iterator<BundleActivator> iter = m_activatorList.iterator(); iter.hasNext(); )
            {
                try
                {
                    Felix.m_secureAction.startActivator(
                        iter.next(), context);
                }
                catch (Throwable throwable)
                {
                    iter.remove();
                    fireFrameworkEvent(FrameworkEvent.ERROR, context.getBundle(),
                            new BundleException("Unable to start Bundle", throwable));
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Exception starting a system bundle activator.",
                        throwable);
                }
            }
        }

        @Override
        public void stop(BundleContext context)
        {
            // The state of the framework should be STOPPING, so
            // acquire the bundle lock to verify it.
            acquireBundleLock(Felix.this, Bundle.STOPPING);
            releaseBundleLock(Felix.this);

            // Use the start level service to set the start level to zero
            // in order to stop all bundles in the framework. Since framework
            // shutdown happens on its own thread, we can wait for the start
            // level service to finish before proceeding by calling the
            // non-spec setStartLevelAndWait() method.
            m_fwkStartLevel.setStartLevelAndWait(0);

            // Stop framework wiring thread.
            m_fwkWiring.stop();
            // Stop framework start level thread.
            m_fwkStartLevel.stop();

            // Shutdown event dispatching queue.
            m_dispatcher.stopDispatching();

            // Since there may be updated and uninstalled bundles that
            // have not been refreshed, we will take care of refreshing
            // them during shutdown.

            // Refresh all updated bundles.
            Bundle[] bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                BundleImpl bundle = (BundleImpl) bundles[i];
                if (bundle.isRemovalPending())
                {
                    try
                    {
                        refreshBundle(bundle);
                    }
                    catch (Exception ex)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                        m_logger.log(bundle, Logger.LOG_ERROR, "Unable to purge bundle "
                            + bundle._getLocation(), ex);
                    }
                }
            }

            // Delete uninstalled bundles.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.size());
                i++)
            {
                try
                {
                    m_uninstalledBundles.get(i).closeAndDelete();
                }
                catch (Exception ex)
                {
                    m_logger.log(m_uninstalledBundles.get(i),
                        Logger.LOG_ERROR,
                        "Unable to remove "
                        + m_uninstalledBundles.get(i)._getLocation(), ex);
                }
            }

            // Dispose of the bundles to close their associated contents.
            bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                ((BundleImpl) bundles[i]).close();
            }

            m_extensionManager.stopExtensionBundles(Felix.this);
            // Stop all system bundle activators.
            for (int i = 0; i < m_activatorList.size(); i++)
            {
                try
                {
                    Felix.m_secureAction.stopActivator((BundleActivator)
                        m_activatorList.get(i), _getBundleContext());
                }
                catch (Throwable throwable)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, context.getBundle(),
                        new BundleException("Unable to stop Bundle", throwable));
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Exception stopping a system bundle activator.",
                        throwable);
                }
            }
            if (m_securityManager != null)
            {
                System.setSecurityManager(null);
                m_securityManager = null;
            }

            m_dependencies.removeDependents(adapt(BundleRevision.class));

            if (m_extensionManager != null)
            {
                m_extensionManager.removeExtensions(Felix.this);
            }

            // Dispose of the bundle cache.
            m_cache.release();
            m_cache = null;

            // Set the framework state to resolved.
            acquireBundleLock(Felix.this, Bundle.STOPPING);
            try
            {
                // Clean up the bundle context.
                ((BundleContextImpl) _getBundleContext()).invalidate();
                setBundleContext(null);

                // Set the framework state to resolved and open
                // the shutdown gate.
                setBundleStateAndNotify(Felix.this, Bundle.RESOLVED);
                m_shutdownGate.open();
                m_shutdownGate = null;
            }
            finally
            {
                releaseBundleLock(Felix.this);
            }
        }
    }

    /**
     * Simple class that is used in <tt>refreshPackages()</tt> to embody
     * the refresh logic in order to keep the code clean. This class is
     * not static because it needs access to framework event firing methods.
    **/
    private class RefreshHelper
    {
        private BundleImpl m_bundle = null;
        private int m_oldState = Bundle.INSTALLED;

        public RefreshHelper(Bundle bundle)
        {
            m_bundle = (BundleImpl) bundle;
        }

        public void stop()
        {
            acquireBundleLock(m_bundle,
                    Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING |
                    Bundle.ACTIVE | Bundle.STOPPING | Bundle.UNINSTALLED);
            try
            {
                m_oldState = m_bundle.getState();
                if (m_oldState != Bundle.UNINSTALLED)
                {
                    if (!Util.isFragment(m_bundle.adapt(BundleRevision.class)))
                    {
                        stopBundle(m_bundle, false);
                    }
                }
            }
            catch (Throwable ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
            }
            finally
            {
                releaseBundleLock(m_bundle);
            }
        }

        public void refreshOrRemove()
        {
            try
            {
                // Delete or refresh the bundle depending on its
                // current state.
                if (m_bundle.getState() == Bundle.UNINSTALLED)
                {
                    // Remove dependencies.
                    m_dependencies.removeDependencies(m_bundle);
                    m_bundle.closeAndDelete();
                    m_bundle = null;
                }
                else
                {
                    // This removes all old bundle revisions from memory and
                    // from disk. It only maintains the newest revision in the
                    // bundle cache.
                    refreshBundle(m_bundle);
                }
            }
            catch (Throwable ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
            }
        }

        public void restart()
        {
            if ((m_bundle != null) && (m_oldState == Bundle.ACTIVE))
            {
                try
                {
// TODO: LAZY - Not sure if this is the best way...
                    int options = Bundle.START_TRANSIENT;
                    options = (m_bundle.getPersistentState() == Bundle.STARTING)
                        ? options | Bundle.START_ACTIVATION_POLICY
                        : options;
                    startBundle(m_bundle, options);
                }
                catch (Throwable ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }
    }

    // Compares bundles by start level. Within a start level,
    // bundles are sorted by bundle ID.
    private static class StartLevelTuple implements Comparable<StartLevelTuple>
    {
        private final BundleImpl m_bundle;
        private int m_level;

        StartLevelTuple(BundleImpl bundle, int level)
        {
            m_bundle = bundle;
            m_level = level;
        }

        @Override
        public int compareTo(StartLevelTuple t)
        {
            int result = 1;

            if (m_level < t.m_level)
            {
                result = -1;
            }
            else if (m_level > t.m_level)
            {
                result = 1;
            }
            else if (m_bundle.getBundleId() < t.m_bundle.getBundleId())
            {
                result = -1;
            }
            else if (m_bundle.getBundleId() == t.m_bundle.getBundleId())
            {
                result = 0;
            }

            return result;
        }
    }

    //
    // Locking related methods.
    //

    private void rememberUninstalledBundle(BundleImpl bundle)
    {
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the calling thread holds bundle locks, then we might not
            // be able to get the global lock.
            throw new IllegalStateException(
                "Unable to acquire global lock to record uninstalled bundle.");
        }
        try
        {
            // Verify that the bundle is not already in the array.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.size());
                i++)
            {
                if (m_uninstalledBundles.get(i) == bundle)
                {
                    return;
                }
            }

            // Use a copy-on-write approach to add the bundle
            // to the uninstalled list.
            List<BundleImpl> uninstalledBundles = new ArrayList(m_uninstalledBundles);
            uninstalledBundles.add(bundle);
            m_uninstalledBundles = uninstalledBundles;
        }
        finally
        {
            releaseGlobalLock();
        }
    }

    private void forgetUninstalledBundle(BundleImpl bundle)
    {
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the calling thread holds bundle locks, then we might not
            // be able to get the global lock.
            throw new IllegalStateException(
                "Unable to acquire global lock to release uninstalled bundle.");
        }
        try
        {
            if (m_uninstalledBundles == null)
            {
                return;
            }

            // Use a copy-on-write approach to remove the bundle
            // from the uninstalled list.
            List<BundleImpl> uninstalledBundles = new ArrayList(m_uninstalledBundles);
            uninstalledBundles.remove(bundle);
            m_uninstalledBundles = uninstalledBundles;
        }
        finally
        {
            releaseGlobalLock();
        }
    }

    void acquireInstallLock(String location)
        throws BundleException
    {
        synchronized (m_installRequestLock_Priority1)
        {
            while (m_installRequestMap.get(location) != null)
            {
                try
                {
                    m_installRequestLock_Priority1.wait();
                }
                catch (InterruptedException ex)
                {
                    throw new BundleException("Unable to install, thread interrupted.");
                }
            }

            m_installRequestMap.put(location, location);
        }
    }

    void releaseInstallLock(String location)
    {
        synchronized (m_installRequestLock_Priority1)
        {
            m_installRequestMap.remove(location);
            m_installRequestLock_Priority1.notifyAll();
        }
    }

    void setBundleStateAndNotify(BundleImpl bundle, int state)
    {
        synchronized (m_bundleLock)
        {
            bundle.__setState(state);
            m_bundleLock.notifyAll();
        }
    }

    /**
     * This method acquires the lock for the specified bundle as long as the
     * bundle is in one of the specified states. If it is not, an exception
     * is thrown. Bundle state changes will be monitored to avoid deadlocks.
     * @param bundle The bundle to lock.
     * @param desiredStates Logically OR'ed desired bundle states.
     * @throws java.lang.IllegalStateException If the bundle is not in one of the
     *         specified desired states.
    **/
    void acquireBundleLock(BundleImpl bundle, int desiredStates)
        throws IllegalStateException
    {
        synchronized (m_bundleLock)
        {
            // Wait if the desired bundle is already locked by someone else
            // or if any thread has the global lock, unless the current thread
            // holds the global lock or the bundle lock already.
            while (!bundle.isLockable() ||
                ((m_globalLockThread != null)
                    && (m_globalLockThread != Thread.currentThread())
                    && (bundle.getLockingThread() != Thread.currentThread())))
            {
                // Check to make sure the bundle is in a desired state.
                // If so, keep waiting. If not, throw an exception.
                if ((desiredStates & bundle.getState()) == 0)
                {
                    throw new IllegalStateException("Bundle in unexpected state.");
                }
                // If the calling thread already owns the global lock, then make
                // sure no other thread is trying to promote a bundle lock to a
                // global lock. If so, interrupt the other thread to avoid deadlock.
                else if (m_globalLockThread == Thread.currentThread()
                    && (bundle.getLockingThread() != null)
                    && m_globalLockWaitersList.contains(bundle.getLockingThread()))
                {
                    bundle.getLockingThread().interrupt();
                }

                try
                {
                    m_bundleLock.wait();
                }
                catch (InterruptedException ex)
                {
                    throw new IllegalStateException("Unable to acquire bundle lock, thread interrupted.");
                }
            }

            // Now that we can acquire the bundle lock, let's check to make sure
            // it is in a desired state; if not, throw an exception and do not
            // lock it.
            if ((desiredStates & bundle.getState()) == 0)
            {
                throw new IllegalStateException("Bundle in unexpected state.");
            }

            // Acquire the bundle lock.
            bundle.lock();
        }
    }

    /**
     * Releases the bundle's lock.
     * @param bundle The bundle whose lock is to be released.
     * @throws java.lang.IllegalStateException If the calling thread does not
     *         own the bundle lock.
    **/
    void releaseBundleLock(BundleImpl bundle)
    {
        synchronized (m_bundleLock)
        {
            // Unlock the bundle.
            bundle.unlock();
            // If the thread no longer holds the bundle lock,
            // then remove it from the held lock map.
            if (bundle.getLockingThread() == null)
            {
                m_bundleLock.notifyAll();
            }
        }
    }

    /**
     * Attempts to acquire the global lock. Will also promote a bundle lock
     * to the global lock, if the calling thread already holds a bundle lock.
     * Since it is possible to deadlock when trying to acquire the global lock
     * while holding a bundle lock, this method may fail if a potential deadlock
     * is detected. If the calling thread does not hold a bundle lock, then it
     * will wait indefinitely to acquire the global.
     * @return <tt>true</tt> if the global lock was successfully acquired,
     *         <tt>false</tt> otherwise.
    **/
    boolean acquireGlobalLock()
    {
        synchronized (m_bundleLock)
        {
            // Wait as long as some other thread holds the global lock
            // and the current thread is not interrupted.
            boolean interrupted = false;
            while (!interrupted
                && (m_globalLockThread != null)
                && (m_globalLockThread != Thread.currentThread()))
            {
                // Add calling thread to global lock waiters list.
                m_globalLockWaitersList.add(Thread.currentThread());
                // We need to wake up all waiting threads so we can
                // recheck for potential deadlock in acquireBundleLock()
                // if this thread was holding a bundle lock and is now
                // trying to promote it to a global lock.
                m_bundleLock.notifyAll();
                // Now wait for the global lock.
                try
                {
                    m_bundleLock.wait();
                }
                catch (InterruptedException ex)
                {
                    interrupted = true;
                }
                // At this point we are either interrupted or will get the
                // global lock, so remove the thread from the waiters list.
                m_globalLockWaitersList.remove(Thread.currentThread());
            }

            // Check to see if we were interrupted, which means someone
            // with the global lock wants our bundle lock, so we should
            // fail gracefully.
            if (!interrupted)
            {
                // Increment the current thread's global lock count.
                m_globalLockCount++;
                m_globalLockThread = Thread.currentThread();
            }

            // Note: If the thread was interrupted, there is no reason to notify
            // anyone, since the thread was likely interrupted to force it to give
            // up a bundle lock it is holding. When it does give up the bundle
            // lock, it will do a notifyAll() in there.

            return !interrupted;
        }
    }

    /**
     * Releases the global lock.
     * @throws java.lang.IllegalStateException If the calling thread does not
     *         own the global lock.
    **/
    void releaseGlobalLock()
    {
        synchronized (m_bundleLock)
        {
            // Decrement the current thread's global lock count;
            if (m_globalLockThread == Thread.currentThread())
            {
                m_globalLockCount--;
                if (m_globalLockCount == 0)
                {
                    m_globalLockThread = null;
                    m_bundleLock.notifyAll();
                }
            }
            else
            {
                throw new IllegalStateException(
                    "The current thread doesn't own the global lock.");
            }
        }
    }

    private volatile URLHandlersActivator m_urlHandlersActivator;

    void setURLHandlersActivator(URLHandlersActivator urlHandlersActivator)
    {
        m_urlHandlersActivator = urlHandlersActivator;
    }

    Object getStreamHandlerService(String protocol)
    {
        return m_urlHandlersActivator.getStreamHandlerService(protocol);
    }

    Object getContentHandlerService(String mimeType)
    {
        return m_urlHandlersActivator.getContentHandlerService(mimeType);
    }

    Collection<BundleCapability> findProviders(final Requirement requirement)
    {
        return m_resolver.findProvidersInternal(null, requirement, true, false);
    }
}
