package org.apache.felix.dependencymanager.samples.customdep;

import org.osgi.service.log.LogService;

public class PathTracker {
    volatile LogService logService;
    
    void add(String path) {
        logService.log(LogService.LOG_INFO, "PathTracker.add: " + path);
    }
}
