package org.apache.felix.dm.itest;

//import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartupFor;
//import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
public class FilterIndexResourceAdapterTest extends TestBase {
  public void testBasicResourceAdapter() throws Exception {
      System.setProperty("org.apache.felix.dependencymanager.filterindex", "objectClass");
      DependencyManager m = getDM();
      // helper class that ensures certain steps get executed in sequence
      Ensure e = new Ensure();
      // create a resource provider
      ResourceProvider provider = new ResourceProvider(context, new URL("file://localhost/path/to/file1.txt"));
      // activate it
      m.add(m.createComponent().setImplementation(provider).add(m.createServiceDependency().setService(ResourceHandler.class).setCallbacks("add", "remove")));
      // create a resource adapter for our single resource
      // note that we can provide an actual implementation instance here because there will be only one
      // adapter, normally you'd want to specify a Class here
      m.add(m.createResourceAdapterService("(&(path=/path/to/*.txt)(host=localhost))", false, null, "changed")
            .setImplementation(new ResourceAdapter(e)));
      // wait until the single resource is available
      e.waitForStep(3, 5000);
      // trigger a 'change' in our resource
      provider.change();
      // wait until the changed callback is invoked
      e.waitForStep(4, 5000);
      m.clear();
   }
  
  static class ResourceAdapter {
      protected URL m_resource; // injected by reflection.
      private Ensure m_ensure;
      
      ResourceAdapter(Ensure e) {
          m_ensure = e;
      }
      
      public void start() {
          m_ensure.step(1);
          Assert.assertNotNull("resource not injected", m_resource);
          m_ensure.step(2);
          try {
              @SuppressWarnings("unused")
              InputStream in = m_resource.openStream();
          } 
          catch (FileNotFoundException e) {
              m_ensure.step(3);
          }
          catch (IOException e) {
              Assert.fail("We should not have gotten this exception.");
          }
      }
      
      public void changed() {
          m_ensure.step(4);
      }
  }  
}
