package org.apache.felix.dependencymanager.samples.util;

public class Helper {
    public static void log(String who, String msg) {
        System.out.println("[" + Thread.currentThread().getName() + " - " + who + "] " + msg);
    }
}
