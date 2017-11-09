/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.scr.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Base class to write bundle extenders.
 * This extender tracks started bundles (or starting if they have a lazy activation
 * policy) and will create an extension for each of them to manage it.
 *
 * The extender will handle all concurrency and synchronization issues.
 *
 * The extender guarantee that all extensions will be stopped synchronously with
 * the STOPPING event of a given bundle and that all extensions will be stopped
 * before the extender bundle is stopped.
 *
 */
public abstract class AbstractExtender implements BundleActivator, BundleTrackerCustomizer<Bundle>, SynchronousBundleListener {

    private final ConcurrentMap<Bundle, Activator.ScrExtension> extensions = new ConcurrentHashMap<>();
    private final ConcurrentMap<Bundle, FutureTask<Void>> destroying = new ConcurrentHashMap<>();
    private volatile boolean stopping;
    private volatile boolean stopped;

    private BundleContext context;
    private BundleTracker<Bundle> tracker;

    public BundleContext getBundleContext() {
        return context;
    }

    public boolean isStopping() {
        return stopping;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        this.context.addBundleListener(this);
        this.tracker = new BundleTracker<>(this.context, Bundle.ACTIVE | Bundle.STARTING, this);
        doStart();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopping = true;
        while (!extensions.isEmpty()) {
            Collection<Bundle> toDestroy = chooseBundlesToDestroy(extensions.keySet());
            if (toDestroy == null || toDestroy.isEmpty()) {
                toDestroy = new ArrayList<>(extensions.keySet());
            }
            for (Bundle bundle : toDestroy) {
                destroyExtension(bundle);
            }
        }
        doStop();
        stopped = true;
    }

    protected void doStart() throws Exception {
        startTracking();
    }

    protected void doStop() throws Exception {
        stopTracking();
    }

    protected void startTracking() {
        this.tracker.open();
    }

    protected void stopTracking() {
        this.tracker.close();
    }

    /**
     * Create the executor used to start extensions asynchronously.
     *
     * @return an
     */
    protected ExecutorService createExecutor() {
        return Executors.newScheduledThreadPool(3);
    }

    protected Collection<Bundle> chooseBundlesToDestroy(Set<Bundle> bundles) {
        return null;
    }


    @Override
    public void bundleChanged(BundleEvent event) {
        if (stopped) {
            return;
        }
        Bundle bundle = event.getBundle();
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the context.  Ignore our own bundle since it
            // needs to kick the orderly shutdown.
            if (bundle != this.context.getBundle()) {
                destroyExtension(bundle);
            }
        }
    }

    @Override
    public Bundle addingBundle(Bundle bundle, BundleEvent event) {
        modifiedBundle(bundle, event, bundle);
        return bundle;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        if (bundle.getState() != Bundle.ACTIVE && bundle.getState() != Bundle.STARTING) {
            // The bundle is not in STARTING or ACTIVE state anymore
            // so destroy the context.  Ignore our own bundle since it
            // needs to kick the orderly shutdown and not unregister the namespaces.
            if (bundle != this.context.getBundle()) {
                destroyExtension(bundle);
            }
            return;
        }
        // Do not track bundles given we are stopping
        if (stopping) {
            return;
        }
        // For starting bundles, ensure, it's a lazy activation,
        // else we'll wait for the bundle to become ACTIVE
        if (bundle.getState() == Bundle.STARTING) {
            String activationPolicyHeader = bundle.getHeaders("").get(Constants.BUNDLE_ACTIVATIONPOLICY);
            if (activationPolicyHeader == null
                || !activationPolicyHeader.startsWith(Constants.ACTIVATION_LAZY)
                || !bundle.adapt(BundleStartLevel.class).isActivationPolicyUsed()) {
                // Do not track this bundle yet
                return;
            }
        }
        createExtension(bundle);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, Bundle object) {
        // Nothing to do
        destroyExtension(bundle);
    }

    private void createExtension(final Bundle bundle) {
        try {
            BundleContext bundleContext = bundle.getBundleContext();
            if (bundleContext == null) {
                // The bundle has been stopped in the mean time
                return;
            }
            final Activator.ScrExtension extension = doCreateExtension(bundle);
            if (extension == null) {
                // This bundle is not to be extended
                return;
            }
            synchronized (extensions) {
                if (extensions.putIfAbsent(bundle, extension) != null) {
                    return;
                }
            }
            debug(bundle, "Starting extension synchronously");
            extension.start();
        } catch (Throwable t) {
            warn(bundle, "Error while creating extension", t);
        }
    }

    private void destroyExtension(final Bundle bundle) {
        FutureTask<Void> future;
        synchronized (extensions) {
            debug(bundle, "Starting destruction process");
            future = destroying.get(bundle);
            if (future == null) {
                final Activator.ScrExtension extension = extensions.remove(bundle);
                if (extension != null) {
                    debug(bundle, "Scheduling extension destruction");
                    future = new FutureTask<>(new Runnable() {
                        @Override
                        public void run() {
                            debug(bundle, "Destroying extension");
                            try {
                                extension.destroy();
                            } catch (Exception e) {
                                warn(bundle, "Error while destroying extension", e);
                            } finally {
                                debug(bundle, "Finished destroying extension");
                                synchronized (extensions) {
                                    destroying.remove(bundle);
                                }
                            }
                        }
                    }, null);
                    destroying.put(bundle, future);
                } else {
                    debug(bundle, "Not an extended bundle or destruction of extension already finished");
                }
            } else {
                debug(bundle, "Destruction already scheduled");
            }
        }
        if (future != null) {
            try {
                debug(bundle, "Waiting for extension destruction");
                future.run();
                future.get();
            } catch (Throwable t) {
                warn(bundle, "Error while destroying extension", t);
            }
        }
    }

    /**
     * Create the extension for the given bundle, or null if the bundle is not to be extended.
     *
     * @param bundle the bundle to extend
     * @return The extension
     * @throws Exception If something goes wrong
     */
    protected abstract Activator.ScrExtension doCreateExtension(Bundle bundle) throws Exception;

    protected abstract void debug(Bundle bundle, String msg);
    protected abstract void warn(Bundle bundle, String msg, Throwable t);

}
