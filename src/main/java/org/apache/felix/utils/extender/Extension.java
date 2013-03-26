/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.utils.extender;

/**
 * A simple interface used by the extender to manage extensions.
 */
public interface Extension {

    /**
     * Start this extension. Starting and stopping of the extension
     * should be synchronized.
     */
    void start() throws Exception;

    /**
     * Destroy should be synchronous and only return when the extension
     * has been fully destroyed.  In addition it must be synchronized with
     * start, because start() and destroy() can be called concurrently.
     */
    void destroy() throws Exception;

}
