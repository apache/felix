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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.executor.CombinedExecutionResult;
import org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor;
import org.apache.felix.hc.core.impl.servlet.ResultTxtVerboseSerializer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Returns a 503 Service Unavailable Page if certain tags are in non-ok result. */
@Component(service = {} /* Filter registers itself for better control */, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ServiceUnavailableFilter.Config.class, factory = true)
public class ServiceUnavailableFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceUnavailableFilter.class);

    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final String CONTENT_TYPE_PLAIN = "text/plain";
    
    private static final String CACHE_CONTROL_KEY = "Cache-control";
    private static final String CACHE_CONTROL_VALUE = "no-cache";

    private static final String CLASSPATH_PREFIX = "classpath:";
    
    private static final String CONTEXT_NAME = "internal.http.serviceunavailablefilter";

    private static final String PROP_STARTUP_CONTEXT_SERVICE_RANKING = "avoid404DuringStartup.contextServiceRanking";
    private static final String PROP_STARTUP_SERVLET_SERVICE_RANKING = "avoid404DuringStartup.servletServiceRanking";


    @ObjectClassDefinition(name = "Health Check Service Unavailable Filter", description = "Returns a 503 Service Unavailable Page if configured tags are in non-ok result")
    public @interface Config {

        String RESPONSE_TEXT_DEFAULT = "<html><head><title>Service Unavailable</title><meta http-equiv=\"refresh\" content=\"5\"></head><body><strong>Service Unavailable</strong></body></html>";

        @AttributeDefinition(name = "Filter Request Path RegEx", description = "Regex to be matched against request path. Either use regex or pattern.")
        String osgi_http_whiteboard_filter_regex();

        @AttributeDefinition(name = "Filter Context", description = "Needs to be set to correct whiteboard context filter (e.g. '(osgi.http.whiteboard.context.name=default)'")
        String osgi_http_whiteboard_context_select() default "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)";

        @AttributeDefinition(name = "Tags", description = "List of tags to query the status in order to decide if it is 503 or not")
        String[] tags() default {};

        @AttributeDefinition(name = "Status for 503 response", description = "First status that causes a 503 response. The default TEMPORARILY_UNAVAILABLE will not send 503 for OK and WARN but for TEMPORARILY_UNAVAILABLE, CRITICAL and HEALTH_CHECK_ERROR")
        Result.Status statusFor503() default Result.Status.TEMPORARILY_UNAVAILABLE;

        @AttributeDefinition(name = "Include execution result in response", description = "Will include the execution result in the response (as html comment for html case, otherwise as text).")
        boolean includeExecutionResult() default true;

        @AttributeDefinition(name = "503 Response Text", description = "Response text for 503 responses. Value can be either the content directly or in the format '"+CLASSPATH_PREFIX+"<symbolic-bundle-id>:/path/to/file.html'. The response content type is auto-detected to either text/html or text/plain.")
        String responseTextFor503() default RESPONSE_TEXT_DEFAULT;

        @AttributeDefinition(name = "Auto-disable filter", description = "If true, will automatically disable the filter once the filter continued the filter chain without 503 for the first time. Useful for server startup scenarios.")
        boolean autoDisableFilter() default false;

        @AttributeDefinition(name = "Avoid 404", description = "If true, will automatically register a dummy servlet to ensure this filter becomes effective. Useful for server startup scenarios.")
        boolean avoid404DuringStartup() default false;

        @AttributeDefinition(name = "Filter Service Ranking", description = "The service.ranking for the filter as respected by http whiteboard.")
        int service_ranking() default Integer.MAX_VALUE;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Send 503 for tags {tags} at status {statusFor503} (and worse) for path(s) {osgi.http.whiteboard.filter.regex}";
    }

    private String[] tags;
    private Result.Status statusFor503;
    private String responseTextFor503;
    private boolean includeExecutionResultInResponse;
    private boolean autoDisableFilter;
    private boolean avoid404DuringStartup;
    
    @Reference
    private ExtendedHealthCheckExecutor executor;

    @Reference
    ResultTxtVerboseSerializer verboseTxtSerializer;

    private BundleContext bundleContext;
    private Dictionary<String, Object> compProperties;
    private ServiceListener healthCheckServiceListener;

    private volatile ServiceRegistration<Filter> filterServiceRegistration;
    private volatile ServiceRegistration<ServletContextHelper> httpContextRegistration;
    private volatile ServiceRegistration<Servlet> defaultServletRegistration;

    private volatile ServiceReference<HealthCheck>[] relevantHealthCheckServiceReferences;

    @Activate
    protected final void activate(BundleContext bundleContext, ComponentContext componentContext, Config config)
            throws InvalidSyntaxException {
        this.bundleContext = bundleContext;
        this.compProperties = componentContext.getProperties();

        this.tags = config.tags();
        this.statusFor503 = config.statusFor503();
        this.responseTextFor503 = getResponseText(bundleContext, config.responseTextFor503());
        
        this.includeExecutionResultInResponse = config.includeExecutionResult();
        this.autoDisableFilter = config.autoDisableFilter();
        this.avoid404DuringStartup = config.avoid404DuringStartup();
        
        registerHealthCheckServiceListener();

        selectHcServiceReferences();
        registerFilter();
        
        LOG.info("ServiceUnavailableFilter active (start level {})",  getCurrentStartLevel());
    }

    @Deactivate
    protected final void deactivate() {
        unregisterHealthCheckServiceListener();

        unregisterFilter();

        LOG.info("ServiceUnavailableFilter deactivated");
    }


    String getResponseText(BundleContext bundleContext, String responseFor503) {
        if(StringUtils.isBlank(responseFor503)) {
            responseFor503 = (String) compProperties.get("htmlFor503"); // backwards-compatibility
        }
        if(StringUtils.startsWith(responseFor503, CLASSPATH_PREFIX)) {
            String[] bits = responseFor503.split(":");
            String symbolicName = bits[1];
            String pathInBundle = bits[2];
            Optional<Bundle> bundleOptional = Arrays.stream(bundleContext.getBundles()).filter(b -> b.getSymbolicName().equals(symbolicName)).findFirst();
            if(bundleOptional.isPresent()) {
                URL entryUrl = bundleOptional.get().getEntry(pathInBundle);
                if(entryUrl!=null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(entryUrl.openStream(), StandardCharsets.UTF_8))) {
                        responseFor503 = reader.lines().collect(Collectors.joining("\n"));
                    } catch (Exception e) {
                        responseFor503 = "503 Service Unavailable\n(Could not read '"+pathInBundle+"' from bundle '"+symbolicName+"': "+e+")";
                    }
                } else {
                    responseFor503 = "503 Service Unavailable\n(Could not read '"+pathInBundle+"' from bundle '"+symbolicName+"': file not found)";
                }
            } else { 
                responseFor503 = "503 Service Unavailable\n(Could not read '"+pathInBundle+"' from bundle '"+symbolicName+"': bundle not found)";
            }
        }
        return responseFor503;
    }

    private int getCurrentStartLevel() {
        return bundleContext.getBundle(Constants.SYSTEM_BUNDLE_ID).adapt(FrameworkStartLevel.class).getStartLevel();
    }
    
    private synchronized void registerFilter() {
        if (filterServiceRegistration == null) {

            if (avoid404DuringStartup) {
                registerHttpContext();
            }

            filterServiceRegistration = bundleContext.registerService(Filter.class, this, compProperties);
            LOG.debug("Registered ServiceUnavailableFilter for tags {}", Arrays.asList(tags));

            if (autoDisableFilter) {
                new UnregisteringFilterThread();
                LOG.debug("Started UnregisteringFilterThread since autoDisableFilter=true");
            }
        }
    }

    private synchronized void unregisterFilter() {
        if (filterServiceRegistration != null) {

            filterServiceRegistration.unregister();
            filterServiceRegistration = null;
            LOG.debug("Filter ServiceUnavailableFilter for tags {} unregistered", Arrays.asList(tags));

            if (avoid404DuringStartup) {
                unregisterHttpContext();
            }

        }
    }

    // using ServiceListener and ExtendedHealthCheckExecutor to avoid overhead of searching the service references on every request
    private synchronized void selectHcServiceReferences() {
        LOG.debug("Reloading HC references for tags {}", Arrays.asList(tags));

        relevantHealthCheckServiceReferences = executor.selectHealthCheckReferences(HealthCheckSelector.tags(tags), new HealthCheckExecutionOptions().setCombineTagsWithOr(true));
        LOG.debug("Found {} health check service references for tags {}", relevantHealthCheckServiceReferences.length, tags);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        CombinedExecutionResult combinedExecutionResult = executeRelevantChecks(false);
        if (is503Result(combinedExecutionResult)) {
            Result overallResult = combinedExecutionResult.getHealthCheckResult();
            LOG.debug("Result for tags {} is {}, sending 503 for {}", tags, overallResult.getStatus(),
                    ((HttpServletRequest) request).getRequestURI());
            String verboseTxtResult = includeExecutionResultInResponse
                    ? verboseTxtSerializer.serialize(overallResult, combinedExecutionResult.getExecutionResults(), false)
                    : null;
            send503((HttpServletResponse) response, verboseTxtResult);

        } else {
            // regular request processing
            filterChain.doFilter(request, response);
        }
    }

    private boolean is503Result(CombinedExecutionResult combinedExecutionResult) {
        return combinedExecutionResult.getHealthCheckResult().getStatus().ordinal() >= statusFor503.ordinal();
    }

    private CombinedExecutionResult executeRelevantChecks(boolean forceInstantExecution) {
        final long startTimeNs = System.nanoTime();

        List<HealthCheckExecutionResult> executionResults = executor.execute(relevantHealthCheckServiceReferences,
                new HealthCheckExecutionOptions().setCombineTagsWithOr(true).setForceInstantExecution(forceInstantExecution));
        CombinedExecutionResult combinedExecutionResult = new CombinedExecutionResult(executionResults, Result.Status.TEMPORARILY_UNAVAILABLE);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Time consumed for executing checks: {}ns", System.nanoTime() - startTimeNs);
        }
        return combinedExecutionResult;
    }

    private void send503(HttpServletResponse response, String verboseTxtResult) throws IOException {

        if(avoid404DuringStartup && LOG.isDebugEnabled()) {
            LOG.debug("Sending 503 at start level {}", getCurrentStartLevel());
        }

        String responseContent = responseTextFor503;
        boolean isHtml = responseContent.contains("<html");
        String bodyClosingTag = "</body>";
        
        if(verboseTxtResult != null) {
            if(isHtml) {
                responseContent = responseContent.replace(bodyClosingTag, "<!--\n\n" + verboseTxtResult + "\n\n-->" + bodyClosingTag);
            } else {
                responseContent += "\n" + verboseTxtResult;
            }
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(isHtml ? CONTENT_TYPE_HTML : CONTENT_TYPE_PLAIN);
        response.setHeader(CACHE_CONTROL_KEY, CACHE_CONTROL_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().append(responseContent);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no action required
    }

    @Override
    public void destroy() {
        // no action required
    }


    // --- HealthCheckServiceListener (to ensure current set of HC service references is up to date

    private synchronized void registerHealthCheckServiceListener() throws InvalidSyntaxException {
        if (healthCheckServiceListener == null) {
            healthCheckServiceListener = new HealthCheckServiceListener();
            bundleContext.addServiceListener(healthCheckServiceListener, "(objectclass=" + HealthCheck.class.getName() + ")");
        }
    }
    
    private synchronized void unregisterHealthCheckServiceListener() {
        if (healthCheckServiceListener != null) {
            bundleContext.removeServiceListener(healthCheckServiceListener);
        }
    }

    private final class HealthCheckServiceListener implements ServiceListener {
        @Override
        public void serviceChanged(ServiceEvent event) {
            
            LOG.debug("Service Event for Health Check: {}", event.getType());
            selectHcServiceReferences();
        }
    }

    // --- http context for avoid404DuringStartup=true

    private void registerHttpContext() {
        
        final Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, CONTEXT_NAME);
        properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, "/");
        Object contextServiceRanking = compProperties.get(PROP_STARTUP_CONTEXT_SERVICE_RANKING);
        properties.put(Constants.SERVICE_RANKING, contextServiceRanking!=null ? contextServiceRanking : Integer.MAX_VALUE);

        this.httpContextRegistration = bundleContext.registerService(ServletContextHelper.class, new ServletContextHelper() {
        }, properties);

        final Dictionary<String, Object> servletProps = new Hashtable<>();
        servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")");
        servletProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/");
        Object servletServiceRanking = compProperties.get(PROP_STARTUP_SERVLET_SERVICE_RANKING);
        servletProps.put(Constants.SERVICE_RANKING, servletServiceRanking!=null ? servletServiceRanking : 0);

        this.defaultServletRegistration = bundleContext.registerService(Servlet.class, new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                send503((HttpServletResponse) resp, "Response from dynamic startup servlet");
            }
            
        }, servletProps);
    }

    private void unregisterHttpContext() {
        if (this.defaultServletRegistration != null) {
            this.defaultServletRegistration.unregister();
            this.defaultServletRegistration = null;
        }
        if (this.httpContextRegistration != null) {
            this.httpContextRegistration.unregister();
            this.httpContextRegistration = null;
        }
    }

    // --- handling for autoDisableFilter=true

    public class UnregisteringFilterThread extends Thread {
        
        UnregisteringFilterThread() {
            setDaemon(true);
            setName("UnregisteringFilterThread for ServiceUnavailableFilter with tags "+Arrays.asList(tags));
            start();
        }

        @Override
        public void run() {

            while(autoDisableFilter && filterServiceRegistration != null) {
                CombinedExecutionResult executionResult = executeRelevantChecks(true);
                boolean is503Result = is503Result(executionResult);
                
                if(!is503Result) {
                    unregisterFilter();
                    int startLevelWhenFilterUnregistered = getCurrentStartLevel();
                    LOG.debug("Unregistered filter ServiceUnavailableFilter for tags {} since result was ok at start level {}", Arrays.asList(tags), startLevelWhenFilterUnregistered);
                    
                    new BundleListenerForReregisteringFilter(bundleContext, startLevelWhenFilterUnregistered);
                    
                    break;
                }
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOG.debug("UnregisteringFilterThread for tags {} was interrupted", Arrays.asList(tags));
                    break;
                }
                
            }
        }
    }
    
    private final class BundleListenerForReregisteringFilter implements BundleListener {
        private final Logger LOG = LoggerFactory.getLogger(getClass());

        private final int startLevelWhenFilterUnregistered;
  
        public BundleListenerForReregisteringFilter(BundleContext bundleContext, int startLevelWhenFilterUnregistered) {
            this.startLevelWhenFilterUnregistered = startLevelWhenFilterUnregistered;
            LOG.debug("BundleListenerForReregisteringFilter registered");
            bundleContext.addBundleListener(this);
        }

        @Override
        public void bundleChanged(BundleEvent event) {
            int currentStartLevel = getCurrentStartLevel();
            if(currentStartLevel != startLevelWhenFilterUnregistered) {
                LOG.debug("Start level changed (current={} previous={}) - reregistering filter", currentStartLevel, startLevelWhenFilterUnregistered);
                registerFilter();
                bundleContext.removeBundleListener(this);
                LOG.debug("Removed self from BundleListeners");
            }
        }
    }

}
