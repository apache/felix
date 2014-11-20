package org.apache.felix.dm.context;

/**
 * DependencyManager Dependencies may log messages using this interface, which can be obtained from the ComponentContext 
 * interface.
 */
public interface Log {
    /**
     * Logs a message using LogService.LOG_ERROR log level
     * @param format the message format
     * @param params the message parameters
     */
    public void err(String format, Object ... params);
    
    /**
     * Logs a message using LogService.LOG_ERROR log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void err(String format, Throwable err, Object ... params);

    /**
     * Logs a message using LogService.LOG_WARNING log level
     * @param format the message format
     * @param params the message parameters
     */
    public void warn(String format, Object ... params);
 
    /**
     * Logs a message using LogService.LOG_WARNING log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void warn(String format, Throwable err, Object ... params);

    /**
     * Is the LogService.LOG_INFO level enabled ?
     */
    public boolean info();
    
    /**
     * Logs a message using LogService.LOG_INFO log level
     * @param format the message format
     * @param params the message parameters
     */
    public void info(String format, Object ... params);

    /**
     * Logs a message using LogService.LOG_INFO log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void info(String format, Throwable err, Object ... params);

    /**
     * Is the LogService.LOG_DEBUG level enabled ?
     */
    public boolean debug();    

    /**
     * Logs a message using LogService.LOG_DEBUG log level
     * @param format the message format
     * @param params the message parameters
     */
    public void debug(String format, Object ... params);

    /**
     * Logs a message using LogService.LOG_DEBUG log level
     * @param format the message format
     * @param err a Throwable stacktrace
     * @param params the message parameters
     */
    public void debug(String format, Throwable err, Object ... params);
}
