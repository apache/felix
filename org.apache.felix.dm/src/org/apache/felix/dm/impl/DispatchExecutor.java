package org.apache.felix.dm.impl;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.log.LogService;

/**   
 * A parallel DispatchExecutor, similar to the SerialExecutor, except that several DispatchQueues can be executed 
 * in parallel inside a shared thread pool. When one thread schedules tasks in DispatchQueue Q1, Q2, then Q1/Q2 
 * queues are dispatched and executed concurrently in a shared thread pool. However, each queue will execute tasks 
 * scheduled in it serially, in FIFO order.
 */
public class DispatchExecutor implements Executor, Runnable {
	/**
	 * The threadpool used for the execution of this queue. When the queue run method is executed, all
	 * scheduled tasks are executed in FIFO order, but in the threadpool.
	 */
	private final Executor m_threadPool;

	/** 
	 * List of tasks scheduled in our queue. 
	 */
	protected final ConcurrentLinkedQueue<Runnable> m_tasks = new ConcurrentLinkedQueue<>();

    /**
     * Marker used to remember the id of the thread currently executing this dispatch queue.
     */
    private volatile Thread m_executingThread;

    /** 
     * Flag telling if this dispatch queue is already scheduled for execution in the threadpool.
     */
    private final AtomicBoolean m_scheduled = new AtomicBoolean();

    /** 
	 * Logger used to log exceptions thrown by scheduled tasks. 
	 */
	private final Logger m_logger;

	/**
	 * Creates a new DispatchQueue, which can be executed within a fixed thread pool. Multiple queue
	 * can be executed concurrently, but all runnables scheduled in a given queue will be executed serially, 
	 * in FIFO order. 
	 */
	public DispatchExecutor(Executor threadPool, Logger logger) {
		m_logger = logger;
		m_threadPool = threadPool;
	}
	
    /**
     * Enqueues a task for later execution. You must call {@link #execute()} in order
     * to trigger the actual task submission.
     */
    public void schedule(Runnable task) {
        m_tasks.add(task);
    }

	/**
	 * Submits a task in this queue, and schedule this dispatch queue execution in the threadpool. 
	 * If the queue is already executing, then the tasks is enqueued and will be executed later.
	 * The task is immediately executed (inline execution) if the queue is currently being executed by 
	 * the current thread (reentrency feature, similar to SerialExecutor behavior).
	 */
	public void execute(Runnable task) {
        Thread currThread = Thread.currentThread();
        if (m_executingThread == currThread) {
            runTask(task);
        } else {
            schedule(task);
            execute();
        }
	}
	
    /**
     * Schedules a task for execution, and then attempts to execute it. This method is thread safe, so 
     * multiple threads can try to execute a task but only the first will be executed, other threads will 
     * return immediately, and the first thread will execute the tasks scheduled by the other threads.<p>
     * <p>
     * This method is reentrant: if the current thread is currently being executed by this executor, then 
     * the task passed to this method will be executed immediately, from the current invoking thread
     * (inline execution).
     */
	public void execute() {
        if (m_scheduled.compareAndSet(false, true)) { // schedules our run method in the tpool.
            m_threadPool.execute(this);
        }
	}

	/**
	 * Executes from the threadpool all currently enqueued tasks
	 */
	@Override
	public void run() {
        try {
            // We do a memory barrier in order to ensure consistent per-thread
            // memory visibility
            m_executingThread = Thread.currentThread();
            Runnable task;
            while ((task = m_tasks.poll()) != null) {
                runTask(task);
            }
        } finally {
            m_scheduled.set(false);
            m_executingThread = null;
            if (m_tasks.peek() != null) {
                execute();
            }
        }
	}

    private void runTask(Runnable command) {
		try {
			command.run();
		} catch (Throwable t) {
			m_logger.log(LogService.LOG_ERROR, "Error processing tasks", t);
		}
	}
}