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
package org.apache.felix.example.extenderbased.host;

import java.util.Dictionary;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.apache.felix.example.extenderbased.host.extension.SimpleShape;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Extends the <tt>BundleTracker</tt> to create a tracker for
 * <tt>SimpleShape</tt> extensions. The tracker is responsible for
 * listening for <tt>SimpleShape</tt> extensions and informing the
 * application about the availability of shapes. This tracker forces
 * all notifications to be processed on the Swing event thread to
 * avoid synchronization and redraw issues.
**/
public class ShapeTracker extends BundleTracker
{
    // The application object to notify.
    private final DrawingFrame m_frame;

    /**
     * Constructs a tracker that uses the specified bundle context to
     * track extensions and notifies the specified application object about
     * changes.
     * @param context The bundle context to be used by the tracker.
     * @param frame The application object to notify about extension changes.
    **/
    public ShapeTracker(BundleContext context, DrawingFrame frame)
    {
        super(context);
        m_frame = frame;
    }

    /**
     * Overrides the <tt>BundleTracker</tt> functionality to inform
     * the application object about the added extensions.
     * @param bundle The activated bundle.
    **/
    @Override
    protected void addedBundle(Bundle bundle)
    {
        processBundleOnEventThread(ShapeEvent.ADDED, bundle);
    }

    /**
     * Overrides the <tt>BundleTracker</tt> functionality to inform
     * the application object about removed extensions.
     * @param bundle The inactivated bundle.
    **/
    @Override
    protected void removedBundle(Bundle bundle)
    {
        processBundleOnEventThread(ShapeEvent.REMOVED, bundle);
    }

    /**
     * Processes a received bundle notification from the <tt>BundleTracker</tt>,
     * forcing the processing of the notification onto the Swing event thread
     * if it is not already on it.
     * @param event The type of action associated with the notification.
     * @param bundle The bundle of the corresponding extension.
    **/
    private void processBundleOnEventThread(ShapeEvent event, Bundle bundle)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                processBundle(event, bundle);
            }
            else
            {
                SwingUtilities.invokeAndWait(new BundleRunnable(event, bundle));
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Actually performs the processing of the bundle notification. Invokes
     * the appropriate callback method on the application object depending on
     * the action type of the notification.
     * @param event The type of action associated with the notification.
     * @param bundle The bundle of the corresponding extension.
    **/
    private void processBundle(ShapeEvent event, Bundle bundle)
    {
        // see http://www.osgi.org/javadoc/r4v43/org/osgi/framework/Bundle.html#getHeaders()
        @SuppressWarnings("unchecked")
        Dictionary<String, String> dict = bundle.getHeaders();

        // Try to get the name of the extension.
        String name = dict.get(SimpleShape.NAME_PROPERTY);
        // Return immediately if the bundle is not an extension.
        if (name == null)
        {
            return;
        }

        switch (event)
        {
            case ADDED:
                // Get the icon resource of the extension.
                String iconPath = dict.get(SimpleShape.ICON_PROPERTY);
                Icon icon = new ImageIcon(bundle.getResource(iconPath));
                // Get the class of the extension.
                String className = dict.get(SimpleShape.CLASS_PROPERTY);
                m_frame.addShape(
                    name,
                    icon,
                    new DefaultShape(m_context, bundle.getBundleId(), className));
                break;

            case REMOVED:
                m_frame.removeShape(name);
                break;
        }
    }

    /**
     * Simple class used to process bundle notification handling on the
     * Swing event thread.
    **/
    private class BundleRunnable implements Runnable
    {
        private final ShapeEvent m_event;
        private final Bundle m_bundle;

        /**
         * Constructs an object with the specified action and bundle
         * object for processing on the Swing event thread.
         * @param event The type of action associated with the notification.
         * @param bundle The bundle of the corresponding extension.
        **/
        public BundleRunnable(ShapeEvent event, Bundle bundle)
        {
            m_event = event;
            m_bundle = bundle;
        }

        /**
         * Calls the <tt>processBundle()</tt> method.
        **/
        public void run()
        {
            processBundle(m_event, m_bundle);
        }
    }

    private static enum ShapeEvent
    {
        ADDED,
        REMOVED
    }
}