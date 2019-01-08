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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HealthCheckService(name = ThreadUsageCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ThreadUsageCheck.Config.class, factory = false)
public class ThreadUsageCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadUsageCheck.class);

    public static final String HC_NAME = "Thread Usage";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks for thread usage and deadlocks")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Sample Period", description = "Period to measure usage per thread")
        long samplePeriodInMs() default 200;

        @AttributeDefinition(name = "CPU Time Threshold for WARN in %", description = "Will WARN once this threshold is reached in average for all threads. This value is multiplied by number of available cores as reported by Runtime.getRuntime().availableProcessors()")
        long cpuPercentageThresholdWarn() default 95;

    }

    private long samplePeriodInMs;

    private long cpuPercentageThresholdWarn;

    @Activate
    protected final void activate(Config config) {
        this.samplePeriodInMs = config.samplePeriodInMs();
        this.cpuPercentageThresholdWarn = config.cpuPercentageThresholdWarn();
        LOG.info("Activated thread usage HC samplePeriodInMs={}ms cpuPercentageThresholdWarn={}%", samplePeriodInMs, cpuPercentageThresholdWarn);
    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        log.debug("Checking threads for exceeding {}% CPU time within time period of {}ms", cpuPercentageThresholdWarn, samplePeriodInMs);

        try {
            ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();

            List<ThreadTimeInfo> threadTimeInfos = collectThreadTimeInfos(log, threadMxBean);

            Collections.sort(threadTimeInfos);

            float totalCpuTimePercentage = 0;
            for (int i = 0; i < threadTimeInfos.size(); i++) {

                ThreadTimeInfo threadInfo = threadTimeInfos.get(i);
                float cpuTimePercentage = ((float) threadInfo.getCpuTime() / ((float) samplePeriodInMs * 1000000)) * 100f;
                totalCpuTimePercentage += cpuTimePercentage;

                String msg = String.format("%4.1f", cpuTimePercentage) + "% used by thread \"" + threadInfo.name + "\"";
                
                // usually just shows the 3 busiest threads. For the case more threads take more than 15% CPU time, up to 10 threads are shown.
                // use hcDebug=true to show all threads
                if (i < 3 || (i < 10 && cpuTimePercentage > 15)) {
                    log.info(msg);
                } else {
                    log.debug(msg);
                }
            }

            int availableProcessors = Runtime.getRuntime().availableProcessors();
            boolean isHighCpuTime = totalCpuTimePercentage > (cpuPercentageThresholdWarn * availableProcessors);
            Result.Status status = isHighCpuTime ? Result.Status.WARN : Result.Status.OK;

            double cpuTimePerProcessor = totalCpuTimePercentage / availableProcessors;
            String msg = threadTimeInfos.size() + " threads using " + String.format("%.1f", totalCpuTimePercentage) + "% CPU Time ("
                    + String.format("%.1f", cpuTimePerProcessor) + "% per single core having " + availableProcessors + " cores)";
            if (isHighCpuTime) {
                msg += ">" + cpuPercentageThresholdWarn + "% threshold for WARN";
            }
            log.add(new ResultLog.Entry(status, msg));

            checkForDeadlock(log, threadMxBean);

        } catch (Exception e) {
            LOG.error("Could not analyse thread usage " + e, e);
            log.healthCheckError("Could not analyse thread usage", e);
        }

        return new Result(log);

    }

    List<ThreadTimeInfo> collectThreadTimeInfos(FormattingResultLog log, ThreadMXBean threadMxBean) {

        Map<Long, ThreadTimeInfo> threadTimeInfos = new HashMap<Long, ThreadTimeInfo>();

        long[] allThreadIds = threadMxBean.getAllThreadIds();
        for (long threadId : allThreadIds) {
            ThreadTimeInfo threadTimeInfo = new ThreadTimeInfo();
            long threadCpuTimeStart = threadMxBean.getThreadCpuTime(threadId);
            threadTimeInfo.start = threadCpuTimeStart;
            ThreadInfo threadInfo = threadMxBean.getThreadInfo(threadId);
            threadTimeInfo.name = threadInfo != null ? threadInfo.getThreadName() : "Thread id " + threadId + " (name not resolvable)";
            threadTimeInfos.put(threadId, threadTimeInfo);
        }

        try {
            Thread.sleep(samplePeriodInMs);
        } catch (InterruptedException e) {
            log.warn("Could not sleep configured samplePeriodInMs={} to gather thread load", samplePeriodInMs);
        }

        for (long threadId : allThreadIds) {
            ThreadTimeInfo threadTimeInfo = threadTimeInfos.get(threadId);
            if (threadTimeInfo == null) {
                continue;
            }
            long threadCpuTimeStop = threadMxBean.getThreadCpuTime(threadId);
            threadTimeInfo.stop = threadCpuTimeStop;
        }

        List<ThreadTimeInfo> threads = new ArrayList<ThreadTimeInfo>(threadTimeInfos.values());

        return threads;
    }

    void checkForDeadlock(FormattingResultLog log, ThreadMXBean threadMxBean) {
        long[] findDeadlockedThreads = threadMxBean.findDeadlockedThreads();
        if (findDeadlockedThreads != null) {
            for (long threadId : findDeadlockedThreads) {
                log.critical("Thread " + threadMxBean.getThreadInfo(threadId).getThreadName() + " is DEADLOCKED");
            }
        }
    }

    static class ThreadTimeInfo implements Comparable<ThreadTimeInfo> {
        long start;
        long stop;
        String name;

        long getCpuTime() {
            long cpuTime = stop - start;
            if (cpuTime < 0) {
                cpuTime = 0;
            }
            return cpuTime;
        }

        @Override
        public int compareTo(ThreadTimeInfo otherThreadTimeInfo) {
            if (otherThreadTimeInfo == null) {
                return -1;
            }
            return (int) (otherThreadTimeInfo.getCpuTime() - this.getCpuTime());
        }
    }
}
