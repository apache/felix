/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.maven.osgicheck.impl.mddocgen;

import java.text.MessageFormat;

import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.metadata.ComponentMetadata;
import org.apache.maven.plugin.logging.Log;
import org.osgi.service.log.LogService;

final class MavenScrLogger implements Logger {

    private final Log log;

    public MavenScrLogger(final Log log) {
        this.log = log;
    }

    @Override
    public boolean isLogEnabled(int level) {
        switch (level) {
            case LogService.LOG_DEBUG:
                return log.isDebugEnabled();

            case LogService.LOG_ERROR:
                return log.isErrorEnabled();

            case LogService.LOG_INFO:
                return log.isInfoEnabled();

            case LogService.LOG_WARNING:
                return log.isWarnEnabled();

            default:
                return false;
        }
    }

    @Override
    public void log(int level, String pattern, Object[] arguments,
            ComponentMetadata metadata, Long componentId, Throwable ex) {
        String message = MessageFormat.format(pattern, arguments);

        log(level, message, metadata, componentId, ex);
    }

    @Override
    public void log(int level, String message, ComponentMetadata metadata,
            Long componentId, Throwable ex) {
        switch (level) {
            case LogService.LOG_DEBUG:
                if (ex != null) {
                    log.debug(message, ex);
                } else {
                    log.debug(message);
                }
                break;

            case LogService.LOG_ERROR:
                if (ex != null) {
                    log.error(message, ex);
                } else {
                    log.error(message);
                }
                break;

            case LogService.LOG_INFO:
                if (ex != null) {
                    log.info(message, ex);
                } else {
                    log.info(message);
                }
                break;

            case LogService.LOG_WARNING:
                if (ex != null) {
                    log.warn(message, ex);
                } else {
                    log.warn(message);
                }
                break;

            default:
                break;
        }
    }

}
