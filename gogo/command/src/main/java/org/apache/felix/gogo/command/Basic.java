/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.gogo.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

public class Basic
{
    private final BundleContext m_bc;
    private final Bundle m_b0;

    public Basic(BundleContext bc)
    {
        m_bc = bc;
        m_b0 = m_bc.getBundle(0);
    }

    @Descriptor("query bundle start level")
    public String bundlelevel(@Descriptor("bundle to query") Bundle bundle)
    {
        return bundle + " is level " + bundle.adapt(BundleStartLevel.class).getStartLevel();
    }

    @Descriptor("set bundle start level or initial bundle start level")
    public String bundlelevel(
        @Descriptor("set the bundle's start level") @Parameter(names = { "-s",
                "--setlevel" }, presentValue = "true", absentValue = "false") boolean set,
        @Descriptor("set the initial bundle start level") @Parameter(names = { "-i",
                "--setinitial" }, presentValue = "true", absentValue = "false") boolean initial,
        @Descriptor("target level") int level,
        @Descriptor("target identifiers") Bundle[] bundles)
    {

        if (set && initial)
        {
            return "Cannot specify '-s' and '-i' at the same time.";
        }
        else if (!set && !initial)
        {
            return "Must specify either '-s' or '-i'.";
        }
        else if (level <= 0)
        {
            return "Specified start level must be greater than zero.";
        }
        // Set the initial bundle start level.
        else if (initial)
        {
            if ((bundles != null) && (bundles.length == 0))
            {
                m_b0.adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(level);
            }
            else
            {
                return "Cannot specify bundles when setting initial start level.";
            }
        }
        // Set the bundle start level.
        else if (set)
        {
            if ((bundles != null) && (bundles.length != 0))
            {
                for (Bundle bundle : bundles)
                {
                    bundle.adapt(BundleStartLevel.class).setStartLevel(level);
                }
            }
            else
            {
                return "Must specify target bundles.";
            }
        }
        return null;
    }

    @Descriptor("query framework active start level")
    public String frameworklevel()
    {
        return "Level is " + m_b0.adapt(FrameworkStartLevel.class).getStartLevel();
    }

    @Descriptor("set framework active start level")
    public void frameworklevel(@Descriptor("target start level") int level)
    {
        m_b0.adapt(FrameworkStartLevel.class).setStartLevel(level);
    }

    @Descriptor("display bundle headers")
    public String headers(@Descriptor("target bundles") Bundle[] bundles)
    {
        try (Formatter f = new Formatter()) {
            bundles = ((bundles == null) || (bundles.length == 0)) ? m_bc.getBundles()
                : bundles;
            String prefix = "";
            for (Bundle bundle : bundles)
            {
                String title = Util.getBundleName(bundle);
                f.format("%s%s%n", prefix, title);
                f.format("%s%n", Util.getUnderlineString(title.length()));
                Dictionary<String, String> dict = bundle.getHeaders();
                Enumeration<String> keys = dict.keys();
                while (keys.hasMoreElements())
                {
                    String k = keys.nextElement();
                    String v = dict.get(k);
                    f.format("%s = %s%n", k, v);
                }
                prefix = "\n";
            }
            return f.toString();
        }
    }

    @Descriptor("displays available commands")
    public String help()
    {
        try (Formatter f = new Formatter()) {
            Map<String, List<Method>> commands = getCommands();
            for (String name : commands.keySet())
            {
                f.format("%s%n", name);
            }
            return f.toString();
        }
    }

