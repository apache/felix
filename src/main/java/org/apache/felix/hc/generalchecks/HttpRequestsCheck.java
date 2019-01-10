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
package org.apache.felix.hc.generalchecks;

import static java.util.stream.StreamSupport.stream;
import static org.apache.felix.hc.api.FormattingResultLog.msHumanReadable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.generalchecks.util.SimpleConstraintChecker;
import org.apache.felix.utils.json.JSONParser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HealthCheckService(name = HttpRequestsCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = HttpRequestsCheck.Config.class, factory = true)
public class HttpRequestsCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRequestsCheck.class);

    public static final String HC_NAME = "Http Requests";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Performs http(s) request(s) and checks the response for return code and optionally checks the response entity")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Request Specs", description = "List of requests to be made. Requests specs have two parts: "
                + "Before '=>' can be a simple URL/path with curl-syntax advanced options (e.g. setting a header with -H \"Test: Test val\"), "
                + "after the '=>' it is a simple response code that can be followed ' && MATCHES <RegEx>' to match the response entity against or other matchers like HEADER, TIME or JSON (see defaults when creating a new configuration for examples).")
        String[] requests() default {
            "/path/example.html",
            "/path/example.html => 200",
            "/protected/example.html => 401",
            "-u admin:admin /protected/example.html => 200",
            "/path/example.html => 200 && MATCHES <title>html title.*</title>",
            "/path/example.html => 200 && MATCHES <title>html title.*</title> && MATCHES anotherRegEx[a-z]",
            "/path/example.html => 200 && HEADER Content-Type MATCHES text/html.*",
            "/path/example.json => 200 && JSON root.arr[3].prop = myval",
            "/path/example-timing-important.html => 200 && TIME < 2000",
            "-X GET -H \"Accept: application/javascript\" http://api.example.com/path/example.json => 200 && JSON root.arr[3].prop = myval",
            "-X HEAD --data \"{....}\" http://www.example.com/path/to/data.json => 303",
            "--proxy proxyhost:2000 /path/example-timing-important.html => 200 && TIME < 2000"
        };
        
        @AttributeDefinition(name = "Connect Timeout", description = "Default connect timeout in ms. Can be overwritten per request with option --connect-timeout (in sec)")
        int connectTimeoutInMs() default 7000;

        @AttributeDefinition(name = "Read Timeout", description = "Default read timeout in ms. Can be overwritten with per request option -m or --max-time (in sec)")
        int readTimeoutInMs() default 7000;
        
        @AttributeDefinition(name = "Status for failed request constraint", description = "Status to fail with if the constraint check fails")
        Result.Status statusForFailedContraint() default Result.Status.WARN;

        @AttributeDefinition(name = "Run in parallel", description = "Run requests in parallel (only active if more than one request spec is configured)")
        boolean runInParallel() default true;
        
        
        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "{hc.name}: {requests}";

    }

    private List<RequestSpec> requestSpecs;
    private int connectTimeoutInMs;
    private int readTimeoutInMs;
    private Result.Status statusForFailedContraint;
    private boolean runInParallel;
    
    private String defaultBaseUrl = null;

    private FormattingResultLog configErrors;
    
    @Activate
    protected void activate(BundleContext bundleContext, Config config) {
        this.requestSpecs = getRequestSpecs(config.requests());
        this.connectTimeoutInMs = config.connectTimeoutInMs();
        this.readTimeoutInMs = config.readTimeoutInMs();
        this.statusForFailedContraint = config.statusForFailedContraint();
        this.runInParallel = config.runInParallel() && requestSpecs.size() > 1;

        setupDefaultBaseUrl(bundleContext);
        
        LOG.info("Default BaseURL: {}", defaultBaseUrl);
        LOG.info("Activated Requests HC: {}", requestSpecs);
    }

    private void setupDefaultBaseUrl(BundleContext bundleContext) {
        ServiceReference<?> serviceReference = bundleContext.getServiceReference("org.osgi.service.http.HttpService");
        boolean isHttp = Boolean.parseBoolean(String.valueOf(serviceReference.getProperty("org.apache.felix.http.enable")));
        boolean isHttps = Boolean.parseBoolean(String.valueOf(serviceReference.getProperty("org.apache.felix.https.enable")));
        if(isHttp) {
            defaultBaseUrl = "http://localhost:"+serviceReference.getProperty("org.osgi.service.http.port");
        } else if(isHttps) {
            defaultBaseUrl = "http://localhost:"+serviceReference.getProperty("org.osgi.service.https.port");
        }
    }

    @Override
    public Result execute() {

        FormattingResultLog overallLog = new FormattingResultLog();
        
        // take over config errors
        for(ResultLog.Entry entry: configErrors) {
            overallLog.add(entry);
        }
        
        // execute requests
        Stream<RequestSpec> requestSpecsStream = runInParallel ? requestSpecs.parallelStream() : requestSpecs.stream();
        List<FormattingResultLog> logsForEachRequest = requestSpecsStream
            .map(requestSpec -> requestSpec.check(defaultBaseUrl, connectTimeoutInMs, readTimeoutInMs, statusForFailedContraint, requestSpecs.size()>1))
            .collect(Collectors.toList());
        
        // aggregate logs never in parallel
        logsForEachRequest.stream().forEach( l -> stream(l.spliterator(), false).forEach(e -> overallLog.add(e)));

        return new Result(overallLog);

    }

    private List<RequestSpec> getRequestSpecs(String[] requestSpecStrArr) {
        
        configErrors = new FormattingResultLog();
        
        List<RequestSpec> requestSpecs = new ArrayList<RequestSpec>();
        for(String requestSpecStr: requestSpecStrArr) {
            try {
                RequestSpec requestSpec = new RequestSpec(requestSpecStr);
                requestSpecs.add(requestSpec);
            } catch(Exception e) {
                configErrors.critical("Invalid config: {}", requestSpecStr);
                configErrors.add(new ResultLog.Entry(Result.Status.CRITICAL, " "+e.getMessage(), e));
            }

        }
        return requestSpecs;
    }
    
    static class RequestSpec {
        
        private static final String HEADER_AUTHORIZATION = "Authorization";
        
        String method = "GET";
        String url;
        Map<String,String> headers = new HashMap<String,String>();
        String data = null;
        
        String user;
        
        Integer connectTimeoutInMs;
        Integer readTimeoutInMs;
        
        Proxy proxy;
        
        List<ResponseCheck> responseChecks = new ArrayList<ResponseCheck>();
  
        RequestSpec(String requestSpecStr) throws ParseException, URISyntaxException {
            
            String[] requestSpecBits = requestSpecStr.split(" *=> *", 2);
            
            String requestInfo = requestSpecBits[0];
            parseCurlLikeRequestInfo(requestInfo);

            if(requestSpecBits.length > 1) {
                parseResponseAssertion(requestSpecBits[1]);
            } else {
                // check for 200 as default
                responseChecks.add(new ResponseCodeCheck(200));
            }
        }

        private void parseResponseAssertion(String responseAssertions) {
            
            String[] responseAssertionArr = responseAssertions.split(" +&& +");
            for(String clause: responseAssertionArr) {
                if(StringUtils.isNumeric(clause)) {
                    responseChecks.add(new ResponseCodeCheck(Integer.parseInt(clause)));
                } else if(StringUtils.startsWithIgnoreCase(clause, ResponseTimeCheck.TIME)) {
                    responseChecks.add(new ResponseTimeCheck(clause.substring(ResponseTimeCheck.TIME.length())));
                } else if(StringUtils.startsWithIgnoreCase(clause, ResponseEntityRegExCheck.MATCHES)) {
                    responseChecks.add(new ResponseEntityRegExCheck(Pattern.compile(clause.substring(ResponseEntityRegExCheck.MATCHES.length()))));
                } else if(StringUtils.startsWithIgnoreCase(clause, ResponseHeaderCheck.HEADER)) {
                    responseChecks.add(new ResponseHeaderCheck(clause.substring(ResponseHeaderCheck.HEADER.length())));
                } else if(StringUtils.startsWithIgnoreCase(clause, JsonPropertyCheck.JSON)) {
                    responseChecks.add(new JsonPropertyCheck(clause.substring(JsonPropertyCheck.JSON.length())));
                } else {
                    throw new IllegalArgumentException("Invalid response content assertion clause: '"+clause+"'");
                }
            }
        }

        private void parseCurlLikeRequestInfo(String requestInfo) throws ParseException, URISyntaxException {
            CommandLineParser parser = new DefaultParser();

            Options options = new Options();
            options.addOption("H", "header", true, "");
            options.addOption("X", "method", true, "");
            options.addOption("d", "data", true, "");
            options.addOption("u", "user", true, "");
            options.addOption(null, "connect-timeout", true, "");
            options.addOption("m", "max-time", true, "");
            options.addOption("x", "proxy", true, "");
            
            String[] args = splitArgsRespectingQuotes(requestInfo); 
            
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("header")) {
                String[] headerValues = line.getOptionValues("header");
                for(String headerVal: headerValues) {
                    String[] headerBits = headerVal.split(" *: *", 2);
                    headers.put(headerBits[0], headerBits[1]);
                }
            }
            if (line.hasOption("method")) {
                method = line.getOptionValue("method");
            }
            if (line.hasOption("data")) {
                data = line.getOptionValue("data");
            }
            if (line.hasOption("user")) {
                String userAndPw = line.getOptionValue("user");
                user = userAndPw.split(":")[0];
                byte[] encodedUserAndPw = Base64.getEncoder().encode(userAndPw.getBytes());
                headers.put(HEADER_AUTHORIZATION, "Basic "+new String(encodedUserAndPw));
            }
            
            if (line.hasOption("connect-timeout")) {
                connectTimeoutInMs = Integer.valueOf(line.getOptionValue("connect-timeout")) * 1000;
            }
            if (line.hasOption("max-time")) {
                readTimeoutInMs = Integer.valueOf(line.getOptionValue("max-time")) * 1000;
            }
            if (line.hasOption("proxy")) {
                String curlProxy = line.getOptionValue("proxy");
                if(curlProxy.contains("@")) {
                    throw new IllegalArgumentException("Proxy authentication is not support");
                }
                String proxyHost;
                int proxyPort;
                if(curlProxy.startsWith("http")) {
                    URI uri = new URI(curlProxy);
                    proxyHost = uri.getHost();
                    proxyPort = uri.getPort();
                } else {
                    String[] curlProxyBits = curlProxy.split(":");
                    proxyHost = curlProxyBits[0];
                    proxyPort = curlProxyBits.length > 1 ? Integer.parseInt(curlProxyBits[1]) : 1080;
                }
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            }

            url = line.getArgList().get(0);

        }

        String[] splitArgsRespectingQuotes(String requestInfo) {
            List<String> argList = new ArrayList<String>();
            Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
            Matcher regexMatcher = regex.matcher(requestInfo);
            while (regexMatcher.find()) {
                argList.add(regexMatcher.group());
            }
            return argList.toArray(new String[argList.size()]);
        }

        @Override
        public String toString() {
            return "RequestSpec [method=" + method + ", url=" + url + ", headers=" + headers + ", responseChecks=" + responseChecks + "]";
        }

        public FormattingResultLog check(String defaultBaseUrl, int connectTimeoutInMs, int readTimeoutInMs, Result.Status statusForFailedContraint, boolean showTiming) {
            
            FormattingResultLog log = new FormattingResultLog();
            String urlWithUser = user!=null ? user + " @ " + url: url;
            log.debug("Checking {}", urlWithUser);
            log.debug(" configured headers {}", headers.keySet());
            
            Response response = null;
            try {
                response = performRequest(defaultBaseUrl, urlWithUser, connectTimeoutInMs, readTimeoutInMs, log);
            } catch (IOException e) {
                // request generally failed
                log.add(new ResultLog.Entry(statusForFailedContraint, urlWithUser+": "+ e.getMessage(), e));
            }
            
            if(response != null) {
                List<String> resultBits = new ArrayList<String>();
                boolean hasFailed = false;
                for(ResponseCheck responseCheck: responseChecks) {
                    ResponseCheck.ResponseCheckResult result = responseCheck.checkResponse(response, log);
                    hasFailed = hasFailed || result.contraintFailed;
                    resultBits.add(result.message);
                }
                Result.Status status = hasFailed ? statusForFailedContraint : Result.Status.OK;
                String timing = showTiming ? " " + msHumanReadable(response.requestDurationInMs) : "";
                // result of response assertion(s)
                log.add(new ResultLog.Entry(status, urlWithUser+timing+": "+ StringUtils.join(resultBits,", ")));
            }

            return log;
        }

        public Response performRequest(String defaultBaseUrl, String urlWithUser, int connectTimeoutInMs, int readTimeoutInMs, FormattingResultLog log) throws IOException {
            Response response = null;
            HttpURLConnection conn = null;
            try {
                URL effectiveUrl;
                if(url.startsWith("/")) {
                    effectiveUrl = new URL(defaultBaseUrl + url);
                } else {
                    effectiveUrl = new URL(url);
                }
                
                conn = openConnection(connectTimeoutInMs, readTimeoutInMs, effectiveUrl, log);
                response = readResponse(conn, log);

            } finally {
                if(conn!=null) {
                    conn.disconnect();
                }
            }
            return response;
        }

        private HttpURLConnection openConnection(int defaultConnectTimeoutInMs, int defaultReadTimeoutInMs, URL effectiveUrl, FormattingResultLog log)
                throws IOException, ProtocolException {
            HttpURLConnection conn;
            conn = (HttpURLConnection) (proxy==null ? effectiveUrl.openConnection() : effectiveUrl.openConnection(proxy));
            conn.setInstanceFollowRedirects(false);
            conn.setUseCaches(false);
            
            int effectiveConnectTimeout = this.connectTimeoutInMs !=null ? this.connectTimeoutInMs : defaultConnectTimeoutInMs;
            int effectiveReadTimeout = this.readTimeoutInMs !=null ? this.readTimeoutInMs : defaultReadTimeoutInMs;
            log.debug("connectTimeout={}ms readTimeout={}ms", effectiveConnectTimeout, effectiveReadTimeout);
            conn.setConnectTimeout(effectiveConnectTimeout); 
            conn.setReadTimeout(effectiveReadTimeout);

            conn.setRequestMethod(method); 
            for(Entry<String,String> header: headers.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue()); 
            }
            if(data != null) {
                conn.setDoOutput(true);
                byte[] bytes = data.getBytes();
                log.debug("Sending request entity with {}bytes", bytes.length);
                try(DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(bytes);
                }
            }
            return conn;
        }

        private Response readResponse(HttpURLConnection conn, FormattingResultLog log) throws IOException {
            
            long startTime = System.currentTimeMillis();
            
            int actualResponseCode = conn.getResponseCode();
            String actualResponseMessage = conn.getResponseMessage();
            log.debug("Result: {} {}", actualResponseCode, actualResponseMessage);
            Map<String, List<String>> responseHeaders = conn.getHeaderFields();
            
            StringWriter responseEntityWriter = new StringWriter();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseEntityWriter.write(inputLine + "\n");
                }
            } catch(IOException e) {
                log.debug("Could not get response entity: {}", e.getMessage());
            }
            
            long requestDurationInMs = System.currentTimeMillis() - startTime;
            Response response = new Response(actualResponseCode, actualResponseMessage, responseHeaders, responseEntityWriter.toString(), requestDurationInMs);
            
            return response;
        }
        
    }

    static class Response {
        final int actualResponseCode;
        final String actualResponseMessage;
        final Map<String, List<String>> actualResponseHeaders;
        final String actualResponseEntity;
        final long requestDurationInMs;
        
        public Response(int actualResponseCode, String actualResponseMessage, Map<String, List<String>> actualResponseHeaders,
                String actualResponseEntity, long requestDurationInMs) {
            super();
            this.actualResponseCode = actualResponseCode;
            this.actualResponseMessage = actualResponseMessage;
            this.actualResponseHeaders = actualResponseHeaders;
            this.actualResponseEntity = actualResponseEntity;
            this.requestDurationInMs = requestDurationInMs;
        }
    }
    
    static interface ResponseCheck {
        
        class ResponseCheckResult {
            final boolean contraintFailed;
            final String message;
            
            ResponseCheckResult(boolean contraintFailed, String message) {
                this.contraintFailed = contraintFailed;
                this.message = message;
            }
            
        }
        
        ResponseCheckResult checkResponse(Response response, FormattingResultLog log);
    }

    static class ResponseCodeCheck implements ResponseCheck {
        
        private final int expectedResponseCode;
        
        public ResponseCodeCheck(int expectedResponseCode) {
            this.expectedResponseCode = expectedResponseCode;
        }

        public ResponseCheckResult checkResponse(Response response, FormattingResultLog log) {

            if(expectedResponseCode != response.actualResponseCode) {
                return new ResponseCheckResult(true, response.actualResponseCode + " (expected "+expectedResponseCode+")");
            } else {
                return new ResponseCheckResult(false, "["+response.actualResponseCode + " "+response.actualResponseMessage+"]");
            }
        }
    }
    
    static class ResponseTimeCheck implements ResponseCheck {
        final static String TIME = "TIME ";
        
        private final String timeConstraint;
        
        private final SimpleConstraintChecker simpleConstraintChecker = new SimpleConstraintChecker();
        
        public ResponseTimeCheck(String timeConstraint) {
            this.timeConstraint = timeConstraint;
        }

        public ResponseCheckResult checkResponse(Response response, FormattingResultLog log) {

            log.debug("Checking request time [{}ms] for constraint [{}]", response.requestDurationInMs, timeConstraint);
            if(!simpleConstraintChecker.check((Long) response.requestDurationInMs, timeConstraint)) {
                return new ResponseCheckResult(true, "time ["+response.requestDurationInMs + "ms] does not fulfil constraint ["+timeConstraint+"]");
            } else {
                return new ResponseCheckResult(false, "time ["+response.requestDurationInMs + "ms] fulfils constraint ["+timeConstraint+"]");
            }
        }
    }
    
    static class ResponseEntityRegExCheck implements ResponseCheck {
        final static String MATCHES = "MATCHES ";
        
        private final Pattern expectedResponseEntityRegEx;
        
        public ResponseEntityRegExCheck(Pattern expectedResponseEntityRegEx) {
            this.expectedResponseEntityRegEx = expectedResponseEntityRegEx;
        }
        
        public ResponseCheckResult checkResponse(Response response, FormattingResultLog log) {
            if(!expectedResponseEntityRegEx.matcher(response.actualResponseEntity).find()) {
                return new ResponseCheckResult(true, "response does not match ["+expectedResponseEntityRegEx+']');
            } else {
                return new ResponseCheckResult(false, "response matches ["+expectedResponseEntityRegEx+"]");
            }
        }
    }

    static class ResponseHeaderCheck implements ResponseCheck {
        final static String HEADER = "HEADER ";
        
        private final String headerName;
        private final String headerConstraint;
        
        private final SimpleConstraintChecker simpleConstraintChecker = new SimpleConstraintChecker();

        
        public ResponseHeaderCheck(String headerExpression) {
            String[] headerCheckBits = headerExpression.split(" +", 2);
            this.headerName = headerCheckBits[0];
            this.headerConstraint = headerCheckBits[1];
        }
        
        public ResponseCheckResult checkResponse(Response response, FormattingResultLog log) {

            List<String> headerValues = response.actualResponseHeaders.get(headerName);
            String headerVal = headerValues!=null && !headerValues.isEmpty() ? headerValues.get(0): null;
            
            log.debug("Checking {} with value [{}] for constraint [{}]", headerName, headerVal, headerConstraint);
            if(!simpleConstraintChecker.check(headerVal, headerConstraint)) {
                return new ResponseCheckResult(true, "header ["+headerName+"] has value ["+headerVal+"] which does not fulfil constraint ["+headerConstraint+"]");
            } else {
                return new ResponseCheckResult(false, "header ["+headerName+"] ok");
            }
        }
    }

    static class JsonPropertyCheck implements ResponseCheck {
        final static String JSON = "JSON ";
        
        private final String jsonPropertyPath;
        private final String jsonPropertyConstraint;
        
        private final SimpleConstraintChecker simpleConstraintChecker = new SimpleConstraintChecker();

        
        public JsonPropertyCheck(String jsonExpression) {
            String[] jsonCheckBits = jsonExpression.split(" +", 2);
            this.jsonPropertyPath = jsonCheckBits[0];
            this.jsonPropertyConstraint = jsonCheckBits[1];
        }
        
        public ResponseCheckResult checkResponse(Response response, FormattingResultLog log) {

            JSONParser jsonParser;
            try {
                jsonParser = new JSONParser(response.actualResponseEntity);
            } catch(Exception e) {
                return new ResponseCheckResult(true, "invalid json response (["+jsonPropertyPath+"] cannot be checked agains constraint ["+jsonPropertyConstraint+"])");
            }

            Object propertyVal = getJsonProperty(jsonParser, jsonPropertyPath);
            
            log.debug("JSON property [{}] has value [{}]", jsonPropertyPath, propertyVal);
            
            log.debug("Checking [{}] with value [{}] for constraint [{}]", jsonPropertyPath, propertyVal, jsonPropertyConstraint);
            if(!simpleConstraintChecker.check(propertyVal, jsonPropertyConstraint)) {
                return new ResponseCheckResult(true, "json ["+jsonPropertyPath+"] has value ["+propertyVal+"] which does not fulfil constraint ["+jsonPropertyConstraint+"]");
            } else {
                return new ResponseCheckResult(false, "json property ["+jsonPropertyPath+"] with ["+propertyVal+"] fulfils constraint ["+jsonPropertyConstraint+"]");
            }
        }

        private Object getJsonProperty(JSONParser jsonParser, String jsonPropertyPath) {
            String[] jsonPropertyPathBits = jsonPropertyPath.split("(?=\\.|\\[)");
            Object currentObject = null;
            for (int i=0; i < jsonPropertyPathBits.length; i++) {
                String jsonPropertyPathBit = jsonPropertyPathBits[i];
                if(jsonPropertyPathBit.startsWith("[")) {
                    int arrayIndex = Integer.parseInt(jsonPropertyPathBit.substring(1,jsonPropertyPathBit.length()-1));
                    if(currentObject==null) {
                        currentObject = jsonParser.getParsedList();
                    }
                    if(!(currentObject instanceof List)) {
                        throw new IllegalArgumentException("Path '"+StringUtils.defaultIfEmpty(StringUtils.join(jsonPropertyPathBits, "", 0, i), "<root>")+"' is not a json list");
                    }
                    currentObject = ((List<?>) currentObject).get(arrayIndex);
                } else {
                    String propertyName = jsonPropertyPathBit.startsWith(".") ? jsonPropertyPathBit.substring(1) : jsonPropertyPathBit;
                    if(currentObject==null) {
                        currentObject = jsonParser.getParsed();
                    }
                    if(!(currentObject instanceof Map)) {
                        throw new IllegalArgumentException("Path '"+StringUtils.defaultIfEmpty(StringUtils.join(jsonPropertyPathBits, "", 0, i), "<root>")+"' is not a json object");
                    }
                    currentObject = ((Map<?,?>) currentObject).get(propertyName);
                }
                if(currentObject==null && /* not last */ i+1 < jsonPropertyPathBits.length) {
                    throw new IllegalArgumentException("Path "+StringUtils.join(jsonPropertyPathBits, "", 0, i+1)+" is null, cannot evaluate left-over part '"+StringUtils.join(jsonPropertyPathBits, "", i+1, jsonPropertyPathBits.length)+"'");
                }
            }
            return currentObject;
        }
    }
    
}
