package org.apache.felix.ipojo.webconsole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

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
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

@Component(immediate=true)
@Provides
@Instantiate
public class IPOJOPlugin extends AbstractWebConsolePlugin {
    
    private static final String CSS[] = { "/res/ui/bundles.css" , "/iPOJO_2/res/ui/ipojo.css" }; // TODO Change

    private final String INSTANCES;
    private final String FACTORIES;
    private final String HANDLERS;
    private final String FACTORY_DETAILS;
    private final String INSTANCE_DETAILS;


    
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
    protected String[] m_css = CSS;

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
        HANDLERS = readTemplateFile(this.getClass(), "/res/handlers.html" );
        FACTORY_DETAILS = readTemplateFile(this.getClass(), "/res/factory.html" );
        INSTANCE_DETAILS = readTemplateFile(this.getClass(), "/res/instance.html" );

    }
    
    private final String readTemplateFile(final Class clazz,
            final String templateFile) {
        InputStream templateStream = getClass().getResourceAsStream(
                templateFile);
        if (templateStream != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            try {
                int len = 0;
                while ((len = templateStream.read(data)) > 0) {
                    baos.write(data, 0, len);
                }
                return baos.toString("UTF-8");
            } catch (IOException e) {
                // don't use new Exception(message, cause) because cause is 1.4+
                throw new RuntimeException("readTemplateFile: Error loading "
                        + templateFile + ": " + e);
            } finally {
                try {
                    templateStream.close();
                } catch (IOException e) {
                    /* ignore */
                }

            }
        }

        // template file does not exist, return an empty string
        log("readTemplateFile: File '" + templateFile
                + "' not found through class " + clazz);
        return "";
    }

    @Override
    protected void renderContent(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);
        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        
        System.out.println("Render content for " + request.getPathInfo());
        
        if (reqInfo.instances) {
            if (reqInfo.name == null) {
                response.getWriter().print( INSTANCES );
            } else {
                System.out.println("Details for " + reqInfo.name);
                vars.put("name", reqInfo.name);
                response.getWriter().print( INSTANCE_DETAILS );
            }
        } else if (reqInfo.factories) {
            if (reqInfo.name == null) {
                response.getWriter().print( FACTORIES );
            } else {
                vars.put("name", reqInfo.name);
                response.getWriter().print( FACTORY_DETAILS );
            }
        } else if (reqInfo.handlers) {
            response.getWriter().print( HANDLERS );
        } else {
            // Default
            response.getWriter().print( INSTANCES );
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
            resp.put("invalid_count", getInvalidFactoriesCount());
            
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
    
    private void renderAllHandlers(PrintWriter pw) {
        try {
            JSONObject resp = new JSONObject();
            resp.put("count", m_handlers.size());
            resp.put("valid_count", getValidHandlersCount());
            resp.put("invalid_count", getInvalidHandlersCount());
            
            JSONArray factories = new JSONArray();
            for (HandlerFactory factory : m_handlers) {
                String version = factory.getVersion();
                String name = factory.getHandlerName();
                
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
                fact.put("type", factory.getType());
                if (! factory.getMissingHandlers().isEmpty()) {
                    fact.put("missing", factory.getMissingHandlers().toString());
                }
                factories.put(fact);
            }
            resp.put("data", factories);
            
            pw.print(resp.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private void renderFactoryDetail(PrintWriter pw, String name) {
        System.out.println("Render factory detail for " + name);
        // Find the factory
        Factory factory = null;
        for (Factory fact : m_factories) {
            if (fact.getName().equals(name)) {
                factory = fact;
            }
        }
        
        if (factory == null) {
            pw.println("The factory " + name + " does not exist or is private");
            System.err.println("factory " + name + "  not found");
            return;
        }
        
        try {
            JSONObject resp = new JSONObject();
            resp.put("count", m_factories.size());
            resp.put("valid_count", getValidFactoriesCount());
            resp.put("invalid_count", getInvalidFactoriesCount());
            
            // Factory object
            JSONObject data = new JSONObject();
            data.put("name", factory.getName());
            data.put("state", getFactoryState(factory.getState()));
            
            String bundle = factory.getBundleContext().getBundle().getSymbolicName()
            + " (" + factory.getBundleContext().getBundle().getBundleId() + ")";
            data.put("bundle", bundle);
            
            if (factory.getComponentDescription().getprovidedServiceSpecification().length != 0) {
                JSONArray services = new JSONArray
                    (Arrays.asList(factory.getComponentDescription().getprovidedServiceSpecification()));
                data.put("services", services);
            }
            
            PropertyDescription[] props = factory.getComponentDescription().getProperties();
            if (props != null  && props.length != 0) {
                JSONArray properties = new JSONArray();
                for (int i = 0; i < props.length; i++) {
                    JSONObject prop = new JSONObject();
                    prop.put("name", props[i].getName());
                    prop.put("type", props[i].getType());
                    prop.put("mandatory", props[i].isMandatory());
                    prop.put("immutable", props[i].isImmutable());
                    if (props[i].getValue() != null) {
                        prop.put("value", props[i].getValue());
                    }
                    properties.put(prop);
                }
                data.put("properties", properties);
            }
            
            if (! factory.getRequiredHandlers().isEmpty()) {
                JSONArray req = new JSONArray
                    (factory.getRequiredHandlers());
                data.put("requiredHandlers", req);
            }
            
            if (! factory.getMissingHandlers().isEmpty()) {
                JSONArray req = new JSONArray
                    (factory.getMissingHandlers());
                data.put("missingHandlers", req);
            }
            
            List instances = getInstanceList(name);
            if (! instances.isEmpty()) {
                JSONArray req = new JSONArray(instances);
                data.put("instances", req);
            }
            
            data.put("architecture", factory.getDescription().toString());
            resp.put("data", data);
            
            pw.print(resp.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    private void renderInstanceDetail(PrintWriter pw, String name) {
        System.out.println("Render instance detail for " + name);
        // Find the factory
        InstanceDescription instance = null;
        for (Architecture arch : m_archs) {
            if (arch.getInstanceDescription().getName().equals(name)) {
                instance = arch.getInstanceDescription();
            }
        }
        
        if (instance == null) {
            pw.println("The instance " + name + " does not exist or does not exposed its architecture");
            System.err.println("instance " + name + "  not found");
            return;
        }
        
        try {
            JSONObject resp = new JSONObject();
            resp.put("count", m_factories.size());
            resp.put("valid_count", getValidFactoriesCount());
            resp.put("invalid_count", getInvalidFactoriesCount());
            
            // instance object
            JSONObject data = new JSONObject();
            data.put("name", instance.getName());
            data.put("state", getInstanceState(instance.getState()));
            data.put("factory", instance.getComponentDescription().getName());
            
            JSONArray services = getProvidedServiceDetail(instance.getHandlerDescription("org.apache.felix.ipojo:provides"));
            if (services != null) {
                data.put("services", services);
            }
            
            JSONArray reqs = getRequiredServiceDetail(instance.getHandlerDescription("org.apache.felix.ipojo:requires"));
            if (reqs != null) {
                data.put("req", reqs);
            }
            
            data.put("architecture", instance.getDescription().toString());
            resp.put("data", data);
            
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
                    this.renderInstanceDetail(response.getWriter(), reqInfo.name);
                    return;
                }
            }
            
            if (reqInfo.factories) {
                if (reqInfo.name == null) {
                    this.renderAllFactories(response.getWriter());
                    return;
                } else {
                    System.out.println("Render details for " + reqInfo.name);
                    this.renderFactoryDetail(response.getWriter(), reqInfo.name);
                    return;
                }
            }
            
            if (reqInfo.handlers) {
                this.renderAllHandlers(response.getWriter());
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
    
    private JSONArray getProvidedServiceDetail(HandlerDescription hd) throws JSONException {
        if (hd == null) {
            return null;
        }

        JSONArray array = new JSONArray();
        ProvidedServiceHandlerDescription desc = (ProvidedServiceHandlerDescription) hd;

        for (ProvidedServiceDescription ps : desc.getProvidedServices()) {
            JSONObject svc = new JSONObject();
            String spec = Arrays.toString(ps.getServiceSpecifications());
            if (spec.startsWith("[")) {
                spec = spec.substring(1, spec.length() - 1);
            }
            svc.put("specification", spec);
            svc.put("state", getProvidedServiceState(ps.getState()));
            
            if (ps.getServiceReference() != null) {
                svc.put("id", (Long) ps.getServiceReference().getProperty(Constants.SERVICE_ID));
            }
            
            if (ps.getProperties() != null  &&!  ps.getProperties().isEmpty()) {
                svc.put("properties", getServiceProperties(ps.getProperties()));
            }

            array.put(svc);
        }
        
        return array;
    }
    
    private JSONArray getServiceProperties(Properties properties) throws JSONException {
        JSONArray array = new JSONArray();
        Enumeration<Object> e = properties.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            Object value = properties.get(key);
            JSONObject prop = new JSONObject();
            prop.put("name", key);
            if (value != null  && value.getClass().isArray()) {
                // TODO Test with primitive types
                prop.put("value", Arrays.toString((Object[]) value));
            } else if (value != null) {
                prop.put("value", value.toString());
            } else {
                prop.put("value", "no value");
            }
            array.put(prop);
        }
        return array;
    }
    
    private JSONArray getRequiredServiceDetail(
            HandlerDescription hd) throws JSONException {
        if (hd == null) {
            return null;
        }
        JSONArray array = new JSONArray();
        DependencyHandlerDescription desc = (DependencyHandlerDescription) hd;
        for (DependencyDescription dep : desc.getDependencies()) {
            JSONObject req = new JSONObject();
            req.put("specification",dep.getSpecification());
            req.put("id", dep.getId());
            req.put("state", getDependencyState(dep.getState()));
            req.put("policy", getDependencyBindingPolicy(dep.getPolicy()));
            req.put("optional", dep.isOptional());
            req.put("aggregate", dep.isMultiple());
            if (dep.getFilter() != null) {
                req.put("filter", dep.getFilter());
            }
            if (dep.getServiceReferences() != null  && dep.getServiceReferences().size() != 0) {
                req.put("matching",  getServiceReferenceList(dep.getServiceReferences()));
            }
            
            if (dep.getUsedServices() != null  && dep.getUsedServices().size() != 0) {
                req.put("used",  getServiceReferenceList(dep.getUsedServices()));
            }
            
            array.put(req);
        }

        return array;
    }
    
    private JSONArray getServiceReferenceList(List<ServiceReference> refs) throws JSONException {
        JSONArray array = new JSONArray();
        if (refs != null) {
            for (ServiceReference ref : refs) {
                JSONObject reference = new JSONObject();
                if (ref.getProperty("instance.name") == null) {
                    reference.put("id", ref.getProperty(Constants.SERVICE_ID));
                } else {
                    reference.put("id", ref.getProperty(Constants.SERVICE_ID));
                    reference.put("instance", ref.getProperty("instance.name"));
                }
                array.put(reference);
            }
        }
        return array;
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
    
    private int getValidHandlersCount() {
        int i = 0;
        for (Factory a : m_handlers) { // Cannot be null, an empty list is returned.
            if (a.getState() == Factory.VALID) {
                i ++;
            }
        }
        return i;
    }
    
    private int getInvalidHandlersCount() {
        int i = 0;
        for (Factory a : m_handlers) { // Cannot be null, an empty list is returned.
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
    
    /**
     * Gets the instance list created by the given factory.
     * @param factory the factory name
     * @return the list containing the created instances (name)
     */
    private List getInstanceList(String factory) {
        List list = new ArrayList();
        for (Architecture arch : m_archs) { // Cannot be null, an empty list is returned.
            String n = arch.getInstanceDescription().getComponentDescription().getName();
            if (factory.equals(n)) {
                list.add(arch.getInstanceDescription().getName());
            }
        }
        return list;
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
                System.out.println("Info " + info);

                instances = path.startsWith("instances");
                factories = path.startsWith("factories");
                handlers = path.startsWith("handlers");
               
                System.out.println("Path " + path);
                
                if (instances  && path.startsWith("instances/")) {
                    name = path.substring("instances".length() + 1);
                } else if (factories  && path.startsWith("factories/")) {
                    name = path.substring("factories".length() + 1);
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
    
    /**
     * Gets the dependency state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private static String getDependencyState(int state) {
        switch(state) {
            case DependencyModel.RESOLVED :
                return "resolved";
            case DependencyModel.UNRESOLVED :
                return "unresolved";
            case DependencyModel.BROKEN :
                return "broken";
            default :
                return "unknown (" + state + ")";
        }
    }

    /**
     * Gets the dependency binding policy as a String.
     * @param policy the policy.
     * @return the String form of the policy.
     */
    private static String getDependencyBindingPolicy(int policy) {
        switch(policy) {
            case DependencyModel.DYNAMIC_BINDING_POLICY :
                return "dynamic";
            case DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY :
                return "dynamic-priority";
            case DependencyModel.STATIC_BINDING_POLICY :
                return "static";
            default :
                return "unknown (" + policy + ")";
        }
    }

    /**
     * Gets the provided service state as a String.
     * @param state the state.
     * @return the String form of the state.
     */
    private static String getProvidedServiceState(int state) {
        switch(state) {
            case ProvidedService.REGISTERED :
                return "registered";
            case ProvidedService.UNREGISTERED :
                return "unregistered";
            default :
                return "unknown (" + state + ")";
        }
    }

}
