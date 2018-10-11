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
package org.apache.felix.systemready.osgi.util;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.cm.ConfigurationAdminOptions.newConfiguration;

import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.apache.felix.systemready.StateType;
import org.apache.felix.systemready.SystemReadyMonitor;
import org.apache.felix.systemready.impl.ComponentsCheck;
import org.apache.felix.systemready.impl.FrameworkStartCheck;
import org.apache.felix.systemready.impl.ServicesCheck;
import org.apache.felix.systemready.impl.servlet.SystemAliveServlet;
import org.apache.felix.systemready.impl.servlet.SystemReadyServlet;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.cm.ConfigurationAdminOptions;
import org.ops4j.pax.exam.options.OptionalCompositeOption;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BaseTest {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Inject
    public BundleContext context;

    @Inject
    public ServiceComponentRuntime scr;

    

    public Option baseConfiguration() {
        return CoreOptions.composite(
        		
                systemProperty("pax.exam.invoker").value("junit"),
                systemProperty("pax.exam.osgi.unresolved.fail").value("true"),
                systemProperty("logback.configurationFile")
                    .value("src/test/resources/logback.xml"),
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-api").version("1.7.6"),
                mavenBundle().groupId("ch.qos.logback").artifactId("logback-core").version("1.0.13"),
                mavenBundle().groupId("ch.qos.logback").artifactId("logback-classic").version("1.0.13"),
                
                bundle("link:classpath:META-INF/links/org.ops4j.pax.tipi.junit.link"),
                bundle("link:classpath:META-INF/links/org.ops4j.pax.exam.invoker.junit.link"),
                mavenBundle().groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.hamcrest").version("1.3_1"),
                mavenBundle().groupId("org.awaitility").artifactId("awaitility").version("3.1.0"),

                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.scr").version("2.0.14"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.8.16"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.rootcause").version("0.1.0-SNAPSHOT"),
                bundle("reference:file:target/classes/")

        );
    }
    
    protected static OptionalCompositeOption localRepo() {
        String localRepo = System.getProperty("maven.repo.local");
        if (localRepo == null) {
            localRepo = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        }
        return when(localRepo != null)
            .useOptions(vmOption("-Dorg.ops4j.pax.url.mvn.localRepository=" + localRepo));
    }
    
    public Option servicesCheckConfig(StateType type, String... services) {
        return ConfigurationAdminOptions.factoryConfiguration(ServicesCheck.PID)
                .put("services.list", services)
                .put("type", type.name())
                .asOption();
    }
    
    public Option componentsCheckConfig(String... components) {
        return newConfiguration(ComponentsCheck.PID)
                .put("components.list", components)
                .asOption();
    }
    
    public Option monitorConfig() {
        return newConfiguration(SystemReadyMonitor.PID)
                .put("poll.interval", 100)
                .asOption();
    }
    
    public Option httpService() {
        return CoreOptions.composite(
                mavenBundle("org.apache.felix", "org.apache.felix.http.servlet-api", "1.1.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.http.jetty", "3.4.8")
                );
    }

    public Option readyServletConfig(String path) {
        return newConfiguration(SystemReadyServlet.PID)
                .put("osgi.http.whiteboard.servlet.pattern", path)
                .asOption();
    }
    
    public Option aliveServletConfig(String path) {
        return newConfiguration(SystemAliveServlet.PID)
                .put("osgi.http.whiteboard.servlet.pattern", path)
                .asOption();
    }

    public ComponentDescriptionDTO getComponentDesc(String compName) {
        return getComponentDesc(desc -> desc.name.equals(compName), compName);
    }

    public ComponentDescriptionDTO getComponentDesc(Class<?> compClass) {
        return getComponentDesc(desc -> desc.implementationClass.equals(compClass.getName()), compClass.getName());
    }

    public ComponentDescriptionDTO getComponentDesc(Predicate<ComponentDescriptionDTO> predicate, String label) {
        Optional<ComponentDescriptionDTO> result = scr.getComponentDescriptionDTOs().stream()
                .filter(predicate)
                .findFirst();
        if (result.isPresent()) {
            return result.get();
        } else {
            throw new RuntimeException("Component " + label + " not found");
        }
    }

    public void disableComponent(String name) {
        ComponentDescriptionDTO desc = getComponentDesc(name);
        log.info("Deactivating component {}", desc.name);
        Promise<Void> promise = scr.disableComponent(desc);
        try {
            promise.getValue();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void disableFrameworkStartCheck() {
        disableComponent(FrameworkStartCheck.PID);
    }

}
