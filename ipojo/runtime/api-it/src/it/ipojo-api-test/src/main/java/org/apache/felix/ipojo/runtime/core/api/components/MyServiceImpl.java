package org.apache.felix.ipojo.runtime.core.api.components;

import org.apache.felix.ipojo.runtime.core.api.services.MyService;

public class MyServiceImpl implements MyService {
    public double compute(double value) {
	return Math.exp(value * Math.cosh(value));
    }
}
