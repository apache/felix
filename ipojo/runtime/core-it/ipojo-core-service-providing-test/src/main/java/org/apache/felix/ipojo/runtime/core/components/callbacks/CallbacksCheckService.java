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

package org.apache.felix.ipojo.runtime.core.components.callbacks;

import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

public class CallbacksCheckService implements FooService, CheckService {

	// 4 Counters
	int registered = 0;
	int unregistered = 0;
	int registered2 = 0;
	int unregistered2 = 0;

	// 4 Methods
	public void registered(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		registered++;
	}

	public void unregistered(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		unregistered++;
	}

	public void registered2(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		registered2++;
	}

	public void unregistered2(ServiceReference ref) {
		if (ref == null) {
			throw new IllegalArgumentException("ref null");
		}
		unregistered2++;
	}

    public boolean foo() {
        return true;
    }

    public Properties fooProps() {
        Properties props = new Properties();
        props.put("registered", new Integer(registered));
        props.put("registered2", new Integer(registered2));
        props.put("unregistered", new Integer(unregistered));
        props.put("unregistered2", new Integer(unregistered2));
        return props;
    }

    public boolean getBoolean() {
        return false;
    }

    public double getDouble() {
        return 0;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0;
    }

    public Boolean getObject() {
        return null;
    }

    public boolean check() {
       return true;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("registered", new Integer(registered));
        props.put("registered2", new Integer(registered2));
        props.put("unregistered", new Integer(unregistered));
        props.put("unregistered2", new Integer(unregistered2));
        return props;
    }

}
