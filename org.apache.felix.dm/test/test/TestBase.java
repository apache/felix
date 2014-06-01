package test;

import static java.lang.System.out;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Base class for all tests.
 * For now, this class provides logging support.
 */
public class TestBase {
    final int WARN = 1;
    final int INFO = 2;
    final int DEBUG = 3;
    
    // Set the enabled log level.
    final int m_level = WARN;
    
    void debug(String format, Object ... params) {
        if (m_level >= DEBUG) {
            out.println(Thread.currentThread().getName() + " - " + String.format(format, params));
        }
    }
    
    void warn(String format, Object ... params) {
        warn(format, null, params);
    }
    
    void info(String format, Object ... params) {
        if (m_level >= INFO) {
            out.println(Thread.currentThread().getName() + " - " + String.format(format, params));
        }
    }

    void warn(String format, Throwable t, Object ... params) {
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().getName()).append(" - ").append(String.format(format, params));
        if (t != null) {
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(System.getProperty("line.separator"));
            sb.append(buffer.toString());
        }
        System.out.println(sb.toString());
    }
}
