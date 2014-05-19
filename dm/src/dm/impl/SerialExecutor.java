package dm.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.osgi.service.log.LogService;

/**
 * Allows you to enqueue tasks from multiple threads and then execute
 * them on one thread sequentially. It assumes more than one thread will
 * try to execute the tasks and it will make an effort to pick the first
 * task that comes along whilst making sure subsequent tasks return
 * without waiting.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SerialExecutor implements Executor {
    private final List<Runnable> m_queue = new LinkedList<Runnable>();
    private Thread m_runningThread;
    private final Logger m_logger;

    public SerialExecutor(Logger logger) {
        m_logger = logger;
    }

    /**
     * Execute a task. This method is thread safe, so multiple threads can try to execute a task
     * but only the first will be executed, other threads will return immediately, and the
     * first thread will execute the tasks scheduled by the other threads.<p>
     * <p>
     * This method is reentrant: if the current thread is currently being executed by this executor, then any
     * subsequent tasks scheduled in this executor will executed immediately (inline execution).
     */
    public void execute(Runnable command) {
        execute(command, true);
    }
    
    /**
     * Execute a task. This method is thread safe, so multiple threads can try to execute a task
     * but only the first will be executed, other threads will return immediately, and the
     * first thread will execute the tasks scheduled by the other threads.
     * 
     * @param command the task to execute (possibly to be delayed and executed by another single thread)
     * @param reentrant true means that if the executor is already being executed on this thread, then the 
     * command is run immediately (inline execution). else, if reentrant == false, then the task is enqueued 
     * even if the executor is currently being executed by the current thread.
     */
    public void execute(Runnable command, boolean reentrant) {
        Runnable next = null;
        Thread currentThread = Thread.currentThread();
        boolean isExecutingOnThisThread = false;

        synchronized (this) {
            if (reentrant && m_runningThread == currentThread) {
                // we are already being executed by this executor, so we'll execute the command directly (inline execution).
                isExecutingOnThisThread = true;
            }
            else {
                if (m_queue.isEmpty()) {
                    // Nobody is currently using the executor, we'll execute the command, but we have to store it
                    // in the queue, so another thread won't execute concurrently. Also, we mark the current executing
                    // thread to our own current thread, in order to inline executions of reentrant scheduled tasks, if any.
                    m_runningThread = currentThread;
                    next = command;
                }
                m_queue.add(command);
            }
        }

        if (reentrant && isExecutingOnThisThread) {
            runTask(command);
        }
        else {
            while (next != null) {
                runTask(next);
                synchronized (this) {
                    m_queue.remove(0); // The first element is the one we have just executed
                    next = m_queue.isEmpty() ? null : (Runnable) m_queue.get(0);
                    if (next == null) {
                        m_runningThread = null;
                    }
                }
            }
        }
    }

    private void runTask(Runnable command) {
        try {
            command.run();
        }
        catch (Throwable t) {
        	if (m_logger != null) {
        		m_logger.log(LogService.LOG_ERROR, "Error processing tasks", t);
        	}
        }
    }

    // TODO ASPECTS: Review. Added methods for separately scheduling and executing tasks
    // on the SerialExecutor. This is used in the ServiceTracker which must ensure
    // customizer callback methods are executed in the correct order.
    
    
	public void schedule(Runnable runnable) {
		synchronized (this) {
			m_queue.add(runnable);
		}
	}

	public void execute() {
		Runnable next = null;
		synchronized (this) {
			if (m_runningThread == null || m_runningThread == Thread.currentThread()) {
				// It's our turn
				if (!m_queue.isEmpty()) {
					next = m_queue.get(0);
					m_runningThread = Thread.currentThread();
				} else {
					return;
				}
			} else {
				return;
			}
		} 
        while (next != null) {
            runTask(next);
            synchronized (this) {
                m_queue.remove(0); // The first element is the one we have just executed
                next = m_queue.isEmpty() ? null : (Runnable) m_queue.get(0);
                if (next == null) {
                    m_runningThread = null;
                }
            }
        }
	}
	
	@Override
	public String toString() {
		return "[Executor: queue size: " + m_queue.size() + "]";
	}
}