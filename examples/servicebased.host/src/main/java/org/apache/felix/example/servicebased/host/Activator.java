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
package org.apache.felix.example.servicebased.host;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * The activator of the host application bundle. The activator creates the
 * main application <tt>JFrame</tt> and starts tracking <tt>SimpleShape</tt>
 * services. All activity is performed on the Swing event thread to avoid
 * synchronization and repainting issues. Closing the application window
 * will result in <tt>Bundle.stop()</tt> being called on the system bundle,
 * which will cause the framework to shutdown and the JVM to exit.
**/
public class Activator implements BundleActivator
{
    private DrawingFrame m_frame = null;
    private ShapeTracker m_shapetracker = null;

    /**
     * Displays the applications window and starts service tracking;
     * everything is done on the Swing event thread to avoid synchronization
     * and repainting issues.
     * @param context The context of the bundle.
    **/
    @Override
    public void start(final BundleContext context)
    {
        SwingUtilities.invokeLater(new Runnable() {
            // This creates of the application window.
            @Override
            public void run()
            {
                m_frame = new DrawingFrame();
                m_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                m_frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent evt)
                    {
                        try
                        {
                            context.getBundle(0).stop();
                        }
                        catch (BundleException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                });

                m_frame.setVisible(true);

                m_shapetracker = new ShapeTracker(context, m_frame);
                m_shapetracker.open();
            }
        });
    }

    /**
     * Stops service tracking and disposes of the application window.
     * @param context The context of the bundle.
    **/
    @Override
    public void stop(BundleContext context)
    {
        Runnable runner = new Runnable() {
            // This disposes of the application window.
            @Override
            public void run()
            {
                m_shapetracker.close();
                m_frame.setVisible(false);
                m_frame.dispose();
            }
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            runner.run();
        }
        else
        {
            try
            {
                SwingUtilities.invokeAndWait(runner);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}