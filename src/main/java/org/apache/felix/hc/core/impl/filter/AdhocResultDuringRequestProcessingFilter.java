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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.executor.CombinedExecutionResult;
import org.apache.felix.hc.core.impl.servlet.ResultTxtVerboseSerializer;
import org.apache.felix.hc.core.impl.util.AdhocStatusHealthCheck;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dynamically adds a HC result for configured requests. */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = AdhocResultDuringRequestProcessingFilter.Config.class, factory = true)
public class AdhocResultDuringRequestProcessingFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(AdhocResultDuringRequestProcessingFilter.class);


    @ObjectClassDefinition(name = "Health Check Adhoc Result during Request Processing", description = "Registers an health check with an adhoc result during request processing")
    public @interface Config {

        @AttributeDefinition(name = "Filter Request Path RegEx", description = "Regex to be matched against request path")
        String osgi_http_whiteboard_filter_regex();
        
        @AttributeDefinition(name = "Filter Context", description = "Needs to be set to correct whiteboard context filter (e.g. '(osgi.http.whiteboard.context.name=default)'")
        String osgi_http_whiteboard_context_select() default "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)";

        @AttributeDefinition(name = "Request Method", description = "Relevant request method (leave empty to not restrict to a method)")
        String method() default "";

        @AttributeDefinition(name = "User Agent RegEx", description = "Relevant user agent header (leave emtpy to not restrict to a user agent)")
        String userAgentRegEx() default "";
        
        @AttributeDefinition(name = "Health Check Name", description = "Name of health check during request processing")
        String hcName() default "Ongoing request";
        
        @AttributeDefinition(name = "Tags to register", description = "List of tags the adhoc result shall be registered for (tags are not active during configured delay in case 'delayProcessingInSec' is configured)")
        String[] tags() default {};

        @AttributeDefinition(name = "Status during request processing", description = "Status to be sent during request processing")
        Result.Status statusDuringRequestProcessing() default Result.Status.TEMPORARILY_UNAVAILABLE;

        @AttributeDefinition(name = "Delay before request processing", description = "Time to delay processing of request in sec (the default 0 turns the delay off). Use together with 'tagsDuringDelayedProcessing' advertise request processing before actual action (e.g. to signal a deployment request to a periodically querying load balancer before deployment starts)")
        long delayProcessingInSec() default 0;

        @AttributeDefinition(name = "Tags to register during delay before processing", description = "List of tags the adhoc result is be registered also during waiting for the configured delay")
        String[] tagsDuringDelayedProcessing() default {};

        @AttributeDefinition(name = "Tags to wait for after processing", description = "List of tags to be waited for after processing (leave empty to not wait). While waiting the tags from property 'tags' remain in configured state.")
        String[] waitAfterProcessing_forTags() default {};

        @AttributeDefinition(name = "Initial waiting time", description = "Initial waiting time until 'waitAfterProcessing.forTags' are checked for the first time.")
        long waitAfterProcessing_initialWait() default 3;

        @AttributeDefinition(name = "Maximum delay after processing", description = "Maximum delay that can be caused when 'waitAfterProcessing.forTags' is configured (waiting is aborted after that time)")
        long waitAfterProcessing_maxDelay() default 120;

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "{hc.name} ({osgi.http.whiteboard.filter.regex} {method} {userAgentRegEx}) -> {statusDuringRequestProcessing} for tags {tags} {tagsDuringDelayedProcessing}";
    }
    
    private BundleContext bundleContext;
    
    private Result.Status statusDuringRequestProcessing;
    private String hcNameDuringRequestProcessing;
    private String[] hcTagsDuringRequestProcessing;

    private Long delayProcessingInSec;
    private String[] tagsDuringDelayedProcessing;

    private String[] waitAfterProcessingForTags;
    private long waitAfterProcessingInitialWait;
    private long waitAfterProcessingMaxDelay;
    
    private String requiredMethod;
    private Pattern userAgentRegEx;
    
    
    @Reference 
    private HealthCheckExecutor executor;
    
    @Reference
    ResultTxtVerboseSerializer verboseTxtSerializer;
    
    @Activate
    protected final void activate(ComponentContext context, Config config) {
        this.bundleContext = context.getBundleContext();
        this.statusDuringRequestProcessing = config.statusDuringRequestProcessing();
        this.hcNameDuringRequestProcessing = config.hcName();
        this.hcTagsDuringRequestProcessing = config.tags();
        
        this.delayProcessingInSec = config.delayProcessingInSec() > 0 ? config.delayProcessingInSec(): null;
        this.tagsDuringDelayedProcessing = config.tagsDuringDelayedProcessing();
        
        this.waitAfterProcessingForTags = config.waitAfterProcessing_forTags();
        this.waitAfterProcessingInitialWait = config.waitAfterProcessing_initialWait();
        this.waitAfterProcessingMaxDelay = config.waitAfterProcessing_maxDelay();
        
        this.requiredMethod = StringUtils.defaultIfBlank(config.method(), null);
        this.userAgentRegEx = StringUtils.isNotBlank(config.userAgentRegEx()) ? Pattern.compile(config.userAgentRegEx()) : null;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestPath = httpServletRequest.getRequestURI();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        String method = httpServletRequest.getMethod();
        boolean isRelevantRequest = /* path is checked by filter pattern */
                (requiredMethod==null || requiredMethod.equals(method))
                && (userAgentRegEx==null || userAgentRegEx.matcher(userAgent).matches());
        if (isRelevantRequest) {
            processRelevantRequest(request, response, filterChain, requestPath);
            
        } else {
            // regular request processing
            filterChain.doFilter(request, response);
        }
    }

    private void processRelevantRequest(ServletRequest request, ServletResponse response, FilterChain filterChain, String requestPath)
            throws IOException, ServletException {
        
        AdhocStatusHealthCheck adhocStatusHealthCheck = null;
        try {
            
            String mainHcMsg = "Request "+requestPath+" is being processed";
            if(delayProcessingInSec!=null) {
                
                String hcNameDuringDelay = hcNameDuringRequestProcessing +" (waiting)";
                AdhocStatusHealthCheck adhocStatusHealthCheckDelayedProcessing = null;
                try {
                    adhocStatusHealthCheckDelayedProcessing = registerDynamicHealthCheck(statusDuringRequestProcessing, tagsDuringDelayedProcessing, hcNameDuringDelay, "Waiting "+delayProcessingInSec+"sec until continuing request "+requestPath);
                    
                    LOG.info("Delaying processing of request {} for {}sec", requestPath, delayProcessingInSec);
                    try {
                        Thread.sleep(delayProcessingInSec * 1000);
                    } catch (InterruptedException e) {
                        LOG.warn("Exception during delaying processing of request {} for {}sec", requestPath, delayProcessingInSec, e);
                    }
                } finally {
                    // for the case delayProcessingInSec is set, register regular HC first and then unregister delay HC to ensure there is not even a short time span without result
                    adhocStatusHealthCheck = registerDynamicHealthCheck(statusDuringRequestProcessing, hcTagsDuringRequestProcessing, hcNameDuringRequestProcessing, mainHcMsg);
                    unregisterDynamicHealthCheck(adhocStatusHealthCheckDelayedProcessing);
                }
            } else {
                adhocStatusHealthCheck = registerDynamicHealthCheck(statusDuringRequestProcessing, hcTagsDuringRequestProcessing, hcNameDuringRequestProcessing, mainHcMsg);
            }
            
            filterChain.doFilter(request, response);
            LOG.info("Request {} is processed", requestPath);
            
            if(waitAfterProcessingForTags.length > 0) {
                
                String initialWaitMsg = "Request "+requestPath +": Waiting for tags "+Arrays.asList(waitAfterProcessingForTags)+": initial wait " + waitAfterProcessingInitialWait+ "sec";
                adhocStatusHealthCheck.updateMessage(initialWaitMsg);
                wait(requestPath, waitAfterProcessingInitialWait * 1000);
                
                long startTime = System.currentTimeMillis();
                for(;;) {
                    List<HealthCheckExecutionResult> executionResults = executor.execute(HealthCheckSelector.tags(waitAfterProcessingForTags).withNames("-"+hcNameDuringRequestProcessing), new HealthCheckExecutionOptions().setCombineTagsWithOr(true).setForceInstantExecution(true));
                    CombinedExecutionResult combinedExecutionResult = new CombinedExecutionResult(executionResults);
                    Result overallResult = combinedExecutionResult.getHealthCheckResult();
                    String verboseTxtResult = verboseTxtSerializer.serialize(overallResult, executionResults, false);

                    String msg = "Request "+requestPath +": Waiting for tags "+Arrays.asList(waitAfterProcessingForTags)+": "+ overallResult.getStatus();
                    LOG.info(msg);
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("\n"+verboseTxtResult);
                    }
                    adhocStatusHealthCheck.updateMessage(msg);
                    if(overallResult.isOk()) {
                        break;
                    }
                    if((System.currentTimeMillis() - startTime) > (waitAfterProcessingMaxDelay*1000)) {
                        LOG.warn("Maximum delay time {}sec for tags {} exceeded - continuing anyway", waitAfterProcessingMaxDelay, Arrays.asList(waitAfterProcessingForTags));
                        throw new ServletException("Maximum wait time "+waitAfterProcessingMaxDelay+"sec for tags "+Arrays.asList(waitAfterProcessingForTags)+" exceeded:\n"+verboseTxtResult);
                    }
                    
                    LOG.info("Waiting for tags {} before returning from {}", waitAfterProcessingForTags, requestPath);
                    wait(requestPath, 500);
                }
            }
            
        } finally {
            unregisterDynamicHealthCheck(adhocStatusHealthCheck);
        }
    }

    private void wait(String requestPath, long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            LOG.warn("Exception during delaying processing of request {} for {}sec", requestPath, delayProcessingInSec, e);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no action required
    }

    @Override
    public void destroy() {
        // no action required
    }

    private AdhocStatusHealthCheck registerDynamicHealthCheck(Result.Status status, String[] tags, String hcName, String msg) {
        AdhocStatusHealthCheck healthCheck = new AdhocStatusHealthCheck(status, msg);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, hcName);
        props.put(HealthCheck.TAGS, tags);

        ServiceRegistration<HealthCheck> registration = bundleContext.registerService(HealthCheck.class, healthCheck, props);
        healthCheck.setServiceRegistration(registration);

        return healthCheck;
    }

    private synchronized void unregisterDynamicHealthCheck(AdhocStatusHealthCheck healthCheck) {
        ServiceRegistration<HealthCheck> serviceRegistration = healthCheck !=null ? healthCheck.getServiceRegistration() : null;
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            LOG.debug("Unregistered adhoc HC");
        }
    }


}
