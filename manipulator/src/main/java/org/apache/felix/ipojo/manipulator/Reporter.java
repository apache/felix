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

package org.apache.felix.ipojo.manipulator;

import java.util.List;

/**
 * A {@code Reporter} is responsible to handle feedback from within the
 * manipulation process in order to let API consumers deal with errors
 * and warnings.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Reporter {

    /**
     * Add aa trace message
     * It accepts a {@link Throwable} as last argument.
     * @param message trace message.
     * @param args message's argument
     */
    void trace(String message, Object... args);

    /**
     * Add an informative message
     * It accepts a {@link Throwable} as last argument.
     * @param message info message.
     * @param args message's argument
     */
    void info(String message, Object... args);

    /**
     * Add a message in the warning list.
     * It accepts a {@link Throwable} as last argument.
     * @param message warning message.
     * @param args message's argument
     */
    void warn(String message, Object... args);

    /**
     * Add a message in the error list.
     * It accepts a {@link Throwable} as last argument.
     * @param message error message.
     * @param args message's argument
     */
    void error(String message, Object... args);

    /**
     * @return all the errors (fatal) reported by the manipulation process.
     */
    List<String> getErrors();

    /**
     * @return all the warnings (non fatal) reported by the manipulation process.
     */
    List<String> getWarnings();
}