    @Descriptor("displays information about a specific command")
    public String help(@Descriptor("target command") String name)
    {
        Map<String, List<Method>> commands = getCommands();

        List<Method> methods = null;

        // If the specified command doesn't have a scope, then
        // search for matching methods by ignoring the scope.
        int scopeIdx = name.indexOf(':');
        if (scopeIdx < 0)
        {
            for (Entry<String, List<Method>> entry : commands.entrySet())
            {
                String k = entry.getKey().substring(entry.getKey().indexOf(':') + 1);
                if (name.equals(k))
                {
                    name = entry.getKey();
                    methods = entry.getValue();
                    break;
                }
            }
        }
        // Otherwise directly look up matching methods.
        else
        {
            methods = commands.get(name);
        }

        if ((methods == null) || (methods.size() <= 0))
        {
            return "No methods found matching: " + name;
        }

        try (Formatter f = new Formatter()) {
            String prefix = "";
            for (Method m : methods)
            {
                Descriptor d = m.getAnnotation(Descriptor.class);
                if (d == null)
                {
                    f.format("%s%s%n", prefix, m.getName());
                }
                else
                {
                    f.format("%s%s - %s%n", prefix, m.getName(), d.value());
                }

                f.format("   scope: %s%n", name.substring(0, name.indexOf(':')));

                // Get flags and options.
                Class<?>[] paramTypes = m.getParameterTypes();
                Map<String, Parameter> flags = new TreeMap<>();
                Map<String, String> flagDescs = new TreeMap<>();
                Map<String, Parameter> options = new TreeMap<>();
                Map<String, String> optionDescs = new TreeMap<>();
                List<String> params = new ArrayList<>();
                Annotation[][] anns = m.getParameterAnnotations();
                for (int paramIdx = 0; paramIdx < anns.length; paramIdx++)
                {
                    Class<?> paramType = m.getParameterTypes()[paramIdx];
                    if (paramType == CommandSession.class) {
                        /* Do not bother the user with a CommandSession. */
                        continue;
                    }
                    Parameter p = findAnnotation(anns[paramIdx], Parameter.class);
                    d = findAnnotation(anns[paramIdx], Descriptor.class);
                    if (p != null)
                    {
                        if (p.presentValue().equals(Parameter.UNSPECIFIED))
                        {
                            options.put(p.names()[0], p);
                            if (d != null)
                            {
                                optionDescs.put(p.names()[0], d.value());
                            }
                        }
                        else
                        {
                            flags.put(p.names()[0], p);
                            if (d != null)
                            {
                                flagDescs.put(p.names()[0], d.value());
                            }
                        }
                    }
                    else if (d != null)
                    {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add(d.value());
                    }
                    else
                    {
                        params.add(paramTypes[paramIdx].getSimpleName());
                        params.add("");
                    }
                }

                // Print flags and options.
                if (flags.size() > 0)
                {
                    f.format("   flags:%n");
                    for (Entry<String, Parameter> entry : flags.entrySet())
                    {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        f.format("      %s", names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++)
                        {
                            f.format(", %s", names[aliasIdx]);
                        }
                        f.format("   %s%n", flagDescs.get(entry.getKey()));
                    }
                }
                if (options.size() > 0)
                {
                    f.format("   options:%n");
                    for (Entry<String, Parameter> entry : options.entrySet())
                    {
                        // Print all aliases.
                        String[] names = entry.getValue().names();
                        f.format("      %s", names[0]);
                        for (int aliasIdx = 1; aliasIdx < names.length; aliasIdx++)
                        {
                            f.format(", %s", names[aliasIdx]);
                        }
                        f.format("   %s%s%n",
                            optionDescs.get(entry.getKey()),
                            ((entry.getValue().absentValue() == null) ? "" : " [optional]"));
                    }
                }
                if (params.size() > 0)
                {
                    f.format("   parameters:%n");
                    for (Iterator<String> it = params.iterator(); it.hasNext();)
                    {
                        f.format("      %s   %s%n", it.next(), it.next());
                    }
                }
                prefix = "\n";
            }
            return f.toString();
        }
    }

    private static <T extends Annotation> T findAnnotation(Annotation[] anns,
        Class<T> clazz)
    {
        for (int i = 0; (anns != null) && (i < anns.length); i++)
        {
            if (clazz.isInstance(anns[i]))
            {
                return clazz.cast(anns[i]);
            }
        }
        return null;
    }

