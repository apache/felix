/**
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

package org.apache.felix.logback.internal;

import java.util.AbstractMap.SimpleEntry;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.bridge.SLF4JBridgeHandler;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private volatile ServiceTracker<LoggerAdmin, LRST> lat;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        lat = new ServiceTracker<LoggerAdmin, LRST>(
            bundleContext, LoggerAdmin.class, null) {

            @Override
            public LRST addingService(
                ServiceReference<LoggerAdmin> reference) {

                LoggerAdmin loggerAdmin = bundleContext.getService(reference);

                LRST lrst = new LRST(bundleContext, loggerAdmin);

                lrst.open();

                return lrst;
            }

            @Override
            public void removedService(
                ServiceReference<LoggerAdmin> reference, LRST lrst) {

                lrst.close();
            }
        };

        lat.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        lat.close();
    }

    class LRST extends ServiceTracker<LogReaderService, Pair> {

        public LRST(BundleContext context, LoggerAdmin loggerAdmin) {
            super(context, LogReaderService.class, null);

            this.loggerAdmin = loggerAdmin;
        }

        @Override
        public Pair addingService(
            ServiceReference<LogReaderService> reference) {

            LogReaderService logReaderService = context.getService(reference);

            LogbackLogListener logbackLogListener = new LogbackLogListener(loggerAdmin);

            logReaderService.addLogListener(logbackLogListener);

            return new Pair(logReaderService, logbackLogListener);
        }

        @Override
        public void removedService(
            ServiceReference<LogReaderService> reference,
            Pair pair) {

            pair.getKey().removeLogListener(pair.getValue());
        }

        private final LoggerAdmin loggerAdmin;

    }

    class Pair extends SimpleEntry<LogReaderService, LogbackLogListener> {

        private static final long serialVersionUID = 1L;

        public Pair(LogReaderService logReaderService, LogbackLogListener logbackLogListener) {
            super(logReaderService, logbackLogListener);
        }

    }

}
