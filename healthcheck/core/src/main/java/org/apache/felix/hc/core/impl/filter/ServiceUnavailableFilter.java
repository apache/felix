/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.core.impl.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.executor.CombinedExecutionResult;
import org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor;
import org.apache.felix.hc.core.impl.servlet.ResultTxtVerboseSerializer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Returns a 503 Service Unavailable Page if certain tags are in non-ok result. */
@Component(service= {} /* Filter registers itself for better control */, immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ServiceUnavailableFilter.Config.class, factory = true)
public class ServiceUnavailableFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceUnavailableFilter.class);

    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CACHE_CONTROL_KEY = "Cache-control";
    private static final String CACHE_CONTROL_VALUE = "no-cache";

    @ObjectClassDefinition(name = "Health Check Service Unavailable Filter", description = "Returns a 503 Service Unavailable Page if configured tags are in non-ok result")
    public @interface Config {

        String HTML_RESPONSE_DEFAULT = "<html><head><title>Service Unavailable</title><meta http-equiv=\"refresh\" content=\"5\"></head><body><strong>Service Unavailable</strong></body></html>";
        
        @AttributeDefinition(name = "Filter Request Path RegEx", description = "Regex to be matched against request path")
        String osgi_http_whiteboard_filter_regex();
        
        @AttributeDefinition(name = "Filter Context", description = "Needs to be set to correct whiteboard context filter (e.g. '(osgi.http.whiteboard.context.name=default)'")
        String osgi_http_whiteboard_context_select() default "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)";

        @AttributeDefinition(name = "Tags", description = "List of tags to query the status in order to decide if it is 503 or not")
        String[] tags() default {};

        @AttributeDefinition(name = "Status for 503 response", description = "First status that causes a 503 response. The default TEMPORARILY_UNAVAILABLE will not send 503 for OK and WARN but for TEMPORARILY_UNAVAILABLE, CRITICAL and HEALTH_CHECK_ERROR")
        Result.Status statusFor503() default Result.Status.TEMPORARILY_UNAVAILABLE;

        @AttributeDefinition(name = "Include execution result as html comment", description = "Will include the execution result in html comment.")
        boolean includeExecutionResultInHtmlComment() default true;

        @AttributeDefinition(name = "503 Html Content", description = "Html content for 503 responses")
        String htmlFor503() default HTML_RESPONSE_DEFAULT;

        @AttributeDefinition(name = "Auto-disable filter", description = "If true, will automatically disable the filter once the filter continued the filter chain without 503 for the first time. Useful for server startup scenarios.")
        boolean autoDisableFilter() default false;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Send 503 for tags {tags} at status {statusFor503} (and worse) for path(s) {osgi.http.whiteboard.filter.regex}";
    }
    
    private String[] tags;
    private Result.Status statusFor503;
    private String htmlFor503;
    private boolean includeExecutionResultInHtmlComment;
    private boolean autoDisableFilter;
    
    @Reference
    private ExtendedHealthCheckExecutor executor;
    
    @Reference
    ResultTxtVerboseSerializer verboseTxtSerializer;
    
    private BundleContext bundleContext;
    private Dictionary<String, Object> compProperties;
    private ServiceListener healthCheckServiceListener;
    private FrameworkListener frameworkListener;
    private volatile ServiceRegistration<Filter> filterServiceRegistration;
    
    private HealthCheckExecutionOptions healthCheckExecutionOptions;
    private ServiceReference<HealthCheck>[] relevantHealthCheckServiceReferences;
    
    @Activate
    protected final void activate(BundleContext bundleContext, ComponentContext componentContext, Config config) throws InvalidSyntaxException {
        this.bundleContext = bundleContext;
        this.compProperties = componentContext.getProperties();
        
        this.tags = config.tags();
        this.statusFor503 = config.statusFor503();
        this.htmlFor503 = config.htmlFor503();
        this.includeExecutionResultInHtmlComment = config.includeExecutionResultInHtmlComment();
        this.autoDisableFilter = config.autoDisableFilter();
        
        healthCheckExecutionOptions = new HealthCheckExecutionOptions().setCombineTagsWithOr(true);
        healthCheckServiceListener = new HealthCheckServiceListener();
        bundleContext.addServiceListener(healthCheckServiceListener, "(objectclass="+HealthCheck.class.getName()+")");
        
        if(autoDisableFilter) {
            frameworkListener = new ReregisteringFilterFramworkListener();
            bundleContext.addFrameworkListener(frameworkListener);
        }

        selectHcServiceReferences();
        registerFilter();
    }
    
    
    @Deactivate
    protected final void deactivate() {
        if(healthCheckServiceListener!=null) {
            bundleContext.removeServiceListener(healthCheckServiceListener);
        }
        if(frameworkListener!=null) {
            bundleContext.removeFrameworkListener(frameworkListener);
        }
        
        // unregisterFilter() last because above listeners potentially register the filter if they are still active
        unregisterFilter();
    }


    private synchronized void registerFilter() {
        if(filterServiceRegistration == null) {
            filterServiceRegistration = bundleContext.registerService(Filter.class, this, compProperties);
            LOG.debug("Registered ServiceUnavailableFilter for tags {}", Arrays.asList(tags));
        }
    }

    private synchronized void unregisterFilter() {
        if(filterServiceRegistration!=null) {
            filterServiceRegistration.unregister();
            filterServiceRegistration = null;
            LOG.debug("Filter ServiceUnavailableFilter for tags {} unregistered", Arrays.asList(tags));
        }
    }

    // using ServiceListener and ExtendedHealthCheckExecutor to avoid overhead of searching the service references on every request
    private final void selectHcServiceReferences() {
        LOG.debug("Reloading HC references for tags {}", Arrays.asList(tags));
        relevantHealthCheckServiceReferences = executor.selectHealthCheckReferences(HealthCheckSelector.tags(tags), healthCheckExecutionOptions);
        LOG.debug("Found {} health check service references for tags {}", relevantHealthCheckServiceReferences.length, tags);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        final long startTimeNs = System.nanoTime();

        List<HealthCheckExecutionResult> executionResults = executor.execute(relevantHealthCheckServiceReferences, healthCheckExecutionOptions);
        CombinedExecutionResult combinedExecutionResult = new CombinedExecutionResult(executionResults);
        Result overallResult = combinedExecutionResult.getHealthCheckResult();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Time consumed for executing checks: {}ns", System.nanoTime() - startTimeNs);
        }
        
        if(overallResult.getStatus().ordinal() >= statusFor503.ordinal()) {
            LOG.debug("Result for tags {} is {}, sending 503 for {}", tags, overallResult.getStatus(), ((HttpServletRequest)request).getRequestURI());
            String verboseTxtResult = includeExecutionResultInHtmlComment ? verboseTxtSerializer.serialize(overallResult, executionResults, false) : null;
            send503((HttpServletResponse) response, verboseTxtResult);
            
        } else {
            if(autoDisableFilter && filterServiceRegistration!=null) {
                LOG.info("Unregistering filter ServiceUnavailableFilter for tags {} since result was ok ", Arrays.asList(tags));
                unregisterFilter();
            }
            
            // regular request processing
            filterChain.doFilter(request, response);
        }
    }

    private void send503(HttpServletResponse response, String verboseTxtResult) throws IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(CONTENT_TYPE_HTML);
        response.setHeader(CACHE_CONTROL_KEY, CACHE_CONTROL_VALUE);
        response.setCharacterEncoding("UTF-8");
        String bodyClosingTag = "</body>";
        String htmlContent = verboseTxtResult!=null ? htmlFor503.replace(bodyClosingTag, "<!--\n\n" + verboseTxtResult + "\n\n-->" + bodyClosingTag) : htmlFor503;
        response.getWriter().append(htmlContent);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no action required
    }

    @Override
    public void destroy() {
        // no action required
    }

    private final class HealthCheckServiceListener implements ServiceListener {
        @Override
        public void serviceChanged(ServiceEvent event) {
            LOG.debug("Service Event for Health Check: {}", event.getType());
            selectHcServiceReferences();
            if(filterServiceRegistration==null) {
                registerFilter();
            }
        }
    }

    private final class ReregisteringFilterFramworkListener implements FrameworkListener {
        @Override
        public void frameworkEvent(FrameworkEvent event) {
            if(event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                if(filterServiceRegistration==null) {
                    registerFilter();
                }
            }
        }
    }
    
}