    private Map<String, List<Method>> getCommands()
    {
        ServiceReference<?>[] refs = null;
        try
        {
            refs = m_bc.getAllServiceReferences(null, "(osgi.command.scope=*)");
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen.
        }

        Map<String, List<Method>> commands = new TreeMap<>();

        for (ServiceReference<?> ref : refs)
        {
            Object svc = m_bc.getService(ref);
            if (svc != null)
            {
                String scope = (String) ref.getProperty("osgi.command.scope");
                Object ofunc = ref.getProperty("osgi.command.function");
                String[] funcs = (ofunc instanceof String[]) ? (String[]) ofunc
                    : new String[] { String.valueOf(ofunc) };

                for (String func : funcs)
                {
                    commands.put(scope + ":" + func, new ArrayList<Method>());
                }

                if (!commands.isEmpty())
                {
                    Method[] methods = svc.getClass().getMethods();
                    for (Method method : methods)
                    {
                        List<Method> commandMethods = commands.get(scope + ":"
                            + method.getName());
                        if (commandMethods != null)
                        {
                            commandMethods.add(method);
                        }
                    }
                }

                // Remove any missing commands.
                Iterator<Entry<String, List<Method>>> it = commands.entrySet().iterator();
                while (it.hasNext())
                {
                    if (it.next().getValue().size() == 0)
                    {
                        it.remove();
                    }
                }
            }
        }

        return commands;
    }

    @Descriptor("install bundle using URLs")
    public String install(@Descriptor("command session")CommandSession session,
                        @Descriptor("target URLs") String[] urls) throws IOException
    {
        try (Formatter f = new Formatter()) {

            StringBuilder sb = new StringBuilder();

            for (String url : urls)
            {
                String location = Util.resolveUri(session, url.trim());
                Bundle bundle = null;
                try
                {
                    bundle = m_bc.installBundle(location, null);
                }
                catch (IllegalStateException ex)
                {
                    f.format("%s%n", ex.toString());
                }
                catch (BundleException ex)
                {
                    if (ex.getNestedException() != null)
                    {
                        f.format("%s%n", ex.getNestedException().toString());
                    }
                    else
                    {
                        f.format("%s%n", ex.toString());
                    }
                }
                catch (Exception ex)
                {
                    f.format("%s%n", ex.toString());
                }
                if (bundle != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(bundle.getBundleId());
                }
            }
            if (sb.toString().indexOf(',') > 0)
            {
                return "Bundle IDs: " + sb.toString();
            }
            else if (sb.length() > 0)
            {
                return "Bundle ID: " + sb.toString();
            }
            return f.toString();
        }
    }

    @Descriptor("list all installed bundles")
    public String lb(
        @Descriptor("show location") @Parameter(names = { "-l", "--location" }, presentValue = "true", absentValue = "false") boolean showLoc,
        @Descriptor("show symbolic name") @Parameter(names = { "-s", "--symbolicname" }, presentValue = "true", absentValue = "false") boolean showSymbolic,
        @Descriptor("show update location") @Parameter(names = { "-u", "--updatelocation" }, presentValue = "true", absentValue = "false") boolean showUpdate)
    {
        return lb(showLoc, showSymbolic, showUpdate, null);
    }

    @Descriptor("list installed bundles matching a substring")
    public String lb(
        @Descriptor("show location") @Parameter(names = { "-l", "--location" }, presentValue = "true", absentValue = "false") boolean showLoc,
        @Descriptor("show symbolic name") @Parameter(names = { "-s", "--symbolicname" }, presentValue = "true", absentValue = "false") boolean showSymbolic,
        @Descriptor("show update location") @Parameter(names = { "-u", "--updatelocation" }, presentValue = "true", absentValue = "false") boolean showUpdate,
        @Descriptor("subtring matched against name or symbolic name") String pattern)
    {
        if ((showLoc && showSymbolic && showUpdate) ||
            (showLoc && showSymbolic) ||
            (showSymbolic && showUpdate) ||
            (showLoc && showUpdate)) {
            return "Only one of -l, -s, -u should be used.";
        }
        List<Bundle> found = new ArrayList<>();

        if (pattern == null)
        {
            found.addAll(Arrays.asList(m_bc.getBundles()));
        }
        else
        {
            Bundle[] bundles = m_bc.getBundles();
            for (Bundle bundle : bundles) {
                String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
                if (matchBundleName(bundle.getSymbolicName(), pattern)
                        || matchBundleName(name, pattern)) {
                    found.add(bundle);
                }
            }
        }

        if (found.size() > 0)
        {
            try (Formatter f = new Formatter()) {
                printBundleList(found.toArray(new Bundle[found.size()]),
                    showLoc, showSymbolic, showUpdate, m_b0, f);
                return f.toString();
            }
        }
        else
        {
            return "No matching bundles found.";
        }
    }

