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
package org.apache.felix.ipojo.online.manipulator;

import static java.lang.String.format;
import static org.apache.felix.ipojo.online.manipulator.Files.dump;
import static org.osgi.service.log.LogService.LOG_DEBUG;
import static org.osgi.service.log.LogService.LOG_WARNING;
import static org.osgi.service.url.URLConstants.URL_HANDLER_PROTOCOL;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.manipulator.ManipulationVisitor;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.CompositeMetadataProvider;
import org.apache.felix.ipojo.manipulator.metadata.FileMetadataProvider;
import org.apache.felix.ipojo.manipulator.reporter.SystemReporter;
import org.apache.felix.ipojo.manipulator.spi.ModuleProvider;
import org.apache.felix.ipojo.manipulator.Pojoization;
import org.apache.felix.ipojo.manipulator.spi.Module;
import org.apache.felix.ipojo.manipulator.spi.provider.CompositeModuleProvider;
import org.apache.felix.ipojo.manipulator.spi.provider.CoreModuleProvider;
import org.apache.felix.ipojo.manipulator.spi.provider.DefaultModuleProvider;
import org.apache.felix.ipojo.manipulator.visitor.check.CheckFieldConsistencyVisitor;
import org.apache.felix.ipojo.manipulator.visitor.writer.ManipulatedResourcesWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * iPOJO URL Handler allowing installation time manipulation.
 *
 * When a bundle is installed with the {@literal ipojo} URL prefix, the referred bundle is
 * manipulated by this handler.

 * The {@literal metadata.xml} file can either be provided:
 * <ul>
 *     <li>Inside the bundle, this handler will look for {@literal /metadata.xml} (at the root of the bundle)
 *     or in {@literal /META-INF/metadata.xml}</li>
 *     <li>Through the URL using the following URL format: {@literal ipojo:URL_BUNDLE!URL_METADATA}, notice
 *     the {@literal !} used as separator</li>
 * </ul>
 *
 * Examples of valid iPojo's URLs:
 * <pre>
 *     ipojo:file:///tmp/bundle.jar
 *     ipojo:/http://www.example.org/bundle.jar
 *     ipojo://mvn://com.acme/bundle/1.0.0  (with Maven Pax Url support installed)
 *     ipojo:file:///tmp/bundle.jar!file:///tmp2/metadata.xml
 * </pre>
 *
 * Notice that trailing {@literal '/'} (or {@literal '//'}) after {@literal ipojo:} are optional.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(publicFactory = false,
           immediate = true)
@Provides(specifications = URLStreamHandlerService.class,
          properties = @StaticServiceProperty(name = URL_HANDLER_PROTOCOL,
                                              value = "ipojo",
                                              type = "java.lang.String"))
@Instantiate
public class IPOJOURLHandler extends AbstractURLStreamHandlerService {

    public static final String IPOJO_SCHEME = "ipojo:";

    private final BundleContext m_context;

    /**
     * The directory storing manipulated bundles.
     */
    private final File m_temp;

    private List<Module> m_modules = new ArrayList<Module>();

    //@Requires(optional = true, defaultimplementation = SystemLogService.class)
    // TODO Change this once NPE is fixed
    private LogService logger = new SystemLogService();

    @Bind(optional = true, aggregate = true)
    public void bindModule(Module module) {
        m_modules.add(module);
    }

    @Unbind
    public void unbindModule(Module module) {
        m_modules.remove(module);
    }

    /**
     * Creates a IPOJOURLHandler.
     * Gets the bundle context and create the working
     * directory.
     *
     * @param bundleContext the bundle context
     */
    public IPOJOURLHandler(BundleContext bundleContext) {
        this(bundleContext, bundleContext.getDataFile("temp"));
    }

    public IPOJOURLHandler(BundleContext context, File work) {
        m_context = context;
        m_temp = work;
        if (!m_temp.exists()) {
            m_temp.mkdir();
        }
    }

