package org.apache.felix.example.extenderbased.host;

import java.util.Dictionary;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.apache.felix.example.extenderbased.host.extension.SimpleShape;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;

/**
 * This is a simple bundle tracker utility class that tracks active
 * bundles. The tracker must be given a bundle context upon creation,
 * which it uses to listen for bundle events. The bundle tracker must be
 * opened to track objects and closed when it is no longer needed.
 *
 * @see BundleTracker
**/
public class ShapeBundleTracker extends BundleTracker<SimpleShape>
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
    public ShapeBundleTracker(BundleContext context, DrawingFrame frame)
    {
        // we only want to track active bundles
        super(context, Bundle.ACTIVE, null);
        this.m_frame = frame;
    }

    // Gets called when a bundle in enters the state ACTIVE
    @Override
    public SimpleShape addingBundle(Bundle bundle, BundleEvent event)
    {
        // Try to get the name of the extension.
        Dictionary<String, String> dict = bundle.getHeaders();
        String name = dict.get(SimpleShape.NAME_PROPERTY);

        // if the name is not null, bundle is a ShapeBundle
        if (name != null)
        {
            // Get the icon resource of the extension.
            String iconPath = dict.get(SimpleShape.ICON_PROPERTY);
            Icon icon = new ImageIcon(bundle.getResource(iconPath));
            // Get the class of the extension.
            String className = dict.get(SimpleShape.CLASS_PROPERTY);
            SimpleShape shape = new DefaultShape(bundle.getBundleContext(),
                bundle.getBundleId(), className);
            processAdd(name, icon, shape);
            return shape;
        }

        // bundle is no ShapeBundle, ingore it
        return null;
    }

    /**
     * Util method to process the addition of a new shape.
     *
     * @param name The name of the new shape
     * @param icon The icon of the new shape
     * @param shape the shape itself
     */
    private void processAdd(final String name, final Icon icon, final SimpleShape shape)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                m_frame.addShape(name, icon, shape);
            }
            else
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        m_frame.addShape(name, icon, shape);
                    }
                });
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // Gets called when a bundle leaves the ACTIVE state
    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, SimpleShape object)
    {
        // Try to get the name of the extension.
        Dictionary<String, String> dict = bundle.getHeaders();
        String name = dict.get(SimpleShape.NAME_PROPERTY);

        // if the name is not null, bundle is a ShapeBundle
        if (name != null)
        {
            prcoessRemove(name);
        }
    }

    /**
     * Util method to process the removal of a shape.
     *
     * @param name the name of the shape that is about to be removed.
     */
    private void prcoessRemove(final String name)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                m_frame.removeShape(name);
            }
            else
            {
                SwingUtilities.invokeAndWait(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        m_frame.removeShape(name);
                    }
                });
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}