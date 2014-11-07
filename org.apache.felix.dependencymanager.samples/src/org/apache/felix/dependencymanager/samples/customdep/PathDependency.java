package org.apache.felix.dependencymanager.samples.customdep;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.osgi.service.log.LogService;

public class PathDependency extends AbstractDependency<PathDependency> implements Runnable {
    private final String m_path;
    private volatile Thread m_thread;

    public class PathEvent implements Event {
        final String m_path;
        public PathEvent(String path) {
            m_path = path;
        }

        @Override
        public boolean equals(Object e) {
            if (e instanceof PathEvent) {
                return m_path.equals(((PathEvent) e).m_path);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return m_path.hashCode();
        }

        @Override
        public int compareTo(Event o) {
            return m_path.compareTo(((PathEvent) o).m_path);
        }

        @Override
        public void close() {
        }

        @Override
        public Object getEvent() {
            return m_path;
        }

        @Override
        public Dictionary<String, Object> getProperties() {
            return EMPTY_PROPERTIES;
        }
    }

    public PathDependency(String path) {
        super.setRequired(true);
        m_path = path;
    }

    public PathDependency(PathDependency prototype) {
        super(prototype);
        m_path = prototype.m_path;
    }

    @Override
    public DependencyContext createCopy() {
        return new PathDependency(this);
    }

    @Override
    public String getName() {
        return m_path;
    }

    @Override
    public String getType() {
        return "path";
    }

    protected void startTracking() {
        m_thread = new Thread(this);
        m_thread.start();
    }

    protected void stopTracking() {
        m_thread.interrupt();
    }
    
    public boolean invoke(String method, Event e, Object[] instances) {
        // specific for this type of dependency
        return m_component.invokeCallbackMethod(instances, method, 
            new Class[][] { {String.class}, 
                            {}}, 
            new Object[][] { { e.getEvent() }, 
                            {}});
    }

    public void run() {
        Path myDir = Paths.get(m_path);

        try {
            WatchService watcher = myDir.getFileSystem().newWatchService();
            myDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
            while (! Thread.currentThread().isInterrupted()) {
				WatchKey watckKey = watcher.take();

				List<WatchEvent<?>> events = watckKey.pollEvents();

				for (WatchEvent event : events) {
					final Kind<?> kind = event.kind();
					if (StandardWatchEventKinds.OVERFLOW == kind) {
						continue;
					}
					if (StandardWatchEventKinds.ENTRY_CREATE == kind) {
						add(new PathEvent(event.context().toString()));
					} else if (StandardWatchEventKinds.ENTRY_DELETE == kind) {
						remove(new PathEvent(event.context().toString()));
					}
				}
				
				watckKey.reset();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}