    private boolean matchBundleName(String name, String pattern)
    {
        return (name != null) && name.toLowerCase().contains(pattern.toLowerCase());
    }

    @Descriptor("display all matching log entries")
    public String log(
        @Descriptor("minimum log level [ debug | info | warn | error ]") String logLevel)
    {
        return log(-1, logLevel);
    }

    @Descriptor("display some matching log entries")
    public String log(@Descriptor("maximum number of entries") int maxEntries,
        @Descriptor("minimum log level [ debug | info | warn | error ]") String logLevel)
    {
        // Keep track of service references.
        List<ServiceReference<?>> refs = new ArrayList<>();

        // Get start level service.
        try {
            LogReaderService lrs = Util.getService(m_bc, LogReaderService.class, refs);
            if (lrs == null)
            {
                return "Log reader service is unavailable.";
            }
            else
            {
                try (Formatter f = new Formatter()) {
                    @SuppressWarnings("unchecked")
                    Enumeration<LogEntry> entries = lrs.getLog();
                    List<LogEntry> select = new ArrayList<>();

                    int minLevel = logLevelAsInt(logLevel);

                    int index = 0;
                    while (entries.hasMoreElements() && (maxEntries < 0 || index < maxEntries))
                    {
                        LogEntry entry = entries.nextElement();
                        if (entry.getLevel() <= minLevel)
                        {
                            select.add(0, entry);
                            index++;
                        }
                    }

                    for (LogEntry e : select) {
                        display(e,f);
                    }
                    Util.ungetServices(m_bc, refs);
                    return f.toString();
                }
            }
        }
        catch (NoClassDefFoundError ncdfe) {
            return "Log reader service is unavailable.";
        }
    }

    private void display(LogEntry entry, Formatter f)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

        StringBuilder buffer = new StringBuilder();
        buffer.append(sdf.format(new Date(entry.getTime()))).append(" ");
        buffer.append(logLevelAsString(entry.getLevel())).append(" - ");
        buffer.append("Bundle: ").append(entry.getBundle().getSymbolicName());
        if (entry.getServiceReference() != null)
        {
            buffer.append(" - ");
            buffer.append(entry.getServiceReference().toString());
        }
        buffer.append(" - ").append(entry.getMessage());
        if (entry.getException() != null)
        {
            buffer.append(" - ");
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            entry.getException().printStackTrace(pw);
            buffer.append(writer.toString());
        }

