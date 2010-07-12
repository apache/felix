package org.apache.felix.ipojo.webconsole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@Component(immediate=true)
@Provides
@Instantiate
public class IPOJOPlugin extends AbstractWebConsolePlugin {
    
    private static final String CSS[] = { "/res/ui/bundles.css" };

    private final String INSTANCES;
    private final String FACTORIES;
    
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
    
    @ServiceProperty(name= "felix.webconsole.css")
    private String m_css = CSS[0];

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
    
    public IPOJOPlugin() {
        INSTANCES = readTemplateFile(this.getClass(), "/res/instances.html" );
        FACTORIES = readTemplateFile(this.getClass(), "/res/factories.html" );

    }
    
    private final String readTemplateFile(final Class clazz, final String templateFile)
    {
        InputStream templateStream = getClass().getResourceAsStream(templateFile);
        if (templateStream != null)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            try
            {
                int len = 0;
                while ((len = templateStream.read(data)) > 0)
                {
                    baos.write(data, 0, len);
                }
                return baos.toString("UTF-8");
            }
            catch (IOException e)
            {
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException("readTemplateFile: Error loading "
                    + templateFile + ": " + e);
            }
            finally
            {
                try
                {
                    templateStream.close();
                }
                catch (IOException e)
                {
                    /* ignore */
                }

            }
        }

        // template file does not exist, return an empty string
        log("readTemplateFile: File '" + templateFile + "' not found through class "
            + clazz);
        return "";
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);
        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        String view = request.getParameter("view");
        
        if (view == null || view.equals("instances")) {
            response.getWriter().print( INSTANCES );
        } else if (view.equals("factories")) {
            response.getWriter().print( FACTORIES );
        }                
    }
    
    private void renderAllInstances(PrintWriter pw) {
        try {
            JSONObject resp = new JSONObject();
            resp.put("count", m_archs.size());
            resp.put("valid_count", getValidCount());
            resp.put("invalid_count", getInvalidCount());
            
            JSONArray instances = new JSONArray();
            for (Architecture arch : m_archs) {
                JSONObject instance = new JSONObject();
                instance.put("name", arch.getInstanceDescription().getName());
                instance.put("factory", arch.getInstanceDescription().getComponentDescription().getName());
                instance.put("state", getInstanceState(arch.getInstanceDescription().getState()));
                instances.put(instance);
            }
            resp.put("data", instances);
            
            pw.print(resp.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void renderAllFactories(PrintWriter pw) {
        try {
            JSONObject resp = new JSONObject();
            resp.put("count", m_factories.size());
            resp.put("valid_count", getValidFactoriesCount());
            resp.put("invalid_count", getValidFactoriesCount());
            
            JSONArray factories = new JSONArray();
            for (Factory factory : m_factories) {
                String version = factory.getVersion();
                String name = factory.getName();
                
                String state = getFactoryState(factory.getState());
                String bundle = factory.getBundleContext().getBundle().getSymbolicName()
                    + " (" + factory.getBundleContext().getBundle().getBundleId() + ")";
                JSONObject fact = new JSONObject();
                fact.put("name", name);
                if (version != null) {
                    fact.put("version", version);
                }
                fact.put("bundle", bundle);
                fact.put("state", state);
                factories.put(fact);
            }
            resp.put("data", factories);
            
            pw.print(resp.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        final RequestInfo reqInfo = new RequestInfo(request);
        
        if ( reqInfo.extension.equals("json")  )
        {
            response.setContentType( "application/json" );
            //response.setCharacterEncoding( "UTF-8" );
            if (reqInfo.instances) {
                if (reqInfo.name == null) {
                    this.renderAllInstances(response.getWriter());
                    return;
                } else {
                    // TODO
                    return;
                }
            }
            
            if (reqInfo.factories) {
                if (reqInfo.name == null) {
                    this.renderAllFactories(response.getWriter());
                    return;
                } else {
                    // TODO
                    return;
                }
            }
            
            if (reqInfo.handlers) {
                //TODO
                return;
            }
            // nothing more to do
            return;
        }
        super.doGet( request, response );
    }
    
    public URL getResource(String path) {
        if (path.contains("/res/ui/")) {
            return this.getClass().getResource(
                    path.substring(m_label.length() + 1));
        }
        return null;
    }
    
    /**
     * Gets the number of valid instances.
     * @return the number of valid instances.
     */
    private int getValidCount() {
        int i = 0;
        for (Architecture a : m_archs) { // Cannot be null, an empty list is returned.
            if (a.getInstanceDescription().getState() == ComponentInstance.VALID) {
                i ++;
            }
        }
        return i;
    }

    /**
     * Gets the number of invalid instances.
     * @return the number of invalid instances.
     */
    private int getInvalidCount() {
        int i = 0;
        for (Architecture a : m_archs) {  // Cannot be null, an empty list is returned.
            if (a.getInstanceDescription().getState() == ComponentInstance.INVALID) {
                i ++;
            }
        }
        return i;
    }
    
    private int getValidFactoriesCount() {
        int i = 0;
        for (Factory a : m_factories) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.VALID) {
                i ++;
            }
        }
        return i;
    }
    
    private int getInvalidFactoriesCount() {
        int i = 0;
        for (Factory a : m_factories) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.INVALID) {
                i ++;
            }
        }
        return i;
    }
    
    /**
     * Gets the instance state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private static String getInstanceState(int state) {
        switch(state) {
            case ComponentInstance.VALID :
                return "valid";
            case ComponentInstance.INVALID :
                return "invalid";
            case ComponentInstance.DISPOSED :
                return "disposed";
            case ComponentInstance.STOPPED :
                return "stopped";
            default :
                return "unknown";
        }
    }

    /**
     * Gets the factory state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private static String getFactoryState(int state) {
        switch(state) {
            case Factory.VALID :
                return "valid";
            case Factory.INVALID :
                return "invalid";
            default :
                return "unknown";
        }
    }
    
    private final class RequestInfo {
        public final String extension;
        public final String path;
        public final boolean instances;
        public final boolean factories;
        public final boolean handlers;
        
        public final String name;

        
        protected RequestInfo( final HttpServletRequest request ) {
            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if (info.endsWith(".json")) {
                extension = "json";
                info = info.substring(0, info.length() - 5);
            } else {
                extension = "html";
            }

            if (info.startsWith("/")) {
                path = info.substring(1);
                instances = path.startsWith("instances");
                factories = path.startsWith("factories");
                handlers = path.startsWith("handlers");
               
                
                if (instances  && path.startsWith("instances/")) {
                    name = path.substring(0, "instances".length() + 1);
                } else if (factories  && path.startsWith("factories/")) {
                    name = path.substring(0, "factories".length() + 1);
                } else {
                    name = null;
                }
            } else {
                path = null;
                name = null;
                instances = false;
                factories = false;
                handlers = false;
            }
           
            request.setAttribute(IPOJOPlugin.class.getName(), this);
        }

    }

    static RequestInfo getRequestInfo(final HttpServletRequest request) {
        return (RequestInfo) request.getAttribute(IPOJOPlugin.class.getName());
    }

    @Override
    public String getLabel() {
       return m_label;
    }

    @Override
    public String getTitle() {
        return m_title;
    }
    
    @Override
    protected String[] getCssReferences() {
        return CSS;
    }

}
