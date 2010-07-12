package org.apache.felix.ipojo.webconsole;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;

@Component
@Provides
@Instantiate
public class IPOJOPlugin extends AbstractWebConsolePlugin {

    /**
     * Label used by the web console.
     */
    @ServiceProperty(name = "felix.webconsole.label")
    private String m_label = "iPOJO_2"; // TODO CHANGE

    /**
     * Title used by the web console.
     */
    @ServiceProperty(name = "felix.webconsole.title")
    private String m_title = "iPOJO_2";  // TODO CHANGE

    /**
     * List of available Architecture service.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.architecture.Architecture")
    private List<Architecture> m_archs;

    /**
     * List of available Factories.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.Factory")
    private List<Factory> m_factories;

    /**
     * List of available Handler Factories.
     */
    @Requires(optional = true, specification = "org.apache.felix.ipojo.HandlerFactory")
    private List<HandlerFactory> m_handlers;
    
    @Override
    public String getLabel() {
        return m_label;
    }

    @Override
    public String getTitle() {
        return m_title;
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        // TODO Auto-generated method stub
        
    }

}