    /**
     * Stops the URL handler:
     * Deletes the working directory.
     */
    @Invalidate
    public void stop() {
        File[] files = m_temp.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }
        m_temp.delete();
    }

    /**
     * Opens a connection using the ipojo url handler.
     * This methods parses the URL and manipulate the given bundle.
     *
     * @param url the url.
     * @return the URL connection on the manipulated bundle
     * @throws java.io.IOException occurs when the bundle cannot be either downloaded, or manipulated or
     *                             installed correctly.
     * @see org.osgi.service.url.AbstractURLStreamHandlerService#openConnection(java.net.URL)
     */
    public URLConnection openConnection(URL url) throws IOException {
        logger.log(LOG_DEBUG, format("Processing URL %s", url));
        String full = removeScheme(url);

        // Now full is like : URL!URL or URL
        String[] urls = full.split("!");
        URL bundleURL = null;
        URL metadataURL = null;
        if (urls.length == 1) {
            // URL form
            bundleURL = new URL(urls[0]);
        } else if (urls.length == 2) {
            // URL!URL form
            bundleURL = new URL(urls[0]);
            metadataURL = new URL(urls[1]);
        } else {
            throw new MalformedURLException("The iPOJO url is not formatted correctly, ipojo:bundle_url[!metadata_url] expected");
        }

        logger.log(LOG_DEBUG, format("Extracted URL %s", url));

        // Dump the referenced bundle on disk
        File original = File.createTempFile("original-", ".jar", m_temp);
        dump(bundleURL.openStream(), original);

        JarFile jf = new JarFile(original);

        File metadata = null;
        if (metadataURL != null) {
            metadata = File.createTempFile("ipojo-", ".xml", m_temp);
            dump(metadataURL, metadata);
        } else {
            // Check that the metadata are in the jar file
            metadata = findMetadata(jf);
        }

        Reporter reporter = new SystemReporter();
        File out = File.createTempFile("ipojo-", ".jar", m_temp);

        ResourceStore store = new BundleAwareJarFileResourceStore(jf, out, m_context);

        CompositeMetadataProvider composite = new CompositeMetadataProvider(reporter);
        if (metadata != null) {
            FileMetadataProvider provider = new FileMetadataProvider(metadata, reporter);
            composite.addMetadataProvider(provider);
        }

        ClassLoader classloader = new BridgeClassLoader(original, m_context);
        // Pojoization
        Pojoization pojoizator = new Pojoization(createModuleProvider());
        try {
            pojoizator.pojoization(store, composite, createVisitor(store, reporter), classloader);
        } catch (Exception e) {
            if (!pojoizator.getErrors().isEmpty()) {
                throw new IOException("Errors occurred during the manipulation : " + pojoizator.getErrors(), e);
            }
            e.printStackTrace();
            throw new IOException("Cannot manipulate the Url: " + url, e);
        }

        if (!pojoizator.getErrors().isEmpty()) {
            throw new IOException("Errors occurred during the manipulation : " + pojoizator.getErrors());
        }
        if (!pojoizator.getWarnings().isEmpty()) {
            logger.log(LOG_WARNING, format("Warnings occurred during the manipulation %s", pojoizator.getWarnings()));
        }

        logger.log(LOG_DEBUG, format("Manipulation done %s", out.exists()));

        // Cleanup
        if (metadata != null) {
            metadata.delete();
        }
        original.delete();
        out.deleteOnExit();

        // Returns the URL Connection
        return out.toURI().toURL().openConnection();


    }

    private String removeScheme(final URL url) {
        String full = url.toExternalForm();
        // Remove ipojo:
        if (full.startsWith(IPOJO_SCHEME)) {
            full = full.substring(IPOJO_SCHEME.length());
        }
        // Remove '/' or '//'
        while (full.startsWith("/")) {
            full = full.substring(1);
        }

        return full.trim();
    }

    private ManipulationVisitor createVisitor(ResourceStore store, Reporter reporter) {
        ManipulatedResourcesWriter writer = new ManipulatedResourcesWriter();
        writer.setReporter(reporter);
        writer.setResourceStore(store);

        CheckFieldConsistencyVisitor checkFieldConsistencyVisitor = new CheckFieldConsistencyVisitor(writer);
        checkFieldConsistencyVisitor.setReporter(reporter);
        return checkFieldConsistencyVisitor;
    }


    private ModuleProvider createModuleProvider() {
        return new CompositeModuleProvider(
                new CoreModuleProvider(),
                new DefaultModuleProvider(m_modules)
        );
    }

    /**
     * Looks for the metadata.xml file in the jar file.
     * Two locations are checked:
     * <ol>
     * <li>the root of the jar file</li>
     * <li>the META-INF directory</li>
     * </ol>
     *
     * @param jar the jar file
     * @return the found file or <code>null</code> if not found.
     * @throws java.io.IOException occurs when the Jar file cannot be read.
     */
    private File findMetadata(JarFile jar) throws IOException {
        JarEntry je = jar.getJarEntry("metadata.xml");
        if (je == null) {
            je = jar.getJarEntry("META-INF/metadata.xml");
        }

        if (je == null) {
            logger.log(LOG_DEBUG, "Metadata file not found, use annotations only.");
            return null; // Not Found, use annotation only
        } else {
            logger.log(LOG_DEBUG, format("Metadata file found at  %s", je.getName()));
            File metadata = File.createTempFile("ipojo-", ".xml", m_temp);
            dump(jar.getInputStream(je), metadata);
            logger.log(LOG_DEBUG, format("Metadata file saved at %s", metadata));
            return metadata;
        }

    }

}