        f.format("%s%n", buffer.toString());
    }

    private static int logLevelAsInt(String logLevel)
    {
        if ("error".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_ERROR;
        }
        else if ("warn".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_WARNING;
        }
        else if ("info".equalsIgnoreCase(logLevel))
        {
            return LogService.LOG_INFO;
        }
        return LogService.LOG_DEBUG;
    }

    private static String logLevelAsString(int level)
    {
        switch (level)
        {
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_WARNING:
                return "WARNING";
            case LogService.LOG_INFO:
                return "INFO";
            default:
                return "DEBUG";
        }
    }

    @Descriptor("refresh bundles")
    public void refresh(
        @Descriptor("target bundles (can be null or empty)") Bundle[] bundles)
    {
        if ((bundles != null) && (bundles.length == 0))
        {
            bundles = null;
        }

        m_b0.adapt(FrameworkWiring.class).refreshBundles(Arrays.asList(bundles));
    }

    @Descriptor("resolve bundles")
    public String resolve(
        @Descriptor("target bundles (can be null or empty)") Bundle[] bundles)
    {
        if (m_b0.adapt(FrameworkWiring.class).resolveBundles(bundles != null ? Arrays.asList(bundles) : null))
        {
            return "Not all bundles could be resolved.";
        }
        return null;
    }

    @Descriptor("start bundles")
    public String start(
        @Descriptor("start bundle transiently") @Parameter(names = { "-t", "--transient" }, presentValue = "true", absentValue = "false") boolean trans,
        @Descriptor("use declared activation policy") @Parameter(names = { "-p",
                "--policy" }, presentValue = "true", absentValue = "false") boolean policy,
        @Descriptor("target bundle identifiers or URLs") String[] ss)
    {
        if ((ss == null) || (ss.length < 1)) {
            return "Please specify the bundles to start.";
        }

        int options = 0;

        // Check for "transient" switch.
        if (trans)
        {
            options |= Bundle.START_TRANSIENT;
        }

        // Check for "start policy" switch.
        if (policy)
        {
            options |= Bundle.START_ACTIVATION_POLICY;
        }

        try (Formatter f = new Formatter()) {
            for (String s : ss)
            {
                String id = s.trim();

                try
                {
                    Bundle bundle = null;

                    // The id may be a number or a URL, so check.
                    if (Character.isDigit(id.charAt(0)))
                    {
                        long l = Long.parseLong(id);
                        bundle = m_bc.getBundle(l);
                    }
                    else
                    {
                        bundle = m_bc.installBundle(id);
                    }

                    if (bundle != null)
                    {
                        bundle.start(options);
                    }
                    else
                    {
                        f.format("Bundle ID '%s'  is invalid.%n", id);
                    }
                }
                catch (NumberFormatException ex)
                {
                    f.format("Unable to parse id '%s'.%n", id);
                }
                catch (BundleException ex)
                {
                    if (ex.getNestedException() != null)
                    {
                        f.format("%s%n", ex.getNestedException().toString());
                    }
                    else
                    {
                        f.format("%s%n", ex.toString());
                    }
                }
                catch (Exception ex)
                {
                    f.format("%s%n", ex.toString());
                }
            }
            return f.toString();
        }
    }

    @Descriptor("stop bundles")
    public String stop(@Descriptor("stop bundle transiently") @Parameter(names = { "-t",
            "--transient" }, presentValue = "true", absentValue = "false") boolean trans,
        @Descriptor("target bundles") Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            return "Please specify the bundles to stop.";
        }

        int options = 0;

        // Check for "transient" switch.
        if (trans)
        {
            options |= Bundle.STOP_TRANSIENT;
        }

        try (Formatter f = new Formatter()) {
            for (Bundle bundle : bundles)
            {
                try
                {
                    bundle.stop(options);
                }
                catch (BundleException ex)
                {
                    if (ex.getNestedException() != null)
                    {
                        f.format("%s%n", ex.getNestedException().toString());
                    }
                    else
                    {
                        f.format("%s%n", ex.toString());
                    }
                }
                catch (Exception ex)
                {
                    f.format("%s%n", ex.toString());
                }
            }
            return f.toString();
        }
    }

    @Descriptor("uninstall bundles")
    public String uninstall(@Descriptor("target bundles") Bundle[] bundles)
    {
        if ((bundles == null) || (bundles.length == 0))
        {
            return "Please specify the bundles to uninstall.";
        }

        try (Formatter f = new Formatter()) {
            for (Bundle bundle : bundles)
            {
                try
                {
                    bundle.uninstall();
                }
                catch (BundleException ex)
                {
                    if (ex.getNestedException() != null)
                    {
                        f.format("%s%n", ex.getNestedException().toString());
                    }
                    else {
                        f.format("%s%n", ex.toString());
                    }
                }
                catch (Exception ex)
                {
                    f.format("%s%n", ex.toString());
                }
            }
            return f.toString();
        }
    }

    @Descriptor("update bundle")
    public String update(@Descriptor("target bundle") Bundle bundle)
    {
        if (bundle == null)
        {
            return "Must specify a bundle.";
        }

        try
        {
            bundle.update();
            return null;
        }
        catch (BundleException ex)
        {
            if (ex.getNestedException() != null)
            {
                return ex.getNestedException().toString();
            }

            return ex.toString();
        }
        catch (Exception ex)
        {
            return ex.toString();
        }
    }

    @Descriptor("update bundle from URL")
    public String update(
            @Descriptor("command session") CommandSession session,
            @Descriptor("target bundle") Bundle bundle,
        @Descriptor("URL from where to retrieve bundle") String location) throws IOException {
        if (bundle == null)
        {
            return "Must specify a bundle.";
        }
        if (location == null)
        {
            return "Must specify a location.";
        }

        try
        {
            location = Util.resolveUri(session, location.trim());
            InputStream is = new URL(location).openStream();
            bundle.update(is);
            return null;
        }
        catch (MalformedURLException ex)
        {
            return "Unable to parse URL";
        }
        catch (IOException ex)
        {
            return "Unable to open input stream: " + ex;
        }
        catch (BundleException ex)
        {
            if (ex.getNestedException() != null)
            {
                return ex.getNestedException().toString();
            }

            return ex.toString();
        }
        catch (Exception ex)
        {
            return ex.toString();
        }
    }

    @Descriptor("determines from where a bundle loads a class")
    public String which(@Descriptor("target bundle") Bundle bundle,
        @Descriptor("target class name") String className)
    {
        if (bundle == null)
        {
            return "Please specify a bundle";
        }

        Class<?> clazz = null;
        try
        {
            clazz = bundle.loadClass(className);
            if (clazz.getClassLoader() == null)
            {
                return "Loaded from: boot class loader";
            }
            else if (clazz.getClassLoader() instanceof BundleReference)
            {
                Bundle p = ((BundleReference) clazz.getClassLoader()).getBundle();
                return "Loaded from: " + p;
            }
            else
            {
                return "Loaded from: " + clazz.getClassLoader();
            }
        }
        catch (ClassNotFoundException ex)
        {
            return "Class not found";
        }
    }

    private static void printBundleList(Bundle[] bundles,
        boolean showLoc, boolean showSymbolic, boolean showUpdate, Bundle b0, Formatter f)
    {
        f.format("START LEVEL %s%n", b0.adapt(FrameworkStartLevel.class).getStartLevel());

        // Determine last column.
        String lastColumn = "Name";
        if (showLoc)
        {
            lastColumn = "Location";
        }
        else if (showSymbolic)
        {
            lastColumn = "Symbolic name";
        }
        else if (showUpdate)
        {
            lastColumn = "Update location";
        }

        f.format("%5s|%-11s|%5s|%s%n", "ID", "State", "Level", lastColumn);
        for (Bundle bundle : bundles)
        {
            // Get the bundle name or location.
            String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
            // If there is no name, then default to symbolic name.
            name = (name == null) ? bundle.getSymbolicName() : name;
            // If there is no symbolic name, resort to location.
            name = (name == null) ? bundle.getLocation() : name;

            // Overwrite the default value is the user specifically
            // requested to display one or the other.
            if (showLoc)
            {
                name = bundle.getLocation();
            }
            else if (showSymbolic)
            {
                name = bundle.getSymbolicName();
                name = (name == null) ? "<no symbolic name>" : name;
            }
            else if (showUpdate)
            {
                name = bundle.getHeaders().get(Constants.BUNDLE_UPDATELOCATION);
                name = (name == null) ? bundle.getLocation() : name;
            }

            // Show bundle version if not showing location.
            name = (!showLoc && !showUpdate) ? name + " (" + bundle.getVersion() + ")"
                : name;

            // Get the bundle's start level.
            int level = bundle.adapt(BundleStartLevel.class).getStartLevel();

            f.format("%5d|%-11s|%5d|%s|%s%n",
                bundle.getBundleId(), getStateString(bundle), level, name,
                bundle.getVersion());
        }
    }

    private static String getStateString(Bundle bundle)
    {
        int state = bundle.getState();
        if (state == Bundle.ACTIVE)
        {
            return "Active     ";
        }
        else if (state == Bundle.INSTALLED)
        {
            return "Installed  ";
        }
        else if (state == Bundle.RESOLVED)
        {
            return "Resolved   ";
        }
        else if (state == Bundle.STARTING)
        {
            return "Starting   ";
        }
        else if (state == Bundle.STOPPING)
        {
            return "Stopping   ";
        }
        else
        {
            return "Unknown    ";
        }
    }
}
