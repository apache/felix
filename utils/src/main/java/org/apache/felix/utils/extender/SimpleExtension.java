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
package org.apache.felix.utils.extender;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SimpleExtension implements Extension {

    private final Bundle bundle;
    private final BundleContext bundleContext;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public SimpleExtension(Bundle bundle) {
        this.bundle = bundle;
        this.bundleContext = bundle.getBundleContext();
    }

    public boolean isDestroyed() {
        synchronized (getLock()) {
            return destroyed.get();
        }
    }

    public void start() throws Exception {
        synchronized (getLock()) {
            if (destroyed.get()) {
                return;
            }
            if (bundle.getState() != Bundle.ACTIVE) {
                return;
            }
            if (bundle.getBundleContext() != bundleContext) {
                return;
            }
            doStart();
        }
    }

    public void destroy() throws Exception {
        synchronized (getLock()) {
            destroyed.set(true);
        }
        doDestroy();
    }

    protected Object getLock() {
        return this;
    }

    protected abstract void doStart() throws Exception;

    protected abstract void doDestroy() throws Exception;

}
